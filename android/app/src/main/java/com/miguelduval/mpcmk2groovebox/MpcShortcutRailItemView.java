package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * One compact MPC Shortcut Rail destination.
 *
 * The rail item owns presentation only: deterministic icon, short label and
 * selection indicator. Navigation remains owned by MainActivity's semantic
 * controller path.
 */
final class MpcShortcutRailItemView extends FrameLayout {
    private static final int BG = Color.rgb(23, 25, 28);
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.rgb(156, 166, 174);
    private static final int RED = Color.rgb(224, 30, 61);

    private final MpcUiState.Mode mode;
    private final String accessibleLabel;
    private final MpcShortcutIconDrawable iconDrawable;
    private final ImageView iconView;
    private final TextView labelView;
    private final View selectionIndicator;

    MpcShortcutRailItemView(
            Context context,
            MpcUiState.Mode mode,
            String label,
            String accessibleLabel,
            int selectionWidthPx) {
        super(context);
        this.mode = mode;
        this.accessibleLabel = accessibleLabel;
        setBackgroundColor(BG);
        setContentDescription("MPC shortcut " + accessibleLabel);
        setFocusable(true);
        setClickable(false);
        setPadding(dp(context, 1), 0, dp(context, 1), 0);

        selectionIndicator = new View(context);
        selectionIndicator.setBackgroundColor(RED);
        selectionIndicator.setVisibility(View.GONE);
        LayoutParams selectionParams = new LayoutParams(
                Math.max(1, selectionWidthPx),
                LayoutParams.MATCH_PARENT,
                Gravity.LEFT);
        addView(selectionIndicator, selectionParams);

        iconView = new ImageView(context);
        iconDrawable = new MpcShortcutIconDrawable(
                mode,
                Math.max(1, selectionWidthPx));
        iconView.setImageDrawable(iconDrawable);
        iconView.setContentDescription("MPC shortcut icon " + accessibleLabel);
        LayoutParams iconParams = new LayoutParams(
                dp(context, 24),
                dp(context, 24),
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        iconParams.topMargin = dp(context, 5);
        addView(iconView, iconParams);

        labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextColor(MUTED);
        labelView.setTextSize(7);
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        labelView.setGravity(Gravity.CENTER);
        labelView.setSingleLine(true);
        labelView.setIncludeFontPadding(false);
        labelView.setContentDescription("MPC shortcut label " + label);
        LayoutParams labelParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(context, 15),
                Gravity.BOTTOM);
        labelParams.leftMargin = dp(context, 1);
        labelParams.rightMargin = dp(context, 1);
        labelParams.bottomMargin = dp(context, 2);
        addView(labelView, labelParams);
    }

    MpcUiState.Mode mode() {
        return mode;
    }

    void setSelectedState(boolean selected) {
        selectionIndicator.setVisibility(selected ? View.VISIBLE : View.GONE);
        iconDrawable.setSelected(selected);
        labelView.setTextColor(selected ? TEXT : MUTED);
        labelView.setAlpha(selected ? 1.0f : 0.82f);
        setSelected(selected);
    }

    void setRailClickListener(View.OnClickListener listener) {
        setOnClickListener(listener);
        setClickable(listener != null);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
