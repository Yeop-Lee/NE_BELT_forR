package com.yonsei.dclab.chart;

/**
 * Created by kevin on 23/08/2017.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.CubicLineChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

/**
 * Created by mingun.baek on 2016-03-10.
 */
public class Moi_Chart extends View {

    private final static String TAG = Moi_Chart.class.getSimpleName();
    private Context mContext;

    public int resid = 0;

    private String title = "BIA vs Time";
    private XYSeries mXYSeries;
    public GraphicalView mGraphicalView;
    private XYMultipleSeriesRenderer mXYMultipleSeriesRenderer;
    private XYMultipleSeriesDataset mXYMultipleSeriesDataset;

    private double MAXBIATime = 20; //250 = 0.250 * 4 * 10 /40
    private double mXAxisMax = MAXBIATime;

    public int staringPoint = 8500;
    public int yUpDown = 500;

    //private double MAXBIAValue = 6000;
    //private double MAXBIAValue = 80000;
    private double MAXBIAValue = staringPoint + yUpDown;
    private double MINBIAValue = staringPoint - yUpDown;
    private double mMinYValue = MINBIAValue;
    private double mMaxYValue = MAXBIAValue;


    public static final int TEXT_SIZE_XXXHDPI = 48;
    //public static final int TEXT_SIZE_XHDPI = 36;
    public static final int TEXT_SIZE_XHDPI = 28;
    public static final int TEXT_SIZE_HDPI = 30;
    public static final int TEXT_SIZE_MDPI = 26;
    public static final int TEXT_SIZE_LDPI = 20;

    private GestureDetector gestureDetector;
    private boolean pause;

    public void setPoint(int setpoint){
        staringPoint = setpoint;
    }
    public Moi_Chart(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.mContext = context;

        mXYSeries = new XYSeries("");

        mXYMultipleSeriesDataset = new XYMultipleSeriesDataset();
        mXYMultipleSeriesDataset.addSeries(mXYSeries);

        mXYMultipleSeriesRenderer = buildRenderer(0xff7d7d7d);

        final String[] types = new String[] { CubicLineChart.TYPE};
        mGraphicalView = ChartFactory.getCombinedXYChartView(mContext, mXYMultipleSeriesDataset, mXYMultipleSeriesRenderer, types);
        mGraphicalView.setBackgroundColor(Color.TRANSPARENT);

        gestureDetector = new GestureDetector(context, new GestureListener());
        pause = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //drawBackground(canvas);
        mGraphicalView.draw(canvas);
        invalidate();
    }

    @Override
    public void setBackgroundResource(int resid) {
        this.resid = resid;
        invalidate();
    }

    public void setColor(int colorID) {

        mMaxYValue = MAXBIAValue;
        mXAxisMax = MAXBIATime;

        mXYSeries = new XYSeries("");

        mXYMultipleSeriesDataset = new XYMultipleSeriesDataset();
        mXYMultipleSeriesDataset.addSeries(mXYSeries);

        mXYMultipleSeriesRenderer = buildRenderer(colorID);

        final String[] types;
        types = new String[]{ CubicLineChart.TYPE };
        mGraphicalView = ChartFactory.getCombinedXYChartView(mContext, mXYMultipleSeriesDataset, mXYMultipleSeriesRenderer, types);
        //mGraphicalView = ChartFactory.getLineChartView(mContext, mXYMultipleSeriesDataset, mXYMultipleSeriesRenderer);
        mGraphicalView.setBackgroundColor(Color.TRANSPARENT);
        Log.d(TAG, String.format("MyDEBUG: Set Color"));
    }

