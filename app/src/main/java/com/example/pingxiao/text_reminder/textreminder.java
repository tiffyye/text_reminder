package com.example.pingxiao.text_reminder;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.pingxiao.test_reminder.R;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.UUID;

/**
 * Created by pingxiao on 4/19/15.
 */
public class textreminder extends Activity{

    static final int TIME_DIALOG_ID = 1111;
    private TextView output;
    private TextView debug;
    public Button btnClick;
    public Button btnOn, btnOff;

    private int hour;
    private int minute;

    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");// whats this
    private static String address = "20:14:10:15:11:47";

    /*PINGXIAO BLUETOOTH MAC ADDRESS: D0:E1:40:9C:2D:FB
     *SNEGHA PHONE NEXUS: 48:59:29:56:12:58
     * HC-06: 20:14:10:15:11:47
     */

    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "LEDOnOff";

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;


    //private Button deleteMsgBtn;
    private TextView text;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.textreminder);

        output = (TextView) findViewById(R.id.output);
        debug = (TextView) findViewById(R.id.debug);
        final Calendar c = Calendar.getInstance();
        hour = c.get(Calendar.HOUR_OF_DAY);
        minute = c.get(Calendar.MINUTE);

        updateTime(hour, minute);

        addButtonClickListener();

        btnOn = (Button) findViewById(R.id.btnOn);
        btnOff = (Button) findViewById(R.id.btnOff);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth Not Supported. Aborting.");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth is Enabled");
            } else {
                btAdapter.enable();
                Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        //Update: BluetoothConnection() this would be placed in OnResume()

        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("N".getBytes());
                Toast msg = Toast.makeText(getBaseContext(),
                        "You have clicked On", Toast.LENGTH_SHORT);
                msg.show();
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("F".getBytes());
                Toast msg = Toast.makeText(getBaseContext(),
                        "You have clicked Off", Toast.LENGTH_SHORT);
                msg.show();
            }
        });

        final EditText editText = (EditText) findViewById(R.id.textreminder);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    Log.v("EditText", editText.getText().toString());
                    sendData(editText.getText().toString().getBytes());
                    handled = true;
                }
                return handled;
            }
        });



    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "...In onResume(), attempting client connect...");
        BluetoothConnection();
        //Note: Before you click on any buttons, this part would be ready already (datastream is ready)
    }
    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()");

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and unable to close socket: " + e2.getMessage() + ".");
        }

    }




    //DO WE NEED TO HAVE ONDESTROY AND ONSTOP
    private void errorExit(String title, String message) {
        Toast msg = Toast.makeText(getBaseContext(),
                title + "-" + message, Toast.LENGTH_SHORT);
        msg.show();
        finish();
    }

    private void BluetoothConnection() { //ADD THE FEATURE WHERE THE USER CAN SEARCH WHEN FIRST PAIRING
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            btSocket = null;
            errorExit("Fatal Error", "In BluetoothConnection() and socket create failed: " + e.getMessage() + ".");
        }

        btAdapter.cancelDiscovery();

        Log.d(TAG, "...Connecting remote");
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
                errorExit("Fatal Error", "In BluetoothConnection() and unable to connect socket" + e.getMessage() + ".");
            } catch (IOException e2) {
                errorExit("Fatal Error", "In BluetoothConnection() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        Log.d(TAG, "...Creating socket");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In sendData() and output stream creation failed" + e.getMessage() + ".");
        }

        // String msg = "We are now right before the bluetooth connection ends";
        // debug.setText(msg);
    }

    private void sendData(byte[] msgBuffer) {
        //byte[] msgBuffer = message.getBytes();
        Log.d(TAG, "...Creating socket");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In sendData() and output stream creation failed" + e.getMessage() + ".");
        }
        //Log.d(TAG, "...Sending data" + message + "...");

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "In OnCreate and an exception occurred during write" + e.getMessage();
            errorExit("Fatal Error", msg);
        }

    }

    public void addButtonClickListener() {

        btnClick = (Button) findViewById(R.id.btnClick);
        btnClick.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showDialog(TIME_DIALOG_ID);
            }
        });
    }
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case TIME_DIALOG_ID:
                return new TimePickerDialog(this, timePickerListener, hour, minute,
                        false);
        }
        return null;
    }

    private TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener() {

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minutes) {
            hour = hourOfDay;
            minute = minutes;
            String hours = "";
            String minutess = "";
            if(hour < 10)
                hours = "0"+hour;
            else
                hours = ""+hour;
            if(minute < 10)
                minutess = "0"+minute;
            else
                minutess = ""+minute;
            String aTime = new StringBuilder().append(hours)
                    .append(minutess).toString();
            System.out.println(aTime);
            sendData((aTime).getBytes());
            updateTime(hour, minute);
        }

    };

    private void updateTime(int hours, int mins) {

        String timeSet = "";
        if (hours > 12) {
            hours -= 12;
            timeSet = "PM";
        } else if (hours == 0) {
            hours += 12;
            timeSet = "AM";
        } else if (hours == 12)
            timeSet = "PM";
        else
            timeSet = "AM";


        String minutes = "";
        if (mins < 10)
            minutes = "0" + mins;
        else
            minutes = String.valueOf(mins);

        String aTime = new StringBuilder().append(hours).append(':')
                .append(minutes).append(" ").append(timeSet).toString();

        output.setText(aTime);

    }
}

