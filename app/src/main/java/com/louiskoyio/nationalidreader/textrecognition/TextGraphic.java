package com.louiskoyio.nationalidreader.textrecognition;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import com.louiskoyio.nationalidreader.textrecognition.GraphicOverlay.Graphic;
import com.google.mlkit.vision.text.Text;

/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
public class TextGraphic extends Graphic {

    private static final String TAG = "TextGraphic";
    private static final int TEXT_COLOR = Color.RED;
    private static final float TEXT_SIZE = 20.0f;
    private static final float STROKE_WIDTH = 4.0f;

    private final Paint rectPaint;
    private final Paint textPaint;
    private Text.Element element;
    private Text.Line line;

    TextGraphic(GraphicOverlay overlay, Text.Element element) {
        super(overlay);

        this.element = element;

        rectPaint = new Paint();
        rectPaint.setColor(TEXT_COLOR);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(STROKE_WIDTH);

        textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(TEXT_SIZE);
        // Redraw the overlay, as this graphic has been added.
        postInvalidate();
    }

    public TextGraphic(GraphicOverlay overlay, Text.Line line) {
        super(overlay);

        this.line = line;

        rectPaint = new Paint();
        rectPaint.setColor(TEXT_COLOR);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(STROKE_WIDTH);

        textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(TEXT_SIZE);
        // Redraw the overlay, as this graphic has been added.
        postInvalidate();
    }

    /**
     * Draws the text block annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Log.d(TAG, "on draw text graphic");
        if (element == null && line == null) {
            throw new IllegalStateException("Attempting to draw a null text.");
        }

        // Draws the bounding box around the TextBlock.
        if(element!=null) {
            RectF rect = new RectF(element.getBoundingBox());
            canvas.drawRect(rect, rectPaint);
            canvas.drawText(element.getText(), rect.left, rect.bottom, textPaint);
        }else if(element == null && line!=null){
            RectF rect = new RectF(line.getBoundingBox());
            canvas.drawRect(rect, rectPaint);
            canvas.drawText(line.getText(), rect.left, rect.bottom, textPaint);
        }


    }
}
