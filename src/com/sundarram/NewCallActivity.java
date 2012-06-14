package com.sundarram;


import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.TextView;
import com.sundarram.R;

public class NewCallActivity extends Activity implements View.OnClickListener {
    LocalBroadcastManager mLocalBroadcastManager;
    public static final String ACTION_ACCEPTED = "com.sundarram.REJECTED";
    public static final String ACTION_REJECTED = "com.sundarram.ACCEPTED";

    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        Bundle localBundle = getIntent().getExtras();
        String target = ((String)localBundle.get("target"));
        ((TextView)findViewById(R.id.new_call_ip)).setText(target);

        findViewById(R.id.accept).setOnClickListener(this);
        findViewById(R.id.reject).setOnClickListener(this);
    }

    public void onClick(View view) {
        Intent intent;

        switch(view.getId()) {
            case R.id.accept:
                intent = new Intent(ACTION_ACCEPTED);
                mLocalBroadcastManager.sendBroadcast(intent);
                break;
            case R.id.reject:
                intent = new Intent(ACTION_REJECTED);
                mLocalBroadcastManager.sendBroadcast(intent);
                break;
        }

    }

}
