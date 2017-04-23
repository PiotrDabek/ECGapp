package com.example.szopen.ecgapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Aktywność zawierająca informacje dotyczące aplikacji oraz urządzenia pomiarowego
 */
public class InfoActivity extends AppCompatActivity {

    Button buttonOK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        buttonOK = (Button) findViewById(R.id.buttonOK);

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
