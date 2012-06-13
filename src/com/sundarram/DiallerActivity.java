package com.sundarram;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class DiallerActivity extends Activity implements View.OnClickListener {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialler);
        new Helper.IpSetter().execute((TextView)findViewById(R.id.ip_address));


        findViewById(R.id.one).setOnClickListener(this);
        findViewById(R.id.two).setOnClickListener(this);
        findViewById(R.id.three).setOnClickListener(this);
        findViewById(R.id.four).setOnClickListener(this);
        findViewById(R.id.five).setOnClickListener(this);
        findViewById(R.id.six).setOnClickListener(this);
        findViewById(R.id.seven).setOnClickListener(this);
        findViewById(R.id.eight).setOnClickListener(this);
        findViewById(R.id.nine).setOnClickListener(this);
        findViewById(R.id.zero).setOnClickListener(this);
        findViewById(R.id.period).setOnClickListener(this);
        findViewById(R.id.clear).setOnClickListener(this);
        findViewById(R.id.call).setOnClickListener(this);

    }

    public void onClick(View view) {
        EditText diallerField = (EditText)findViewById(R.id.dtmfDialerField);

        switch (view.getId()) {
            case R.id.one:
                diallerField.append("1");
                checkDiallerField(diallerField.getText().toString());
                break;
            case R.id.two:
                diallerField.append("2");
                checkDiallerField(diallerField.getText().toString());
                break;
            case R.id.three:
                diallerField.append("3");
                checkDiallerField(diallerField.getText().toString());
                break;
            case R.id.four:
                diallerField.append("4");
                checkDiallerField(diallerField.getText().toString());
                break;
            case R.id.five:
                diallerField.append("5");
                checkDiallerField(diallerField.getText().toString());
                break;
            case R.id.six:
                diallerField.append("6");
                checkDiallerField(diallerField.getText().toString());
                break;
            case R.id.seven:
                diallerField.append("7");
                checkDiallerField(diallerField.getText().toString());
                break;
            case R.id.eight:
                diallerField.append("8");
                checkDiallerField(diallerField.getText().toString());
                break;
            case R.id.nine:
                diallerField.append("9");
                checkDiallerField(diallerField.getText().toString());
                break;
            case R.id.zero:
                diallerField.append("0");
                checkDiallerField(diallerField.getText().toString());
                break;
            case R.id.period:
                diallerField.append(".");
                checkDiallerField(diallerField.getText().toString());
                break;
            case R.id.clear:
                diallerField.setText("");
                findViewById(R.id.call).setEnabled(false);
                break;
            case R.id.call:
                makeCall(diallerField.getText().toString());
                break;

        }
    }

    private void checkDiallerField(String ip) {
        if(ip.contains(".")) {
            String[] subIpStr = ip.split("\\.");
            Integer[] subIpInt = new Integer[4];
            if(subIpStr.length == 4) {
                for(int index = 0; index < 4; index++)
                    subIpInt[index] = Integer.parseInt(subIpStr[index]);
                if(valid(subIpInt[0]) && valid(subIpInt[1]) && valid(subIpInt[2]) && valid(subIpInt[3]))
                    findViewById(R.id.call).setEnabled(true);
                else
                    findViewById(R.id.call).setEnabled(false);
            }
            else
                findViewById(R.id.call).setEnabled(false);
        }

    }

    private boolean valid(int number) {
        if(number >= 0 && number <=255)
            return true;
        else
            return false;
    }

    private void makeCall(String ip) {
        Intent intent = new Intent(this, InCallActivity.class);
        intent.putExtra("target", ip);
        intent.putExtra("requestCode", InCallActivity.DIALLED);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }
}