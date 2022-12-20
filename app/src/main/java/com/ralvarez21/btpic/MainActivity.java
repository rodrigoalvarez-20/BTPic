package com.ralvarez21.btpic;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;

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
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bAdapter;
    private ArrayList<BluetoothDevice> pairedDevices;
    private ToggleButton tgBt;
    private Spinner spPaired;
    private Button btnConnect, btnSend;
    private BTHelper btThread = null;
    private BluetoothDevice btDevSelected = null;
    public TextView lblConStatus;
    public BluetoothSocket btSocket;
    public ProgressBar pbConStatus;
    public LinearLayout lyData;
    public EditText txtData;
    private int selectedIndex = 0;
    public TextView lblAngle;
    public int actualAngle = 0;

    @SuppressLint("MissingPermission")
    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.i("A-Result", "Result code: " + result.getResultCode());
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.e("Activity result", "OK");
                    // There are no request codes
                }else if(result.getResultCode() == 120){
                    if (!bAdapter.isDiscovering()){
                        bAdapter.startDiscovery();
                    }

                }
            });

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter btFilters = new IntentFilter();

        btFilters.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

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

        bAdapter = BluetoothAdapter.getDefaultAdapter();



        tgBt.setOnCheckedChangeListener((btn, b) -> {
            if (b) {
                turnOnBT();
            } else {
                bAdapter.cancelDiscovery();
                bAdapter.disable();
            }
        });

        spPaired.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedIndex = i;
                if (i != 0){
                    btDevSelected = pairedDevices.get(i-1);
                    Log.i("BTSEL", "Selected item: " + btDevSelected.getAddress());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.i("BTSEL", "Sin seleccion");
            }
        });

        btnConnect.setOnClickListener(v -> {
            if (btDevSelected != null){
                if(btThread != null){
                    btThread.cancel();
                    btThread = null;
                    btnConnect.setText("Conectar dispositivo");
                    lblConStatus.setText("Desconectado");
                    setLayoutData(false);
                }else {
                    btThread = new BTHelper(btDevSelected);
                    btThread.start();
                }
            }else {
                Toast.makeText(this, "Por favor seleccione un dispositivo valido", Toast.LENGTH_SHORT).show();
            }

        });

        btnSend.setOnClickListener(v -> {
            if(btSocket != null) {
                try{
                    OutputStream out = btSocket.getOutputStream();
                    String dataToSend = txtData.getText().toString();
                    try {




                        int data = Integer.parseInt(dataToSend);
                        double totalSteps = data / 1.8;
                        data = (int) Math.ceil(totalSteps);
                        Log.i("BTDATA", "Sending data: " + data);
                        out.write(data);
                    }catch (Exception ex) {
                        Log.e("BTDATA", ex.getLocalizedMessage());
                        Toast.makeText(this, "Los valores permitidos son de 0 a 180", Toast.LENGTH_SHORT).show();
                    }
                }catch(IOException e) {
                    Toast.makeText(this, "Ha ocurrido un error al enviar los datos", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mapBTStatus();
    }

    public void setLayoutData(boolean status){
        lyData.setVisibility(status ? View.VISIBLE : View.GONE);
        txtData.setText("");
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("BTRCV", "Accion recibida: " + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 1);
                    return;
                }
                String deviceName = device.getName();
                //String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.i("BTDEVFOUND", "Device found: " + deviceName); //+ " - " + deviceHardwareAddress);
            }else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                int estado = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (estado){
                    case BluetoothAdapter.STATE_ON: {
                        mapBTStatus();
                        tgBt.setTextOn("Encendiendo...");
                        Log.i("BTSTATUS", "STATE_ON - " + estado);
                        setPairedAdapter();
                        break;
                    } case BluetoothAdapter.STATE_OFF: {
                        tgBt.setTextOff("Apagado");
                        mapBTStatus();
                        Log.i("BTSTATUS", "STATE_OFF - " + estado);
                        break;
                    } case BluetoothAdapter.STATE_TURNING_ON: {
                        tgBt.setTextOn("Encendido");
                        Log.i("BTSTATUS", "PREV_ON - " + estado);
                        break;
                    } case BluetoothAdapter.STATE_TURNING_OFF: {
                        tgBt.setTextOff("Apagando...");
                        Log.i("BTSTATUS", "PREV_OFF - " + estado);
                        break;
                    } case BluetoothAdapter.STATE_CONNECTED: {
                        String devName = intent.getStringExtra(BluetoothAdapter.EXTRA_LOCAL_NAME);
                        Log.i("BTSTATUS", devName);
                    }
                }
                spPaired.setEnabled(bAdapter.isEnabled());
            }
        }
    };

    private void turnOnBT(){
        if (!bAdapter.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activityResultLauncher.launch(intent);
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

    private void setPairedAdapter(){
        if (bAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 1);
                return;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Se ha concedido el acceso a BT", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "Se ha rechazado la solicitud de BT", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                // Bluetooth is on
                Toast.makeText(this, "BT Encendido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ha ocurrido un error al encender el BT", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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

    @SuppressLint("MissingPermission")
    private class BTHelper extends Thread {
        private final BluetoothSocket thSocket;
        private final BluetoothDevice thDev;

        public BTHelper(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            thDev = device;
            try {
                tmp = thDev.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
            } catch (IOException e) {
                Log.e("BTDEVICE", "Can't connect to service");
            }

            thSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            toogleLoad(true);
            if (bAdapter.isDiscovering()) {
                bAdapter.cancelDiscovery();
            }

            try {
                thSocket.connect();
                InputStream socketIn = thSocket.getInputStream();
                if (socketIn != null){
                    BTListener thRecv = new BTListener(socketIn);
                    thRecv.start();
                }
                runOnUiThread(() -> {
                    lblConStatus.setText("Connectado a " + thDev.getAddress());
                    btnConnect.setText("Desconectar dispositivo");
                    setLayoutData(true);
                });
                Log.i("BTDEVICE", "Connected to device");
            } catch (IOException connectException) {
                try {
                    if (btSocket!= null) {
                        btSocket.close();
                    }
                    runOnUiThread(() ->{
                        lblConStatus.setText("Desconectado");
                        btnConnect.setText("Conectar dispositivo");
                        setLayoutData(false);
                    });
                } catch (IOException closeException) {
                    Log.e("BTDEVICE", "Can't close socket");
                }
            }finally {
                toogleLoad(false);
            }

            btSocket = thSocket;

        }

        public void cancel() {
            try {
                thSocket.close();
            } catch (IOException e) {
                Log.e("BTDEVICE", "Can't close socket");
            }
        }

        private void toogleLoad(boolean status){
            runOnUiThread(() -> {
                btnConnect.setVisibility(status ? View.GONE : View.VISIBLE);
                pbConStatus.setVisibility(status ? View.VISIBLE : View.GONE);
                pbConStatus.setIndeterminate(status);
            });
        }
    }

    private class BTListener extends Thread {
        private InputStream sckIn = null;

        public BTListener(InputStream in){
            sckIn = in;
        }

        public void run(){
            if (sckIn != null){
                byte[] data = new byte[256];
                try {
                    int status = sckIn.read(data);
                    int actualSteps = Integer.parseInt(String.valueOf(data[0]));
                    int actualAngle = (int) Math.floor(actualSteps * 1.8);
                    if (status > 0){
                        Log.i("BTDATA", "Datos recibidos: " + Arrays.toString(data));
                        runOnUiThread(() ->{
                            lblAngle.setText("Angulo actual: " + actualAngle);
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void cancel(){
            try{
                if (sckIn != null){
                    sckIn.close();
                }
            }catch(IOException ex){
                Log.e("BTDEVICE", "Can't close receive socket");
            }

        }


    }


}