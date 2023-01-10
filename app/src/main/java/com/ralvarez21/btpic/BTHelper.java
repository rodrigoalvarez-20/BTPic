package com.ralvarez21.btpic;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class BTHelper extends Thread {
    public BluetoothSocket thSocket;
    private BluetoothDevice thDev;
    private BluetoothAdapter btAdapter;
    private Context ctx;

    public BTHelper(BluetoothDevice btDevice, BluetoothAdapter btAdapter, Context ctx){
        this.btAdapter = btAdapter;
        this.thDev = btDevice;
        this.ctx = ctx;
        BluetoothSocket tmp = null;
        try {
            tmp = thDev.createRfcommSocketToServiceRecord(UUID.fromString(Constants.APP_UUID));
        } catch (IOException e) {
            Log.e("", "Can't connect to service");
        }
        thSocket = tmp;
    }

    public void run(){
        Intent setLoadIntent = new Intent(Constants.TOOGLE_LOADING_BR);
        setLoadIntent.putExtra(Constants.TOOGLE_LOADING_EXTRA_STATE, true);
        this.ctx.sendBroadcast(setLoadIntent);
        if (this.btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }

        try {
            thSocket.connect();
            InputStream socketIn = thSocket.getInputStream();
            if (socketIn != null){
                BTListener thRecv = new BTListener(socketIn, ctx);
                thRecv.start();
            }
            Intent setConnectedIntent = new Intent(Constants.SET_CONNECTED_STATUS_BR);
            setConnectedIntent.putExtra(Constants.SET_CONNECTED_STATUS_EXTRA_DEVICE, thDev.getAddress());
            this.ctx.sendBroadcast(setConnectedIntent);

            Log.i(Constants.BTTAG, "Connected to device");
        } catch (IOException connectException) {
            try {
                if (this.thSocket!= null) {
                    thSocket.close();
                }
            } catch (IOException closeException) {
                Log.e("BTDEVICE", "Can't close socket");
            }
        }finally {
            setLoadIntent.putExtra(Constants.TOOGLE_LOADING_EXTRA_STATE, false);
            this.ctx.sendBroadcast(setLoadIntent);
        }

        Intent updateBTSocket = new Intent(Constants.UPDATE_BTSOCKET_BR);
        this.ctx.sendBroadcast(updateBTSocket);
    }

}
