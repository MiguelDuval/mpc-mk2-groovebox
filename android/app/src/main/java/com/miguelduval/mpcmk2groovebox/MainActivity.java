package com.miguelduval.mpcmk2groovebox;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public final class MainActivity extends Activity {
    static {
        System.loadLibrary("mpcgroovebox");
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        TextView root = new TextView(this);
        root.setGravity(Gravity.CENTER);
        root.setTextSize(22.0f);
        root.setText("MPC Studio MkII Groovebox\nFoundation build 0.1.0");
        setContentView(root);
    }
}
