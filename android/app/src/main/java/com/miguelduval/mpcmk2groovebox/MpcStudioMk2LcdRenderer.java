package com.miguelduval.mpcmk2groovebox;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.List;
import java.util.Locale;

final class MpcStudioMk2LcdRenderer {
    static final int WIDTH = 160;
    static final int HEIGHT = 80;

    static final class State {
        final String page;
        final int sequenceIndex;
        final int sequenceCount;
        final int queuedSequenceIndex;
        final int trackIndex;
        final int trackCount;
        final double tempo;
        final int numerator;
        final int denominator;
        final long positionTicks;
        final boolean playing;
        final boolean recordArmed;
        final boolean overdub;
        final int selectedPad;
        final int selectedLayer;
        final int touchStripMode;
        final int noteRepeatRateIndex;
        final boolean noteRepeat;
        final boolean locate;
        final boolean erase;
        final int stepEditParameter;
        final int selectedStep;
        final String status;

        State(
                String page,
                int sequenceIndex,
                int sequenceCount,
                int queuedSequenceIndex,
                int trackIndex,
                int trackCount,
                double tempo,
                int numerator,
                int denominator,
                long positionTicks,
                boolean playing,
                boolean recordArmed,
                boolean overdub,
                int selectedPad,
                int selectedLayer,
                int touchStripMode,
                int noteRepeatRateIndex,
                boolean noteRepeat,
                boolean locate,
                boolean erase,
                int stepEditParameter,
                int selectedStep,
                String status) {
            this.page = page == null ? "MAIN" : page;
            this.sequenceIndex = Math.max(0, sequenceIndex);
            this.sequenceCount = Math.max(1, sequenceCount);
            this.queuedSequenceIndex = queuedSequenceIndex;
            this.trackIndex = Math.max(0, trackIndex);
            this.trackCount = Math.max(1, trackCount);
            this.tempo = tempo;
            this.numerator = Math.max(1, numerator);
            this.denominator = Math.max(1, denominator);
            this.positionTicks = Math.max(0L, positionTicks);
            this.playing = playing;
            this.recordArmed = recordArmed;
            this.overdub = overdub;
            this.selectedPad = Math.max(0, selectedPad);
            this.selectedLayer = Math.max(0, selectedLayer);
            this.touchStripMode = Math.max(0, Math.min(4, touchStripMode));
            this.noteRepeatRateIndex = Math.max(0, Math.min(7, noteRepeatRateIndex));
            this.noteRepeat = noteRepeat;
            this.locate = locate;
            this.erase = erase;
            this.stepEditParameter = Math.max(0, Math.min(4, stepEditParameter));
            this.selectedStep = selectedStep;
            this.status = status == null ? "" : status;
        }

        String signature() {
            final Position p = position(this);
            return String.format(
                    Locale.ROOT,
                    "%s|S%d/%d|Q%d|T%d/%d|%.1f|%d/%d|%d.%d|%b|%b|%b|P%d|L%d|TS%d|NR%d:%d|LOC%d|ER%d|STEP%d:%d|%s",
                    page, sequenceIndex, sequenceCount, queuedSequenceIndex,
                    trackIndex, trackCount, tempo, numerator, denominator,
                    p.bar, p.beat, playing, recordArmed, overdub,
                    selectedPad, selectedLayer, touchStripMode,
                    noteRepeat, noteRepeatRateIndex, locate, erase,
                    stepEditParameter, selectedStep, clip(status, 24));
        }
    }

    private MpcStudioMk2LcdRenderer() {}

