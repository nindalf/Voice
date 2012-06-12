package com.sundarram;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.sundarram.R;

public class InCallActivity extends Activity
{
    public static final int DIALLED = 1;
    public static final int RECEIVED = 2;
    Handler mHandler;
    long mStartTime;
    TextView mTimer;
    private Integer requestedByActivity;
    private String target;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.incall);
        initTimer();

        Bundle localBundle = getIntent().getExtras();
        requestedByActivity = ((Integer)localBundle.get("requestCode"));
        target = ((String)localBundle.get("target"));
        ((TextView)findViewById(R.id.peers_ip)).setText(this.target);

        findViewById(R.id.end).setOnClickListener(this.onEndListener);
        findViewById(R.id.hold).setOnClickListener(this.onHoldListener);
        findViewById(R.id.mute).setOnClickListener(this.onMuteListener);
    }

    private View.OnClickListener onEndListener = new View.OnClickListener() {
        public void onClick(View paramView) {
            mHandler.removeCallbacks(mUpdateTimeTask);
            finish();
        }
    };

    private View.OnClickListener onHoldListener = new View.OnClickListener() {
        public void onClick(View paramView) {
        }
    };

    private View.OnClickListener onMuteListener = new View.OnClickListener() {
        public void onClick(View paramView) {
        }
    };

    private void initTimer() {
        mTimer = ((TextView)findViewById(R.id.timer));
        mStartTime = SystemClock.uptimeMillis();
        mHandler = new Handler();
        mHandler.removeCallbacks(mUpdateTimeTask);
        mHandler.postDelayed(mUpdateTimeTask, 100L);
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            final long start = mStartTime;
            long millis = SystemClock.uptimeMillis() - start;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds     = seconds % 60;

            if (seconds < 10) {
                mTimer.setText("" + minutes + ":0" + seconds);
            } else {
                mTimer.setText("" + minutes + ":" + seconds);
            }

            mHandler.postAtTime(this,
                    start + (((minutes * 60) + seconds + 1) * 1000));
        }
    };

    public void finish() {
        super.finish();
    }

}
