package com.hereliesaz.hg2gui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class PanicGraphView extends View {

    private Paint gridPaint;
    private Paint fillPaint;
    private Paint strokePaint;
    private Paint playheadPaint;

    private Path graphPath;

    public PanicGraphView(Context context) {
        super(context);
        init();
    }

    public PanicGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint = new Paint();
        gridPaint.setColor(Color.WHITE);
        gridPaint.setAlpha(25); // ~10% opacity
        gridPaint.setStrokeWidth(1);
        gridPaint.setStyle(Paint.Style.STROKE);

        strokePaint = new Paint();
        strokePaint.setColor(Color.parseColor("#39ff14")); // secondary
        strokePaint.setStrokeWidth(5);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setAntiAlias(true);

        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        playheadPaint = new Paint();
        playheadPaint.setColor(Color.parseColor("#f20d0d")); // primary
        playheadPaint.setStrokeWidth(3);
        playheadPaint.setStyle(Paint.Style.STROKE);
        playheadPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

        graphPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Draw Grid
        for(int i=1; i<4; i++) {
            float y = height * i / 4.0f;
            canvas.drawLine(0, y, width, y, gridPaint);
        }

        float h = height;
        float w = width;

        // Path logic based on:
        // M0,150 Q50,150 75,100 T150,50 T225,120 T300,30 T400,80

        // Recreating the curve using cubicTo
        Path path = new Path();
        path.moveTo(0, h);

        // First curve section
        // 0,150 -> 150,50 via control point approx
        path.cubicTo(w * 0.125f, h, w * 0.25f, h * 0.33f, w * 0.375f, h * 0.33f);

        // Second curve section
        path.cubicTo(w * 0.5f, h * 0.33f, w * 0.6f, h * 0.8f, w * 0.75f, h * 0.2f);

        // End
        path.lineTo(w, h * 0.5f);

        // Prepare Fill Path
        graphPath.reset();
        graphPath.addPath(path);
        graphPath.lineTo(w, h);
        graphPath.lineTo(0, h);
        graphPath.close();

        // Gradient
        LinearGradient shader = new LinearGradient(0, 0, 0, height,
            Color.parseColor("#3339ff14"), // secondary with opacity
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP);
        fillPaint.setShader(shader);

        canvas.drawPath(graphPath, fillPaint);

        // Draw Stroke
        canvas.drawPath(path, strokePaint);

        // Draw Playhead
        float x = (System.currentTimeMillis() % 3000) / 3000.0f * width;
        canvas.drawLine(x, 0, x, height, playheadPaint);

        // Animate
        postInvalidateOnAnimation();
    }
}
