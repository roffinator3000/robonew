package com.app.activitys;

import android.widget.SeekBar;

import com.app.activitys.ORB.ORB;


public class SeekBarSteering implements SeekBar.OnSeekBarChangeListener{

    private ORB orb;
    private static com.app.activitys.SeekBarSpeed mySeekBarSpeed;

    SeekBarSteering(ORB orb){
        this.orb = orb;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (progress > 205)     // falls im oberen puffer
            progress = 205;
        if (progress < 5)       // falls im unteren puffer
            progress = 5;

        progress -= 5;          //wegen seitlichen puffern

        mySeekBarSpeed.control(progress-100);    // -100 um bei 0 grade aus zu fahren
        mySeekBarSpeed.refresh();
//        orb.setMotor(0, ORB.Mode.MOVETO, +(progress -750), 0);
//        orb.setMotor(1, ORB.Mode.MOVETO, -(progress -750), 0);
    }

    static public void setMySeekBarSpeed(com.app.activitys.SeekBarSpeed sbs){
        mySeekBarSpeed = sbs;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
