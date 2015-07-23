package com.ramesh.rvcam;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Arrays;


public class RV_Camera extends Activity {
    private TextureView mTextureView;
    private Size mPreviewSize;
    private CameraDevice mCamDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;


    protected void updatePreview() {
        if(null == mCamDevice) {
            Log.e("RV...", "Update preview error");
        }

        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);

        HandlerThread thread = new HandlerThread("CameraPreview");
        thread.start();
        Handler backgroundHandler = new Handler(thread.getLooper());

        try {
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void startPreview(){
        if(null == mCamDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            Log.e("RV....", "StartPreview fail");
            return;
        }

        //get texture associated with texture view
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        if(null == texture) {
            Log.e("RV...", "Texture is null");
        }

        //set size of texture
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        //create surface for the texture
        Surface surface = new Surface(texture);

        try {
            mPreviewBuilder = mCamDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
        mPreviewBuilder.addTarget(surface);

        try {

            mCamDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mPreviewSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    //Toast.makeText(RV_Camera.this, "onConfigureFailed", Toast.LENGTH_LONG).show();
                }
            }, null);
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onPause() {

        Log.e("RV....", "onPause");
        super.onPause();
        if (null != mCamDevice) {
            mCamDevice.close();
            mCamDevice = null;
        }
    }

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camDevice) {
            mCamDevice = camDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camDevice) {
        }

        @Override
        public void onError(CameraDevice camDevice, int error) {
        }
    };

    private void openCamera() {

        try {
            //get an instance of CameraManager
            CameraManager camMgr = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            //get Camera ID, if 0 it is back camera and 1 is front camera
            String camId = camMgr.getCameraIdList()[0];

            //get Camera Characteristics
            CameraCharacteristics camCharacteristics = camMgr.getCameraCharacteristics(camId);

            //get Stream config map
            StreamConfigurationMap map = camCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            //get preview size
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            //open the camera
            camMgr.openCamera(camId, mStateCallback, null);
        } catch(CameraAccessException e) {
            e.printStackTrace();
        }

    }
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_rv__camera);

        mTextureView = (TextureView) findViewById(R.id.previewTexture);

        //Set surface listener
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

    }
}