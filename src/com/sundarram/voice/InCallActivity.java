package com.sundarram.voice;

import android.app.Activity;
import android.content.*;
import android.net.rtp.AudioGroup;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class InCallActivity extends Activity
{
    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mReceiver;
    private TextView mStatus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        setContentView(R.layout.incall);

        Bundle localBundle = getIntent().getExtras();
        ((TextView)findViewById(R.id.peers_ip)).setText((String)localBundle.get("target"));
        mStatus = (TextView)findViewById(R.id.status);
        mStatus.setText(R.string.status_ringing);

        findViewById(R.id.end).setOnClickListener(this.onEndListener);
        findViewById(R.id.hold).setOnClickListener(this.onHoldListener);
        findViewById(R.id.mute).setOnClickListener(this.onMuteListener);
    }

    private View.OnClickListener onEndListener = new View.OnClickListener() {
        public void onClick(View paramView) {
            mService.send(VoiceService.END, VoiceService.SIGNAL_RECEIVE_PORT, VoiceService.SIGNAL_SEND_PORT);
            mHandler.removeCallbacks(mUpdateTimeTask);
            mStatus.setText(R.string.status_ended);
            mService.endCall();
            finish();
        }
    };

    private View.OnClickListener onHoldListener = new View.OnClickListener() {
        public void onClick(View paramView) {
            Button holdButton = (Button)findViewById(R.id.hold);
            Button muteButton = (Button)findViewById(R.id.mute);
            if(mService.isAudioGroupSet()) {
                int curMode = mService.getAudioGroupMode();
                if(curMode != AudioGroup.MODE_ON_HOLD) {
                    // will hold the AudioGroup
                    mService.holdGroup(true);
                    holdButton.setText(R.string.text_button_resume);
                    muteButton.setEnabled(false);
                    mService.send(VoiceService.HOLD, VoiceService.SIGNAL_RECEIVE_PORT, VoiceService.SIGNAL_SEND_PORT);
                }
                else {
                    // will unhold the AudioGroup - set it to NORMAL
                    mService.holdGroup(false);
                    holdButton.setText(R.string.text_button_hold);
                    muteButton.setEnabled(true);
                    mService.send(VoiceService.UNHOLD, VoiceService.SIGNAL_RECEIVE_PORT, VoiceService.SIGNAL_SEND_PORT);
                }
            }
        }
    };

    private View.OnClickListener onMuteListener = new View.OnClickListener() {
        public void onClick(View paramView) {
            Button holdButton = (Button)findViewById(R.id.hold);
            Button muteButton = (Button)findViewById(R.id.mute);
            if(mService.isAudioGroupSet()) {
                int curMode = mService.getAudioGroupMode();
                if(curMode != AudioGroup.MODE_MUTED) {
                    // will mute the AudioGroup
                    mService.muteGroup(true);
                    muteButton.setText(R.string.text_button_unmute);
                    holdButton.setEnabled(false);
                    mService.send(VoiceService.MUTE, VoiceService.SIGNAL_RECEIVE_PORT, VoiceService.SIGNAL_SEND_PORT);
                }
                else {
                    // will unmute the AudioGroup
                    mService.muteGroup(false);
                    muteButton.setText(R.string.text_button_mute);
                    holdButton.setEnabled(true);
                    mService.send(VoiceService.UNMUTE, VoiceService.SIGNAL_RECEIVE_PORT, VoiceService.SIGNAL_SEND_PORT);
                }
            }
        }
    };

    /**
     * Timer related data and functions.
    */
    Handler mHandler = new Handler();
    long mStartTime;
    TextView mTimer;

    /** Initializes timer data memebers and start. */
    private void startTimer() {
        mTimer = ((TextView)findViewById(R.id.timer));
        mStartTime = SystemClock.uptimeMillis();
        mHandler = new Handler();
        mHandler.removeCallbacks(mUpdateTimeTask);
        mHandler.postDelayed(mUpdateTimeTask, 100L);
    }

    /** Runnable that updates the timer every second. */
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

            mHandler.postAtTime(this, start + (((minutes * 60) + seconds + 1) * 1000));
        }
    };

    @Override
    public void finish() {
        super.finish();
    }


    /**
     * The following code provides a connection to VoiceService.
     * I wish to deprecate this soon, replacing it with Broadcasts.
    */

    VoiceService mService;
    boolean mBound = false;

    @Override
    public void onStart() {
        super.onStart();
        //Bind to VoiceService.
        Intent intent = new Intent(this, VoiceService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        startLocalBroadcastManager();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        stopLocalBroadcastManager();
    }
    /** Binds this activity to the VoiceService */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            VoiceService.VoiceBinder binder = (VoiceService.VoiceBinder) iBinder;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };


    /**
     * LocalBroadcastManager and BroadcastReceiver to handle intents from VoiceService.
     */

    private void startLocalBroadcastManager() {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mReceiver = new BroadcastReceiver() {
            @Override
            //TODO:Add for all intents
            public void onReceive(Context context, Intent intent) {
                String currentAction = intent.getAction();
                if(currentAction.equals(VoiceService.ACTION_REMOTE_READY)) {
                    startTimer();
                    mStatus.setText("");
                    findViewById(R.id.hold).setEnabled(true);
                    findViewById(R.id.mute).setEnabled(false);
                }
                else if(currentAction.equals(VoiceService.ACTION_REMOTE_END)) {
                    mStatus.setText(R.string.status_ended);
                    mHandler.removeCallbacks(mUpdateTimeTask);
                    finish();
                }
                else if(currentAction.equals(VoiceService.ACTION_REMOTE_REJECT)) {
                    mStatus.setText(R.string.status_rejected);
                    finish();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(VoiceService.ACTION_REMOTE_READY);
        filter.addAction(VoiceService.ACTION_REMOTE_HOLD);
        filter.addAction(VoiceService.ACTION_REMOTE_UNHOLD);
        filter.addAction(VoiceService.ACTION_REMOTE_MUTE);
        filter.addAction(VoiceService.ACTION_REMOTE_UNMUTE);
        filter.addAction(VoiceService.ACTION_REMOTE_END);
        filter.addAction(VoiceService.ACTION_REMOTE_REJECT);
        mLocalBroadcastManager.registerReceiver(mReceiver, filter);
    }

    private void stopLocalBroadcastManager() {
        mLocalBroadcastManager.unregisterReceiver(mReceiver);
    }
}
