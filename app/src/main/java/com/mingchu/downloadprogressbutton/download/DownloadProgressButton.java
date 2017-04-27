package com.mingchu.downloadprogressbutton.download;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.mingchu.downloadprogressbutton.R;


/**
 * 下载按钮(可以显示进度条、暂停、下载中状态)
 */

public class DownloadProgressButton extends View {

    private int statue = downloadStatue; //下载状态
    public static final int downloadStatue = 0;
    public static final int progressStatue = 1;
    public static final int textStatue = 2;
    public static final int pausedStatue = 3;


    private int mViewWidth;// 控件的宽度
    private int mViewHeight;// 控件的高度

    private int mViewCenterX;
    private int mViewCenterY;

    private int radius;// 圆形的半径
    private int socktwidth = dp2px(2);// 圆环进度条的宽度
    private Paint paint = new Paint();
    private int progress = 70;// 百分比0~100;
    private int textSize = dp2px(10);// 文字大小

    @Deprecated
    float scale = 0.35f;// 中间背景图片相对圆环的大小的比例

    private int preColor = Color.parseColor("#ffffff");// 进度条未完成的颜色
    private int progressColor = Color.parseColor("#2DB6E6");// 进度条颜色
    private float paddingscale = 0.8f;// 控件内偏距占空间本身的比例
    private int CircleColor = Color.parseColor("#ffffff");// 圆中间的背景颜色
    private int whiteColor = Color.parseColor("#ffffff");// 矩形中间的背景颜色
    private int textColor = progressColor;// 文字颜色


    private onProgressListener monProgress;// 进度时间监听
    private int startAngle = 270;
    RectF rectf = new RectF();

    private Bitmap bitmpre;

    private Bitmap bitmapStop;
    private Bitmap bitmapPaused;


    private String mText = "运行";

    public DownloadProgressButton(Context context, AttributeSet attrs) {
        super(context, attrs);
//        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DownloadProgressButton);


        bitmapStop = BitmapFactory.decodeResource(getResources(), R.mipmap.icn_sque_64);
        bitmpre = BitmapFactory.decodeResource(getResources(), R.mipmap.icn_download_cloud);
        bitmapPaused = BitmapFactory.decodeResource(getResources(), R.mipmap.icn_paused_64);

        mViewWidth = bitmpre.getWidth();
        mViewHeight = bitmpre.getHeight();

        mViewCenterX = mViewWidth / 2;
        mViewCenterY = mViewHeight / 2;

    }

