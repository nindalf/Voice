package com.sundarram.voice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;


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
    public void onCreate() {
        super.onCreate();
        startLocalBroadcastManager();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!newCallListenerWorking) {
            new Thread(new NewCallListener(NEW_CALL_PORT)).start();
        }
        Log.i("VoiceService", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopLocalBroadcastManager();
        Log.i("VoiceService", "Service destroyed");
    }


    /**
     * Client functions to manipulate the AudioGroup and AudioStream.
     */

    private AudioGroup audioGroup;
    private AudioStream audioStream;
    private boolean inCall, newCallListenerWorking;
    private InetAddress localInetAddress, remoteInetAddress;
    private int localAudioPort, remoteAudioPort;
    public static final int NEW_CALL_PORT = 8237;
    public static final int SIGNAL_RECEIVE_PORT = 8236;
    public static final int SIGNAL_SEND_PORT = 8235;

    /** Starts an AudioStream on localInetAddress on a random port(in accordance with the RFC). */
    public void startAudioStream() {
        localInetAddress = Helper.getLocalIpAddress();
        try  {
            audioStream= new AudioStream(localInetAddress);
        }
        catch(SocketException e) {
            e.printStackTrace();
        }
        localAudioPort = audioStream.getLocalPort();
        Log.i("VoiceService", "audioStream started on " + localInetAddress.getHostName() + " on port " + localAudioPort);
    }

    /**
     * To be called when the following variables are set:
     * remoteInetAddress, remoteAudioPort
     * localInetAddress, localAudioPort
     * When both users run this function, full-duplex audio conversation starts.
     */
    public void setStreams() {
        AudioCodec localAudioCodec = AudioCodec.AMR;
        audioGroup = new AudioGroup();
        audioGroup.setMode(AudioGroup.MODE_NORMAL);
        audioStream.associate(remoteInetAddress, remoteAudioPort);
        audioStream.setCodec(localAudioCodec);
        audioStream.setMode(AudioGroup.MODE_NORMAL);
        audioStream.join(audioGroup);
        Log.i("VoiceService", "audioStream associated with remote peer.");

        Intent intent = new Intent(ACTION_REMOTE_READY);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    public void holdGroup(boolean hold) {
        if (hold) {
            audioGroup.setMode(AudioGroup.MODE_ON_HOLD);
            Log.i("VoiceService", "Call on hold. Microphone and Speaker disabled.");
        }
        else {
            audioGroup.setMode(AudioGroup.MODE_NORMAL);
            Log.i("VoiceService", "Call off hold. Microphone and Speaker enabled.");
        }
    }

    public void muteGroup(boolean mute) {
        if (mute) {
            audioGroup.setMode(AudioGroup.MODE_MUTED);
            Log.i("VoiceService", "Microphone muted.");
        }
        else {
            audioGroup.setMode(AudioGroup.MODE_NORMAL);
            Log.i("VoiceService", "Microphone unmuted.");
        }
    }

    public int getAudioGroupMode() {
        return audioGroup.getMode();
    }

    public int getAudioStreamMode() {
        return audioStream.getMode();
    }

    public boolean isAudioGroupSet() {
        if(audioGroup != null)
            return true;
        return false;
    }

    public boolean isAudioStreamSet() {
        if(audioStream != null)
            return true;
        return false;
    }

    /** Resets all data of the Service */
    public void closeAll() {
        audioStream.join(null);
        audioGroup = null;
        audioStream = null;
        remoteAudioPort = 0;
        localAudioPort = 0;
        remoteInetAddress = null;
        localInetAddress = null;
        Log.i("VoiceService", "Resources reset.");
    }

    public void newCall() {
        Intent newCallIntent = new Intent(this, NewCallActivity.class);
        newCallIntent.putExtra("target", remoteInetAddress.getHostName());
        newCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplication().startActivity(newCallIntent);
    }

    /** Called when a new call is made and when a call is accepted. */
    public void startCall() {
        localInetAddress = Helper.getLocalIpAddress();

        startAudioStream();
        send(localAudioPort, SIGNAL_RECEIVE_PORT, SIGNAL_SEND_PORT);

        Intent intent = new Intent(this, InCallActivity.class);
        intent.putExtra("target", remoteInetAddress.getHostName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void endCall() {
        inCall = false;
        closeAll();
    }

    /** Parses the incoming message and takes action. */
    public void parseSignal(int message) {
        if(message > 10000) {
            if(!isAudioGroupSet() && localAudioPort != 0)
                setStreams();
        }
        else {
            Intent intent = new Intent("");
            switch (message) {
                case HOLD:
                    intent = new Intent(ACTION_REMOTE_HOLD);
                    break;
                case UNHOLD:
                    intent = new Intent(ACTION_REMOTE_UNHOLD);
                    break;
                case MUTE:
                    intent = new Intent(ACTION_REMOTE_MUTE);
                    break;
                case UNMUTE:
                    intent = new Intent(ACTION_REMOTE_UNMUTE);
                    break;
                case END:
                    intent = new Intent(ACTION_REMOTE_END);
                    endCall();
                    break;
                case REJECT:
                    intent = new Intent(ACTION_REMOTE_REJECT);
                    endCall();
                    break;
            }
            mLocalBroadcastManager.sendBroadcast(intent);
        }
    }


    /**
     * LocalBroadcastManager and BroadcastReceiver to handle intents from activities.
     */

    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mReceiver;

    public static final String ACTION_REMOTE_READY = "com.sundarram.voice.REMOTE_READY";
    public static final String ACTION_REMOTE_HOLD = "com.sundarram.voice.REMOTE_HOLD";
    public static final String ACTION_REMOTE_UNHOLD = "com.sundarram.voice.REMOTE_UNHOLD";
    public static final String ACTION_REMOTE_MUTE = "com.sundarram.voice.REMOTE_MUTE";
    public static final String ACTION_REMOTE_UNMUTE = "com.sundarram.voice.REMOTE_UNMUTE";
    public static final String ACTION_REMOTE_END = "com.sundarram.voice.REMOTE_END";
    public static final String ACTION_REMOTE_REJECT = "com.sundarram.voice.REMOTE_REJECT";

    public static final String ACTION_LOCAL_READY = "com.sundarram.voice.LOCAL_READY";
    public static final String ACTION_LOCAL_HOLD = "com.sundarram.voice.LOCAL_HOLD";
    public static final String ACTION_LOCAL_UNHOLD = "com.sundarram.voice.LOCAL_UNHOLD";
    public static final String ACTION_LOCAL_MUTE = "com.sundarram.voice.LOCAL_MUTE";
    public static final String ACTION_LOCAL_UNMUTE = "com.sundarram.voice.LOCAL_UNMUTE";
    public static final String ACTION_LOCAL_END = "com.sundarram.voice.LOCAL_END";
    public static final String ACTION_LOCAL_REJECT = "com.sundarram.voice.LOCAL_REJECT";

    private void startLocalBroadcastManager() {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String currentAction = intent.getAction();
                if(currentAction.equals(NewCallActivity.ACTION_ACCEPTED))
                    startCall();
                else if(currentAction.equals(NewCallActivity.ACTION_REJECTED)) {
                    send(REJECT, SIGNAL_RECEIVE_PORT, SIGNAL_SEND_PORT);
                    inCall = false;
                    closeAll();
                }
                else if(currentAction.equals(DiallerActivity.ACTION_MAKE_CALL)) {
                    Bundle localBundle = intent.getExtras();
                    String target = ((String)localBundle.get("target"));
                    remoteInetAddress = Helper.getTargetInetaddress(target);
                    localInetAddress = Helper.getLocalIpAddress();
                    inCall = true;
                    send(START, NEW_CALL_PORT, SIGNAL_SEND_PORT);
                    receive(SIGNAL_RECEIVE_PORT);
                    startCall();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(NewCallActivity.ACTION_ACCEPTED);
        filter.addAction(NewCallActivity.ACTION_REJECTED);
        filter.addAction(DiallerActivity.ACTION_MAKE_CALL);
        mLocalBroadcastManager.registerReceiver(mReceiver, filter);
    }

    private void stopLocalBroadcastManager() {
        mLocalBroadcastManager.unregisterReceiver(mReceiver);
    }


    /**
     * Threads that enable sending and receiving of signalling data.
     * Signal sending on SIGNAL_SEND_PORT.
     * Signal receiving on SHORT_SIGNAL_RECEIVE_PORT.
     */

    public static final int START = 100;
    //public static final int READY = 101;
    public static final int HOLD = 102;
    public static final int UNHOLD = 103;
    public static final int MUTE = 104;
    public static final int UNMUTE = 105;
    public static final int END = 106;
    public static final int REJECT = 107;

    public void send(int message, int remotePort, int localPort) {
        new Thread(new Send(message, remotePort, localPort)).start();
    }

    public void receive(int localPort) {
        new Thread(new SignalListener(localPort) ).start();
    }

    //TODO: create persistent socket to send. More efficient.
    private class Send implements Runnable {

        int toSend, remoteSignalPort, localSignalPort;

        Send(int message, int remotePort, int localPort) {
            toSend = message;
            remoteSignalPort = remotePort;
            localSignalPort = localPort;
        }

        public void run() {
            Socket socket = null;
            DataOutputStream dataOutputStream = null;
            try {
                socket = new Socket(remoteInetAddress.getHostName(), remoteSignalPort, localInetAddress, localSignalPort);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeShort(toSend);
                Log.i("VoiceService", "Sent " + toSend + "to " + remoteInetAddress.getHostName() + " port " + remoteSignalPort);
            }
            catch (UnknownHostException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (NullPointerException e) {
                e.printStackTrace();
            }
            finally {
                if(socket != null) {
                    try {
                        socket.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class SignalListener implements Runnable {

        int localSignalPort;

        SignalListener(int localPort) {
            localSignalPort = localPort;
        }

        @Override
        public void run() {
            int message;
            ServerSocket serverSocket = null;
            Socket socket = null;
            DataInputStream dataInputStream = null;
            boolean flag = true;
            try {
                serverSocket = new ServerSocket(localSignalPort);
            }
            catch(IOException e) {
                e.printStackTrace();
                flag = false;
            }
            while(flag && inCall) {
                try {
                    Log.i("VoiceService", "Listening for Signals on port " + localSignalPort);
                    socket = serverSocket.accept();
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    message = dataInputStream.readShort();
                    if(remoteInetAddress.equals(socket.getInetAddress())) {
                        Log.i("VoiceService", "Received " + message + " from " + remoteInetAddress.getHostName() + " on port " + localSignalPort);
                        parseSignal(message);
                    }
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
                finally  {
                    if(socket != null) {
                        try {
                            socket.close();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if(dataInputStream != null) {
                        try {
                            dataInputStream.close();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private class NewCallListener implements Runnable {

        int localSignalPort;

        NewCallListener(int localPort) {
            localSignalPort = localPort;
        }

        @Override
        public void run() {
            int message;
            ServerSocket serverSocket = null;
            Socket socket = null;
            DataInputStream dataInputStream = null;
            boolean flag = true;
            try {
                serverSocket = new ServerSocket(localSignalPort);
                newCallListenerWorking = true;
            }
            catch(IOException e) {
                e.printStackTrace();
                flag = false;
                newCallListenerWorking = false;
            }
            while(flag) {
                try {
                    Log.i("VoiceService", "Listening for new calls on port " + localSignalPort);
                    socket = serverSocket.accept();
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    message = dataInputStream.readShort();
                    if(message == 100 && !inCall) {
                        remoteInetAddress = socket.getInetAddress();
                        localInetAddress = Helper.getLocalIpAddress();
                        Log.i("VoiceService", "Received " + message + " from " + remoteInetAddress.getHostName() + " on port " + localSignalPort);
                        inCall = true;
                        receive(SIGNAL_RECEIVE_PORT);
                        // start NewCallActivity
                        newCall();
                    }
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
                finally  {
                    newCallListenerWorking = false;
                    if(socket != null) {
                        try {
                            socket.close();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if(dataInputStream != null) {
                        try {
                            dataInputStream.close();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
