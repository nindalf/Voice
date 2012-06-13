package com.sundarram;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

class NewCallService extends Service {
    IBinder mBinder;

    public IBinder onBind(Intent paramIntent) {
        return this.mBinder;
    }
}