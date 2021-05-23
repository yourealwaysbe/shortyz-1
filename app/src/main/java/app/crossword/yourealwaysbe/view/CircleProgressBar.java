package app.crossword.yourealwaysbe.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import app.crossword.yourealwaysbe.forkyz.R;

/**
 * Created by rcooper on 10/26/14.
 */
public class CircleProgressBar extends View {
    private int nullColor;
    private int inProgressColor;
    private int doneColor;
    private int errorColor;
    private int height;
    private int width;
    private int percentFilled;
    private boolean complete;
    private DisplayMetrics metrics;
    private float circleStroke;
    private float circleFine;
    private Paint paint;
    private RectF pcntFilledRect;

    public CircleProgressBar(Context context) {
        super(context);
        initMetrics(context);
        loadColors(context);
    }

    public CircleProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initMetrics(context);
        loadColors(context);
    }

    public CircleProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initMetrics(context);
        loadColors(context);
    }

    private void loadColors(Context context) {
        paint = new Paint();
        nullColor = ContextCompat.getColor(context, R.color.progressNull);
        inProgressColor = ContextCompat.getColor(context, R.color.progressInProgress);
        doneColor = ContextCompat.getColor(context, R.color.progressDone);
        errorColor = ContextCompat.getColor(context, R.color.progressError);
    }

    private final void initMetrics(Context context){
        metrics = context.getResources().getDisplayMetrics();
        circleStroke = metrics.density * 6F;
        circleFine = metrics.density * 2f;
        pcntFilledRect = new RectF(0, 0, 0, 0);
    }

    public void setPercentFilled(int percentFilled) {
        this.percentFilled = percentFilled;
        this.invalidate();
    }

    public int getPercentFilled() {
        return percentFilled;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
        this.invalidate();
    }

    public boolean getComplete() {
        return complete;
    }

    @Override
    protected void onMeasure(int widthSpecId, int heightSpecId) {
        this.height = View.MeasureSpec.getSize(heightSpecId);
        this.width = View.MeasureSpec.getSize(widthSpecId);
        setMeasuredDimension(this.width, this.height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float halfWidth = width / 2;
        float halfHeight = height / 2;
        float halfStroke = circleStroke / 2;
        float textSize = halfWidth * 0.75f;

        paint.setAntiAlias(true);
        paint.setColor(nullColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setTypeface(Typeface.SANS_SERIF);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(textSize);

        if (this.complete) {
            paint.setColor(doneColor);
            paint.setStrokeWidth(circleStroke);
            canvas.drawCircle(
                halfWidth, halfWidth,
                halfWidth - halfStroke - metrics.density * 2f,
                paint
            );
            drawDrawable(
                R.drawable.ic_done, halfWidth, halfWidth, doneColor, canvas
            );
        } else if (this.percentFilled < 0) {
            paint.setColor(errorColor);
            paint.setStrokeWidth(circleStroke);
            canvas.drawCircle(halfWidth, halfWidth, halfWidth - halfStroke - metrics.density * 2f, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText("?", halfWidth, halfWidth + textSize / 3f, paint);
        } else if (this.percentFilled == 0) {
            drawDrawable(
                R.drawable.ic_play, halfWidth, textSize, nullColor, canvas
            );
        } else {
            paint.setColor(inProgressColor);
            paint.setStrokeWidth(circleFine);
            canvas.drawCircle(halfWidth, halfWidth, halfWidth - halfStroke - 1f, paint);
            paint.setStrokeWidth(circleStroke);

            pcntFilledRect.set(
                0 + circleStroke, 0 + circleStroke,
                width - circleStroke , width - circleStroke
            );
            canvas.drawArc(
                pcntFilledRect, -90,  360F * percentFilled / 100F, false, paint
            );
            paint.setStyle(Paint.Style.FILL);
            textSize = halfWidth * 0.5f;
            paint.setTextSize(textSize);
            canvas.drawText(percentFilled+"%", halfWidth, halfHeight + textSize / 3f, paint);
        }
    }

    private void drawDrawable(
        int resId, float halfWidth, float diameter, int color, Canvas canvas
    ) {
        Drawable icon = ContextCompat.getDrawable(getContext(), resId);
        Drawable iconWrapped = DrawableCompat.wrap(icon);
        DrawableCompat.setTint(iconWrapped, color);
        iconWrapped.setBounds(
            (int) (halfWidth - 0.5 * diameter),
            (int) (halfWidth - 0.5 * diameter),
            (int) (halfWidth + 0.5 * diameter),
            (int) (halfWidth + 0.5 * diameter)
        );
        iconWrapped.draw(canvas);
    }

}