    static List<byte[]> render(State state) {
        final Bitmap frame = Bitmap.createBitmap(
                WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(frame);
        canvas.drawColor(0xFF0E1012);

        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.DEFAULT_BOLD);

        paint.setColor(0xFFEBEFF2);
        paint.setTextSize(11.0f);
        canvas.drawText(clip(state.page, 9), 5.0f, 12.0f, paint);

        final String transport = transportLabel(state);
        paint.setColor(
                state.erase || state.recordArmed
                        ? 0xFFEC5353
                        : state.playing
                                ? 0xFF3FCF75
                                : 0xFFEBEFF2);
        paint.setTextSize(9.0f);
        canvas.drawText(transport, 108.0f, 12.0f, paint);

        paint.setColor(0xFF404850);
        canvas.drawRect(5.0f, 17.0f, 155.0f, 18.0f, paint);

        paint.setColor(0xFFEBEFF2);
        paint.setTextSize(9.5f);
        canvas.drawText(
                String.format(
                        Locale.ROOT,
                        "S%02d/%02d  T%02d/%02d",
                        state.sequenceIndex + 1,
                        state.sequenceCount,
                        state.trackIndex + 1,
                        state.trackCount),
                5.0f, 30.0f, paint);

        paint.setColor(0xFF9CA6AE);
        paint.setTextSize(7.5f);
        canvas.drawText(
                String.format(
                        Locale.ROOT,
                        "%.1f BPM %d/%d",
                        state.tempo, state.numerator, state.denominator),
                95.0f, 30.0f, paint);

        final Position pos = position(state);
        paint.setColor(0xFFFFB448);
        paint.setTextSize(11.0f);
        canvas.drawText(
                String.format(Locale.ROOT, "%03d.%d",
                        pos.bar + 1, pos.beat + 1),
                5.0f, 45.0f, paint);

        paint.setColor(0xFFEBEFF2);
        paint.setTextSize(9.0f);
        canvas.drawText(
                String.format(
                        Locale.ROOT,
                        "PAD %02d  L%d",
                        state.selectedPad + 1, state.selectedLayer + 1),
                58.0f, 45.0f, paint);

        if (state.queuedSequenceIndex >= 0
                && state.queuedSequenceIndex < state.sequenceCount) {
            paint.setColor(0xFF45D3FF);
            canvas.drawText(
                    String.format(
                            Locale.ROOT,
                            "→S%02d",
                            state.queuedSequenceIndex + 1),
                    126.0f, 45.0f, paint);
        }

        paint.setColor(0xFF404850);
        canvas.drawRect(5.0f, 49.0f, 155.0f, 50.0f, paint);

        paint.setColor(0xFFFFB448);
        paint.setTextSize(8.5f);
        canvas.drawText(contextLabel(state), 5.0f, 62.0f, paint);

        paint.setColor(0xFFEBEFF2);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(8.0f);
        canvas.drawText(clip(state.status, 27), 5.0f, 77.0f, paint);

        final List<byte[]> messages =
                MpcStudioMk2MidiMessages.encodeLcdFrame(frame);
        frame.recycle();
        return messages;
    }

    private static String transportLabel(State state) {
        if (!state.playing) return "STOP";
        if (state.recordArmed) {
            return state.overdub ? "OVERDUB" : "REC";
        }
        return "PLAY";
    }

    private static String contextLabel(State state) {
        if (state.erase) return "ERASE ARMED";
        if (state.locate) return "LOCATE";
        if (state.noteRepeat) {
            return "NOTE REPEAT " + noteRepeatRateLabel(state.noteRepeatRateIndex);
        }
        if ("SEQ".equals(state.page) && state.selectedStep >= 0) {
            return "STEP " + parameterLabel(state.stepEditParameter)
                    + " #" + (state.selectedStep + 1);
        }
        return "TOUCH " + touchStripModeLabel(state.touchStripMode);
    }

    private static String parameterLabel(int value) {
        switch (value) {
            case 1: return "PROB";
            case 2: return "RATCH";
            case 3: return "NUDGE";
            case 4: return "DUR";
            default: return "VEL";
        }
    }

    private static String touchStripModeLabel(int value) {
        switch (value) {
            case 1: return "PAN";
            case 2: return "TUNE";
            case 3: return "START";
            case 4: return "END";
            default: return "LEVEL";
        }
    }

    private static String noteRepeatRateLabel(int index) {
        switch (index) {
            case 0: return "1/4";
            case 1: return "1/8";
            case 2: return "1/16";
            case 3: return "1/32";
            case 4: return "1/64";
            case 5: return "1/4T";
            case 6: return "1/8T";
            case 7: return "1/16T";
            default: return "RATE";
        }
    }

    private static Position position(State state) {
        final long ticksPerBeat = Math.max(
                1L, Math.round(960.0 * 4.0 / state.denominator));
        final long ticksPerBar = Math.max(
                ticksPerBeat, ticksPerBeat * state.numerator);
        return new Position(
                state.positionTicks / ticksPerBar,
                (state.positionTicks % ticksPerBar) / ticksPerBeat);
    }

    private static String clip(String value, int maxChars) {
        if (value == null) return "";
        final String trimmed =
                value.replace('\n', ' ').replace('\r', ' ').trim();
        if (trimmed.length() <= maxChars) return trimmed;
        return trimmed.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    private static final class Position {
        final long bar;
        final long beat;

        Position(long bar, long beat) {
            this.bar = bar;
            this.beat = beat;
        }
    }
}
