package com.hereliesaz.hg2gui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class SnakeMenuActivity extends AppCompatActivity {

    private SnakePathView snakePathView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snake_menu);

        snakePathView = findViewById(R.id.snake_path_view);

        View head = findViewById(R.id.snake_head);
        View btnNavigate = findViewById(R.id.btn_navigate);
        View btnDataLogs = findViewById(R.id.btn_data_logs);
        View btnComms = findViewById(R.id.btn_comms);
        View btnSysConfig = findViewById(R.id.btn_sys_config);

        if (snakePathView != null) {
            if (head != null) snakePathView.addNode(head);
            if (btnNavigate != null) snakePathView.addNode(btnNavigate);
            if (btnDataLogs != null) snakePathView.addNode(btnDataLogs);
            if (btnComms != null) snakePathView.addNode(btnComms);
            if (btnSysConfig != null) snakePathView.addNode(btnSysConfig);
        }

        // Animate Head Pulse
        if (head != null) {
            ObjectAnimator pulseX = ObjectAnimator.ofFloat(head, "scaleX", 1f, 1.1f, 1f);
            pulseX.setDuration(1500);
            pulseX.setRepeatCount(ValueAnimator.INFINITE);
            pulseX.setInterpolator(new AccelerateDecelerateInterpolator());
            pulseX.start();

            ObjectAnimator pulseY = ObjectAnimator.ofFloat(head, "scaleY", 1f, 1.1f, 1f);
            pulseY.setDuration(1500);
            pulseY.setRepeatCount(ValueAnimator.INFINITE);
            pulseY.setInterpolator(new AccelerateDecelerateInterpolator());
            pulseY.start();
        }

        // Animate Buttons
        animateEntrance(btnNavigate, 200);
        animateEntrance(btnDataLogs, 400);
        animateEntrance(btnComms, 600);
        animateEntrance(btnSysConfig, 800);

        View reinitiate = findViewById(R.id.btn_reinitiate);
        if (reinitiate != null) {
            reinitiate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    recreate();
                }
            });
        }

        View exit = findViewById(R.id.btn_exit);
        if (exit != null) {
            exit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    private void animateEntrance(View view, long delay) {
        if (view == null) return;

        view.setAlpha(0f);
        view.setTranslationY(50f);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        ObjectAnimator transY = ObjectAnimator.ofFloat(view, "translationY", 50f, 0f);

        transY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
             @Override
             public void onAnimationUpdate(ValueAnimator animation) {
                 if (snakePathView != null) snakePathView.invalidate();
             }
        });

        alpha.setDuration(500);
        alpha.setStartDelay(delay);
        transY.setDuration(500);
        transY.setStartDelay(delay);
        transY.setInterpolator(new AccelerateDecelerateInterpolator());

        alpha.start();
        transY.start();
    }
}
