package org.feup.apm.cmeb_login.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Bitmap;

public class DrawingView extends View {
    private Paint paint;
    private Path path;
    private Bitmap bitmap;
    private Canvas canvas;
    private boolean isDrawingEnabled = false;
    private boolean isEraserMode = false;
    private EraserView eraserView;
    private OnDrawingListener onDrawingListener;

    public DrawingView(Context context) {
        super(context);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        path = new Path();
        eraserView = new EraserView();

        // Configure paint
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(8f);
        paint.setColor(Color.BLUE); // Default drawing color
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Create a new bitmap and canvas
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); // Support transparency
        canvas = new Canvas(bitmap);
    }

    @Override
    protected void onDraw(Canvas drawCanvas) {
        super.onDraw(drawCanvas);

        // Draw the bitmap
        drawCanvas.drawBitmap(bitmap, 0, 0, null);

        // Draw the current path
        drawCanvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (isEraserMode) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    eraserView.fingerDown(x, y, canvas);
                    break;
                case MotionEvent.ACTION_MOVE:
                    eraserView.fingerMove(x, y, canvas);
                    break;
                case MotionEvent.ACTION_UP:
                    eraserView.fingerUp(x, y, canvas);
                    if (onDrawingListener != null) {
                        onDrawingListener.onDrawingFinished();
                    }
                    break;
                default:
                    return false;
            }
        } else if(isDrawingEnabled) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    path.lineTo(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    // Commit the path to the canvas
                    canvas.drawPath(path, paint);
                    path.reset();
                    if (onDrawingListener != null) {
                        onDrawingListener.onDrawingFinished();
                    }
                    break;
                default:
                    return false;
            }
        }
        else
            return false;

        // Redraw the view
        invalidate();
        return true;
    }

    public void setDrawingEnabled(boolean enabled) {
        isDrawingEnabled = enabled;
    }

    public void setEraserMode(boolean isEraser) {
        isEraserMode = isEraser;

        if (isEraserMode) {
            // Eraser mode: Clear pixels where the path is drawn
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            paint.setColor(Color.TRANSPARENT);
            paint.setStrokeWidth(50); // Increase stroke width for eraser
        } else {
            // Drawing mode: Reset to default
            paint.setXfermode(null);
            paint.setAlpha(255); // Fully opaque
            paint.setColor(Color.BLUE); // Default drawing color
            paint.setStrokeWidth(8f); // Reset stroke width
        }
    }

    public void setPaintColor(int color) {
        if (!isEraserMode) {
            paint.setColor(color); // Change the paint color dynamically
        }
    }

    public void clear() {
        if (bitmap != null) {
            bitmap.eraseColor(Color.TRANSPARENT); // Clear the bitmap
        }
        path.reset();
        invalidate(); // Redraw the view
    }

    public void setOnDrawingListener(OnDrawingListener listener) {
        this.onDrawingListener = listener;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public interface OnDrawingListener {
        void onDrawingFinished();
    }
}