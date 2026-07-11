package com.miketwo.workouttracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {
    private static final long SPLASH_DURATION_MS = 900;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openHome = () -> {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Ui.CREAM);
        getWindow().setNavigationBarColor(Ui.CREAM);
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        LinearLayout body = Ui.column(this);
        body.setGravity(Gravity.CENTER);
        TextView name = Ui.title(this, "Workout Tracker");
        name.setTextSize(38);
        name.setGravity(Gravity.CENTER);
        body.addView(name);
        TextView version = Ui.text(this, "Version " + BuildConfig.VERSION_NAME + "  ·  build " + BuildConfig.VERSION_CODE, 17, Ui.MUTED);
        version.setGravity(Gravity.CENTER);
        body.addView(version);
        Ui.spacer(body, 20);
        TextView promise = Ui.text(this, "Plan ahead. Follow the next step.", 16, Ui.FOREST);
        promise.setGravity(Gravity.CENTER);
        body.addView(promise);
        setContentView(body);
        handler.postDelayed(openHome, SPLASH_DURATION_MS);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(openHome);
        super.onDestroy();
    }
}