    public DownloadProgressButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec); //宽度测量规则
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);//宽度大小
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);//高度测量规则
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);//高度大小
        int currWidth;
        int currHeight;
        if (widthMode == MeasureSpec.EXACTLY) {
            currWidth = widthSize;
        } else {
            int desired = (int) (getPaddingLeft() + mViewWidth + getPaddingRight());
            currWidth = desired;
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            currHeight = heightSize;
        } else {
            int desired = (int) (getPaddingTop() + mViewHeight + getPaddingBottom());
            currHeight = desired;
        }
        setMeasuredDimension(currWidth, currHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        switch (statue) {
            case downloadStatue:
                drawDownload(canvas);
                break;
            case progressStatue:
                drawProcressOrPaused(canvas, bitmapStop);
                break;
            case textStatue:
                drawText(canvas, mText);
                break;
            case pausedStatue:
                drawProcressOrPaused(canvas, bitmapPaused);
                break;
        }
        super.onDraw(canvas);
    }

    private void drawDownload(Canvas canvas) {

        drawOutsideCircle(canvas);

//        图片缩小显示
        drawIcon(canvas, bitmpre);

    }


    /**
     * 绘制下载进度条或者停止状态
     *
     * @param canvas 画笔
     * @param icon   bitmap
     */
    private void drawProcressOrPaused(Canvas canvas, Bitmap icon) {

        drawOutsideCircle(canvas);

        rectf.set((mViewWidth - radius * 2) / 2f, (mViewHeight - radius * 2) / 2f,
                ((mViewWidth - radius * 2) / 2f) + (2 * radius),
                ((mViewHeight - radius * 2) / 2f) + (2 * radius));

        paint.setColor(progressColor);
        canvas.drawArc(rectf, startAngle, progress * 3.6f, true, paint);
        paint.setColor(CircleColor);
        // 绘制用于遮住伞形两个边的小圆
        canvas.drawCircle(mViewWidth / 2, mViewHeight / 2, radius - socktwidth, paint);

        drawIcon(canvas, icon);
    }


    /**
     * 绘制字体
     * @param canvas  画笔
     * @param text  需要绘制的文字
     */
    private void drawText(Canvas canvas, String text) {


        int border = 1;
        int h = bitmpre.getHeight() / 2;
        RectF outRect = new RectF(0, h / 2, mViewWidth, h / 2 + h);
        RectF inRect = new RectF(border, h / 2 + border, mViewWidth - border, h / 2 + h - border);
        paint.setAntiAlias(true);
        paint.setColor(progressColor);
        // 绘制外矩形
        canvas.drawRoundRect(outRect, 5, 5, paint);
        paint.setColor(whiteColor);
        // 绘制内矩形
        canvas.drawRoundRect(inRect, 5, 5, paint);

        paint.setColor(textColor);
        paint.setTextSize(textSize);
        // 绘制中间文字
        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        int baseline = ((int) (inRect.bottom + inRect.top) - fontMetrics.bottom - fontMetrics.top) / 2;
        // 下面这行是实现水平居中，drawText对应改为传入targetRect.centerX()
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, inRect.centerX(), baseline, paint);


    }

    /**
     * 绘制进度条的外圆
     *
     * @param canvas 画笔
     */
    private void drawOutsideCircle(Canvas canvas) {


        int size = mViewHeight;
        if (mViewHeight > mViewWidth)
            size = mViewWidth;
        radius = (int) (size * paddingscale / 2f);
//

        paint.setAntiAlias(true);
        paint.setColor(progressColor);
        canvas.drawCircle(mViewCenterX, mViewCenterY, radius + 2, paint);//画最外面的边线
        paint.setColor(preColor);
        // 绘制最大的圆 进度条圆环的背景颜色（未走到的进度）就是这个哦
        canvas.drawCircle(mViewCenterX, mViewCenterY, radius, paint);


    }

    /**
     * 绘制icon
     * @param canvas 画笔
     * @param bitmap bitmap
     */
    private void drawIcon(Canvas canvas, Bitmap bitmap) {


        rectf.set((mViewWidth - radius * 2) / 2f, (mViewHeight - radius * 2) / 2f,
                ((mViewWidth - radius * 2) / 2f) + (2 * radius),
                ((mViewHeight - radius * 2) / 2f) + (2 * radius));


        int width2 = (int) (rectf.width() * scale);
        int height2 = (int) (rectf.height() * scale);
        rectf.set(rectf.left + width2, rectf.top + height2, rectf.right
                - width2, rectf.bottom - height2);
        canvas.drawBitmap(bitmap, null, rectf, null);
    }


    /**
     * dp 转 px
     * @param dp  需要转换的dp数值
     * @return  px(int)
     */
    private int dp2px(int dp) {
        return (int) ((getResources().getDisplayMetrics().density * dp) + 0.5);
    }


    /**
     * 设置字体
     * @param text  需要设置的字体
     */
    public void setText(String text) {

        this.mText = text;
        setStatue(textStatue);
        invalidate();


    }


    /**
     * 进度
     */
    public void progress() {

        this.setStatue(progressStatue);
        this.progress = 0;
        invalidate();
    }

    /**
     * 下载状态
     */
    public void download() {

        this.setStatue(downloadStatue);
        invalidate();
    }

    /**
     * 暂停状态
     */
    public void paused() {

        this.setStatue(pausedStatue);
        invalidate();
    }


    /**
     * 设置进度
     *
     * @param progress <p>
     *                 ps: 百分比 0~100;
     */
    public void setProgress(int progress) {
        if (progress > 100)
            return;
        this.progress = progress;
        setStatue(progressStatue);
        invalidate();
        if (monProgress != null)
            monProgress.onProgress(progress);
    }

    /**
     * 设置状态
     */
    public void setStatue(int statue) {
        this.statue = statue;
        invalidate();
    }

    /**
     * 设置圆环进度条的宽度 px
     */
    public DownloadProgressButton setProgressWidth(int width) {
        this.socktwidth = width;
        return this;
    }

    /**
     * 设置文字大小
     *
     * @param value  文字大小值
     */
    public DownloadProgressButton setTextSize(int value) {
        textSize = value;
        return this;
    }

    /**
     * 设置文字大小
     */
    public DownloadProgressButton setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    /**
     * 设置进度条之前的颜色
     */
    public DownloadProgressButton setPreProgress(int precolor) {
        this.preColor = precolor;
        return this;
    }

    /**
     * 设置进度颜色
     *
     * @param color  颜色值
     */
    public DownloadProgressButton setProgressColor(int color) {
        this.progressColor = color;
        return this;
    }

    /**
     * 设置圆心中间的背景颜色
     *
     * @param color  颜色值
     * @return  DownloadProgressButton
     */
    public DownloadProgressButton setCircleBackgroud(int color) {
        this.CircleColor = color;
        return this;
    }

    /**
     * 设置圆相对整个控件的宽度或者高度的占用比例
     *
     * @param scale  占用比例
     */
    public DownloadProgressButton setPaddingscale(float scale) {
        this.paddingscale = scale;
        return this;
    }

    /**
     * 设置开始的位置
     *
     * @param startAngle 0~360
     *                   <p>
     *                   ps 0代表在最右边 90 最下方 按照然后顺时针旋转
     */
    public DownloadProgressButton setStartAngle(int startAngle) {
        this.startAngle = startAngle;
        return this;
    }

    /**
     * 进度条监听器
     */
    public interface onProgressListener {
        void onProgress(int value);
    }


}
