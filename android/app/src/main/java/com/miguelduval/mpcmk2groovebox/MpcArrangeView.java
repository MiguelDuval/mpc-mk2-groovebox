package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * MPC-style linear arrangement surface.
 *
 * It is intentionally a presentation-only view. Arrangement editing commands
 * are not invented here; loop selection and track selection call back into the
 * existing semantic/native path owned by MainActivity.
 */
final class MpcArrangeView extends View {
    static final class Event {
        final long tick;
        final int durationTicks;

        Event(long tick, int durationTicks) {
            this.tick = Math.max(0L, tick);
            this.durationTicks = Math.max(1, durationTicks);
        }
    }

    static final class Lane {
        final int trackIndex;
        final String name;
        final String type;
        final String program;
        final boolean muted;
        final boolean armed;
        final int lengthTicks;
        final List<Event> events;

        Lane(
                int trackIndex,
                String name,
                String type,
                String program,
                boolean muted,
                boolean armed,
                int lengthTicks,
                List<Event> events) {
            this.trackIndex = trackIndex;
            this.name = name == null ? "" : name;
            this.type = type == null ? "" : type;
            this.program = program == null ? "" : program;
            this.muted = muted;
            this.armed = armed;
            this.lengthTicks = Math.max(1, lengthTicks);
            this.events = events == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(events));
        }
    }

    interface Listener {
        void onTrackSelected(int trackIndex);
        void onEventDoubleTapped(int trackIndex, long tick);
        void onLoopCommitted(int startBar, int endBar);
    }

    private static final int BG = 0xff101316;
    private static final int PANEL = 0xff191e22;
    private static final int GRID = 0xff343b42;
    private static final int TEXT = 0xffe9edf0;
    private static final int MUTED = 0xff9ca6ae;
    private static final int LOOP = 0xffd84a55;
    private static final int PLAYHEAD = 0xffffffff;
    private static final int EVENT = 0xff56646d;
    private static final int EVENT_SELECTED = 0xffffb448;
    private static final float LEFT_HEADER_DP = 138f;
    private static final float TOP_RULER_DP = 34f;
    private static final float LANE_DP = 46f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    private final ArrayList<Lane> lanes = new ArrayList<>();

    private Listener listener;
    private int selectedTrack = 0;
    private int barCount = 1;
    private int loopStartBar = 1;
    private int loopEndBar = 1;
    private long positionTicks;
    private int numerator = 4;
    private int denominator = 4;
    private boolean playing;
    private int visibleBars = 8;
    private int viewportStartBar = 1;
    private int loopHandle = -1;
    private float scaleBaseBars = 8f;

    MpcArrangeView(Context context) {
        super(context);
        scaleDetector = createScaleDetector(context);
        gestureDetector = createGestureDetector(context);
        init();
    }

    MpcArrangeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleDetector = createScaleDetector(context);
        gestureDetector = createGestureDetector(context);
        init();
    }

    MpcArrangeView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        scaleDetector = createScaleDetector(context);
        gestureDetector = createGestureDetector(context);
        init();
    }

    private void init() {
        setFocusable(true);
        setClickable(true);
        setContentDescription("MPC linear arrangement editor");
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setLanes(List<Lane> values, int selectedTrack) {
        lanes.clear();
        if (values != null) {
            lanes.addAll(values);
        }
        this.selectedTrack = Math.max(0, selectedTrack);
        invalidate();
    }

    void setTimeline(
            int bars,
            int startBar,
            int endBar,
            int numerator,
            int denominator,
            long positionTicks,
            boolean playing) {
        barCount = Math.max(1, bars);
        loopStartBar = clampBar(startBar);
        loopEndBar = clampBar(endBar);
        if (loopStartBar > loopEndBar) {
            loopStartBar = loopEndBar;
        }
        this.numerator = Math.max(1, numerator);
        this.denominator = Math.max(1, denominator);
        this.positionTicks = Math.max(0L, positionTicks);
        this.playing = playing;
        visibleBars = Math.max(
                1,
                Math.min(barCount, visibleBars));
        viewportStartBar = clampViewportStart(viewportStartBar);
        ensurePlayheadVisible();
        invalidate();
    }

    void zoomIn() {
        final int next = MpcSequenceZoomPolicy.zoomTimelineBarsIn(
                visibleBars, barCount);
        if (next == visibleBars) {
            return;
        }
        zoomAroundPlayhead(next);
    }

    void zoomOut() {
        final int next = MpcSequenceZoomPolicy.zoomTimelineBarsOut(
                visibleBars, barCount);
        if (next == visibleBars) {
            return;
        }
        zoomAroundPlayhead(next);
    }

    void resetZoom() {
        visibleBars = Math.min(8, barCount);
        viewportStartBar = 1;
        ensurePlayheadVisible();
        invalidate();
    }

    private ScaleGestureDetector createScaleDetector(Context context) {
        return new ScaleGestureDetector(
                context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScaleBegin(ScaleGestureDetector detector) {
                        scaleBaseBars = visibleBars;
                        return true;
                    }

                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        final float factor = detector.getScaleFactor();
                        final int next = clamp(
                                Math.round(scaleBaseBars / Math.max(0.25f, factor)),
                                1,
                                barCount);
                        zoomAroundPlayhead(next);
                        return true;
                    }
                });
    }

    private GestureDetector createGestureDetector(Context context) {
        return new GestureDetector(
                context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent event) {
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent event) {
                        final Hit hit = hit(event.getX(), event.getY());
                        if (hit.trackIndex >= 0
                                && hit.eventIndex >= 0
                                && listener != null) {
                            listener.onEventDoubleTapped(
                                    hit.trackIndex,
                                    hit.eventTick);
                        }
                        return true;
                    }
                });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(BG);

        final float density = getResources().getDisplayMetrics().density;
        final float headerWidth = LEFT_HEADER_DP * density;
        final float rulerHeight = TOP_RULER_DP * density;
        final float laneHeight = LANE_DP * density;
        final float timelineWidth = Math.max(
                1f, getWidth() - headerWidth);
        final float pxPerBar = timelineWidth / Math.max(1, visibleBars);

        drawRuler(canvas, headerWidth, rulerHeight, timelineWidth, pxPerBar);
        drawLanes(canvas, headerWidth, rulerHeight, laneHeight, timelineWidth, pxPerBar);
        drawLoopBrace(canvas, headerWidth, rulerHeight, timelineWidth, pxPerBar);
        drawPlayhead(canvas, headerWidth, rulerHeight, timelineWidth, pxPerBar);
    }

    private void drawRuler(
            Canvas canvas,
            float headerWidth,
            float rulerHeight,
            float timelineWidth,
            float pxPerBar) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PANEL);
        canvas.drawRect(0f, 0f, getWidth(), rulerHeight, paint);

        paint.setColor(GRID);
        paint.setStrokeWidth(dp(1));
        for (int visible = 0; visible <= visibleBars; visible++) {
            final float x = headerWidth + visible * pxPerBar;
            canvas.drawLine(x, rulerHeight, x, getHeight(), paint);
        }

        drawText(canvas, "ARRANGE", dp(8), dp(13), TEXT, 11, true);

        for (int visible = 0; visible < visibleBars; visible++) {
            final int bar = viewportStartBar + visible;
            drawText(
                    canvas,
                    String.format(Locale.ROOT, "%02d", bar),
                    headerWidth + visible * pxPerBar + dp(4),
                    dp(24),
                    MUTED,
                    9,
                    true);
        }
    }

    private void drawLanes(
            Canvas canvas,
            float headerWidth,
            float rulerHeight,
            float laneHeight,
            float timelineWidth,
            float pxPerBar) {
        final long ticksPerBar = ticksPerBar();

        for (int i = 0; i < lanes.size(); i++) {
            final Lane lane = lanes.get(i);
            final float top = rulerHeight + i * laneHeight;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(i == selectedTrack
                    ? 0xff26343a
                    : PANEL);
            canvas.drawRect(0f, top, getWidth(), top + laneHeight - dp(1), paint);

            paint.setColor(GRID);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            canvas.drawRect(
                    0f, top, getWidth(), top + laneHeight, paint);

            drawText(
                    canvas,
                    String.format(
                            Locale.ROOT,
                            "%02d  %s",
                            lane.trackIndex + 1,
                            clip(lane.name, 15)),
                    dp(8),
                    top + dp(17),
                    i == selectedTrack ? TEXT : MUTED,
                    9,
                    true);

            drawText(
                    canvas,
                    lane.type,
                    dp(8),
                    top + dp(33),
                    MUTED,
                    7,
                    false);

            drawText(
                    canvas,
                    clip(lane.program, 15),
                    dp(68),
                    top + dp(33),
                    MUTED,
                    7,
                    false);

            if (lane.muted) {
                drawText(canvas, "M", headerWidth - dp(30),
                        top + dp(28), LOOP, 8, true);
            }
            if (lane.armed) {
                drawText(canvas, "R", headerWidth - dp(17),
                        top + dp(28), LOOP, 8, true);
            }

            // Current domain has one pattern per track with no placement
            // position yet. Render it as the truthful full-length clip region.
            final float clipStart = xForTick(
                    0L, headerWidth, timelineWidth, pxPerBar, 0L);
            final float clipEnd = xForTick(
                    lane.lengthTicks,
                    headerWidth, timelineWidth, pxPerBar, 0L);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(i == selectedTrack
                    ? 0xff34474f
                    : EVENT);
            rect.set(
                    clipStart,
                    top + dp(5),
                    Math.max(clipStart + dp(4), clipEnd),
                    top + laneHeight - dp(5));
            canvas.drawRoundRect(rect, dp(4), dp(4), paint);

            // Compact note activity inside the clip keeps the arrangement
            // visually meaningful while the domain remains pattern-based.
            paint.setColor(i == selectedTrack
                    ? EVENT_SELECTED
                    : 0xff94a2aa);
            final int maxEvents = Math.min(96, lane.events.size());
            for (int eventIndex = 0; eventIndex < maxEvents; eventIndex++) {
                Event event = lane.events.get(eventIndex);
                final float ex = xForTick(
                        event.tick,
                        headerWidth,
                        timelineWidth,
                        pxPerBar,
                        0L);
                final float ew = Math.max(
                        dp(1),
                        xForTick(
                                event.tick + event.durationTicks,
                                headerWidth,
                                timelineWidth,
                                pxPerBar,
                                0L) - ex);
                rect.set(
                        ex,
                        top + dp(15),
                        Math.min(
                                getWidth() - dp(1),
                                ex + Math.max(dp(2), ew)),
                        top + dp(28));
                canvas.drawRect(rect, paint);
            }

            paint.setColor(
                    i == selectedTrack ? LOOP : GRID);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(
                    headerWidth,
                    top,
                    getWidth(),
                    top,
                    paint);
        }

        // Bottom boundary/header area remains visually consistent when no lanes exist.
        if (lanes.isEmpty()) {
            drawText(canvas, "NO TRACKS", dp(8),
                    rulerHeight + dp(26), MUTED, 10, true);
        }

        // Silence unused variable warning in Java source by using it in a
        // cheap, deterministic vertical reference calculation.
        if (ticksPerBar <= 0) {
            return;
        }
    }

    private void drawLoopBrace(
            Canvas canvas,
            float headerWidth,
            float rulerHeight,
            float timelineWidth,
            float pxPerBar) {
        final float left = xForBar(
                loopStartBar, headerWidth, timelineWidth, pxPerBar);
        final float right = xForBar(
                loopEndBar + 0.999f, headerWidth, timelineWidth, pxPerBar);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(LOOP);

        canvas.drawLine(left, dp(2), right, dp(2), paint);
        canvas.drawLine(left, dp(2), left, dp(10), paint);
        canvas.drawLine(right, dp(2), right, dp(10), paint);

        drawText(canvas, "LOOP",
                left + dp(4), dp(11), LOOP, 6, true);
    }

    private void drawPlayhead(
            Canvas canvas,
            float headerWidth,
            float rulerHeight,
            float timelineWidth,
            float pxPerBar) {
        final double ticksPerBar = ticksPerBar();
        final float bar = 1f + (float) (
                positionTicks / Math.max(1.0, ticksPerBar));
        if (bar < viewportStartBar - 1
                || bar > viewportStartBar + visibleBars + 1) {
            return;
        }

        final float x = xForBar(
                bar, headerWidth, timelineWidth, pxPerBar);
        paint.setColor(PLAYHEAD);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(playing ? dp(2) : dp(1));
        canvas.drawLine(
                x, rulerHeight, x, getHeight(), paint);
    }

    static List<Event> decodeEventData(String data) {
        if (data == null || data.trim().isEmpty()) {
            return Collections.emptyList();
        }
        final ArrayList<Event> result = new ArrayList<>();
        final String[] events = data.split(";");
        for (String eventData : events) {
            final String[] fields = eventData.split(",");
            if (fields.length < 2) {
                continue;
            }
            try {
                final long tick = Long.parseLong(fields[0]);
                final int duration = Integer.parseInt(fields[1]);
                result.add(new Event(tick, duration));
            } catch (NumberFormatException ignored) {
                // Malformed UI projection entries are ignored rather than
                // becoming a UI crash or feeding invalid data back to native.
            }
        }
        return result;
    }

    private Hit hit(float x, float y) {
        final float headerWidth = dp(LEFT_HEADER_DP);
        final float rulerHeight = dp(TOP_RULER_DP);
        if (y < rulerHeight) {
            return Hit.NONE;
        }

        final int laneIndex = (int) (
                (y - rulerHeight) / Math.max(1f, dp(LANE_DP)));
        if (laneIndex < 0 || laneIndex >= lanes.size()) {
            return Hit.NONE;
        }

        final Lane lane = lanes.get(laneIndex);
        final float timelineWidth = Math.max(1f, getWidth() - headerWidth);
        final float pxPerBar = timelineWidth / Math.max(1, visibleBars);
        final float relativeBars = (x - headerWidth) / Math.max(1f, pxPerBar);
        final long ticksPerBar = ticksPerBar();
        final long tick = Math.max(
                0L,
                Math.round(
                        (viewportStartBar - 1 + relativeBars)
                                * ticksPerBar));

        int eventIndex = -1;
        long eventTick = tick;
        for (int i = 0; i < lane.events.size(); i++) {
            Event event = lane.events.get(i);
            if (tick >= event.tick
                    && tick <= event.tick + event.durationTicks) {
                eventIndex = i;
                eventTick = event.tick;
                break;
            }
        }
        return new Hit(lane.trackIndex, eventIndex, eventTick);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            final float headerWidth = dp(LEFT_HEADER_DP);
            final float rulerHeight = dp(TOP_RULER_DP);

            if (event.getY() < rulerHeight) {
                final float timelineWidth =
                        Math.max(1f, getWidth() - headerWidth);
                final float pxPerBar =
                        timelineWidth / Math.max(1, visibleBars);
                final float startX = xForBar(
                        loopStartBar, headerWidth,
                        timelineWidth, pxPerBar);
                final float endX = xForBar(
                        loopEndBar + 0.999f, headerWidth,
                        timelineWidth, pxPerBar);
                if (Math.abs(event.getX() - startX) <= dp(22)) {
                    loopHandle = 0;
                } else if (Math.abs(event.getX() - endX) <= dp(22)) {
                    loopHandle = 1;
                } else {
                    loopHandle = -1;
                }
            } else {
                loopHandle = -1;
                final Hit hit = hit(event.getX(), event.getY());
                if (hit.trackIndex >= 0
                        && listener != null) {
                    selectedTrack = hit.trackIndex;
                    listener.onTrackSelected(hit.trackIndex);
                    invalidate();
                }
            }
            return true;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_MOVE
                && loopHandle >= 0
                && !scaleDetector.isInProgress()) {
            updateLoopHandle(event.getX());
            return true;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (loopHandle >= 0) {
                updateLoopHandle(event.getX());
                if (listener != null) {
                    listener.onLoopCommitted(loopStartBar, loopEndBar);
                }
                loopHandle = -1;
            }
            return true;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            loopHandle = -1;
        }

        return true;
    }

    private void updateLoopHandle(float x) {
        final float headerWidth = dp(LEFT_HEADER_DP);
        final float timelineWidth =
                Math.max(1f, getWidth() - headerWidth);
        final float pxPerBar =
                timelineWidth / Math.max(1, visibleBars);
        final int bar = clampBar(Math.max(
                1,
                viewportStartBar
                        + (int) Math.floor(
                                Math.max(0f, Math.min(
                                        0.99999f,
                                        (x - headerWidth)
                                                / Math.max(1f, timelineWidth)))
                                        * visibleBars)));

        if (loopHandle == 0) {
            loopStartBar = Math.min(bar, loopEndBar);
        } else {
            loopEndBar = Math.max(bar, loopStartBar);
        }
        invalidate();
    }

    private void zoomAroundPlayhead(int nextVisibleBars) {
        visibleBars = Math.max(
                1,
                Math.min(barCount, nextVisibleBars));
        final int centerBar = clampBar(
                Math.round(
                        1f + positionTicks
                                / Math.max(1.0, ticksPerBar())));
        final int desiredStart =
                centerBar - (visibleBars - 1) / 2;
        viewportStartBar = clampViewportStart(desiredStart);
        invalidate();
    }

    private void ensurePlayheadVisible() {
        final double ticksPerBar = ticksPerBar();
        final int currentBar = clampBar(
                (int) Math.floor(
                        positionTicks / Math.max(1.0, ticksPerBar)) + 1);
        if (currentBar < viewportStartBar) {
            viewportStartBar = clampViewportStart(currentBar);
        } else if (currentBar >= viewportStartBar + visibleBars) {
            viewportStartBar = clampViewportStart(
                    currentBar - visibleBars + 1);
        }
    }

    private float xForBar(
            float bar,
            float headerWidth,
            float timelineWidth,
            float pxPerBar) {
        return headerWidth
                + (bar - viewportStartBar) * pxPerBar;
    }

    private float xForTick(
            long tick,
            float headerWidth,
            float timelineWidth,
            float pxPerBar,
            long ignored) {
        final double bars = tick / Math.max(1.0, ticksPerBar());
        return headerWidth
                + (float) (bars - (viewportStartBar - 1))
                * pxPerBar;
    }

    private long ticksPerBar() {
        final long ticksPerBeat = Math.max(
                1L,
                Math.round(
                        960.0 * 4.0 / Math.max(1, denominator)));
        return Math.max(
                ticksPerBeat,
                ticksPerBeat * Math.max(1, numerator));
    }

    private int clampBar(int value) {
        return Math.max(1, Math.min(barCount, value));
    }

    private int clampViewportStart(int start) {
        return Math.max(
                1,
                Math.min(
                        Math.max(1, barCount - visibleBars + 1),
                        start));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private void drawText(
            Canvas canvas,
            String value,
            float x,
            float baseline,
            int color,
            float size,
            boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(dp(size));
        paint.setTypeface(
                bold
                        ? android.graphics.Typeface.DEFAULT_BOLD
                        : android.graphics.Typeface.DEFAULT);
        canvas.drawText(value, x, baseline, paint);
    }

    private String clip(String value, int maxChars) {
        if (value == null) return "";
        final String normalized = value.replace("\n", " ").trim();
        if (normalized.length() <= maxChars) return normalized;
        return normalized.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    private static final class Hit {
        static final Hit NONE = new Hit(-1, -1, -1L);
        final int trackIndex;
        final int eventIndex;
        final long eventTick;

        Hit(int trackIndex, int eventIndex, long eventTick) {
            this.trackIndex = trackIndex;
            this.eventIndex = eventIndex;
            this.eventTick = eventTick;
        }
    }
}
