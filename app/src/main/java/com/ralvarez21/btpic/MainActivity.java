package com.ralvarez21.btpic;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private ToggleButton tgBt;
    private Spinner spPaired, spDiscovered;
    private ImageButton btnReload;


    @SuppressLint("MissingPermission")
    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.i("A-Result", "Result code: " + result.getResultCode());
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.e("Activity result", "OK");
                    // There are no request codes
                    Intent data = result.getData();
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
        //btFilters.addAction(BluetoothDevice.ACTION_FOUND);


        registerReceiver(receiver, btFilters);

        tgBt = findViewById(R.id.tgBT);
        spPaired = findViewById(R.id.spPairedDev);
        spDiscovered = findViewById(R.id.spDiscDev);
        btnReload = findViewById(R.id.btnFind);

        bAdapter = BluetoothAdapter.getDefaultAdapter();

        tgBt.setOnCheckedChangeListener((btn, b) -> {
            if (b) {
                turnOnBT();
            } else {
                bAdapter.cancelDiscovery();
                bAdapter.disable();
            }
        });

        btnReload.setOnClickListener(v -> {
            if (!bAdapter.isDiscovering()){
                //Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                //activityResultLauncher.launch(intent);
                bAdapter.startDiscovery();
                Log.i("DISC STATUS", "Discovery initiated ");
                registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }else{
                Toast.makeText(this, "BT is in discover mode", Toast.LENGTH_SHORT).show();
            }
        });

        mapBTStatus();
        setPairedAdapter();

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
                        bAdapter.enable();
                        tgBt.setTextOn("Encendido");
                        Log.i("BTSTATUS", "STATE_ON - " + estado);
                        activityResultLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE));
                        setPairedAdapter();
                        break;
                    } case BluetoothAdapter.STATE_OFF: {
                        tgBt.setTextOff("Apagado");
                        Log.i("BTSTATUS", "STATE_OFF - " + estado);
                        if(bAdapter.isDiscovering()){
                            bAdapter.cancelDiscovery();
                        }
                        break;
                    } case BluetoothAdapter.STATE_TURNING_ON: {
                        tgBt.setTextOn("Encendiendo...");
                        Log.i("BTSTATUS", "PREV_ON - " + estado);
                        boolean st = bAdapter.startDiscovery();
                        Log.i("DISC STATUS", "Discovery Status: " + st);
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
                spDiscovered.setEnabled(bAdapter.isEnabled());
                spPaired.setEnabled(bAdapter.isEnabled());
                btnReload.setEnabled(bAdapter.isEnabled());
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
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 1);
            return;
        }
        tgBt.setChecked(bAdapter.isEnabled());
        spDiscovered.setEnabled(bAdapter.isEnabled());
        spPaired.setEnabled(bAdapter.isEnabled());
        btnReload.setEnabled(bAdapter.isEnabled());
    }

    private void setPairedAdapter(){
        if (bAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 1);
                return;
            }
            Set<BluetoothDevice> devices = bAdapter.getBondedDevices();
            ArrayList<String> lstPairedDev = new ArrayList<>();
            lstPairedDev.add("");
            for (BluetoothDevice device: devices){
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 1);
            return;
        }
        if(bAdapter.isDiscovering()){
            bAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(receiver);
    }


}