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

    public static InetAddress getTargetInetaddress(String target) {
        InetAddress targetInetAddress = null;
        try {
            targetInetAddress = InetAddress.getByName(target.trim());
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return targetInetAddress;
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
