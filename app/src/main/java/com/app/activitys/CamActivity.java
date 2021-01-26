package com.app.activitys;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.app.R;
import com.app.activitys.Bluetooth.BluetoothDeviceListActivity;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Vector;

import static java.lang.Math.round;
import static org.opencv.core.Core.addWeighted;
import static org.opencv.core.Core.inRange;
import static org.opencv.imgproc.Imgproc.COLOR_HSV2BGR;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2HSV;
import static org.opencv.imgproc.Imgproc.CV_HOUGH_GRADIENT;
import static org.opencv.imgproc.Imgproc.cvtColor;

public class CamActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "CamActivity";
    JavaCameraView javaCameraView;

    Mat mRGBA, mBGR, hsvImg, hsvImgT;

    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(CamActivity.this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status) {
                case BaseLoaderCallback.SUCCESS: {
                    javaCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                }
            }
        }
    };

//    static {
//        if(OpenCVLoader.initDebug()) {
//            Log.d(TAG, "YESSSSSSSS");
//        } else {
//            Log.d(TAG, "NOOOOOOOOOOOOOOOOO");
//        }
//    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam);

        javaCameraView = findViewById(R.id.CameraView);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setMaxFrameSize(1280, 720);

        javaCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "YESSSSSSSS");
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        } else {
            Log.d(TAG, "NOOOOOOOOOOOOOOOOO");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_connect:
                BluetoothDeviceListActivity.startBluetoothDeviceSelect(this, 0);
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(width, height, CvType.CV_8UC4);
        mBGR = new Mat(width, height, CvType.CV_8UC3);
        hsvImg = new Mat(width, height, CvType.CV_8UC3);
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        cvtColor(mRGBA, hsvImg, COLOR_RGB2HSV);



        cvtColor(hsvImg, mBGR, COLOR_HSV2BGR);
        return mBGR;    // needs to be in BRG
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}