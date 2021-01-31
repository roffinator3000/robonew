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

import static org.opencv.core.Core.bitwise_and;
import static org.opencv.core.Core.bitwise_not;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2HSV;

import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.rectangle;

public class CamActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static com.app.activitys.SeekBarSpeed mySeekBarSpeed;

    private static String TAG = "CamActivity";
    JavaCameraView javaCameraView;

    Mat mRGBA, mBGR, hsvImg, hsvImgT;
    Mat mask;
    Scalar scalarLow = getHsvScalar(205, 30, 5);
    Scalar scalarHigh = getHsvScalar(290, 400, 400);
    Scalar rectColor;
    Point lo = new Point(new double[] {0,0});
    Point ru = new Point(new double[] {60,60});

    int picWidth, picHeight;
    private boolean togglePic = true;

    //-----------------------------------------------------------------
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

    //-----------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam);

        javaCameraView = findViewById(R.id.CameraView);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setMaxFrameSize(1280, 720);

        javaCameraView.setCvCameraViewListener(this);

        scalarLow = getHsvScalar(205, 30, 5);
        scalarHigh = getHsvScalar(290, 400, 400);
    }

    //-----------------------------------------------------------------
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    //-----------------------------------------------------------------
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

    //-----------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //-----------------------------------------------------------------
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

    //-----------------------------------------------------------------
    //-----------------------------------------------------------------
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(width, height, CvType.CV_8UC4);
        mBGR = new Mat(width, height, CvType.CV_8UC4);
        hsvImg = new Mat(width, height, CvType.CV_8UC4);

        mask = new Mat(width, height, CvType.CV_8UC4);
    }

    //-----------------------------------------------------------------
    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
        mBGR.release();
        hsvImg.release();
        mask.release();

        mySeekBarSpeed.setSpeed(0);
        mySeekBarSpeed.setSteering(0);
        mySeekBarSpeed.refresh();
    }

    //-----------------------------------------------------------------
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        if (togglePic) {

            mRGBA = inputFrame.rgba();
            cvtColor(mRGBA, hsvImg, COLOR_RGB2HSV);

            Core.inRange(hsvImg, scalarLow, scalarHigh, mask);


            //"schwerpunkt" der erkannten roten flächen berechnen
            picHeight       = mask.rows();
            picWidth        = mask.cols();
            int avgHeight   = 0;
            int avgWidth    = 0;  // average height and width
            int count       = 1;  // init mit 1 um divideByZeroException zu vermeiden

            for (int i = 0; i< picHeight; i += 4) {
                for (int ii = 0; ii< picWidth; ii += 4) {
                    if (0 < mask.get(i,ii)[0]) {
                        avgHeight += i;
                        avgWidth += ii;
                        ++count;
                    }
                }
            }

            avgHeight /= count;
            avgWidth /= count;

            // ecken um den schwerpunkt setzen
            lo.set( new double[]{ avgWidth - 30, avgHeight - 30 });
            ru.set( new double[]{ avgWidth + 30, avgHeight + 30 });

            if (50 < count) {    // neue fahrtrichtung nur setzen, wenn nennenswerte menge an punkten gefunden wurde
                drive(avgWidth, avgHeight);
                rectColor = getHsvScalar(20,200,50);
            }
            else{
                rectColor= getHsvScalar(250, 30, 40);
            }
//            bitwise_not(mask, mask);                              // zum debuggen, um *nicht* erkannte bereiche zu sehen
            bitwise_and(mRGBA, mRGBA, hsvImg, mask);// farbbild auf filter legen
//            cvtColor(hsvImg, hsvImg, COLOR_RGB2BGR);              // zum debuggen, da die erkennung auf falschfarben (rgb <-> bgr) läuft

            rectangle(hsvImg, lo, ru, rectColor,3);       // quadrat über das erzeugte bild legen

        }

        return hsvImg;
    }

    //-----------------------------------------------------------------
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    //-----------------------------------------------------------------
    private Scalar getHsvScalar(double H, double S, double V) {
        // konvertiert normale HSV-Scalare (360,100,100) in OpenCV-HSV-Scalare (255,255,255)
        return new Scalar(((int)(H/360.0*255.0)), ((int)(S*2.55)), ((int)(V*2.55)));
    }

    private void drive(int avgWidht, int avgHeight){
        int steer = (int)( avgHeight * 200.0  / picHeight );
        int speed = (int)( avgWidht  * 1000.0 / picWidth  );
        speed -= 1000;
        speed *= -1;

        speed *= 2;

        System.out.println("speed: " + speed + "  steer: " + (steer-100) + "  height: " + picHeight + "  width: " + picWidth);

        mySeekBarSpeed.setSpeed(speed);
        mySeekBarSpeed.setSteering(steer -100);    // -100 um bei 0 grade aus zu fahren
        mySeekBarSpeed.refresh();
    }

    //-----------------------------------------------------------------
    static public void setMySeekBarSpeed(com.app.activitys.SeekBarSpeed sbs){
        mySeekBarSpeed = sbs;
    }


}