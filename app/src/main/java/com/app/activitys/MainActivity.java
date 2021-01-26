package com.app.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import com.app.R;
import com.app.activitys.Bluetooth.BluetoothDeviceListActivity;
import com.app.activitys.ORB.ORB;

public class MainActivity extends AppCompatActivity {

    private Menu menuLocal;
    private ORB orb;
    private Handler msgHandler;
    private SeekBar seekBarSpeed;
    private SeekBar seekBarSteering;

    private final int ORB_REQUEST_CODE = 0;
    private final int ORB_DATA_RECEIVED_MSG_ID = 999;

    //---------------------------------------------------------------
    public MainActivity() {
        msgHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case ORB_DATA_RECEIVED_MSG_ID:
                        setMsg();
                        break;
                    default:
                }
                super.handleMessage(msg);
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        seekBarSpeed = findViewById(R.id.seekBarSpeed);
        seekBarSteering = findViewById(R.id.seekBarSteering);
        orb = new ORB();
        orb.init(this, msgHandler, ORB_DATA_RECEIVED_MSG_ID);
        orb.configMotor(0, 144, 50, 50, 30);
        orb.configMotor(1, 144, 50, 50, 30);

        seekBarSpeed.setOnSeekBarChangeListener(new SeekBarSpeed(orb));
        seekBarSteering.setOnSeekBarChangeListener((new SeekBarSteering(orb)));
    }

    public void startCamera(View view) {

        Intent intent = new Intent(this, CamActivity.class);
////        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        startActivity(intent);
    }


    @Override
    public void onDestroy() {
        orb.close();
        super.onDestroy();
    }

    //---------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (menuLocal == null) {
            menuLocal = menu;
            super.onCreateOptionsMenu(menu);
        }
        return true;
    }


    //---------------------------------------------------------------
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_connect:
                BluetoothDeviceListActivity.startBluetoothDeviceSelect(this, ORB_REQUEST_CODE);
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    //-----------------------------------------------------------------
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ORB_REQUEST_CODE:
                if (!orb.openBluetooth(BluetoothDeviceListActivity.onActivityResult(resultCode, data))) {
                    Toast.makeText(getApplicationContext(), "Bluetooth not connected", Toast.LENGTH_LONG).show();
                }
                break;
            default:
        }
    }


    public void Drive(View view, int x) {
        orb.setMotor(0, ORB.Mode.SPEED, -x, 0);
        orb.setMotor(1, ORB.Mode.SPEED, +x, 0);
    }

    public void onClick_Drive_s(View view) {
        Drive(view, 250);
    }

    public void onClick_Stop(View view) {
        seekBarSpeed.setProgress(1000);
        seekBarSteering.setProgress(105);

        orb.setMotor(0, ORB.Mode.POWER, 0, 0);
        orb.setMotor(1, ORB.Mode.POWER, 0, 0);
    }


    private void setMsg() {
        TextView view;

        view = (TextView) findViewById(R.id.msgVoltage);
        view.setText("Battary:" + String.format("%.1f V", orb.getVcc()));

        view = (TextView) findViewById(R.id.msgORB1);
        view.setText("M0:\n" + String.format("%6d\n%6d\n%6d", orb.getMotorSpeed((byte) 0),
                orb.getMotorPos((byte) 0),
                orb.getMotorPwr((byte) 0)));

        view = (TextView) findViewById(R.id.msgORB2);
        view.setText("M1:\n" + String.format("%6d\n%6d\n%6d", orb.getMotorSpeed((byte) 1),
                orb.getMotorPos((byte) 1),
                orb.getMotorPwr((byte) 1)));
    }


}