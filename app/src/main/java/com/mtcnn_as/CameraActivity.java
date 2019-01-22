package com.mtcnn_as;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.example.lvzcl.ncnn_demo.R;

import java.io.IOException;

/**
 * Created by lvzcl on 2019/1/22.
 */

public class CameraActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "CameraActivity";

    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private Button buttonReturn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        buttonReturn = (Button)findViewById(R.id.buttonReturn);
        buttonReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //释放相机资源后再进行跳转
                Intent intent = new Intent(CameraActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    // apk暂停时执行的动作：把相机关闭，避免占用导致其他应用无法使用相机
    protected void onPause() {
        super.onPause();

        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    @Override
    // 恢复apk时执行的动作
    protected void onResume() {
        super.onResume();
        if (null!=camera){
            camera = getCameraInstance();
            try {
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
            } catch(IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }
    }


    public void surfaceCreated(SurfaceHolder holder){
        camera = getCameraInstance();
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch(IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){
        refreshCamera();
        int rotation = getDisplayOrientation(); //获取当前窗口方向
        camera.setDisplayOrientation(rotation); //设定相机显示方向
    }

    public void surfaceDestroyed(SurfaceHolder holder){
        surfaceHolder.removeCallback(this);
        try {
            camera.setPreviewDisplay(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.stopPreview();
        camera.release();
        camera = null;
    }


    // 获取camera实例
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch(Exception e){
            Log.d("TAG", "camera is not available");
        }
        return c;
    }

    // 获取当前窗口管理器显示方向
    private int getDisplayOrientation(){
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation){
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        android.hardware.Camera.CameraInfo camInfo =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, camInfo);

        int result = (camInfo.orientation - degrees + 360) % 360;

        return result;
    }

    // 刷新相机
    private void refreshCamera(){
        if (surfaceHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch(Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {

        }
    }
}
