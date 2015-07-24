package com.ramesh.rvcam;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RV_Camera extends Activity {
    private TCPServerThread testServer;
    private Thread mServerThread;

    private TextureView mTextureView;
    private Size mPreviewSize;
    private CameraDevice mCamDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;
    private Button mBtnShot;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

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

    private CameraCaptureSession.StateCallback mCameraCaptureSessionCallback  = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            mPreviewSession = session;
            updatePreview();
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Toast.makeText(RV_Camera.this, "onConfigureFailed", Toast.LENGTH_LONG).show();
        }
    };

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

            mCamDevice.createCaptureSession(Arrays.asList(surface), mCameraCaptureSessionCallback, null);
        } catch(CameraAccessException e) {
            e.printStackTrace();
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

        testServer = new TCPServerThread();
        mServerThread = new Thread(testServer);
        mServerThread.start();

        mTextureView = (TextureView) findViewById(R.id.previewTexture);

        //Set surface listener
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

        mBtnShot = (Button)findViewById(R.id.btn_takepicture);
        mBtnShot.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.e("RV...", "mBtnShot clicked");
                takePicture();
            }
        });

    }

    @Override
    protected void onPause() {

        Log.e("RV....", "onPause");
        if (null != mCamDevice) {
            mCamDevice.close();
            mCamDevice = null;
        }
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("RV...", "onResume");
        startBackgroundThread();
    }

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;



    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    private final File file = new File(Environment.getExternalStorageDirectory()+"/DCIM", "rv_pic.jpg");

    private ImageReader.OnImageAvailableListener mReadListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image img = null;

            try {
                img = reader.acquireLatestImage();
                ByteBuffer buf = img.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buf.capacity()];
                buf.get(bytes);
                testServer.sendSnapshotResponse(4, bytes);
                save(bytes);
            }
            catch(FileNotFoundException e) {
                e.printStackTrace();
            } catch(IOException e) {
                e.printStackTrace();
            }
            finally {
                if (img != null) {
                    img.close();
                }
            }
        }
        private void save(byte[] bytes) throws IOException {
            OutputStream output = null;
            try {
                output = new FileOutputStream(file);
                output.write(bytes);
            } finally {
                if (null != output) {
                    output.close();
                }
            }
        }
    };


    private final CameraCaptureSession.CaptureCallback mCaptureListener = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request, TotalCaptureResult result) {

            super.onCaptureCompleted(session, request, result);
            Toast.makeText(RV_Camera.this, "Saved:"+file, Toast.LENGTH_SHORT).show();
            startPreview();
        }

    };


    public void takePicture() {
        if(null == mCamDevice){
            Log.e("RV...", "Camera device is null");
            return;
        }

        CameraManager camMgr = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try{
            CameraCharacteristics  camCharacteristics = camMgr.getCameraCharacteristics(mCamDevice.getId());

            Size[] jpegSizes = null;
            if(camCharacteristics != null) {
                jpegSizes = camCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }

            int width = 640;
            int height = 480;

            if(jpegSizes != null && jpegSizes.length > 0) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }

            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);

            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));


            // get request builder
            final CaptureRequest.Builder pictureBuilder = mCamDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            // add reader target
            pictureBuilder.addTarget(reader.getSurface());

            // set builder property
            pictureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // set builder property - rotation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            pictureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));


            reader.setOnImageAvailableListener(mReadListener, mBackgroundHandler);


            mCamDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession session) {

                    try {
                        session.capture(pictureBuilder.build(), mCaptureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {

                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {}

            }, mBackgroundHandler);

        } catch(CameraAccessException e) {
            e.printStackTrace();
        }
    }
}