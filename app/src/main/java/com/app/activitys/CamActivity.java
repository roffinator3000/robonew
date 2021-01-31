package com.app.activitys;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import com.app.R;
import com.app.activitys.Bluetooth.BluetoothDeviceListActivity;

import static java.lang.Math.round;
import static org.opencv.core.Core.addWeighted;
import static org.opencv.core.Core.bitwise_and;
import static org.opencv.core.Core.bitwise_not;
import static org.opencv.core.Core.bitwise_or;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2HSV;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2BGR;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2HSV;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.HoughCircles;
import static org.opencv.imgproc.Imgproc.circle;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.rectangle;

public class CamActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "CamActivity";
    JavaCameraView javaCameraView;

    Mat mRGBA, mBGR, hsvImg, hsvImgT;
    Mat maskUninv, maskU, mask;
    private boolean togglePic = true;

    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(CamActivity.this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
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

    //    static
//    {
//
//        if(OpenCVLoader.initDebug())
//        {
//            Log.d(TAG, "YESSSSSSSS");
//        }
//        else{
//            Log.d(TAG, "NOOOOOOOOOOOOOOOOO");
//        }
//
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

        if (javaCameraView != null) {
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


    //---------------------------------------------------------------
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
        mBGR = new Mat(width, height, CvType.CV_8UC4);
        hsvImg = new Mat(width, height, CvType.CV_8UC4);

        maskUninv = new Mat(width, height, CvType.CV_8UC4);
        mask = new Mat(width, height, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if (togglePic) {
            mRGBA = inputFrame.rgba();
            cvtColor(mRGBA, hsvImg, COLOR_RGB2HSV);
//            cvtColor(mRGBA, hsvImg, COLOR_BGR2HSV);

            Scalar scalarLow = getHsvScalar(205, 30, 5);
            Scalar scalarHigh = getHsvScalar(290, 400, 400);

            Core.inRange(hsvImg, scalarLow, scalarHigh, mask);

            bitwise_not(mask, mask);                  // zum debuggen
            bitwise_and(mRGBA, mRGBA, hsvImg, mask);    // farbbild auf filter legen
//            cvtColor(hsvImg, hsvImg, COLOR_RGB2BGR);  // zum debuggen, da die erkennung auf falschfarben (rgb <-> bgr) läuft


            //"schwerpunkt" der erkannten roten flächen berechnen
            int h = mask.rows();
            int w = mask.width();
            int avH = 0;
            int avW = 0;   //average height and width
            int count = 1;

            for (int i = 0; i<h; i += 2) {
                for (int ii = 0; ii<w; ii += 2) {
                    if (0 < mask.get(i,ii)[0]) {
                    avH += i;
                    avW += ii;
                    ++count;
                    }
                }
            }
            avH /= count;
            avW /= count;

            // ecken um den schwerpunkt setzen
            double[] aPu = new double[2];
            aPu[0] = avW - 30;
            aPu[1] = avH - 30;
            Point lo = new Point(aPu);
            aPu[0] = avW + 30;
            aPu[1] = avH + 30;
            Point ru = new Point(aPu);

            Scalar green = getHsvScalar(140, 100, 100); // ein grünes
            rectangle(hsvImg, lo, ru, green,3);          // quadrat über das erzeugt bild legen
        }
        togglePic = !togglePic;
        return hsvImg;
    }

    private Scalar getHsvScalar(double H, double S, double V) {
        return new Scalar(((int)(H/360.0*255.0)), ((int)(S*2.55)), ((int)(V*2.55)));
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}