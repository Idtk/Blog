package com.example.administrator.customview.PieChart;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.example.administrator.customview.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by DoBest on 2016/4/15.
 * author : Idtk
 */
public class PieChart extends View {

    //画笔
    private Paint mPaint = new Paint();
    //宽高
    private int mWidth,mViewWidth;
    private int mHeight,mViewHeight;
    //数据
    private ArrayList<PieData> mPieData = new ArrayList<>();
    //饼状图初始绘制角度
    private float mStartAngle = 0;
    private RectF rectF=new RectF(),rectFTra = new RectF(),rectFIn = new RectF();
    private float r,rTra,rWhite;
    private RectF rectFF = new RectF(),rectFTraF = new RectF(),reatFWhite = new RectF();
    private float rF,rTraF,rWhiteF;
    //动画
    private ValueAnimator animator;
    private float animatedValue;
    private long animatorDuration = 5000;
    private TimeInterpolator timeInterpolator = new AccelerateDecelerateInterpolator();
    private boolean animatedFlag = true;
    //Touch
    private boolean touchFlag = true;
    private float[] pieAngles;
    private int angleId;
    private double offsetScaleRadius = 1.1;
    //圆环半径比例
    private double widthScaleRadius = 0.9;
    private double radiusScaleTransparent = 0.6;
    private double radiusScaleInside = 0.5;
    //Paint的字体大小
    private int percentTextSize = 45;
    private int centerTextSize = 60;

    //中间文字颜色
    private int centerTextColor = Color.BLACK;
    //百分比文字颜色
    private int percentTextColor = Color.WHITE;
    //百分比的小数位
    private int percentDecimal = 0;
    //饼图名
    private String name = "PieChart";
    //居中点
    private Point mPoint = new Point();
    //小于此角度在未点击状态下不显示百分比
    private float minAngle = 30;
    //引入Path
    private Path outPath = new Path();
    private Path midPath = new Path();
    private Path inPath = new Path();
    private Path outMidPath = new Path();
    private Path midInPath = new Path();
    //百分比最长字符
    private int stringId = 0;
    //百分比文字是否绘制
    private boolean percentFlag = true;


    public PieChart(Context context) {
        this(context,null);
    }

    public PieChart(Context context, @Nullable AttributeSet attrs) {
        this(context,attrs,0);
    }

