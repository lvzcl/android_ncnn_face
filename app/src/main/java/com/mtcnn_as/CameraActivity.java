package com.mtcnn_as;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.example.lvzcl.ncnn_demo.R;

import static android.content.ContentValues.TAG;

public class CameraActivity extends Activity implements Camera.PreviewCallback{

    private TextureView textureView;
    private SurfaceView surfaceView;


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

    private int CameraWidth = 960;
    private int CameraHeight = 540;

    private MTCNN mtcnn = new MTCNN();

    private SVDraw svdraw;


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
        parameters.setPreviewSize(CameraWidth, CameraHeight);
        parameters.setPreviewFpsRange(4, 10);
        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.setPictureSize(CameraWidth, CameraHeight);
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

        //int width = ps.getPreviewSize().width;
        //int height = ps.getPreviewSize().height;

        //int[] imgs = new int[ps.getPreviewSize().width * ps.getPreviewSize().height];

        Bitmap bitmap = convertYUVtoRGB(bytes, CameraWidth, CameraHeight);

        camera.addCallbackBuffer(bytes);
        isImageProcess=false;
        int[] predict_Info = processImage(bitmap);

        svdraw = (SVDraw) findViewById(R.id.svdraw);
        svdraw.setFaceInfo(predict_Info);
        svdraw.drawRect();

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
    private int[] processImage(Bitmap bitmap){

        if (bitmap==null){
            return null;
        }
        timestamp++;
        final long currTimeStamp = timestamp;
        Log.i(TAG, String.valueOf(timestamp));

        int threadsNumber = 4;
        int minFaceSize = 20;
        int testTimeCount = 2;

        mtcnn.SetMinFaceSize(minFaceSize);
        mtcnn.SetTimeCount(testTimeCount);
        mtcnn.SetThreadsNumber(threadsNumber);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.i(TAG, String.valueOf(width) + "*" + String.valueOf(height));

        byte[] imageDate = getPixelsRGBA(bitmap);

        long timeDetectFace = System.currentTimeMillis();
        int faceInfo[] = null;

        faceInfo = mtcnn.FaceDetect(imageDate, CameraWidth, CameraHeight, 4);
        Log.i(TAG, "检测所有人脸");

        timeDetectFace = System.currentTimeMillis() - timeDetectFace;
        Log.i(TAG, "人脸平均检测时间："+timeDetectFace/testTimeCount);

        return faceInfo;

    }


    //提取像素点
    private byte[] getPixelsRGBA(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer
        byte[] temp = buffer.array(); // Get the underlying array containing the

        return temp;
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



