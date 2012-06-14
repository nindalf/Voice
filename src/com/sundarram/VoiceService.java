package com.sundarram;

import android.app.Activity;
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
        if(newCallListenerWorking == false) {
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

    public void startAudioStream() {
        //remove the following line. set the localInetAddress somewhere else.
        localInetAddress = Helper.getLocalIpAddress();
        try  {
            audioStream= new AudioStream(localInetAddress);
        }
        catch(SocketException e) {
            e.printStackTrace();
        }
        catch(NullPointerException e) {
            e.printStackTrace();
        }
        localAudioPort = audioStream.getLocalPort();
        Log.i("VoiceService", "audioStream started on " + localInetAddress.getHostName() + " on port " + localAudioPort);
    }

    public void setStreams() {
        AudioCodec localAudioCodec = AudioCodec.AMR;
        audioGroup = new AudioGroup();
        audioGroup.setMode(AudioGroup.MODE_NORMAL);
        audioStream.associate(remoteInetAddress, remoteAudioPort);
        audioStream.setCodec(localAudioCodec);
        audioStream.setMode(AudioGroup.MODE_NORMAL);
        audioStream.join(audioGroup);
        Log.i("VoiceService", "audioStream associated with remote peer.");
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
    //TODO: BOTH OF THE FOLLOWING FUNCTIONS
    public void newCall() {
        localInetAddress = Helper.getLocalIpAddress();

        receive(SIGNAL_RECEIVE_PORT);
        startAudioStream();
        send(localAudioPort, SIGNAL_RECEIVE_PORT, SIGNAL_SEND_PORT);
        // wait for remoteAudioPort
        // start InCallActivity, don't forget addExtra(target)


    }

    public void parseSignal(int message) {
        Intent intent;
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
                break;
            case REJECT:
                intent = new Intent(ACTION_REMOTE_REJECT);
                break;
        }
    }

    /**
     * LocalBroadcastManager and BroadcastReceiver to handle intents from activities.
     */

    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mReceiver;

    private void startLocalBroadcastManager() {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() == NewCallActivity.ACTION_ACCEPTED)
                    newCall();
                else if(intent.getAction() == NewCallActivity.ACTION_REJECTED) {
                    send(REJECT, SIGNAL_RECEIVE_PORT, SIGNAL_SEND_PORT);
                    inCall = false;
                    closeAll();
                }
                else if(intent.getAction() == DiallerActivity.ACTION_MAKE_CALL) {
                    Bundle localBundle = intent.getExtras();
                    String target = ((String)localBundle.get("target"));
                    remoteInetAddress = Helper.getTargetInetaddress(target);
                    localInetAddress = Helper.getLocalIpAddress();
                    inCall = true;
                    send(START, SIGNAL_RECEIVE_PORT, SIGNAL_SEND_PORT);
                    newCall();
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
    public static final String ACTION_REMOTE_READY = "com.sundarram.READY";
    public static final String ACTION_REMOTE_HOLD = "com.sundarram.HOLD";
    public static final String ACTION_REMOTE_UNHOLD = "com.sundarram.UNHOLD";
    public static final String ACTION_REMOTE_MUTE = "com.sundarram.MUTE";
    public static final String ACTION_REMOTE_UNMUTE = "com.sundarram.UNMUTE";
    public static final String ACTION_REMOTE_END = "com.sundarram.END";
    public static final String ACTION_REMOTE_REJECT = "com.sundarram.REJECT";


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
                    if(remoteInetAddress.equals(socket.getInetAddress())) {
                        message = dataInputStream.readShort();
                        Log.i("VoiceService", "Received " + message + " from " + remoteInetAddress.getHostName() + " on port " + localSignalPort);
                        parseSignal(message);
                        // parse the message and take action.
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
                    // parse the message and take action.
                    if(message == 100 && inCall == false) {
                        remoteInetAddress = socket.getInetAddress();
                        localInetAddress = Helper.getLocalIpAddress();
                        Log.i("VoiceService", "Received " + message + " from " + remoteInetAddress.getHostName() + " on port " + localSignalPort);
                        inCall = true;
                        // start NewCallActivity
                        Intent newCallIntent = new Intent(getBaseContext(), NewCallActivity.class);
                        newCallIntent.putExtra("target", remoteInetAddress.getHostName());
                        newCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplication().startActivity(newCallIntent);

                        //newCall();
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
