package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * Shared MPC 3.9-style screen composition.
 *
 * The shell owns layout regions, not musical behavior. MainActivity supplies
 * the actual context views and semantic callbacks until each workspace is
 * migrated to its permanent context implementation.
 */
final class MpcShell {
    // MPC One-style graphite shell chrome. Keep this class presentation-only.
    // All visual tokens are centralised in MpcStyle; this class only composes regions.

    private final LinearLayout root;
    private final LinearLayout toolbar;
    private final MpcPlayheadStrip playheadStrip;
    private final LinearLayout shortcutRail;
    private final LinearLayout contextArea;
    private final FrameLayout workspace;
    private final Button channelStripRestoreButton;
    private final LinearLayout functionBar;

    MpcShell(Context context) {
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(MpcStyle.BG);
        // Keep shell chrome edge-tight; workspaces own their internal gutters.

        toolbar = new LinearLayout(context);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundColor(MpcStyle.BG);
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, MpcStyle.dp(context, MpcStyle.TOOLBAR_HEIGHT_DP)));

        playheadStrip = new MpcPlayheadStrip(context);
        root.addView(playheadStrip, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, MpcStyle.dp(context, MpcStyle.POSITION_STRIP_HEIGHT_DP)));

        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.HORIZONTAL);

        shortcutRail = new LinearLayout(context);
        shortcutRail.setOrientation(LinearLayout.VERTICAL);
        shortcutRail.setBackgroundColor(MpcStyle.PANEL_DARK);
        shortcutRail.setPadding(
                MpcStyle.dp(context, MpcStyle.RAIL_CONTENT_INSET_DP),
                MpcStyle.dp(context, MpcStyle.RAIL_CONTENT_INSET_DP),
                MpcStyle.dp(context, MpcStyle.RAIL_CONTENT_INSET_DP),
                MpcStyle.dp(context, MpcStyle.RAIL_CONTENT_INSET_DP));
        body.addView(shortcutRail, new LinearLayout.LayoutParams(
                MpcStyle.dp(context, MpcStyle.SHORTCUT_RAIL_WIDTH_DP), ViewGroup.LayoutParams.MATCH_PARENT));

        /*
         * Shortcuts and persistent context are separate columns. The context
         * column remains visible while the active workspace changes, keeping
         * sequence/track/program/pad state glanceable without stealing height
         * from the workspace.
         */
        contextArea = new LinearLayout(context);
        contextArea.setOrientation(LinearLayout.VERTICAL);
        contextArea.setBackgroundColor(MpcStyle.PANEL);
        contextArea.setPadding(
                MpcStyle.dp(context, MpcStyle.CONTEXT_CONTENT_INSET_DP),
                MpcStyle.dp(context, MpcStyle.CONTEXT_CONTENT_INSET_DP),
                MpcStyle.dp(context, MpcStyle.CONTEXT_CONTENT_INSET_DP),
                MpcStyle.dp(context, MpcStyle.CONTEXT_CONTENT_INSET_DP));
        body.addView(contextArea, new LinearLayout.LayoutParams(
                MpcStyle.dp(context, MpcStyle.CHANNEL_STRIP_WIDTH_DP), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout mainColumn = new LinearLayout(context);
        mainColumn.setOrientation(LinearLayout.VERTICAL);

        workspace = new FrameLayout(context);
        workspace.setBackgroundColor(BG);

        channelStripRestoreButton = new Button(context);
        channelStripRestoreButton.setText("");
        channelStripRestoreButton.setTextColor(Color.TRANSPARENT);
        channelStripRestoreButton.setForeground(new MpcMixerStripIconDrawable(
                MpcMixerStripIconDrawable.Mode.PERSONAL_CHANNEL_STRIP, false));
        channelStripRestoreButton.setTextColor(Color.WHITE);
        channelStripRestoreButton.setTextSize(16);
        channelStripRestoreButton.setMinHeight(0);
        channelStripRestoreButton.setMinimumHeight(0);
        channelStripRestoreButton.setPadding(0, 0, 0, 0);
        channelStripRestoreButton.setGravity(android.view.Gravity.CENTER);
        channelStripRestoreButton.setBackgroundColor(MpcStyle.SURFACE);
        channelStripRestoreButton.setContentDescription(
                "MPC XL Channel Strip show");
        channelStripRestoreButton.setVisibility(View.GONE);
        workspace.addView(channelStripRestoreButton, new FrameLayout.LayoutParams(
                MpcStyle.dp(context, 20), MpcStyle.dp(context, 30),
                android.view.Gravity.LEFT | android.view.Gravity.TOP));

        // mainColumn is vertical: weight distributes height, so workspace must
        // remain MATCH_PARENT horizontally or the Main workspace collapses to 0px.
        mainColumn.addView(workspace, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        functionBar = new LinearLayout(context);
        functionBar.setOrientation(LinearLayout.HORIZONTAL);
        functionBar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        functionBar.setBackgroundColor(MpcStyle.SURFACE);
        mainColumn.addView(functionBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, MpcStyle.dp(context, MpcStyle.FUNCTION_BAR_HEIGHT_DP)));

        body.addView(mainColumn, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        root.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    }

    View root() {
        return root;
    }

    LinearLayout toolbar() {
        return toolbar;
    }

    MpcPlayheadStrip playheadStrip() {
        return playheadStrip;
    }

    LinearLayout shortcuts() {
        return shortcutRail;
    }

    LinearLayout contextArea() {
        return contextArea;
    }

    FrameLayout workspace() {
        return workspace;
    }

    LinearLayout functionBar() {
        return functionBar;
    }

    void setChannelStripRestoreListener(View.OnClickListener listener) {
        channelStripRestoreButton.setOnClickListener(listener);
    }

    void setChannelStripVisible(boolean visible) {
        LinearLayout.LayoutParams contextParams =
                (LinearLayout.LayoutParams) contextArea.getLayoutParams();
        contextParams.width = visible
                ? MpcStyle.dp(contextArea.getContext(), MpcStyle.CHANNEL_STRIP_WIDTH_DP)
                : 0;
        contextParams.weight = 0.0f;
        contextArea.setLayoutParams(contextParams);
        channelStripRestoreButton.setVisibility(
                visible ? View.GONE : View.VISIBLE);
        channelStripRestoreButton.setContentDescription(
                visible
                        ? "MPC XL Channel Strip visible"
                        : "MPC XL Channel Strip show");
    }

}
