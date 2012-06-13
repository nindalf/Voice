package com.sundarram;

import android.app.Service;
import android.content.Intent;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.os.Binder;
import android.os.IBinder;
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Voice service", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i("Voice service", "Service destroyed");
    }

    /**
     * Client functions to manipulate the AudioGroup and AudioStream.
     */

    private AudioGroup audioGroup;
    private AudioStream audioStream;
    private boolean inCall;
    private InetAddress localInetAddress, remoteInetAddress;
    private int localAudioPort, remoteAudioPort;
    private int sendSignalPort, receiveSignalPort;
    public static final int LONG_SIGNAL_RECEIVE_PORT = 8237;
    public static final int SHORT_SIGNAL_RECEIVE_PORT = 8236;
    public static final int SIGNAL_SEND_PORT = 8235;

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
        audioStream.associate(remoteInetAddress, remoteAudioPort);
        audioStream.setCodec(localAudioCodec);
        audioStream.setMode(AudioGroup.MODE_NORMAL);
        audioStream.join(audioGroup);
        Log.i("RTP", "audioStream associated with remote peer.");
    }

    public void holdGroup(boolean hold) {
        if (hold) {
            audioGroup.setMode(AudioGroup.MODE_ON_HOLD);
            Log.i("RTP", "Call on hold. Microphone and Speaker disabled.");
        }
        else {
            audioGroup.setMode(AudioGroup.MODE_NORMAL);
            Log.i("RTP", "Call off hold. Microphone and Speaker enabled.");
        }
    }

    public void muteGroup(boolean mute) {
        if (mute) {
            audioGroup.setMode(AudioGroup.MODE_MUTED);
            Log.i("RTP", "Microphone muted.");
        }
        else {
            audioGroup.setMode(AudioGroup.MODE_NORMAL);
            Log.i("RTP", "Microphone unmuted.");
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
        Log.i("RTP", "Resources reset.");
    }

    /**
     * Threads that enable sending and receiving of signalling data.
     * Signal sending on 8235.
     * Signal receiving on 8236.
     */
    public static final int START = 100;
    public static final int READY = 101;
    public static final int HOLD = 102;
    public static final int UNHOLD = 103;
    public static final int MUTE = 104;
    public static final int UNMUTE = 105;
    public static final int END = 106;
    public static final int REJECT = 107;
    public int toSendSignal;
    public int receivedSignal;

    // Is this method better than making the Send class public?
    public void send(int message, int sendPort) {
        toSendSignal = message;
        sendSignalPort = sendPort;
        new Thread(new Send()).start();
    }

    public void receive(int receivePort) {
        receiveSignalPort = receivePort;
        new Thread(new Receive()).start();
    }

    private class Send implements Runnable {

        public void run() {
            Socket socket = null;
            DataOutputStream dataOutputStream = null;
            try {
                socket = new Socket(remoteInetAddress.getHostName(), receiveSignalPort, localInetAddress, sendSignalPort);
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeShort(toSendSignal);
                Log.i("Send", "Sent " + toSendSignal + "to " + remoteInetAddress.getHostName() + " port " + receiveSignalPort);
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

    private class Receive implements Runnable {
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            Socket socket = null;
            DataInputStream dataInputStream = null;
            boolean flag = true;
            try {
                serverSocket = new ServerSocket(receiveSignalPort);
            }
            catch(IOException e) {
                e.printStackTrace();
                flag = false;
            }
            while(flag) {
                try {
                    Log.i("Receive", "Listening on port " + receiveSignalPort);
                    socket = serverSocket.accept();
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    if(remoteInetAddress.equals(socket.getInetAddress()))
                        receivedSignal = dataInputStream.readShort();
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

}
