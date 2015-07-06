package com.ramesh.stopwatchapp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


public class StopWatchActivity extends Activity {
    private int seconds = 0;
    private boolean isRunning;
    private boolean wasRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop_watch);
        if(savedInstanceState != null) {
            seconds = savedInstanceState.getInt("seconds");
            isRunning = savedInstanceState.getBoolean("isRunning");
            wasRunning = savedInstanceState.getBoolean("wasRunning");
        }
        runTimer();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("seconds", seconds);
        savedInstanceState.putBoolean("isRunning", isRunning);
        savedInstanceState.putBoolean("wasRunning", wasRunning);
    }

    @Override
    protected void onStop() {
        super.onStop();
        wasRunning = isRunning;
        isRunning = false;
        Log.e("RV.........", "called onStop method");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(wasRunning) {
            isRunning = true;
        }
        Log.e("RV.........", "called onResume");
    }
    @Override
    protected void onPause(){
        super.onPause();
        wasRunning = isRunning;
        isRunning = false;
        Log.e("RV.......", "called onPause method");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(wasRunning) {
            isRunning = true;
        }
        Log.e("RV.........", "called onStart method");
    }

    private void runTimer() {
        final TextView timeText =   (TextView) findViewById(R.id.textViewTime);

        final Handler handle = new Handler();
        handle.post(new Runnable() {
            @Override
            public void run() {
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;
                String time = String.format("%d:%02d:%02d", hours, minutes, secs);
                Log.e("RV.........", time);
                timeText.setText(time);

                if (isRunning) {
                    seconds++;
                }
                handle.postDelayed(this, 1000);
                Log.e("RV....................", "curVal"+ seconds);
            }
        });
    }

    public void onClickStartButton(View view) {
        Log.e("RV................", "start Button clicked");
        isRunning = true;
    }

    public void onClickStopButton(View view) {
        isRunning = false;
        Log.e("RV.................", "stop Button clicked");
    }

    public void onClickResetButton(View view) {
        isRunning = false;
        seconds = 0;
        Log.e("RV.................", "reset Button clicked");

    }
}