    public PieChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs,defStyleAttr,0);
    }

    public PieChart(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context,attrs,defStyleAttr,defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = measureDimension(widthMeasureSpec);
        int height = measureDimension(heightMeasureSpec);
        setMeasuredDimension(width,height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w-getPaddingLeft()-getPaddingRight();
        mHeight = h-getPaddingTop()-getPaddingBottom();
        mViewWidth = w;
        mViewHeight = h;
        //标准圆环
        //圆弧
        r = (float) (Math.min(mWidth,mHeight)/2*widthScaleRadius);// 饼状图半径
        if (r>Math.min(mWidth,mHeight)){
            r =0;
            percentFlag = false;
            name = "";
        }
        // 饼状图绘制区域
        rectF.left = -r;
        rectF.top = -r;
        rectF.right =r;
        rectF.bottom = r;
        //白色圆弧
        //透明圆弧
        rTra = (float) (r*radiusScaleTransparent);
        rectFTra.left = -rTra;
        rectFTra.top = -rTra;
        rectFTra.right = rTra;
        rectFTra.bottom = rTra;
        //白色圆
        rWhite = (float) (r*radiusScaleInside);
        rectFIn.left = -rWhite;
        rectFIn.top = -rWhite;
        rectFIn.right = rWhite;
        rectFIn.bottom = rWhite;

        //浮出圆环
        //圆弧
        rF = (float) (Math.min(mWidth,mHeight)/2*widthScaleRadius*offsetScaleRadius);// 饼状图半径
        // 饼状图绘制区域
        rectFF.left = -rF;
        rectFF.top = -rF;
        rectFF.right = rF;
        rectFF.bottom = rF;
        //白色圆弧
        //透明圆弧
        rTraF = (float) (rF*radiusScaleTransparent);
        rectFTraF.left = -rTraF;
        rectFTraF.top = -rTraF;
        rectFTraF.right = rTraF;
        rectFTraF.bottom = rTraF;
        //白色扇形
        rWhiteF = (float) (rF*radiusScaleInside);
        reatFWhite.left = -rWhiteF;
        reatFWhite.top = -rWhiteF;
        reatFWhite.right = rWhiteF;
        reatFWhite.bottom = rWhiteF;

        //animated
        if (animatedFlag){
            initAnimator(animatorDuration);
        }else {
            animatedValue = 360f;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mPieData == null)
            return;
        float currentStartAngle = 0;// 当前起始角度
        canvas.translate(mViewWidth/2,mViewHeight/2);// 将画布坐标原点移动到中心位置

        canvas.save();
        canvas.rotate(mStartAngle);
        float drawAngle;
        for (int i=0; i<mPieData.size(); i++){
            PieData pie = mPieData.get(i);

            if (Math.min(pie.getAngle()-1,animatedValue-currentStartAngle)>=0){
                drawAngle = Math.min(pie.getAngle()-1,animatedValue-currentStartAngle);
            }else {
                drawAngle = 0;
            }
            if (i==angleId){
                drawArc(canvas,currentStartAngle,drawAngle,pie,rF,rTraF,rWhiteF,rectFF,rectFTraF,reatFWhite,mPaint);
            }else {
                drawArc(canvas,currentStartAngle,drawAngle,pie,r,rTra,rWhite,rectF,rectFTra,rectFIn,mPaint);
            }
            currentStartAngle += pie.getAngle();
        }
        canvas.restore();

        currentStartAngle = mStartAngle;
        //扇形百分比文字
        for (int i=0; i<mPieData.size(); i++){
            PieData pie = mPieData.get(i);
            mPaint.setColor(percentTextColor);
            mPaint.setTextSize(percentTextSize);
            mPaint.setTextAlign(Paint.Align.CENTER);
            NumberFormat numberFormat =NumberFormat.getPercentInstance();
            numberFormat.setMinimumFractionDigits(percentDecimal);
            //根据Paint的TextSize计算Y轴的值
            int textPathX;
            int textPathY;

            if (animatedValue>pieAngles[i]-pie.getAngle()/2&&percentFlag) {
                if (i == angleId) {
                    textPathX = (int) (Math.cos(Math.toRadians(currentStartAngle + (pie.getAngle() / 2))) * (rF + rTraF) / 2);
                    textPathY = (int) (Math.sin(Math.toRadians(currentStartAngle + (pie.getAngle() / 2))) * (rF + rTraF) / 2);
                    mPoint.x = textPathX;
                    mPoint.y = textPathY;
                    String[] strings = new String[]{pie.getName() + "", numberFormat.format(pie.getPercentage()) + ""};
                    if (strings.length == 2)
                        textCenter(strings, mPaint, canvas, mPoint, Paint.Align.CENTER);
                } else {
                    if (pie.getAngle() > minAngle) {
                        textPathX = (int) (Math.cos(Math.toRadians(currentStartAngle + (pie.getAngle() / 2))) * (r + rTra) / 2);
                        textPathY = (int) (Math.sin(Math.toRadians(currentStartAngle + (pie.getAngle() / 2))) * (r + rTra) / 2);
                        mPoint.x = textPathX;
                        mPoint.y = textPathY;
                        String[] strings = new String[]{numberFormat.format(pie.getPercentage()) + ""};
                        if (strings.length == 1)
                            textCenter(strings, mPaint, canvas, mPoint, Paint.Align.CENTER);
                    }
                }
                currentStartAngle += pie.getAngle();
            }
        }
        //饼图名
        mPaint.setColor(centerTextColor);
        mPaint.setTextSize(centerTextSize);
        mPaint.setTextAlign(Paint.Align.CENTER);
        //根据Paint的TextSize计算Y轴的值
        mPoint.x=0;
        mPoint.y=0;
        String[] strings = new String[]{name+""};
        if (strings.length==1)
        textCenter(strings,mPaint,canvas,mPoint, Paint.Align.CENTER);


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (touchFlag&&mPieData.size()>0){
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    float x = event.getX()-(mWidth/2);
                    float y = event.getY()-(mHeight/2);
                    float touchAngle = 0;
                    if (x<0&&y<0){
                        touchAngle += 180;
                    }else if (y<0&&x>0){
                        touchAngle += 360;
                    }else if (y>0&&x<0){
                        touchAngle += 180;
                    }
                    touchAngle +=Math.toDegrees(Math.atan(y/x));
                    touchAngle = touchAngle-mStartAngle;
                    if (touchAngle<0){
                        touchAngle = touchAngle+360;
                    }
                    float touchRadius = (float) Math.sqrt(y*y+x*x);
                    if (rTra< touchRadius && touchRadius< r){
                        angleId = -Arrays.binarySearch(pieAngles,(touchAngle))-1;
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    angleId = -1;
                    invalidate();
                    return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private void init(Context context,AttributeSet attrs, int defStyleAttr, int defStyleRes){
//        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PieChart, defStyleAttr,defStyleRes);
        int n = array.getIndexCount();
        for (int i=0; i<n; i++){
            switch (i){
                case R.styleable.PieChart_name:
                    name = array.getString(i);
                    break;
                case R.styleable.PieChart_percentDecimal:
                    percentDecimal = array.getInt(i,percentDecimal);
                    break;
                case R.styleable.PieChart_textSize:
                    percentTextSize = array.getDimensionPixelSize(i,percentTextSize);
                    break;
            }
        }
        array.recycle();

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);//抗锯齿
    }

    private void initDate(ArrayList<PieData> mPieData){

        float dataMax=0;
        if (mPieData ==null||mPieData.size()==0)
            return;
        pieAngles = new float[mPieData.size()];
        float sumValue = 0;
        for (int i=0; i<mPieData.size(); i++){
            PieData pie = mPieData.get(i);
            sumValue += pie.getValue();
        }

        float sumAngle = 0;
        for (int i=0; i<mPieData.size();i++){
            PieData pie = mPieData.get(i);
            float percentage = pie.getValue()/sumValue;
            float angle = percentage*360;
            pie.setPercentage(percentage);
            pie.setAngle(angle);
            sumAngle += angle;
            pieAngles[i]=sumAngle;

            NumberFormat numberFormat =NumberFormat.getPercentInstance();
            numberFormat.setMinimumFractionDigits(percentDecimal);
            if (dataMax<textWidth(numberFormat.format(pie.getPercentage()),percentTextSize,mPaint)){
                stringId = i;
            }
        }
        angleId = -1;
    }

    private float textWidth(String string, int size, Paint paint){
        paint.setTextSize(size);
        float textWidth =paint.measureText(string+"");
        return textWidth;
    }

    private void initAnimator(long duration){
        if (animator !=null &&animator.isRunning()){
            animator.cancel();
            animator.start();
        }else {
            animator=ValueAnimator.ofFloat(0,360).setDuration(duration);
            animator.setInterpolator(timeInterpolator);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    animatedValue = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            animator.start();
        }
    }

    private void textCenter(String[] strings, Paint paint, Canvas canvas, Point point, Paint.Align align){
        paint.setTextAlign(align);
        Paint.FontMetrics fontMetrics= paint.getFontMetrics();
        float top = fontMetrics.top;
        float bottom = fontMetrics.bottom;
        int length = strings.length;
        float total = (length-1)*(-top+bottom)+(-fontMetrics.ascent+fontMetrics.descent);
        float offset = total/2-bottom;
        for (int i = 0; i < length; i++) {
            float yAxis = -(length - i - 1) * (-top + bottom) + offset;
            canvas.drawText(strings[i], point.x, point.y + yAxis, paint);
//            Log.d("TAG",mPaint.measureText(strings[i])+":"+strings[i]);
        }
    }

    private void drawArc(Canvas canvas, float currentStartAngle, float drawAngle, PieData pie,
                         float outR, float midR, float inR, RectF outRectF, RectF midRectF, RectF inRectF,Paint paint){
        outPath.lineTo(outR*(float) Math.cos(Math.toRadians(currentStartAngle)),outR*(float) Math.sin(Math.toRadians(currentStartAngle)));
        outPath.arcTo(outRectF,currentStartAngle,drawAngle);
        midPath.lineTo(midR*(float) Math.cos(Math.toRadians(currentStartAngle)),midR*(float) Math.sin(Math.toRadians(currentStartAngle)));
        midPath.arcTo(midRectF,currentStartAngle,drawAngle);
        inPath.lineTo(inR*(float) Math.cos(Math.toRadians(currentStartAngle)),inR*(float) Math.sin(Math.toRadians(currentStartAngle)));
        inPath.arcTo(inRectF,currentStartAngle,drawAngle);
        outMidPath.op(outPath,midPath, Path.Op.DIFFERENCE);
        midInPath.op(midPath,inPath, Path.Op.DIFFERENCE);
        paint.setColor(pie.getColor());
        canvas.drawPath(outMidPath,paint);
        paint.setAlpha(0x80);//设置透明度
        canvas.drawPath(midInPath,paint);
        outPath.reset();
        midPath.reset();
        inPath.reset();
        outMidPath.reset();
        midInPath.reset();
    }

    private int measureWrap(Paint paint){
        float wrapSize;
        if (mPieData!=null&mPieData.size()>1){
            NumberFormat numberFormat =NumberFormat.getPercentInstance();
            numberFormat.setMinimumFractionDigits(percentDecimal);
            paint.setTextSize(percentTextSize);
            float percentWidth = paint.measureText(numberFormat.format(mPieData.get(stringId).getPercentage())+"");
            paint.setTextSize(centerTextSize);
            float nameWidth = paint.measureText(name+"");
            wrapSize = (percentWidth*4+nameWidth*1.0f)*(float) offsetScaleRadius;
        }else {
            wrapSize = 0;
        }
        return (int) wrapSize;
    }

    private int measureDimension(int measureSpec){
        int size;

        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode){
            case MeasureSpec.UNSPECIFIED:
                size = measureWrap(mPaint);
                break;
            case MeasureSpec.EXACTLY:
                size = specSize;
                break;
            case MeasureSpec.AT_MOST:
                size = Math.min(specSize,measureWrap(mPaint));
                break;
            default:
                size = measureWrap(mPaint);
                break;
        }
        return size;
    }

    /**
     * 设置起始角度
     * @param mStartAngle 起始角度
     */
    public void setStartAngle(float mStartAngle) {
        while (mStartAngle<0){
            mStartAngle = mStartAngle+360;
        }
        while (mStartAngle>360){
            mStartAngle = mStartAngle-360;
        }
        this.mStartAngle = mStartAngle;
    }

    /**
     * 设置数据
     * @param mPieData 数据
     */
    public void setPieData(ArrayList<PieData> mPieData) {
        this.mPieData = mPieData;
        initDate(mPieData);
    }

    /**
     * 是否显示点触效果
     * @param touchFlag 是否显示点触效果
     */
    public void setTouchFlag(boolean touchFlag) {
        this.touchFlag = touchFlag;
    }

    /**
     * 设置绘制圆环的动画时间
     * @param animatorDuration 动画时间
     */
    public void setAnimatorDuration(long animatorDuration) {
        this.animatorDuration = animatorDuration;
    }

    /**
     * 设置偏移扇形与原扇形的半径比例
     * @param offsetScaleRadius 点触扇形的偏移比例
     */
    public void setOffsetScaleRadius(double offsetScaleRadius) {
        this.offsetScaleRadius = offsetScaleRadius;
    }

    /**
     * 设置圆环外层园的半径与视图的宽度比
     * @param widthScaleRadius 外圆环半径与视图宽度比
     */
    public void setWidthScaleRadius(double widthScaleRadius) {
        this.widthScaleRadius = widthScaleRadius;
    }

    /**
     * 设置透明圆环与外圆环半径比
     * @param radiusScaleTransparent 透明圆环与外圆环半径比
     */
    public void setRadiusScaleTransparent(double radiusScaleTransparent) {
        this.radiusScaleTransparent = radiusScaleTransparent;
    }

    /**
     * 设置内部圆与外部圆环半径比
     * @param radiusScaleInside 内部圆与外部圆环半径比
     */
    public void setRadiusScaleInside(double radiusScaleInside) {
        this.radiusScaleInside = radiusScaleInside;
    }

    /**
     * 设置圆环显示的百分比画笔大小
     * @param percentTextSize 百分比画笔大小
     */
    public void setPercentTextSize(int percentTextSize) {
        this.percentTextSize = percentTextSize;
    }

    /**
     * 设置圆环显示的百分比字体颜色
     * @param percentTextColor 百分比字体颜色
     */
    public void setPercentTextColor(int percentTextColor) {
        this.percentTextColor = percentTextColor;
    }

    /**
     * 设置中心文字画笔大小
     * @param centerTextSize 中心文字画笔大小
     */
    public void setCenterTextSize(int centerTextSize) {
        this.centerTextSize = centerTextSize;
    }

    /**
     * 设置中心文字颜色
     * @param centerTextColor 中心文字颜色
     */
    public void setCenterTextColor(int centerTextColor) {
        this.centerTextColor = centerTextColor;
    }

    /**
     * 设置动画类型
     * @param timeInterpolator 动画类型
     */
    public void setTimeInterpolator(TimeInterpolator timeInterpolator) {
        this.timeInterpolator = timeInterpolator;
    }

    /**
     * 设置百分比的小数位
     * @param percentDecimal 百分比的小数位
     */
    public void setPercentDecimal(int percentDecimal) {
        this.percentDecimal = percentDecimal;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMinAngle(float minAngle) {
        this.minAngle = minAngle;
    }

    /**
     * 是否开启动画
     * @param animatedFlag 默认为true，开启
     */
    public void setAnimatedFlag(boolean animatedFlag) {
        this.animatedFlag = animatedFlag;
    }

    /**
     * 设置旋转的角度
     * @param animatedValue 默认由动画控制
     */
    public void setAnimatedValue(float animatedValue) {
        this.animatedValue = animatedValue;
    }

}
