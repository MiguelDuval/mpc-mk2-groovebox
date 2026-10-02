package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
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
    private static final int BG = Color.rgb(14, 16, 18);
    private static final int SURFACE = Color.rgb(25, 29, 33);

    private final LinearLayout root;
    private final LinearLayout toolbar;
    private final LinearLayout shortcutRail;
    private final LinearLayout contextArea;
    private final FrameLayout workspace;
    private final LinearLayout functionBar;

    MpcShell(Context context) {
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        toolbar = new LinearLayout(context);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundColor(SURFACE);
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 52)));

        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.HORIZONTAL);

        shortcutRail = new LinearLayout(context);
        shortcutRail.setOrientation(LinearLayout.VERTICAL);
        shortcutRail.setBackgroundColor(Color.rgb(18, 21, 24));
        shortcutRail.setPadding(dp(context, 6), dp(context, 6), dp(context, 6), dp(context, 6));
        body.addView(shortcutRail, new LinearLayout.LayoutParams(
                dp(context, 104), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout mainColumn = new LinearLayout(context);
        mainColumn.setOrientation(LinearLayout.VERTICAL);

        contextArea = new LinearLayout(context);
        contextArea.setOrientation(LinearLayout.HORIZONTAL);
        contextArea.setGravity(android.view.Gravity.CENTER_VERTICAL);
        contextArea.setBackgroundColor(SURFACE);
        mainColumn.addView(contextArea, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 54)));

        workspace = new FrameLayout(context);
        workspace.setBackgroundColor(BG);
        mainColumn.addView(workspace, new LinearLayout.LayoutParams(
                0, 0, 1));

        functionBar = new LinearLayout(context);
        functionBar.setOrientation(LinearLayout.HORIZONTAL);
        functionBar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        functionBar.setBackgroundColor(SURFACE);
        mainColumn.addView(functionBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 48)));

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

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
