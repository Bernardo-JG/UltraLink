package org.feup.apm.cmeb_login.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;

import org.feup.apm.cmeb_login.Brush;
import org.feup.apm.cmeb_login.Drawing;

/**
 * An earser, drawing the track line with the color of the bitmap's background
 * color.
 */
public class EraserView extends Drawing {

    private Path path;
    private static final float TOUCH_TOLERANCE = 4;
    private Paint eraser;

    public EraserView() {
        Eraser(); // Ensure the Eraser method is called in the constructor
    }

    public void Eraser(){
        path = new Path();
        eraser = Brush.getPen();
        eraser.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
    }

    @Override
    public void draw(Canvas canvas){
        canvas.drawPath(path, eraser);
    }

    @Override
    public void fingerDown(float x, float y, Canvas canvas){
        super.fingerDown(x, y, canvas);
        path.reset();
        path.moveTo(x, y);
    }

    @Override
    public void fingerMove(float x, float y, Canvas canvas){
        float dx = Math.abs(x - startX);
        float dy = Math.abs(y - startY);
        if(dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE){
            path.quadTo(startX, startY, (x + startX) / 2, (y + startY) / 2);
            startX = x;
            startY = y;
        }
        draw(canvas);
    }

    @Override
    public void fingerUp(float x, float y, Canvas canvas){
        path.lineTo(startX, startY);
        super.fingerUp(x, y, canvas);
    }
}
