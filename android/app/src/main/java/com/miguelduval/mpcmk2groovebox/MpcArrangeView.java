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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
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
        final int noteNumber;
        Event(long tick, int durationTicks) { this(tick, durationTicks, -1); }
        Event(long tick, int durationTicks, int noteNumber) {
            this.tick = Math.max(0L, tick);
            this.durationTicks = Math.max(1, durationTicks);
            this.noteNumber = noteNumber >= 0 && noteNumber <= 127 ? noteNumber : -1;
        }
    }

    static final class Placement {
        final String id;
        final String patternId;
        final long startTick;
        final int lengthTicks;
        final int sourceOffsetTicks;

        Placement(String id, String patternId, long startTick,
                int lengthTicks, int sourceOffsetTicks) {
            this.id = id == null ? "" : id;
            this.patternId = patternId == null ? "" : patternId;
            this.startTick = Math.max(0L, startTick);
            this.lengthTicks = Math.max(1, lengthTicks);
            this.sourceOffsetTicks = Math.max(0, sourceOffsetTicks);
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
        final List<Placement> placements;

        Lane(
                int trackIndex,
                String name,
                String type,
                String program,
                boolean muted,
                boolean armed,
                int lengthTicks,
                List<Event> events) {
            this(trackIndex, name, type, program, muted, armed,
                    lengthTicks, events, Collections.emptyList());
        }

        Lane(
                int trackIndex,
                String name,
                String type,
                String program,
                boolean muted,
                boolean armed,
                int lengthTicks,
                List<Event> events,
                List<Placement> placements) {
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
            this.placements = placements == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(placements));
        }
    }

    interface Listener {
        void onTrackSelected(int trackIndex);
        void onEventSelected(int trackIndex, long tick, int noteNumber);
        void onEventSelectionCleared();
        void onInsertionTargetSelected(int trackIndex, long tick);
        void onEventDoubleTapped(int trackIndex, long tick);
        void onLoopCommitted(int startBar, int endBar);
        void onViewportChanged(int previousStartBar, int currentStartBar);
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
    private static final int INSERTION_TARGET = 0xff4dd0e1;
    private static final float LEFT_HEADER_DP = 138f;
    private static final float TOP_RULER_DP = 34f;
    private static final float LANE_DP = 46f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    private final ArrayList<Lane> lanes = new ArrayList<>();

    private Listener listener;
    private boolean eventDoubleTapDispatched;
    private int selectedTrack = 0;
    private int insertionTargetTrackIndex = -1;
    private long insertionTargetTick = -1L;
    private long quantizeGridTicks = 240L;
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
    private boolean timelinePanActive;
    private boolean timelinePanMoved;
    private float timelinePanStartX;
    private int timelinePanStartViewportStartBar = 1;
    private float scaleBaseBars = 8f;
    private final Deque<ZoomTransition> zoomHistory = new ArrayDeque<>();

    private static final class ZoomSnapshot {
        final int visibleBars;
        final int viewportStartBar;

        ZoomSnapshot(int visibleBars, int viewportStartBar) {
            this.visibleBars = visibleBars;
            this.viewportStartBar = viewportStartBar;
        }
    }

    private static final class ZoomTransition {
        final boolean zoomIn;
        final ZoomSnapshot before;
        final ZoomSnapshot after;

        ZoomTransition(boolean zoomIn, ZoomSnapshot before, ZoomSnapshot after) {
            this.zoomIn = zoomIn;
            this.before = before;
            this.after = after;
        }
    }

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
        if (!hasInsertionTarget(insertionTargetTrackIndex, insertionTargetTick)) {
            insertionTargetTrackIndex = -1;
            insertionTargetTick = -1L;
        }
        invalidate();
    }

    void setQuantizeGridTicks(long ticks) {
        quantizeGridTicks = Math.max(1L, ticks);
    }

    void setInsertionTarget(int trackIndex, long tick) {
        if (!hasInsertionTarget(trackIndex, tick)) {
            clearInsertionTarget();
            return;
        }
        insertionTargetTrackIndex = trackIndex;
        insertionTargetTick = tick;
        invalidate();
    }

    void clearInsertionTarget() {
        insertionTargetTrackIndex = -1;
        insertionTargetTick = -1L;
        invalidate();
    }

    private boolean hasInsertionTarget(int trackIndex, long tick) {
        if (trackIndex < 0 || tick < 0L) return false;
        for (Lane lane : lanes) {
            if (lane.trackIndex == trackIndex) {
                return tick < lane.lengthTicks && tick % Math.max(1L, quantizeGridTicks) == 0L;
            }
        }
        return false;
    }

    float[] trackLanePointForAudit(int trackIndex) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return null;
        }
        final float headerX = Math.min(
                dp(24f), Math.max(0f, getWidth() - dp(1f)));
        final float rulerHeight = dp(TOP_RULER_DP);
        final float laneHeight = dp(LANE_DP);
        for (int laneIndex = 0; laneIndex < lanes.size(); laneIndex++) {
            if (lanes.get(laneIndex).trackIndex != trackIndex) {
                continue;
            }
            final float y = rulerHeight + laneIndex * laneHeight + laneHeight / 2f;
            if (y < rulerHeight || y >= getHeight()) {
                return null;
            }
            final Hit actual = hit(headerX, y);
            if (actual.trackIndex != trackIndex) {
                return null;
            }
            return new float[] {headerX, y};
        }
        return null;
    }

    int loopStartBarForAudit() {
        return loopStartBar;
    }

    int loopEndBarForAudit() {
        return loopEndBar;
    }

    int totalBarsForAudit() {
        return barCount;
    }

    int visibleBarsForAudit() {
        return visibleBars;
    }

    int viewportStartBarForAudit() {
        return viewportStartBar;
    }

    int selectedTrackForAudit() {
        return selectedTrack;
    }

    float[] loopEndHandlePointForAudit(int bar) {
        if (getWidth() <= 0 || getHeight() <= 0
                || bar < 1 || bar > barCount) {
            return null;
        }
        final float headerWidth = dp(LEFT_HEADER_DP);
        final float timelineWidth = Math.max(1f, getWidth() - headerWidth);
        final float pxPerBar = timelineWidth / Math.max(1, visibleBars);
        final float x = xForBar(
                bar + 0.999f, headerWidth, timelineWidth, pxPerBar);
        final float y = Math.max(1f, dp(4));
        if (x < headerWidth || x >= getWidth() || y >= dp(TOP_RULER_DP)) {
            return null;
        }
        return new float[] {x, y};
    }

    float[] eventCenterForAudit(int trackIndex, long eventTick) {
        if (getWidth() <= 0 || getHeight() <= 0) return null;

        int laneIndex = -1;
        Event targetEvent = null;
        for (int i = 0; i < lanes.size(); i++) {
            final Lane lane = lanes.get(i);
            if (lane.trackIndex != trackIndex) continue;
            laneIndex = i;
            for (Event event : lane.events) {
                if (event.tick == eventTick) {
                    targetEvent = event;
                    break;
                }
            }
            break;
        }
        if (laneIndex < 0 || targetEvent == null) return null;

        final float headerWidth = dp(LEFT_HEADER_DP);
        final float rulerHeight = dp(TOP_RULER_DP);
        final float laneHeight = dp(LANE_DP);
        final float timelineWidth = Math.max(1f, getWidth() - headerWidth);
        final float pxPerBar = timelineWidth / Math.max(1, visibleBars);
        final float startX = xForTick(
                targetEvent.tick, headerWidth, timelineWidth, pxPerBar, 0L);
        final float endX = xForTick(
                targetEvent.tick + targetEvent.durationTicks,
                headerWidth, timelineWidth, pxPerBar, 0L);
        final float left = Math.max(headerWidth + dp(1), startX);
        final float right = Math.min(
                getWidth() - dp(1),
                Math.max(startX + dp(2), endX));
        if (right <= left) return null;

        final float x = (left + right) / 2f;
        final float y = rulerHeight + laneIndex * laneHeight + dp(21.5f);
        final Hit actual = hit(x, y);
        if (actual.trackIndex != trackIndex
                || actual.eventIndex < 0
                || actual.eventTick != eventTick) {
            return null;
        }
        return new float[] {x, y};
    }

    void setTimeline(
            int bars,
            int startBar,
            int endBar,
            int numerator,
            int denominator,
            long positionTicks,
            boolean playing) {
        final int previousBarCount = barCount;
        final int previousVisibleBars = visibleBars;
        final int previousViewportStartBar = viewportStartBar;
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
        // Invalidate inverse-zoom history only when the actual timeline geometry
        // changes externally (sequence length, visible range or viewport start).
        // A normal refresh after our own zoom click leaves this tuple unchanged.
        if (previousBarCount != barCount
                || previousVisibleBars != visibleBars
                || previousViewportStartBar != viewportStartBar) {
            zoomHistory.clear();
        }
        invalidate();
    }

    void zoomIn() {
        if (restorePreviousZoomWindow(true)) {
            return;
        }
        final ZoomSnapshot before = snapshotZoomWindow();
        final int next = MpcSequenceZoomPolicy.zoomTimelineBarsIn(
                visibleBars, barCount);
        if (next == visibleBars) {
            return;
        }
        zoomAroundPlayhead(next);
        zoomHistory.push(new ZoomTransition(true, before, snapshotZoomWindow()));
    }

    void zoomOut() {
        if (restorePreviousZoomWindow(false)) {
            return;
        }
        final ZoomSnapshot before = snapshotZoomWindow();
        final int next = MpcSequenceZoomPolicy.zoomTimelineBarsOut(
                visibleBars, barCount);
        if (next == visibleBars) {
            return;
        }
        zoomAroundPlayhead(next);
        zoomHistory.push(new ZoomTransition(false, before, snapshotZoomWindow()));
    }

    void resetZoom() {
        zoomHistory.clear();
        visibleBars = Math.min(8, barCount);
        viewportStartBar = 1;
        ensurePlayheadVisible();
        invalidate();
    }

    private ZoomSnapshot snapshotZoomWindow() {
        return new ZoomSnapshot(visibleBars, viewportStartBar);
    }

    /**
     * Reverses the last opposite zoom action exactly when the current viewport
     * still matches that transition. This keeps ZOOM + / ZOOM − symmetric even
     * when zooming centers on a playhead away from bar one.
     */
    private boolean restorePreviousZoomWindow(boolean requestedZoomIn) {
        final ZoomTransition last = zoomHistory.peek();
        if (last == null) {
            return false;
        }
        if (last.after.visibleBars != visibleBars
                || last.after.viewportStartBar != viewportStartBar) {
            zoomHistory.clear();
            return false;
        }
        if (last.zoomIn == requestedZoomIn) {
            return false;
        }
        zoomHistory.pop();
        visibleBars = Math.max(1, Math.min(barCount, last.before.visibleBars));
        viewportStartBar = clampViewportStart(last.before.viewportStartBar);
        invalidate();
        return true;
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
                        // A pinch is a fresh viewport gesture, not the inverse
                        // of the previous discrete ZOOM + / ZOOM − button press.
                        zoomHistory.clear();
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
                            eventDoubleTapDispatched = true;
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
        drawInsertionTarget(canvas, headerWidth, rulerHeight, laneHeight, timelineWidth, pxPerBar);
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

            // Clip rectangles come from the authoritative PatternPlacement list;
            // note content is mapped through each placement's source offset.
            final List<Placement> placements = lane.placements.isEmpty()
                    ? Collections.singletonList(new Placement(
                            lane.trackIndex + "-legacy", "", 0L, lane.lengthTicks, 0))
                    : lane.placements;
            paint.setStyle(Paint.Style.FILL);
            for (Placement placement : placements) {
                final long placementEnd = placement.startTick + placement.lengthTicks;
                final float clipStart = xForTick(
                        placement.startTick, headerWidth, timelineWidth, pxPerBar, 0L);
                final float clipEnd = xForTick(
                        placementEnd, headerWidth, timelineWidth, pxPerBar, 0L);
                if (clipEnd <= headerWidth || clipStart >= getWidth()) continue;
                paint.setColor(i == selectedTrack ? 0xff34474f : EVENT);
                rect.set(
                        Math.max(headerWidth, clipStart),
                        top + dp(5),
                        Math.max(Math.max(headerWidth, clipStart) + dp(4), clipEnd),
                        top + laneHeight - dp(5));
                canvas.drawRoundRect(rect, dp(4), dp(4), paint);

                paint.setColor(i == selectedTrack ? EVENT_SELECTED : 0xff94a2aa);
                final long sourceStart = placement.sourceOffsetTicks;
                final long sourceEnd = sourceStart + placement.lengthTicks;
                for (int eventIndex = 0;
                     eventIndex < lane.events.size() && eventIndex < 96;
                     eventIndex++) {
                    Event event = lane.events.get(eventIndex);
                    final long eventSourceEnd = event.tick + event.durationTicks;
                    if (event.tick >= sourceEnd || eventSourceEnd <= sourceStart) continue;
                    final long visibleSourceStart = Math.max(event.tick, sourceStart);
                    final long visibleSourceEnd = Math.min(eventSourceEnd, sourceEnd);
                    if (visibleSourceEnd <= visibleSourceStart) continue;
                    final long eventTimelineStart = placement.startTick
                            + visibleSourceStart - sourceStart;
                    final long eventTimelineEnd = placement.startTick
                            + visibleSourceEnd - sourceStart;
                    final float ex = xForTick(
                            eventTimelineStart, headerWidth, timelineWidth, pxPerBar, 0L);
                    final float endX = xForTick(
                            eventTimelineEnd, headerWidth, timelineWidth, pxPerBar, 0L);
                    final float ew = Math.max(dp(1), endX - ex);
                    rect.set(
                            Math.max(headerWidth, ex),
                            top + dp(15),
                            Math.min(getWidth() - dp(1),
                                    Math.max(Math.max(headerWidth, ex) + dp(2), ex + ew)),
                            top + dp(28));
                    canvas.drawRect(rect, paint);
                }
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

    private void drawInsertionTarget(
            Canvas canvas,
            float headerWidth,
            float rulerHeight,
            float laneHeight,
            float timelineWidth,
            float pxPerBar) {
        if (!hasInsertionTarget(insertionTargetTrackIndex, insertionTargetTick)) return;
        int laneIndex = -1;
        for (int i = 0; i < lanes.size(); i++) {
            if (lanes.get(i).trackIndex == insertionTargetTrackIndex) {
                laneIndex = i;
                break;
            }
        }
        if (laneIndex < 0) return;
        final float x = xForTick(
                insertionTargetTick, headerWidth, timelineWidth, pxPerBar, 0L);
        if (x < headerWidth || x >= getWidth()) return;
        final float top = rulerHeight + laneIndex * laneHeight;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(INSERTION_TARGET);
        canvas.drawLine(x, top + dp(3), x, top + laneHeight - dp(3), paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, top + dp(7), dp(4), paint);
        canvas.drawCircle(x, top + laneHeight - dp(7), dp(2), paint);
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

    static int decodePlacementTimelineLength(String data) {
        if (data == null || data.trim().isEmpty()) return 1;
        final int separator = data.indexOf('|');
        final String lengthText = separator < 0 ? data : data.substring(0, separator);
        try {
            return Math.max(1, Integer.parseInt(lengthText));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    static List<Placement> decodePlacementData(String data) {
        if (data == null || data.trim().isEmpty()) return Collections.emptyList();
        final int separator = data.indexOf('|');
        if (separator < 0 || separator + 1 >= data.length()) {
            return Collections.emptyList();
        }
        final ArrayList<Placement> result = new ArrayList<>();
        for (String row : data.substring(separator + 1).split(";")) {
            final String[] fields = row.split(",", -1);
            if (fields.length != 5) continue;
            try {
                result.add(new Placement(
                        fields[0], fields[1],
                        Long.parseLong(fields[2]),
                        Integer.parseInt(fields[3]),
                        Integer.parseInt(fields[4])));
            } catch (NumberFormatException ignored) {
                // Ignore malformed projection rows; native validation remains authoritative.
            }
        }
        return result;
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
                final int noteNumber = fields.length >= 3
                        ? Integer.parseInt(fields[2]) : -1;
                result.add(new Event(tick, duration, noteNumber));
            } catch (NumberFormatException ignored) {
                // Malformed UI projection entries are ignored rather than
                // becoming a UI crash or feeding invalid data back to native.
            }
        }
        return result;
    }

    private Lane laneForTrack(int trackIndex) {
        for (Lane lane : lanes) {
            if (lane.trackIndex == trackIndex) return lane;
        }
        return null;
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
        Placement hitPlacement = null;
        for (Placement placement : lane.placements) {
            if (tick >= placement.startTick
                    && tick < placement.startTick + placement.lengthTicks) {
                hitPlacement = placement;
                break;
            }
        }
        final boolean hasTimelineSource = hitPlacement != null
                || lane.placements.isEmpty();
        final long sourceTick = hitPlacement == null
                ? tick
                : hitPlacement.sourceOffsetTicks + tick - hitPlacement.startTick;
        if (hasTimelineSource) {
            for (int i = 0; i < lane.events.size(); i++) {
                Event event = lane.events.get(i);
                if (sourceTick >= event.tick
                        && sourceTick <= event.tick + event.durationTicks) {
                    eventIndex = i;
                    eventTick = event.tick;
                    break;
                }
            }
        }
        return new Hit(lane.trackIndex, eventIndex, eventTick);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        // The second ACTION_DOWN triggers GestureDetector.onDoubleTap.
        // That callback may navigate away immediately, so do not also treat
        // the same gesture as a fresh lane-selection tap.
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                && eventDoubleTapDispatched) {
            eventDoubleTapDispatched = false;
            return true;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            final float headerWidth = dp(LEFT_HEADER_DP);
            final float rulerHeight = dp(TOP_RULER_DP);

            timelinePanActive = false;
            timelinePanMoved = false;
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
                    // A drag on the blank ruler pans the viewport. Loop handles
                    // keep their existing higher-priority drag behavior; pad/clip
                    // selection remains unchanged because this branch is ruler-only.
                    if (event.getX() >= headerWidth) {
                        timelinePanActive = true;
                        timelinePanStartX = event.getX();
                        timelinePanStartViewportStartBar = viewportStartBar;
                    }
                }
            } else {
                loopHandle = -1;
                final Hit hit = hit(event.getX(), event.getY());
                if (hit.trackIndex >= 0 && listener != null) {
                    Event hitEvent = null;
                    if (hit.eventIndex >= 0) {
                        for (Lane lane : lanes) {
                            if (lane.trackIndex == hit.trackIndex
                                    && hit.eventIndex < lane.events.size()) {
                                hitEvent = lane.events.get(hit.eventIndex);
                                break;
                            }
                        }
                    }
                    selectedTrack = hit.trackIndex;
                    listener.onTrackSelected(hit.trackIndex);
                    if (hitEvent != null && hitEvent.noteNumber >= 0) {
                        clearInsertionTarget();
                        listener.onEventSelected(
                                hit.trackIndex, hitEvent.tick, hitEvent.noteNumber);
                    } else if (event.getX() >= headerWidth) {
                        final Lane hitLane = laneForTrack(hit.trackIndex);
                        listener.onEventSelectionCleared();
                        if (hitLane != null) {
                            final float timelineWidth = Math.max(1f, getWidth() - headerWidth);
                            final float pxPerBar = timelineWidth / Math.max(1, visibleBars);
                            final long rawTick = Math.max(0L, Math.round(
                                    (viewportStartBar - 1
                                            + (event.getX() - headerWidth)
                                                    / Math.max(1f, pxPerBar))
                                            * ticksPerBar()));
                            final long grid = Math.max(1L, quantizeGridTicks);
                            final long nearestGrid = Math.round(rawTick / (double) grid) * grid;
                            final long lastValidGrid = ((hitLane.lengthTicks - 1L) / grid) * grid;
                            final long targetTick = Math.max(
                                    0L, Math.min(nearestGrid, lastValidGrid));
                            listener.onInsertionTargetSelected(hit.trackIndex, targetTick);
                        }
                    } else {
                        clearInsertionTarget();
                        listener.onEventSelectionCleared();
                    }
                    invalidate();
                }
            }
            return true;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_MOVE
                && timelinePanActive
                && loopHandle < 0
                && !scaleDetector.isInProgress()) {
            final float deltaX = timelinePanStartX - event.getX();
            if (!timelinePanMoved && Math.abs(deltaX) >= dp(10f)) {
                timelinePanMoved = true;
            }
            if (timelinePanMoved) {
                final float timelineWidth =
                        Math.max(1f, getWidth() - dp(LEFT_HEADER_DP));
                final float pxPerBar = timelineWidth / Math.max(1, visibleBars);
                final int movedBars = Math.round(deltaX / Math.max(1f, pxPerBar));
                viewportStartBar = clampViewportStart(
                        timelinePanStartViewportStartBar + movedBars);
                invalidate();
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
            if (timelinePanActive) {
                final int previousStart = timelinePanStartViewportStartBar;
                final int currentStart = viewportStartBar;
                final boolean moved = timelinePanMoved;
                timelinePanActive = false;
                timelinePanMoved = false;
                if (moved && previousStart != currentStart) {
                    // Manual panning is a new viewport state, not an inverse zoom step.
                    zoomHistory.clear();
                    if (listener != null) {
                        listener.onViewportChanged(previousStart, currentStart);
                    }
                }
                return true;
            }
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
            if (timelinePanActive && timelinePanMoved) {
                viewportStartBar = clampViewportStart(
                        timelinePanStartViewportStartBar);
                invalidate();
            }
            timelinePanActive = false;
            timelinePanMoved = false;
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
                        1.0f + (float) positionTicks
                                / (float) Math.max(1.0, ticksPerBar())));
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

    long ticksPerBarForAudit() { return ticksPerBar(); }

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
