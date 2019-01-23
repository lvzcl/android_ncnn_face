package com.mtcnn_as;

import java.io.IOException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.example.lvzcl.ncnn_demo.R;

public class CameraActivity extends Activity implements Camera.PreviewCallback{

    private TextureView textureView;
    private Camera mCamera;
    private boolean isPreview = false;
    private SurfaceTexture mSurfaceTexture;
    private Camera.AutoFocusCallback myAutoFocusCallback;
    private static final String TAG = "cameraActivity";

    private long timestamp = 0;
    private boolean isImageProcess = false;

    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);
        initView();


        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        myAutoFocusCallback = new Camera.AutoFocusCallback() {

            public void onAutoFocus(boolean success, Camera camera) {
                // TODO Auto-generated method stub
                if (success)//success表示对焦成功
                {
                    Log.i(TAG, "myAutoFocusCallback: success...");
                    //myCamera.setOneShotPreviewCallback(null);

                } else {
                    //未对焦成功
                    Log.i(TAG, "myAutoFocusCallback: 失败了...");

                }


            }

        };
    }

    private void initView() {
        textureView = (TextureView) this.findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(new MySurfaceTextureListener());

    }

    public void initCamera(){
        if(!isPreview && null != mSurfaceTexture){
            mCamera = Camera.open();
            mCamera.setDisplayOrientation(90);
        }
        Parameters parameters = mCamera.getParameters();
        Size mSize = parameters.getSupportedPreviewSizes().get(0);
        Log.i(TAG, String.valueOf(mSize.width)+ " * " +String.valueOf(mSize.height));
        parameters.setPreviewSize(mSize.width, mSize.height);
        parameters.setPreviewFpsRange(4, 10);
        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.setPictureSize(mSize.width, mSize.height);
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.setPreviewCallback(this);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //mCamera.setParameters(parameters);
        mCamera.startPreview();
        isPreview = true;
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (isImageProcess){
            Log.w(TAG, "DROP Images");
            return;
        }
        Camera.Parameters ps = camera.getParameters();

        int width = ps.getPreviewSize().width;
        int height = ps.getPreviewSize().height;

        //int[] imgs = new int[ps.getPreviewSize().width * ps.getPreviewSize().height];

        Bitmap bitmap = convertYUVtoRGB(bytes, width, height);

        camera.addCallbackBuffer(bytes);
        isImageProcess=false;
        processImage(bitmap);
        //drawContent();
    }


    public Bitmap convertYUVtoRGB(byte[] yuvData, int width, int height) {
        if (yuvType == null) {
            yuvType = new Type.Builder(rs, Element.U8(rs)).setX(yuvData.length);
            in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

            rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
            out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
        }
        in.copyFrom(yuvData);
        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(bmpout);
        return bmpout;
    }

    /**
     * 进行图片的处理
     */
    private void processImage(Bitmap bitmap){

        timestamp++;
        final long currTimeStamp = timestamp;
        Log.i(TAG, String.valueOf(timestamp));



        isImageProcess=false;
    }

    private void drawContent() {
        //锁定画布
        Canvas canvas = textureView.lockCanvas();
        //画内容
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        canvas.drawCircle(200, 300, 100, paint);
        textureView.unlockCanvasAndPost(canvas);
    }

    private final class MySurfaceTextureListener implements SurfaceTextureListener{

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                              int width, int height) {
            mSurfaceTexture = surface;
            initCamera();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            if(null != mCamera){
                if(isPreview){
                    mCamera.stopPreview();
                }
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
            return true;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {

        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // TODO Auto-generated method stub

        }
    }

}



