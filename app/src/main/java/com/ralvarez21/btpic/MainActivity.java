package com.ralvarez21.btpic;

import static com.ralvarez21.btpic.Constants.ANGLE_PREF_NAME;
import static com.ralvarez21.btpic.Constants.APP_PREF_NAME;
import static com.ralvarez21.btpic.Constants.BTTAG;
import static com.ralvarez21.btpic.Constants.BT_DATA_RCV_BR;
import static com.ralvarez21.btpic.Constants.BT_DATA_RCV_BR_EXTRA;
import static com.ralvarez21.btpic.Constants.BT_DATA_RCV_ERROR;
import static com.ralvarez21.btpic.Constants.BT_TIMER_ANGLE;
import static com.ralvarez21.btpic.Constants.BT_TIMER_FINISH;
import static com.ralvarez21.btpic.Constants.MASS_PREF_NAME;
import static com.ralvarez21.btpic.Constants.SET_CONNECTED_STATUS_BR;
import static com.ralvarez21.btpic.Constants.SET_CONNECTED_STATUS_EXTRA_DEVICE;
import static com.ralvarez21.btpic.Constants.SET_DISCONNECTED_STATUS_BR;
import static com.ralvarez21.btpic.Constants.TOOGLE_LOADING_BR;
import static com.ralvarez21.btpic.Constants.TOOGLE_LOADING_EXTRA_STATE;
import static com.ralvarez21.btpic.Constants.UPDATE_BTSOCKET_BR;
import static com.ralvarez21.btpic.Constants.VEL_PREF_NAME;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