    public XYMultipleSeriesRenderer buildRenderer(int color) {

        int color1, color2;
        color1 = color;
        color2 = (color1 & 0x00ffffff) + 0x80000000;

        XYSeriesRenderer tXYSeriesRenderer = new XYSeriesRenderer();
        tXYSeriesRenderer.setColor(color1);
        tXYSeriesRenderer.setLineWidth(3);
        //tXYSeriesRenderer.setPointStyle(PointStyle.CIRCLE);
        //tXYSeriesRenderer.setFillPoints(true);

        XYMultipleSeriesRenderer tXYMultipleSeriesRenderer = new XYMultipleSeriesRenderer();
        tXYMultipleSeriesRenderer.addSeriesRenderer(tXYSeriesRenderer);

        tXYMultipleSeriesRenderer.setXTitle("Moisture/Time (sec.)");

//        tXYMultipleSeriesRenderer.setYTitle("Moi - Value");

        tXYMultipleSeriesRenderer.setXAxisMin(0);
        tXYMultipleSeriesRenderer.setXAxisMax(mXAxisMax);
        tXYMultipleSeriesRenderer.setYAxisMin(mMinYValue);
        tXYMultipleSeriesRenderer.setYAxisMax(mMaxYValue);

        tXYMultipleSeriesRenderer.setAxesColor(color1);
        tXYMultipleSeriesRenderer.setLabelsColor(color1);
        tXYMultipleSeriesRenderer.setXLabelsColor(color1);
        tXYMultipleSeriesRenderer.setYLabelsColor(0, color1);
        tXYMultipleSeriesRenderer.setYLabelsAlign(Paint.Align.RIGHT);

        switch (getResources().getDisplayMetrics().densityDpi) {
            case DisplayMetrics.DENSITY_XXXHIGH:
                tXYMultipleSeriesRenderer.setMargins(new int[] { 60, 80, 15, 15 });
                tXYMultipleSeriesRenderer.setAxisTitleTextSize(TEXT_SIZE_XXXHDPI);
                tXYMultipleSeriesRenderer.setChartTitleTextSize(TEXT_SIZE_XXXHDPI);
                tXYMultipleSeriesRenderer.setLabelsTextSize((float)(TEXT_SIZE_XXXHDPI*0.7));
                tXYMultipleSeriesRenderer.setLegendTextSize(TEXT_SIZE_XXXHDPI);
                break;
            case DisplayMetrics.DENSITY_XHIGH:
                //tXYMultipleSeriesRenderer.setMargins(new int[] { 80, 120, 50, 20 });
                tXYMultipleSeriesRenderer.setMargins(new int[] { 60, 80, 40, 20 });
                tXYMultipleSeriesRenderer.setAxisTitleTextSize(TEXT_SIZE_XHDPI);
                tXYMultipleSeriesRenderer.setChartTitleTextSize(TEXT_SIZE_XHDPI);
                tXYMultipleSeriesRenderer.setLabelsTextSize((float)(TEXT_SIZE_XHDPI*0.7));
                tXYMultipleSeriesRenderer.setLegendTextSize(TEXT_SIZE_XHDPI);
                break;
            case DisplayMetrics.DENSITY_HIGH:
                tXYMultipleSeriesRenderer.setMargins(new int[] { 60, 60, 40, 20 });
                tXYMultipleSeriesRenderer.setAxisTitleTextSize(TEXT_SIZE_HDPI);
                tXYMultipleSeriesRenderer.setChartTitleTextSize(TEXT_SIZE_HDPI);
                tXYMultipleSeriesRenderer.setLabelsTextSize((float)(TEXT_SIZE_HDPI*0.7));
                tXYMultipleSeriesRenderer.setLegendTextSize(TEXT_SIZE_HDPI);
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                tXYMultipleSeriesRenderer.setMargins(new int[] { 60, 60, 40, 20 });
                tXYMultipleSeriesRenderer.setAxisTitleTextSize(TEXT_SIZE_MDPI);
                tXYMultipleSeriesRenderer.setChartTitleTextSize(TEXT_SIZE_MDPI);
                tXYMultipleSeriesRenderer.setLabelsTextSize((float)(TEXT_SIZE_MDPI*0.7));
                tXYMultipleSeriesRenderer.setLegendTextSize(TEXT_SIZE_MDPI);
                break;
            default:
                tXYMultipleSeriesRenderer.setMargins(new int[] { 60, 60, 40, 20 });
                tXYMultipleSeriesRenderer.setAxisTitleTextSize(TEXT_SIZE_LDPI);
                tXYMultipleSeriesRenderer.setChartTitleTextSize(TEXT_SIZE_LDPI);
                tXYMultipleSeriesRenderer.setLabelsTextSize(TEXT_SIZE_LDPI);
                tXYMultipleSeriesRenderer.setLegendTextSize(TEXT_SIZE_LDPI);
                break;
        }

        tXYMultipleSeriesRenderer.setShowGrid(true);
        tXYMultipleSeriesRenderer.setGridColor(color2);
        //tXYMultipleSeriesRenderer.setXLabels(10);
        //tXYMultipleSeriesRenderer.setYLabels(10);

        tXYMultipleSeriesRenderer.setPanEnabled(false, false);
        tXYMultipleSeriesRenderer.setZoomEnabled(false, false);

        tXYMultipleSeriesRenderer.setShowAxes(true);
        tXYMultipleSeriesRenderer.setShowLabels(true);
        tXYMultipleSeriesRenderer.setShowLegend(false);

        //tXYMultipleSeriesRenderer.setMargins(new int[] { 40, 90, 25, 10 });
        tXYMultipleSeriesRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
        tXYMultipleSeriesRenderer.setApplyBackgroundColor(true);
        tXYMultipleSeriesRenderer.setBackgroundColor(Color.TRANSPARENT);

        return tXYMultipleSeriesRenderer;
    }

    public void updateChart(int[] biaVal) {

//        Log.d(TAG, String.format("MyDEBUG: Update Chart"));
        // if (pause) return;
        mXYSeries.clear();

        for (int i = 0; i < biaVal.length; i++) {
            if (biaVal[i] > (int)MAXBIAValue) biaVal[i] = (int)MAXBIAValue;
        }

        for (int i = 0; i < biaVal.length; i++) {
            if (biaVal[i] < (int)MINBIAValue) biaVal[i] = (int)MINBIAValue;
        }

        for (int i = 0; i < biaVal.length; i++) {
            double paramX = (double) i * mXAxisMax / (double) biaVal.length;
            double paramY = (double) biaVal[i];
            mXYSeries.add(paramX, paramY);
        }

        mGraphicalView.repaint();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        //Single Tap
        return gestureDetector.onTouchEvent(e);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            //pause = !pause;
            return true;
        }
        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            //  pause = !pause;
            return true;
        }
    }
}
