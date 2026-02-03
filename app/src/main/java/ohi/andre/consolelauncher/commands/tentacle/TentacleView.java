package ohi.andre.consolelauncher.commands.tentacle;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

import ohi.andre.consolelauncher.R;

public class TentacleView extends View {

    private Paint paint;
    private Paint glowPaint;

    // Optimization: Pre-allocate objects
    private Path dstPath = new Path();
    private List<PathMeasure> pathMeasures = new ArrayList<>();

    private float progress = 0f;
    private int primaryColor;

    public TentacleView(Context context) {
        super(context);
        init();
    }

    public TentacleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TentacleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        primaryColor = getResources().getColor(R.color.tentacle_primary);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(primaryColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setStrokeCap(Paint.Cap.ROUND);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(primaryColor);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(12f);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setAlpha(80); // Glow effect
    }

    /**
     * Called by the Activity once the buttons are laid out.
     * Calculates the Bezier curves connecting the center to the satellites.
     */
    public void setTargetPoints(float centerX, float centerY,
                                float guideX, float guideY,
                                float manualX, float manualY,
                                float tipsX, float tipsY) {

        pathMeasures.clear();

        // Path 1: Center -> Guide (Top Left)
        Path p1 = new Path();
        p1.moveTo(centerX, centerY);
        // Control points for a nice "tentacle" curve
        p1.cubicTo(centerX, centerY - 100, guideX + 50, guideY + 100, guideX, guideY);
        pathMeasures.add(new PathMeasure(p1, false));

        // Path 2: Center -> Manual (Top Right)
        Path p2 = new Path();
        p2.moveTo(centerX, centerY);
        p2.cubicTo(centerX, centerY - 100, manualX - 50, manualY + 100, manualX, manualY);
        pathMeasures.add(new PathMeasure(p2, false));

        // Path 3: Center -> Tips (Bottom)
        Path p3 = new Path();
        p3.moveTo(centerX, centerY);
        p3.cubicTo(centerX - 100, centerY + 50, tipsX + 100, tipsY - 50, tipsX, tipsY);
        pathMeasures.add(new PathMeasure(p3, false));

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (pathMeasures.isEmpty()) return;

        for (PathMeasure measure : pathMeasures) {
            dstPath.reset();
            // Draw segment based on progress
            measure.getSegment(0, measure.getLength() * progress, dstPath, true);

            canvas.drawPath(dstPath, glowPaint);
            canvas.drawPath(dstPath, paint);
        }
    }

    public void startAnimation() {
        // Draw animation
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1500);
        animator.setStartDelay(200); // Wait a bit
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                progress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();

        // Pulse animation (infinite)
        ValueAnimator pulse = ValueAnimator.ofFloat(12f, 25f);
        pulse.setDuration(1200);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                glowPaint.setStrokeWidth(val);
                // We must invalidate to see the pulse even if progress is static
                invalidate();
            }
        });
        pulse.start();
    }
}
