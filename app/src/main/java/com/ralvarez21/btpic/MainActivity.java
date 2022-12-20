package com.ralvarez21.btpic;

import static com.ralvarez21.btpic.Constants.ANGLE_PREF_NAME;
import static com.ralvarez21.btpic.Constants.APP_PREF_NAME;
import static com.ralvarez21.btpic.Constants.BTTAG;
import static com.ralvarez21.btpic.Constants.SET_CONNECTED_STATUS_BR;
import static com.ralvarez21.btpic.Constants.SET_CONNECTED_STATUS_EXTRA_DEVICE;
import static com.ralvarez21.btpic.Constants.SET_DISCONNECTED_STATUS_BR;
import static com.ralvarez21.btpic.Constants.TOOGLE_LOADING_BR;
import static com.ralvarez21.btpic.Constants.TOOGLE_LOADING_EXTRA_STATE;
import static com.ralvarez21.btpic.Constants.UPDATE_BTSOCKET_BR;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.ProcessCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity {


    private BluetoothAdapter bAdapter;
    private ArrayList<BluetoothDevice> pairedDevices;
    private ToggleButton tgBt;
    private Spinner spPaired;
    private Button btnConnect, btnSend;
    private BTHelper btHelperThread = null;
    private BluetoothDevice btDevSelected = null;
    private TextView lblConStatus;
    private BluetoothSocket btSocket;
    private ProgressBar pbConStatus;
    private LinearLayout lyData;
    private EditText txtData;
    private int selectedIndex = 0;
    private TextView lblAngle;
    private int actualAngle = 0;
    private RadioButton rdAngle, rdDistance, rdHeight;
    private final String[] permissionsRequired = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN};
    private final int[] permissionsCodes = new int[]{100, 101, 102};
    SharedPreferences shPrefs;


    @SuppressLint("MissingPermission")
    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.i(BTTAG, "Result code: " + result.getResultCode());
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.i(BTTAG, "OK");
                    // There are no request codes
                } else if (result.getResultCode() == 120) {
                    if (!bAdapter.isDiscovering()) {
                        bAdapter.startDiscovery();
                    }
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        shPrefs = getSharedPreferences(APP_PREF_NAME, MODE_PRIVATE);
        actualAngle = shPrefs.getInt(ANGLE_PREF_NAME, 0);

        if (ContextCompat.checkSelfPermission(this, permissionsRequired[0]) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, new String[]{permissionsRequired[0]}, permissionsCodes[0]);
                return;
            }
        }

        IntentFilter btFilters = new IntentFilter();

        btFilters.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        btFilters.addAction(TOOGLE_LOADING_BR);
        btFilters.addAction(SET_CONNECTED_STATUS_BR);
        btFilters.addAction(SET_DISCONNECTED_STATUS_BR);
        btFilters.addAction(UPDATE_BTSOCKET_BR);

        registerReceiver(receiver, btFilters);

        tgBt = findViewById(R.id.tgBT);
        spPaired = findViewById(R.id.spPairedDev);
        btnConnect = findViewById(R.id.btnConnect);
        lblConStatus = findViewById(R.id.lblConStatus);
        pbConStatus = findViewById(R.id.pbConStatus);
        lyData = findViewById(R.id.lyData);
        btnSend = findViewById(R.id.btnSend);
        txtData = findViewById(R.id.txtData);
        lblAngle = findViewById(R.id.lblActualAngle);
        rdAngle = findViewById(R.id.rdAngle);
        rdDistance = findViewById(R.id.rdDistance);
        rdHeight = findViewById(R.id.rdHeight);

        lblAngle.setText(getString(R.string.lbl_actual_angle, actualAngle));

        bAdapter = BluetoothAdapter.getDefaultAdapter();

        tgBt.setOnCheckedChangeListener((btn, b) -> {
            if (b) {
                turnOnBT();
            } else {
                turnOffBT();
            }
        });

        addSpPairedListener();

        addBtnConnectListener();

        addBtnSendListener();

        mapBTStatus();

    }

    public void setLayoutData(boolean status){
        lyData.setVisibility(status ? View.VISIBLE : View.GONE);
        txtData.setText("");
    }

    private void setPairedAdapter(){
        if (bAdapter.isEnabled()) {
            if (ContextCompat.checkSelfPermission(this, permissionsRequired[1]) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(this, new String[]{permissionsRequired[1]}, permissionsCodes[1]);
                    return;
                }
            }
            pairedDevices = new ArrayList<>(bAdapter.getBondedDevices());
            ArrayList<String> lstPairedDev = new ArrayList<>();
            lstPairedDev.add("");
            for (BluetoothDevice device: pairedDevices){
                lstPairedDev.add(device.getName() + " - " + device.getAddress());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, lstPairedDev);

            spPaired.setAdapter(adapter);

        }else{
            spPaired.setAdapter(null);
        }
    }

    private void mapBTStatus(){
        tgBt.setChecked(bAdapter.isEnabled());
        if(bAdapter.isEnabled()){
            setPairedAdapter();
            btnConnect.setEnabled(true);
            btnConnect.setBackground(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.rd_btn_blue));
        }else{
            spPaired.setAdapter(null);
            btnConnect.setEnabled(false);
            btnConnect.setBackground(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.rd_btn_blue_dis));
        }
    }

    private void addSpPairedListener() {
        spPaired.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedIndex = i;
                if (i != 0) {
                    btDevSelected = pairedDevices.get(i - 1);
                    Log.i(BTTAG, "Selected item: " + btDevSelected.getAddress());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.i(BTTAG, "Sin seleccion");
            }
        });
    }

    private void addBtnConnectListener(){
        btnConnect.setOnClickListener(v -> {
            if (btDevSelected != null){
                if(btHelperThread != null){
                    //btHelperThread.cancel();
                    btHelperThread = null;
                    btnConnect.setText(getString(R.string.btn_connectToDevice));
                    lblConStatus.setText(getString(R.string.bt_statusDisconnected));
                    setLayoutData(false);
                }else {
                    btHelperThread = new BTHelper(btDevSelected, bAdapter, this);
                    btHelperThread.start();
                }
            }else {
                Toasty.error(this, getString(R.string.ts_no_device_error), Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void addBtnSendListener(){
        btnSend.setOnClickListener(v -> {
            if(btSocket != null) {
                try{
                    OutputStream out = btSocket.getOutputStream();
                    String dataToSend = txtData.getText().toString();
                    try {
                        // Algo debe de ir aqui, pero no recuerdo que
                        int data = Integer.parseInt(dataToSend);
                        boolean sendData = true;
                        if (rdAngle.isChecked()){
                            Log.i(BTTAG, "Validando datos del angulo");
                            if (data > 180 || data < 0){
                                Toasty.warning(this, getString(R.string.ts_angle_error)).show();
                                sendData = false;
                            }else {
                                data = convertToSteps(data);
                            }
                        } // Aplicar las formulas de tiro parabolico para calcular el angulo y con base a ello, dividir entre 1.8 y mandar
                        if (sendData){
                            Log.i(BTTAG, "Sending data: " + data);
                            out.write(data);
                        }
                    }catch (Exception ex) {
                        Log.e(BTTAG, ex.getLocalizedMessage());
                        Toasty.warning(this, getString(R.string.ts_data_warning)).show();
                    }
                }catch(IOException e) {
                    Toasty.error(this, getString(R.string.ts_data_error)).show();
                }
            }
        });
    }

    private int convertToSteps(int in){
        return (int) Math.ceil(in / 1.8);
    }

    private void turnOnBT() {
        if (ContextCompat.checkSelfPermission(this, permissionsRequired[1]) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, new String[]{permissionsRequired[1]}, permissionsCodes[1]);
                return;
            }
        }
        if (!bAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activityResultLauncher.launch(intent);
        }
    }

    private void turnOffBT() {
        if (ContextCompat.checkSelfPermission(this, permissionsRequired[2]) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, new String[]{permissionsRequired[2]}, permissionsCodes[2]);
                return;
            }
        }
        if (bAdapter.isEnabled()) {
            bAdapter.cancelDiscovery();
            bAdapter.disable();
        }
    }

    private void toogleLoad(boolean status){
        btnConnect.setVisibility(status ? View.GONE : View.VISIBLE);
        pbConStatus.setVisibility(status ? View.VISIBLE : View.GONE);
        pbConStatus.setIndeterminate(status);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == permissionsCodes[0] && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(this, this.getClass()));
        } else if (requestCode == permissionsCodes[1] && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (!bAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activityResultLauncher.launch(intent);
            }
        } else if (requestCode == permissionsCodes[2] && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            turnOffBT();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        mapBTStatus();
        if (btDevSelected != null){
            spPaired.setSelection(selectedIndex);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(receiver);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(BTTAG, "Accion recibida: " + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 1);
                    return;
                }
                String deviceName = device.getName();
                //String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.i(BTTAG, "Device found: " + deviceName); //+ " - " + deviceHardwareAddress);
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int estado = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (estado) {
                    case BluetoothAdapter.STATE_ON: {
                        mapBTStatus();
                        tgBt.setTextOn(getString(R.string.btStatus_PreOn));
                        Log.i(BTTAG, "STATE_ON - " + estado);
                        setPairedAdapter();
                        break;
                    }
                    case BluetoothAdapter.STATE_OFF: {
                        tgBt.setTextOff(getString(R.string.btStatus_Off));
                        mapBTStatus();
                        Log.i(BTTAG, "STATE_OFF - " + estado);
                        break;
                    }
                    case BluetoothAdapter.STATE_TURNING_ON: {
                        tgBt.setTextOn(getString(R.string.btStatus_On));
                        Log.i(BTTAG, "PREV_ON - " + estado);
                        break;
                    }
                    case BluetoothAdapter.STATE_TURNING_OFF: {
                        tgBt.setTextOff(getString(R.string.btStatus_PreOff));
                        Log.i(BTTAG, "PREV_OFF - " + estado);
                        break;
                    }
                    case BluetoothAdapter.STATE_CONNECTED: {
                        String devName = intent.getStringExtra(BluetoothAdapter.EXTRA_LOCAL_NAME);
                        Log.i(BTTAG, devName);
                    }
                }
                spPaired.setEnabled(bAdapter.isEnabled());
            }else if (action.equals(TOOGLE_LOADING_BR)){
                boolean status = intent.getBooleanExtra(TOOGLE_LOADING_EXTRA_STATE, false);
                toogleLoad(status);
            }else if (action.equals(SET_CONNECTED_STATUS_BR)){
                String devName = intent.getStringExtra(SET_CONNECTED_STATUS_EXTRA_DEVICE);
                lblConStatus.setText(getString(R.string.lbl_connected_to_device, devName));
                btnConnect.setText(getString(R.string.btn_disconnectFromDevice));
                setLayoutData(true);
            }else if (action.equals(UPDATE_BTSOCKET_BR)){
                if(btHelperThread != null){
                    btSocket = btHelperThread.thSocket;
                }

            }
        }
    };

}