import android.os.CountDownTimer;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.airbnb.lottie.LottieAnimationView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity {

    private CountDownTimer rcvTimer;
    private BluetoothAdapter bAdapter;
    private ArrayList<BluetoothDevice> pairedDevices;
    private ToggleButton tgBt;
    private Spinner spPaired;
    private Button btnConnect, btnSend, btnGraph;
    private LineChart chart;
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
    private float rocketVel;
    private boolean isAngleSendMinor = false;
    private RadioButton rdAngle, rdDistance, rdHeight;
    private final String[] permissionsRequired = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN};
    private final int[] permissionsCodes = new int[]{100, 101, 102};
    SharedPreferences shPrefs;
    private LottieAnimationView btnLaunch;


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
        //rocketMass = shPrefs.getFloat(MASS_PREF_NAME, 2);
        rocketVel = shPrefs.getFloat(VEL_PREF_NAME, 20);

        shPrefs.registerOnSharedPreferenceChangeListener(shListener);

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
        btFilters.addAction(BT_DATA_RCV_BR);
        btFilters.addAction(BT_DATA_RCV_ERROR);
        btFilters.addAction(BT_TIMER_FINISH);

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
        btnGraph = findViewById(R.id.btnGraph);
        chart = findViewById(R.id.chtLine);
        btnLaunch = findViewById(R.id.btnLaunch);

        lblAngle.setText(getString(R.string.lbl_actual_angle, actualAngle));

        bAdapter = BluetoothAdapter.getDefaultAdapter();

        tgBt.setOnCheckedChangeListener((btn, b) -> {
            if (b) {
                turnOnBT();
            } else {
                turnOffBT();
            }
        });

        btnLaunch.setOnClickListener(v -> {
            btnLaunch.setAnimation(R.raw.rocket_launch);
            btnLaunch.playAnimation();
            //Enviar aqui la notificacion
            if(btSocket != null) {
                try {
                    OutputStream out = btSocket.getOutputStream();
                    out.write(255);
                } catch(Exception ex){
                    Log.e(BTTAG, ex.toString());
                }
            }
            new CountDownTimer(5000, 1000){
                @Override
                public void onTick(long l) {
                    Log.i(BTTAG,"T - " + (l / 1000));
                }

                @Override
                public void onFinish() {
                    btnLaunch.setAnimation(R.raw.rocket_wait);
                    btnLaunch.playAnimation();
                }
            }.start();
        });

        addSpPairedListener();

        addBtnConnectListener();

        addBtnSendListener();

        addBtnGraphListener();

        mapBTStatus();

        //setLayoutData(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        MenuItem item = menu.findItem(R.id.action_settings);
        item.setOnMenuItemClickListener(menuItem -> {
            startActivity(new Intent(this, Settings.class));
            return true;
        });
        return true;
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
                        int data = Integer.parseInt(dataToSend);
                        int totalSteps = 0; // Esta es la variable que va a controlar cuantos pasos va a dar y en que direccion. Si es 0 no enviar
                        int angle = 0;
                        int fullAngle = 0;
                        boolean sendData = true;
                        double initialVel = Math.pow(rocketVel, 2);
                        double sin_90 = Math.sin(90 * Math.PI / 180);
                        double sin_45 = Math.sin(45 * Math.PI / 180);
                        double sin_90_squared = Math.pow(sin_90, 2);
                        if (rdAngle.isChecked()){
                            if (!validateAngleValue(data)){
                                Toasty.warning(this, getResources().getString(R.string.ts_angle_error)).show();
                                sendData = false;
                            }else {
                                angle = data;

                            }
                        }else if(rdDistance.isChecked()){
                            // Validar el valor de la distancia seleccionada
                            int maxDistance = (int) Math.floor(initialVel/9.81 * sin_45);
                            if (data > maxDistance){
                                Toasty.warning(this, getResources().getString(R.string.ts_distance_error, maxDistance)).show();
                                sendData = false;
                            }else {
                                angle = data == 0 ? 90 : (int) Math.ceil(Math.asin(data*9.81/initialVel) * (180 / Math.PI));
                            }
                        }else if(rdHeight.isChecked()){
                            double  maxHeight = ((initialVel/9.81) * sin_90_squared);
                            Log.i(BTTAG, "Max height: " + maxHeight);
                            if (data < rocketVel || data > maxHeight){
                                Toasty.warning(this, getResources().getString(R.string.ts_height_error, maxHeight)).show();
                                sendData = false;
                            }else{
                                angle = (int) Math.ceil( Math.asin((9.81*data)/(initialVel)) * 180 / Math.PI );
                            }
                        }
                        fullAngle = angle;
                        Log.i(BTTAG, "Angulo obtenido: " + angle);

                        // Validar que el angulo actual (a enviar) sea mayor que el que se tiene en el dispositivo (preferencias)
                        // Si el angulo es menor, activar la bandera de negativo
                        if (angle < actualAngle){
                            //int tempActualAngle = actualAngle;
                            angle = actualAngle - angle;
                            Log.i(BTTAG, "Angulo a calibrar: " + angle);
                            int tempSteps  = convertToSteps(angle);
                            Log.i(BTTAG, "Pasos a dar: " + tempSteps);
                            String binData = RPad(Integer.toBinaryString(tempSteps), 8, '0');
                            Log.i(BTTAG, "Pasos a dar en binario: " + binData);
                            char[] stepsInBin = binData.toCharArray();
                            stepsInBin[0] = '1';
                            StringBuilder tempResultNumber = new StringBuilder();
                            for (char c :  stepsInBin){
                                tempResultNumber.append(c);
                            }
                            Log.i(BTTAG, "Valor binario con bandera: " + tempResultNumber);
                            angle = Integer.parseInt(tempResultNumber.toString(), 2);
                            Log.i(BTTAG, "Angulo obtenido" + angle);
                            totalSteps = angle;
                            isAngleSendMinor = true;
                        }else if (angle > actualAngle) {
                            angle -= actualAngle;
                            fullAngle = angle;
                            totalSteps = convertToSteps(angle);
                            isAngleSendMinor = false;
                        }else {
                            sendData = false;
                            isAngleSendMinor = false;
                        }

                        if (sendData){
                            //totalSteps = convertToSteps(angle);
                            Log.i(BTTAG, "Sending steps: " + totalSteps);
                            out.write(totalSteps);
                            int finalTotalAngle = fullAngle;
                            rcvTimer = new CountDownTimer( 30000, 1000) {
                                @Override
                                public void onTick(long l) {
                                    Log.i(BTTAG, "Remaining time: " + (l / 1000));
                                }

                                @Override
                                public void onFinish() {
                                    Intent timeOutIntent = new Intent(BT_TIMER_FINISH);
                                    timeOutIntent.putExtra(BT_TIMER_ANGLE, finalTotalAngle);
                                    getApplicationContext().sendBroadcast(timeOutIntent);
                                }
                            }.start();
                        }
                    }catch (Exception ex) {
                        Log.e(BTTAG, ex.getMessage());
                        Toasty.warning(this, getString(R.string.ts_data_warning)).show();
                    }
                }catch(IOException e) {
                    Toasty.error(this, getString(R.string.ts_data_error)).show();
                }
            }
        });
    }

    private void addBtnGraphListener(){
        btnGraph.setOnClickListener(v -> {
            if (txtData.getText().toString().isEmpty()){
                Toasty.error(this, getResources().getString(R.string.ts_data_warning)).show();
            }else {
                float angle = 0, distance = 0, height = 0;
                double initialVel = Math.pow(rocketVel, 2);
                if (rdAngle.isChecked()){
                    angle = Float.parseFloat(txtData.getText().toString());
                    distance = (float) ((Math.pow(initialVel,2)*(Math.sin(Math.toRadians(angle*2))))/9.81);
                    height = (float)((Math.pow(initialVel,2)*Math.pow(Math.sin(Math.toRadians(angle)),2))/(9.81*2));
                }else if (rdDistance.isChecked()){
                    distance = Float.parseFloat(txtData.getText().toString());
                    angle = distance == 0 ? 90 : (int) Math.ceil(Math.asin(distance*9.81/initialVel) * (180 / Math.PI));
                    height = (float)((Math.pow(initialVel,2)*Math.pow(Math.sin(Math.toRadians(angle)),2))/(9.81*2));
                }else if (rdHeight.isChecked()){
                    height = Float.parseFloat(txtData.getText().toString());
                    angle = (float) Math.ceil( Math.asin((9.81*height)/(initialVel)) * 180 / Math.PI );
                    distance = (float) ((Math.pow(initialVel,2)*(Math.sin(Math.toRadians(angle*2))))/9.81);
                }
                LineDataSet line1 = new LineDataSet(dataValues1(angle, height, distance),"Grafica del tiro parabolico");
                ArrayList<ILineDataSet> dataset = new ArrayList<>();
                dataset.add(line1);
                LineData data = new LineData(dataset);
                chart.setData(data);
                chart.invalidate();
                chart.setVisibility(View.VISIBLE);
            }
        });
    }

    private ArrayList<Entry> dataValues1(float angulo, float altura, float distancia){
        ArrayList<Entry> dataVals = new ArrayList<>();
        float p = 0,y = 0,h = 0, k = 0, s = 0, x = 0;
        s = distancia/20;
        h = distancia/2;
        k = altura;
        p = (float) Math.pow(h,2)/(-4*k);
        while(x < distancia){
            y = (float) ((Math.pow(x,2) - 2*x*h + Math.pow(h,2) + 4*p*k)/(4*p));
            dataVals.add(new Entry(x,y));
            x += s;
        }
        dataVals.add(new Entry(distancia,0)); // Distancia Maxima
        return dataVals;
    }

    private boolean validateAngleValue(int angle_value){
        Log.i(BTTAG, "Validando datos del angulo");
        return angle_value > 0 && angle_value <= 180;
    }

    public static String RPad(String str, Integer length, char car) {
        return (String.format("%" + length + "s", "").replace(" ", String.valueOf(car)) + str).substring(str.length(), length + str.length());
    }

    private int convertToSteps(int in){
        return (int) Math.round(in / 1.8);
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
        shPrefs.registerOnSharedPreferenceChangeListener(shListener);
        if (btDevSelected != null){
            spPaired.setSelection(selectedIndex);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(receiver);
        shPrefs.unregisterOnSharedPreferenceChangeListener(shListener);
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener shListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            Log.i(BTTAG, "Prefs changed");
            actualAngle = shPrefs.getInt(ANGLE_PREF_NAME, 0);
            //rocketMass = shPrefs.getFloat(MASS_PREF_NAME, 2);
            rocketVel = shPrefs.getFloat(VEL_PREF_NAME, 20);
            lblAngle.setText(getString(R.string.lbl_actual_angle, actualAngle));
        }
    };

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
                Log.i(BTTAG, "Device found: " + deviceName);
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
            }else if (action.equals(BT_DATA_RCV_BR)){
                if(rcvTimer != null){
                    rcvTimer.cancel();
                }
                int newDevAngle = intent.getIntExtra(BT_DATA_RCV_BR_EXTRA, 0);
                int angleToSave = 0;
                Log.i(BTTAG, "Data from Intent BT_RCV: " + newDevAngle);
                if (isAngleSendMinor){
                    Log.i(BTTAG, "El angulo recibido es menor al actual");
                    angleToSave = actualAngle - newDevAngle;
                }else {
                    angleToSave = actualAngle + newDevAngle;
                }
                actualAngle = angleToSave;
                shPrefs.edit().putInt(ANGLE_PREF_NAME, angleToSave).apply();
                Toasty.success(getApplicationContext(),  getResources().getString(R.string.bt_rcv_info, actualAngle)).show();
            }else if (action.equals(BT_TIMER_FINISH)){
                int sentAngle = intent.getIntExtra(BT_TIMER_ANGLE, 0);
                int angleToSave = 0;
                Log.i(BTTAG, "Data from timeout: " + sentAngle);
                if (isAngleSendMinor){
                    Log.i(BTTAG, "El angulo recibido es menor al actual");
                    angleToSave = actualAngle - sentAngle;
                }else {
                    angleToSave = actualAngle + sentAngle;
                }
                actualAngle = angleToSave;
                shPrefs.edit().putInt(ANGLE_PREF_NAME, angleToSave).apply();
                Toasty.success(getApplicationContext(),  getResources().getString(R.string.bt_rcv_info, actualAngle)).show();
            } else{
                Toasty.error(getApplicationContext(), getResources().getString(R.string.bt_rcv_error)).show();
            }
        }
    };

}