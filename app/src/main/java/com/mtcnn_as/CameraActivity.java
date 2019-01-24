package com.mtcnn_as;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.ImageView;
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

    private int CameraWidth = 1280;
    private int CameraHeight = 720;

    private MTCNN mtcnn = new MTCNN();

    //private SVDraw svdraw;

    ImageView imageView;
    private boolean Is_Save = true;

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

        imageView = (ImageView) findViewById(R.id.camera_imageview);
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
        camera.addCallbackBuffer(bytes);
        isImageProcess=true;
        Camera.Parameters ps = camera.getParameters();


        int width = ps.getPreviewSize().width;
        int height = ps.getPreviewSize().height;

        //int[] imgs = new int[ps.getPreviewSize().width * ps.getPreviewSize().height];

        Bitmap bitmap = convertYUVtoRGB(bytes, width, height);

        //旋转90度
        bitmap = rotate_bitmap(bitmap);
        bitmap = compress_bitmap(bitmap);
        int[] predict_Info = processImage(bitmap);
        isImageProcess = false;

    }

    public Bitmap rotate_bitmap(Bitmap bitmap){
        Matrix m = new Matrix();
        m.postRotate(90);

        Bitmap res = Bitmap.createBitmap(bitmap, 0,0, bitmap.getWidth(),  bitmap.getHeight(), m, true);

        return res;
    }

    public Bitmap compress_bitmap(Bitmap bitmap){
        int REQUIRED_SIZE = 400;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int scale = 1;
        while (true) {
            if (width / 2 < REQUIRED_SIZE
                    || height / 2 < REQUIRED_SIZE) {
                break;
            }
            width /= 2;
            height /= 2;
            scale *= 2;
        }
        Matrix matrix = new Matrix();
        matrix.setScale((float)(1.0/scale), (float)(1.0/scale));
        Bitmap res = Bitmap.createBitmap(bitmap, 0, 0,bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);

        return bitmap;
    }


    /**
     *将camera的YUV数据格式转换成bitmap
     * @param yuvData
     * @param width
     * @param height
     * @return
     */
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

        faceInfo = mtcnn.FaceDetect(imageDate, width, height, 4);

        timeDetectFace = System.currentTimeMillis() - timeDetectFace;
        Log.i(TAG, "人脸平均检测时间："+timeDetectFace/testTimeCount);


        if (faceInfo.length > 1) {
            int faceNum = faceInfo[0];
            Log.i(TAG, "检测人脸数目为" + faceInfo[0]);
            Bitmap drawBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            for(int i=0;i<faceNum;i++) {
                int left, top, right, bottom;
                Canvas canvas = new Canvas(drawBitmap);
                Paint paint = new Paint();
                left = faceInfo[1+14*i];
                top = faceInfo[2+14*i];
                right = faceInfo[3+14*i];
                bottom = faceInfo[4+14*i];
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);//不填充
                paint.setStrokeWidth(5);  //线的宽度
                canvas.drawRect(left, top, right, bottom, paint);
                //画特征点
                canvas.drawPoints(new float[]{faceInfo[5+14*i],faceInfo[10+14*i],
                        faceInfo[6+14*i],faceInfo[11+14*i],
                        faceInfo[7+14*i],faceInfo[12+14*i],
                        faceInfo[8+14*i],faceInfo[13+14*i],
                        faceInfo[9+14*i],faceInfo[14+14*i]}, paint);//画多个点
            }
            imageView.setImageBitmap(drawBitmap);
        }else{
            imageView.setImageBitmap(bitmap);
        }
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



