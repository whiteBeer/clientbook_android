package com.example.myapplication;

import android.os.AsyncTask;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.appcompat.app.AppCompatActivity;
import android.telephony.SmsManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.PendingIntent;

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

// TODO:
class BackendQuery {
    public void getUrl (String urlString) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                URL url;
                HttpURLConnection urlConnection = null;
                try {
                    url = new URL(urlString);
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
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
        });
        thread.start();
    }
}

public class MainActivity extends AppCompatActivity {

    private static final int CLIENTBOOK_PERMISSIONS_REQUEST_SEND_SMS = 0;
    Button sendBtn;
    Boolean isError = false;

    final private Timer mTimer = new Timer();
    final private TimerTask mTask = new TimerTask() {
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
                    String phone = payloadObject.getString("phone");
                    String message = payloadObject.getString("message");
                    String smsId = payloadObject.getString("smsId");
                    createSmsMessage(phone, message, smsId);
                } else {
                    System.out.print("No messages");
                }
                isError = false;
            } catch (java.net.UnknownHostException e) {
                System.out.println("Wi-fi problem");
            } catch (Exception e) {
                if (!isError) {
                    createSmsMessage("0715009860", "Alarm! clientbook.ru server error!", "");
                    isError = true;
                }
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

        sendBtn.setOnClickListener(view -> {
            createSmsMessage("0715009860", "Test message.", "");
        });

        mTimer.scheduleAtFixedRate(mTask, 3000, 10000);
    }

    public void createSmsMessage (String phone, String message, String smsId) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {
                    Manifest.permission.SEND_SMS
            }, CLIENTBOOK_PERMISSIONS_REQUEST_SEND_SMS);
        } else {
            sendSmsMessage(phone, message, smsId);
        }
    }

    public void sendSmsMessage (String phone, String message, String smsId) {
        String DELIVERED = "DELIVERED";
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);
        registerReceiver(
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context arg0, Intent arg1) {
                    new BackendQuery().getUrl("https://clientbook.ru/rest/sms/sentSuccess?auth=clientbook_secret&smsId=" + smsId);
                }
            },
            new IntentFilter(DELIVERED)
        );
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phone, null, message, null, deliveredPI);
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CLIENTBOOK_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // sendSmsMessage();
                } else {
                    String errMsg = "SMS failed, please try again.";
                    Toast.makeText(getApplicationContext(), errMsg, Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }
}