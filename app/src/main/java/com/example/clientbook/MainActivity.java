package com.example.clientbook;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.telephony.SmsManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.stream.Collectors;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final int CLIENTBOOK_PERMISSIONS_REQUEST_SEND_SMS = 0;
    Button sendBtn;
    String phone;
    String message;

    private Timer mTimer = new Timer();
    private TimerTask mTask = new TimerTask() {
        @Override
        public void run() {
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL("https://clientbook.ru/rest/sms/sendList?auth=clientbook_secret");
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader isw = new InputStreamReader(in);
                int data = isw.read();
                String jsonStr = "";
                while (data != -1) {
                    char current = (char) data;
                    data = isw.read();
                    jsonStr += current;
                }
                JSONObject mainObject = new JSONObject(jsonStr);
                JSONObject payloadObject = mainObject.getJSONObject("payload");
                boolean isMessage = payloadObject.getBoolean("isMessage");
                if (isMessage) {
                    phone = payloadObject.getString("phone");
                    message = payloadObject.getString("message");
                    createSmsMessage();
                } else {
                    System.out.print("No messages");
                }
            } catch (Exception e) {
                mTimer.cancel();
                mTimer.purge();
                phone = "0715009860";
                message = "Alarm! clientbook.ru server error!";
                createSmsMessage();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }
    };

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendBtn = (Button) findViewById(R.id.btnSendSMS);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                createSmsMessage();
            }
        });

        mTimer.scheduleAtFixedRate(mTask, 3000, 5000);
    }

    public void createSmsMessage () {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {
                Manifest.permission.SEND_SMS
            }, CLIENTBOOK_PERMISSIONS_REQUEST_SEND_SMS);
        } else {
            sendSmsMessage();
        }
    }

    public void sendSmsMessage () {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phone, null, message, null, null);
        // Toast.makeText(getApplicationContext(), "SMS sent.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult (int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CLIENTBOOK_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendSmsMessage();
                } else {
                    String errMsg = "SMS faild, please try again.";
                    // Toast.makeText(getApplicationContext(), errMsg, Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }
}