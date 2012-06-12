package com.sundarram;

import android.app.Activity;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class Helper
{
    public static AudioGroup audioGroup;
    public static AudioStream audioStream;
    public static boolean inCall;
    public static InetAddress localInetAddress;
    public static int localPort;
    public static InetAddress remoteInetAddress;
    public static int remotePort;

    public static void closeAll() {
        audioStream.join(null);
        audioGroup = null;
        audioStream = null;
        remotePort = 0;
        localPort = 0;
        remoteInetAddress = null;
        localInetAddress = null;
        Log.i("xxx", "Resources reset.");
    }

    public static InetAddress getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        Log.i("IP", inetAddress.getHostAddress());
                        return inetAddress;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("IPNo", ex.toString());
        }
        return null;
    }

    private InetAddress getTargetInetaddress(String target) {
        InetAddress targetInetAddress = null;
        try {
            targetInetAddress = InetAddress.getByName(target.trim());
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return targetInetAddress;
    }

    public static void holdGroup(boolean hold) {
        if (hold) {
            audioGroup.setMode(AudioGroup.MODE_ON_HOLD);
            Log.i("xxx", "Call on hold. Microphone and Speaker disabled.");
        }
        else {
            audioGroup.setMode(AudioGroup.MODE_NORMAL);
            Log.i("xxx", "Call off hold. Microphone and Speaker enabled.");
        }
    }

    public static void muteGroup(boolean mute) {
        if (mute) {
            audioGroup.setMode(AudioGroup.MODE_MUTED);
            Log.i("xxx", "Microphone muted.");
        }
        else {
            audioGroup.setMode(AudioGroup.MODE_NORMAL);
            Log.i("xxx", "Microphone unmuted.");
        }
    }

    public static void setStreams() {
        AudioCodec localAudioCodec = AudioCodec.AMR;
        audioGroup = new AudioGroup();
        audioGroup.setMode(AudioGroup.MODE_NORMAL);
        audioStream.associate(remoteInetAddress, remotePort);
        audioStream.setCodec(localAudioCodec);
        audioStream.setMode(AudioGroup.MODE_NORMAL);
        audioStream.join(audioGroup);
        Log.i("xxx", "audioStream associated with remote peer.");
    }

    public void startAudioStream() {
        InetAddress localAddress = getLocalIpAddress();
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

    public static class IpSetter extends AsyncTask<TextView, Void, String>
    {
        TextView element;
        @Override
        protected String doInBackground(TextView... elements) {
            element = elements[0];
            String ip = "Not Found";
            try {
                ip = getLocalIpAddress().getHostAddress();
            }
            catch(NullPointerException e) {
                Log.e("xxx", e.toString());
            }
            return ip;

        }
        protected void onPostExecute(String result) {
            element.setText(result);
        }
    }
}
