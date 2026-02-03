package ohi.andre.consolelauncher;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

public class MagnetMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magnet_menu);

        // Pulse Animation for Magnet
        View magnet = findViewById(R.id.magnet);
        if (magnet != null) {
            ObjectAnimator pulse = ObjectAnimator.ofFloat(magnet, "scaleX", 1f, 1.1f, 1f);
            pulse.setDuration(3000);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.setInterpolator(new AccelerateDecelerateInterpolator());
            pulse.start();

            ObjectAnimator pulseY = ObjectAnimator.ofFloat(magnet, "scaleY", 1f, 1.1f, 1f);
            pulseY.setDuration(3000);
            pulseY.setRepeatCount(ValueAnimator.INFINITE);
            pulseY.setInterpolator(new AccelerateDecelerateInterpolator());
            pulseY.start();
        }

        // Rotation for Scraps
        View scrapLogs = findViewById(R.id.scrap_logs);
        if (scrapLogs != null) scrapLogs.setRotation(-6f);

        View scrapNet = findViewById(R.id.scrap_net);
        if (scrapNet != null) scrapNet.setRotation(-12f);

        View scrapAudio = findViewById(R.id.scrap_audio);
        if (scrapAudio != null) scrapAudio.setRotation(3f);

        View scrapDebug = findViewById(R.id.scrap_debug);
        if (scrapDebug != null) scrapDebug.setRotation(12f);

        // Float Animation for Scraps
        animateFloat(scrapLogs, 0);
        animateFloat(scrapNet, 800);
        animateFloat(scrapAudio, 1500);
        animateFloat(scrapDebug, 2200);
    }

    private void animateFloat(View view, long delay) {
        if (view == null) return;
        ObjectAnimator floatAnim = ObjectAnimator.ofFloat(view, "translationY", 0f, -20f, 0f);
        floatAnim.setDuration(6000);
        floatAnim.setStartDelay(delay);
        floatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        floatAnim.setRepeatCount(ValueAnimator.INFINITE);
        floatAnim.start();
    }

    public static class WaveView extends View {
        private Paint paint;
        private Paint glowPaint;
        private Path path;
        private float phase = 0f;

        public WaveView(Context context) {
            super(context);
            init();
        }

        public WaveView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public WaveView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }

        private void init() {
            int primaryColor = ContextCompat.getColor(getContext(), R.color.primary);

            paint = new Paint();
            paint.setColor(primaryColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setAntiAlias(true);
            paint.setAlpha(100);

            glowPaint = new Paint();
            glowPaint.setColor(primaryColor);
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeWidth(8f);
            glowPaint.setAntiAlias(true);
            glowPaint.setAlpha(50);
            // BlurMaskFilter might need software layer on some APIs/devices for consistent look
            // but we'll try without enforcing layer type first.
            glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(10, android.graphics.BlurMaskFilter.Blur.NORMAL));


            path = new Path();

            // Animation loop
            ValueAnimator animator = ValueAnimator.ofFloat(0f, (float) (2 * Math.PI));
            animator.setDuration(2000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    phase = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            animator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int width = getWidth();
            int height = getHeight();
            int centerY = height / 2;

            path.reset();
            path.moveTo(0, centerY);

            for (int x = 0; x <= width; x += 5) { // Optimization: step by 5
                float normalizedX = (float) x / width;
                float angle = normalizedX * 2 * (float) Math.PI + phase;
                float y = centerY + 30f * (float) Math.sin(angle);
                path.lineTo(x, y);
            }

            // Draw glow then line
            canvas.drawPath(path, glowPaint);
            canvas.drawPath(path, paint);
        }
    }
}
