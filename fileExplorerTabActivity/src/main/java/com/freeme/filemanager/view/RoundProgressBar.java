package com.freeme.filemanager.view;

import com.freeme.filemanager.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


public class RoundProgressBar extends View {
    
    private Paint paint;
    
    
    private int roundColor;
    
    
    private int roundProgressColor;
    
    
    private int textColor;
    
    
    private float textSize;
    
    private float roundWidth;
    
    private int dyrRoundWidth;
    
    
    private long max;
    
    private long Card;
    
    private long sdCardFrees;
    
    private boolean isInfo;
    
    private String sdCardUsed;
    
    private int isCard;
    
    private int DIF=11;
    
    private int startPosition = 122; 
    
    private long progress;
    
    private boolean textIsDisplayable;
    
    
    private int style;
    
    public static final int STROKE = 0;
    public static final int FILL = 1;

    //*/ freeme.liuhaoran , 20160726 , percent Infinity
    private int percent;

    private float textWidth;
    
    private static final int ZERO = 0;
    //*/
    
    public RoundProgressBar(Context context) {
        this(context, null);
    }

    public RoundProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public RoundProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        paint = new Paint();

        
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.RoundProgressBar);
        
        roundColor = mTypedArray.getColor(R.styleable.RoundProgressBar_roundColor, Color.RED);
        roundProgressColor = mTypedArray.getColor(R.styleable.RoundProgressBar_roundProgressColor, Color.GREEN);
        textColor = mTypedArray.getColor(R.styleable.RoundProgressBar_textColor, Color.GREEN);
        textSize = mTypedArray.getDimension(R.styleable.RoundProgressBar_textSize, 15);
        roundWidth = mTypedArray.getDimension(R.styleable.RoundProgressBar_roundWidth, 15);
//      max = mTypedArray.getInteger(R.styleable.RoundProgressBar_max, 100);
        textIsDisplayable = mTypedArray.getBoolean(R.styleable.RoundProgressBar_textIsDisplayable, true);
        style = mTypedArray.getInt(R.styleable.RoundProgressBar_style, 0);
        
        mTypedArray.recycle();
    }
    

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    
        int centre = getWidth()/2; 
        int radius = (int) (centre - roundWidth/2); 
        paint.setColor(getResources().getColor(R.color.roundProgressColorb)); 
        paint.setStyle(Paint.Style.STROKE); 
        paint.setStrokeWidth(roundWidth);
        paint.setAntiAlias(true);

        paint.setStrokeWidth(0); 
        paint.setColor(getResources().getColor(R.color.textColor));
        paint.setTypeface(Typeface.DEFAULT); 
        
        //*/ freeme.liuhaoran , 20160726 , percent Infinity
        if (max != 0) {
        percent = (int)(((float)progress / (float)max) * 100);
        textWidth = paint.measureText(percent + "%");
        }else {
        textWidth = paint.measureText(ZERO + "%");
        }
        //*/
        
        //*/  freeme.liuhaoran , 20160723 ,  version of the adapter
        if(style == STROKE){
            if(isInfo){
                dyrRoundWidth = getResources().getDimensionPixelSize(R.dimen.round_Large_Width);
                DIF = getResources().getDimensionPixelSize(R.dimen.round_Large_dif);
                startPosition = getResources().getDimensionPixelSize(R.dimen.roundprogressbar_startPosition);
                paint.setTextSize(getResources().getDimension(R.dimen.memory_percent_textsize));
                canvas.drawText(percent +getResources().getString(R.string.percent), centre - textWidth / 2 + getResources().getDimension(R.dimen.roundprogressbar_percent_left2), (centre + textSize/2)+ getResources().getDimension(R.dimen.roundprogressbar_Large_percent_top), paint);
            }else{
                dyrRoundWidth = getResources().getDimensionPixelSize(R.dimen.round_Small_Width);
                DIF = getResources().getDimensionPixelSize(R.dimen.round_Small_dif);
                startPosition = getResources().getDimensionPixelSize(R.dimen.roundprogressbar_startPosition);
                paint.setTextSize(getResources().getDimension(R.dimen.memory_percent_textsize));
                if(max!=0){
                        canvas.drawText(percent + getResources().getString(R.string.percent), centre - textWidth / 2 - getResources().getDimension(R.dimen.roundprogressbar_percent_left), (centre + textSize/2)+getResources().getDimension(R.dimen.roundprogressbar_percent_top), paint);
                }else{
                paint.setColor(getResources().getColor(R.color.textColor2));
                canvas.drawText(0 + getResources().getString(R.string.percent), centre - textWidth / 2 - getResources().getDimension(R.dimen.roundprogressbar_percent__zero_left), (centre + textSize/2)+getResources().getDimension(R.dimen.roundprogressbar_percent_top), paint);
                }
                paint.setTextSize(24);
                }
        }
        //*/

        paint.setStrokeWidth(dyrRoundWidth);
        paint.setColor(getResources().getColor(R.color.roundProgressColor));  
        RectF oval = new RectF(centre - radius+DIF, centre - radius+DIF, centre
                + radius-DIF, centre + radius-DIF);  
        Log.i("oval", "centre = " + centre+ "------" +  "DIF = " + DIF + "------" + "radius =" + radius);
        switch (style) {
        case STROKE:{
            paint.setStyle(Paint.Style.STROKE);

            if(max!=0&&(360 * (progress * 5) / (max * 6))!=0){
                canvas.drawArc(oval, startPosition, 360 * (progress * 5) / (max * 6), false, paint);
            }
            break;
        }
        }
        
    }
    
    
    public synchronized long getMax() {
        return max;
    }


    public synchronized void setMax(long max, long card, long sdCardFrees, int b, boolean info) {
        if(max < 0){
            throw new IllegalArgumentException("max not less than 0");
        }
        this.max = max;
        this.Card = card;
        this.sdCardFrees = sdCardFrees;
        this.isInfo = info;
        this.isCard = b;
    }

    public synchronized long getProgress() {
        return progress;
    }

    public synchronized void setProgress(long progress) {
        if(progress < 0){
            throw new IllegalArgumentException("progress not less than 0");
        }
        if(progress > max){
            progress = max;
        }
        if(progress <= max){
            this.progress = progress;
            postInvalidate();
        }
        
    }
    
    
    public int getCricleColor() {
        return roundColor;
    }

    public void setCricleColor(int cricleColor) {
        this.roundColor = cricleColor;
    }

    public int getCricleProgressColor() {
        return roundProgressColor;
    }

    public void setCricleProgressColor(int cricleProgressColor) {
        this.roundProgressColor = cricleProgressColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public float getRoundWidth() {
        return roundWidth;
    }

    public void setRoundWidth(float roundWidth) {
        this.roundWidth = roundWidth;
    }

    



}
