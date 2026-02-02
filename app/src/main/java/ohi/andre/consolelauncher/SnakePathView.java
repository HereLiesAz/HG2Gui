package ohi.andre.consolelauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class SnakePathView extends View {

    private Paint paint;
    private Path path;
    private List<View> nodes = new ArrayList<>();

    public SnakePathView(Context context) {
        super(context);
        init();
    }

    public SnakePathView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SnakePathView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2 * density); // 2dp width
        paint.setAntiAlias(true);
        // Default to green, but should be set via setPrimaryColor
        paint.setColor(0xFF13ec6a);

        path = new Path();
    }

    public void setPrimaryColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    public void addNode(View view) {
        nodes.add(view);
        invalidate();
    }

    public void clearNodes() {
        nodes.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (nodes.size() < 2) return;

        path.reset();

        View first = nodes.get(0);
        // We assume the views are siblings in the same relative container
        // so getX() / getY() are in the same coordinate space.
        // We want the center of the view.
        float prevX = first.getX() + first.getWidth() / 2f;
        float prevY = first.getY() + first.getHeight() / 2f;

        // If layout hasn't happened yet, these might be 0.
        // But usually onDraw is after layout.

        // To be safe against 0 values if views are gone/invisible?
        // We'll just draw.

        path.moveTo(prevX, prevY);

        for (int i = 1; i < nodes.size(); i++) {
            View current = nodes.get(i);
            float currX = current.getX() + current.getWidth() / 2f;
            float currY = current.getY() + current.getHeight() / 2f;

            // Draw curve
            // Control points for smooth S-curve (vertical hold)
            // We want to go down, then curve to the side.

            float midY = (prevY + currY) / 2f;

            // Cubic Bezier: Start(prevX, prevY) -> Control1(prevX, midY) -> Control2(currX, midY) -> End(currX, currY)
            path.cubicTo(prevX, midY, currX, midY, currX, currY);

            prevX = currX;
            prevY = currY;
        }

        // Draw a small tail for the last segment
        path.lineTo(prevX, prevY + 50);

        canvas.drawPath(path, paint);
    }
}
