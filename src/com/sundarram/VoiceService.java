package com.sundarram;

import android.app.Service;
import android.content.Intent;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.net.InetAddress;
import java.net.SocketException;


public class VoiceService extends Service {

    private final IBinder mBinder = new VoiceBinder();

    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class VoiceBinder extends Binder {
        VoiceService getService() {
            return VoiceService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Voice service", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i("Voice service", "Service destroyed");
    }

    /**
     * Client functions
     */

    private AudioGroup audioGroup;
    private AudioStream audioStream;
    private boolean inCall;
    private InetAddress localInetAddress;
    private int localPort;
    private InetAddress remoteInetAddress;
    private int remotePort;

    public void startAudioStream() {
        InetAddress localAddress = Helper.getLocalIpAddress();
        try  {
            audioStream= new AudioStream(localAddress);
        }
        catch(SocketException e) {
            e.printStackTrace();
        }
        catch(NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void setStreams() {
        AudioCodec localAudioCodec = AudioCodec.AMR;
        audioGroup = new AudioGroup();
        audioGroup.setMode(AudioGroup.MODE_NORMAL);
        audioStream.associate(remoteInetAddress, remotePort);
        audioStream.setCodec(localAudioCodec);
        audioStream.setMode(AudioGroup.MODE_NORMAL);
        audioStream.join(audioGroup);
        Log.i("xxx", "audioStream associated with remote peer.");
    }

    public void holdGroup(boolean hold) {
        if (hold) {
            audioGroup.setMode(AudioGroup.MODE_ON_HOLD);
            Log.i("xxx", "Call on hold. Microphone and Speaker disabled.");
        }
        else {
            audioGroup.setMode(AudioGroup.MODE_NORMAL);
            Log.i("xxx", "Call off hold. Microphone and Speaker enabled.");
        }
    }

    public void muteGroup(boolean mute) {
        if (mute) {
            audioGroup.setMode(AudioGroup.MODE_MUTED);
            Log.i("xxx", "Microphone muted.");
        }
        else {
            audioGroup.setMode(AudioGroup.MODE_NORMAL);
            Log.i("xxx", "Microphone unmuted.");
        }
    }


    public void closeAll() {
        audioStream.join(null);
        audioGroup = null;
        audioStream = null;
        remotePort = 0;
        localPort = 0;
        remoteInetAddress = null;
        localInetAddress = null;
        Log.i("xxx", "Resources reset.");
    }
}
