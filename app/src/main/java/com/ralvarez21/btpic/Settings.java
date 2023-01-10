package com.ralvarez21.btpic;

import static com.ralvarez21.btpic.Constants.ANGLE_PREF_NAME;
import static com.ralvarez21.btpic.Constants.APP_PREF_NAME;
import static com.ralvarez21.btpic.Constants.MASS_PREF_NAME;
import static com.ralvarez21.btpic.Constants.VEL_PREF_NAME;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import es.dmoral.toasty.Toasty;

public class Settings extends AppCompatActivity {

    private SharedPreferences shPrefs;
    private float actualAngle, rocketMass, rocketVel;
    private EditText txtAngle, txtMass, txtVel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if(getSupportActionBar() != null){
            getSupportActionBar().setTitle(getResources().getString(R.string.settings_title));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        shPrefs = getSharedPreferences(APP_PREF_NAME, MODE_PRIVATE);
        actualAngle = shPrefs.getInt(ANGLE_PREF_NAME, 0);
        rocketMass = shPrefs.getFloat(MASS_PREF_NAME, 0);
        rocketVel = shPrefs.getFloat(VEL_PREF_NAME, 0);

        txtAngle = findViewById(R.id.txtActualAngle);
        txtMass = findViewById(R.id.txtMass);
        txtVel = findViewById(R.id.txtVel);

        txtAngle.setText(String.valueOf(actualAngle));
        txtMass.setText(String.valueOf(rocketMass));
        txtVel.setText(String.valueOf(rocketVel));

        FloatingActionButton fab = findViewById(R.id.btn_save_settings);

        fab.setOnClickListener(v -> {
            if(txtAngle.getText().toString().isEmpty() || txtMass.getText().toString().isEmpty() || txtVel.getText().toString().isEmpty()){
                Toasty.error(this, getResources().getString(R.string.ts_settings_error)).show();
            }else {
                int newAngle = !txtAngle.getText().toString().equals("0.0") ?  Integer.parseInt(txtAngle.getText().toString()) : 0;
                float newMass = Float.parseFloat(txtMass.getText().toString());
                float newVel = Float.parseFloat(txtVel.getText().toString());

                if (newAngle > 180 || newAngle < 0){
                    Toasty.error(this, getResources().getString(R.string.ts_angle_error)).show();
                }else {
                    SharedPreferences.Editor shEditor = shPrefs.edit();
                    shEditor.putInt(ANGLE_PREF_NAME, newAngle);
                    shEditor.putFloat(MASS_PREF_NAME, newMass);
                    shEditor.putFloat(VEL_PREF_NAME, newVel);
                    shEditor.apply();
                    Toasty.success(this, getResources().getString(R.string.ts_settings_saved)).show();
                }
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
        }
        return true;
    }
}