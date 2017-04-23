package com.example.szopen.ecgapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

/**
 * Aplikacja ECGApp sluzy do obslugi mobilnego urzadzenia do monitorowania aktywnosci serca.
 * Oprogramowanie laczy sie z urzadzeniem poprzez technologie Bluetooth i umożliwia wizualizację
 * i zapis odebranych danych oraz przeglądanie wcześniej zapisanych pomiarów.
 *
 * Klasa ekranu startowego, inicjuje elementy GUI i pozwala na przechodzenie do innych aktywnosci
 *
 * @author Piotr Dąbek
 * @version 2.0
 */

public class MenuActivity extends AppCompatActivity {

    ImageButton startButton, settingsButton, historyButton, infoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ekran_startowy2);

        startButton =(ImageButton)findViewById(R.id.buttonStart);      //tworzenie przyciskow
        settingsButton =(ImageButton)findViewById(R.id.buttonSettings);
        historyButton =(ImageButton)findViewById(R.id.buttonHistory);
        infoButton = (ImageButton) findViewById(R.id.buttonInfo);

        startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d("Klikniecie w", "Start");
                startActivity(new Intent(getBaseContext(), DiagnosticModeActivity.class));
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), InfoActivity.class));
            }
        });

        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), FileSelectActivity.class));
            }
        });

        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), InfoActivity.class));
            }
        });


    }


}
