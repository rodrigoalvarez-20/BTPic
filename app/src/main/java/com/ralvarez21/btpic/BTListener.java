package com.ralvarez21.btpic;

import static com.ralvarez21.btpic.Constants.ANGLE_PREF_NAME;
import static com.ralvarez21.btpic.Constants.APP_PREF_NAME;
import static com.ralvarez21.btpic.Constants.BTTAG;
import static com.ralvarez21.btpic.Constants.BT_DATA_RCV_BR;
import static com.ralvarez21.btpic.Constants.BT_DATA_RCV_BR_EXTRA;
import static com.ralvarez21.btpic.Constants.BT_DATA_RCV_ERROR;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class BTListener extends Thread {
    private InputStream sckIn;
    private Context ctx;

    public BTListener(InputStream in, Context ctx){
        sckIn = in;
        this.ctx = ctx;
    }

    public void run(){
        if (sckIn != null){
            byte[] data = new byte[256];
            try {
                int status = sckIn.read(data);
                if (status > 0){
                    int actualSteps = Integer.parseInt(String.valueOf(data[0]));
                    int rcvAngle = (int) Math.floor(actualSteps * 1.8);
                    Log.i(BTTAG, "Datos recibidos: " + Arrays.toString(data));
                    Log.i(BTTAG, "Angulo recibido: " + rcvAngle);
                    // Enviar intent o notificacion de recepcion de datos
                    Intent setConnectedIntent = new Intent(BT_DATA_RCV_BR);
                    setConnectedIntent.putExtra(BT_DATA_RCV_BR_EXTRA, rcvAngle);
                    this.ctx.sendBroadcast(setConnectedIntent);
                }
            } catch (IOException e) {
                // Notificar error en recepcion de datos
                Log.e(BTTAG, e.toString());
                this.ctx.sendBroadcast(new Intent(BT_DATA_RCV_ERROR));
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