package ohi.andre.consolelauncher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.Random;

public class MitosisMenuActivity extends AppCompatActivity {

    private FrameLayout bubbleContainer;
    private Random random = new Random();
    private Handler handler = new Handler();
    private int screenHeight;
    private int screenWidth;

    // Strategies
    private static final int STRATEGY_CALM = 0;
    private static final int STRATEGY_WINDY = 1;
    private static final int STRATEGY_CHAOTIC = 2;
    private int currentStrategy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mitosis_menu);

        bubbleContainer = findViewById(R.id.bubble_container);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;

        // Randomize Strategy
        currentStrategy = random.nextInt(3);

        // Start spawning bubbles
        startBubbleSpawner();

        // Header Pulse
        View statusDot = findViewById(R.id.status_dot);
        if (statusDot != null) {
            ObjectAnimator pulse = ObjectAnimator.ofFloat(statusDot, "alpha", 1f, 0.4f, 1f);
            pulse.setDuration(1500);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.start();
        }

        // Setup Surface Icons
        setupIconInteraction(findViewById(R.id.icon_home));
        setupIconInteraction(findViewById(R.id.icon_map));
        setupIconInteraction(findViewById(R.id.icon_boost));
    }

    private void setupIconInteraction(final View view) {
        if (view == null) return;
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Squash and Stretch
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.25f, 0.9f, 1f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.75f, 1.1f, 1f);
                scaleX.setDuration(300);
                scaleY.setDuration(300);
                scaleX.start();
                scaleY.start();
            }
        });
    }

    private Runnable spawnerRunnable = new Runnable() {
        @Override
        public void run() {
            spawnBubble();
            // Random delay between spawns
            int delay = 800 + random.nextInt(2000);
            if (currentStrategy == STRATEGY_CHAOTIC) delay = 200 + random.nextInt(500);
            else if (currentStrategy == STRATEGY_WINDY) delay = 500 + random.nextInt(1000);

            handler.postDelayed(this, delay);
        }
    };

    private void startBubbleSpawner() {
        handler.post(spawnerRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && spawnerRunnable != null) {
            handler.removeCallbacks(spawnerRunnable);
        }
        if (bubbleContainer != null) {
            bubbleContainer.removeAllViews();
        }
    }

    private void spawnBubble() {
        if (bubbleContainer == null) return;

        final ImageView bubble = new ImageView(this);
        bubble.setBackgroundResource(R.drawable.circle_shape);

        // Random Size
        int size = 30 + random.nextInt(120);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);

        // Random X Position
        params.leftMargin = random.nextInt(screenWidth - size);
        // Start just below the screen bottom, handled by translationY usually but adding to container at bottom
        params.topMargin = screenHeight;

        bubble.setLayoutParams(params);

        // Style
        bubble.setAlpha(0.1f + random.nextFloat() * 0.4f);
        // We can set tint color if needed, but shape is white so it takes alpha well.
        // Maybe slight green tint?
        // bubble.setColorFilter(Color.parseColor("#13ec6a"));

        bubbleContainer.addView(bubble);

        // Vertical Movement
        long duration = 5000 + random.nextInt(5000);
        if (currentStrategy == STRATEGY_WINDY) duration = 3000 + random.nextInt(2000);
        if (currentStrategy == STRATEGY_CHAOTIC) duration = 2000 + random.nextInt(3000);

        ObjectAnimator rise = ObjectAnimator.ofFloat(bubble, "translationY", 0, -(screenHeight + size));
        rise.setDuration(duration);
        rise.setInterpolator(new LinearInterpolator());
        rise.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                bubbleContainer.removeView(bubble);
            }
        });
        rise.start();

        // Horizontal Movement (Wobble)
        float startX = 0;
        float endX = 0;

        if (currentStrategy == STRATEGY_CALM) {
            endX = (random.nextBoolean() ? 1 : -1) * (50 + random.nextInt(50));
        } else if (currentStrategy == STRATEGY_WINDY) {
            endX = (random.nextBoolean() ? 1 : -1) * (200 + random.nextInt(300));
        } else {
            endX = (random.nextBoolean() ? 1 : -1) * (100 + random.nextInt(400));
        }

        ObjectAnimator drift = ObjectAnimator.ofFloat(bubble, "translationX", startX, endX);
        drift.setDuration(duration);
        drift.setInterpolator(new AccelerateDecelerateInterpolator());
        drift.start();
    }
}
