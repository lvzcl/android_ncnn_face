package com.mtcnn_as;

/**
 * Created by lvzcl on 2019/1/24.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

/*定义一个画矩形框的类*/
public class SVDraw extends SurfaceView implements SurfaceHolder.Callback{

    private static final String TAG = "SVDRAW";

    protected SurfaceHolder sh;
    private int mWidth;
    private int mHeight;

    private int[] faceInfo = null;

    public void setFaceInfo(int[] info){
        faceInfo = info;
    }

    public SVDraw(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        sh = getHolder();
        sh.addCallback(this);
        sh.setFormat(PixelFormat.TRANSPARENT);
        setZOrderOnTop(true);
    }

    public void surfaceChanged(SurfaceHolder arg0, int arg1, int w, int h) {
        // TODO Auto-generated method stub
        mWidth = w;
        mHeight = h;
    }

    public void surfaceCreated(SurfaceHolder arg0) {
        // TODO Auto-generated method stub

    }

    public void surfaceDestroyed(SurfaceHolder arg0) {
        // TODO Auto-generated method stub

    }
    void clearDraw()
    {
        Canvas canvas = sh.lockCanvas();
        canvas.drawColor(Color.BLUE);
        sh.unlockCanvasAndPost(canvas);
    }
    public void drawLine()
    {
        Canvas canvas = sh.lockCanvas();
        canvas.drawColor(Color.TRANSPARENT);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.RED);
        p.setStyle(Style.STROKE);
        //canvas.drawPoint(100.0f, 100.0f, p);
        canvas.drawLine(0,110, 500, 110, p);
        canvas.drawCircle(110, 110, 10.0f, p);
        sh.unlockCanvasAndPost(canvas);

    }
    public void drawRect(){
        int faceNum = faceInfo[0];
        Log.i(TAG, String.valueOf(faceNum));
        if (faceInfo.length > 1) {
            Canvas canvas = sh.lockCanvas();
            canvas.drawColor(Color.TRANSPARENT);
            Paint p = new Paint();
            p.setAntiAlias(true);
            p.setColor(Color.RED);
            p.setStyle(Style.STROKE);
            for(int i=0;i<faceNum;i++) {
                int left, top, right, bottom;
                left = faceInfo[1+14*i];
                top = faceInfo[2+14*i];
                right = faceInfo[3+14*i];
                bottom = faceInfo[4+14*i];
                canvas.drawRect(left, top, right, bottom, p);
            }
            sh.unlockCanvasAndPost(canvas);
        }else{
            return;
        }
    }

}
