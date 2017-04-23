package com.example.szopen.ecgapp;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.logging.Handler;

/**
 * Aktywność ECGPlayerActivity umożliwia użytkownikowi przeglądanie wcześniej zarejestrowanych pomiarów.
 * Za pomocą slidera znajdującego się w dolnej części ekranu można wybierać fragment , który ma być wyświetlony.
 *
 * Aktywność nadal jest rozwijana
 */
public class ECGPlayerActivity extends AppCompatActivity {

    String filePath;                                                            //przekazana sciezka do pliku
    final private int REQUEST_CODE_ASK_PERMISSIONS = 120;                       // request code dla pozwolen
    final private int REQUEST_CODE_ASK_PERMISSIONS2 = 122;                      // request code dla pozwolen

    private short[] dataArray;                                                  //tablica do przechowywania danych
    private int numberOfSamples = 1000;                                         // ilosc wyswietlanych probek
    private long fileSize =  0;                                                 //rozmiar pliku
    private long filePointer = 0;                                               // aktualne ustawienie pointera
    private long filePointerFinishRead;                                         // maksymalne ustawienie pointera aby mozliwy byl odczyt
                                                                                // ilosci probek numberOfSamples

    ImageButton buttonPlay, buttonPause, buttonStats, buttonOptions;
    SeekBar seekBar;

    //elementy grafu
    private LineGraphSeries<DataPoint> mSeries;                                 //seria danych do wyswietlenia
    private double graph2LastXValue = 5d;                                       // os x
    GraphView graph;                                                            //obiekt UI

    private Runnable function;
    private Handler hdr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecgplayer);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);     // landscape mode

        filePath = getIntent().getExtras().getString("filePath");               // odebranie sciezki do pliku

        buttonPlay = (ImageButton) findViewById(R.id.buttonPlay);               // przypisanie elementow gui
        buttonPause = (ImageButton) findViewById(R.id.buttonPause);
        buttonStats = (ImageButton) findViewById(R.id.buttonStats);
        buttonOptions = (ImageButton) findViewById(R.id.buttonOptions);
        seekBar = (SeekBar) findViewById(R.id.seekBar);

                                                                                //inicjalizacja grafu
        graph= (GraphView)findViewById(R.id.graph);
        mSeries = new LineGraphSeries<>();
        graph.addSeries(mSeries);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(numberOfSamples);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-50);
        graph.getViewport().setMaxY(260);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);


        /*
        Listener ubslugujacy seekBar

        Dla nowo wyranej wartości wczytywane są z pliku dane odpowiadające położeniu na seekBarze
*/
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float ratio = 1;
                if(fileSize>Integer.MAX_VALUE)                                          // jeśli rozmiar pliku jest większy niż maxymalna wartośc integera
                {
                    ratio = fileSize/Integer.MAX_VALUE;                                 // ustawienie mnoznika
                }
                try {
                    float x = (seekBar.getProgress()*ratio);
                    readFile((long)(seekBar.getProgress()*ratio));                      // wczytanie odpowiednich danych
                } catch (IOException e) {
                    e.printStackTrace();
                }
                paintStaticDataSet();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(ECGPlayerActivity.this, "buttonPlay nie jest okodzony", Toast.LENGTH_LONG).show();
            }
        });

        buttonPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(ECGPlayerActivity.this, "buttonPause nie jest okodzony", Toast.LENGTH_LONG).show();
            }
        });

        buttonStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(ECGPlayerActivity.this, "buttonStats nie jest okodzony", Toast.LENGTH_LONG).show();
            }
        });

        buttonOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(ECGPlayerActivity.this, "buttonOptions nie jest okodzony", Toast.LENGTH_LONG).show();
            }
        });

        dataArray = new short[numberOfSamples];                                 // inicjalizacja tablicy
        Arrays.fill(dataArray,(short) 0);                                       // wypelnienie tablicy zerami
        try {
            getFileInfo();                                                      // pobranie informacji o pliku
            readFile(0);                                                        // odczyt pierwszych probek
        } catch (IOException e) {
            e.printStackTrace();
        }
        paintStaticDataSet();                                                   // wyrysowanie wykresu
//// TODO: 10.04.2017 setMax jest intem a filesize longiem
        if(fileSize>Integer.MAX_VALUE) seekBar.setMax(Integer.MAX_VALUE);               // jeśli rozmiar pliku jest większy niż maxymalna wartośc integera
        else seekBar.setMax((int)filePointerFinishRead);

    }

    /**
     * Obsluga RunTimePermissions
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    try {
                        readFile(filePointer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "Nie uzyskano zezwolenia na zapis do " +
                            "pamieci zewnetrznej", Toast.LENGTH_LONG);
                }
                return;
            }
            case REQUEST_CODE_ASK_PERMISSIONS2: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    try {
                        getFileInfo();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    seekBar.setMax((int)filePointerFinishRead);

                } else {
                    Toast.makeText(getApplicationContext(), "Nie uzyskano zezwolenia na zapis do " +
                            "pamieci zewnetrznej", Toast.LENGTH_LONG);
                }
                return;
            }
        }
    }

    /**
     * Metoda służąca do wczytania danych z odpowiedniego miejsca w pliku.
     * Odcztytuje kolejne wiersze z pilku i zapisuje do odpowiedniej tablicy.
     * W przypadku, gdy plik jest za krotki wypelnia tablice zerami
     *
     * @param tmpFilePointer miejsce w pliku od ktorego nalezy rozpoczac sczytywanie danych
     * @throws IOException
     */
    private void readFile(final long tmpFilePointer) throws IOException {

        function = new Runnable() {
            @Override
            public void run() {
                if (ContextCompat.checkSelfPermission(ECGPlayerActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(ECGPlayerActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_CODE_ASK_PERMISSIONS);
                    return;
                }
                else {
                    RandomAccessFile randomAccessFile = null;                           // nowy plik
                    try {
                        randomAccessFile = new RandomAccessFile(filePath, "r");         // przypisanie do uchwytu
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    long raFilePointer = tmpFilePointer;

                    try {
                        randomAccessFile.seek(raFilePointer);                           // ustawienie miejsca z ktorego bedziemy czytac
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    int i = 0;                                                          // zmienne pomocnicze
                    String tmp;
                    //// TODO: 23.04.2017 pierwszy odczyt od znaku nowej linii
                    for(int k = 0; k < numberOfSamples; k++)
                    {
                        if(raFilePointer < fileSize)                                    // sprawdzenie czy nie wykraczamy poza rozmiar pliku
                        {
                            try {
                                tmp = randomAccessFile.readLine();                      // odczyt nowej linii
                            } catch (IOException e) {
                                e.printStackTrace();
                                tmp = null;
                            }
                            if (tmp.equalsIgnoreCase("")) break;                        // jesli nie ma juz czego czytac

                            dataArray[i++] = Short.parseShort(tmp) ;                    // dodanie elementu do tablicy

                            try {
                                raFilePointer = randomAccessFile.getFilePointer();      // odebranie wskaznika do miejsca w pliku
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else dataArray[i++] = 0;                                        // uzupelnienie tablicy zerami
                    }
                    try {
                        randomAccessFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        function.run();
    }

    /**
     * Metoda służąca do wyrysowania nowego grafu
     */
    void paintStaticDataSet()
    {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        DataPoint[] dataTable = new DataPoint[numberOfSamples];
                        for(int k = 0; k < dataArray.length ; k++) {
                            // graph2LastXValue += 1d;
                            //mSeries.appendData(new DataPoint(graph2LastXValue, dataArray[k]), true, 2000);
                            dataTable[k] = new DataPoint(k,dataArray[k]);
                        }
                        mSeries.resetData(dataTable);
                    }
                });
    }

    /**
     * Metoda pobierajaca dlugosc pliku oraz wskaźnik do ostatniego miejsca z ktorego mozna odczytac ilosc probek
     * okreslona w nuberOfSamples.
     *
     * @throws IOException
     */
    private void getFileInfo() throws IOException {
        if (ContextCompat.checkSelfPermission(ECGPlayerActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(ECGPlayerActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }
        else{
            RandomAccessFile randomAccessFile = null;
            try {
                randomAccessFile = new RandomAccessFile(filePath, "r");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                fileSize = randomAccessFile.length();
            } catch (IOException e) {
                e.printStackTrace();
            }

            int count = 0;
            long pointer = randomAccessFile.length();               // ustawienie wskaznika na koniec pliku
            while(count <= numberOfSamples)
            {
                pointer--;                                          // cofniecie wskaznika o jeden bajt
                randomAccessFile.seek(pointer);                     // ustawienie punktu odczytu
                byte tmp = randomAccessFile.readByte();             // odczyt jednego bajta
                char c = (char) (tmp & 0xFF);                       // zapisanie bajtu do chara
                if(String.valueOf(c).matches("\n")) count++;        // sprawdzenie czy odczytany znak to znak nowej linii

                if(pointer == 0) break;                             // zabezpieczenie przed cofnieciem sie dalej niz zero
            }
            filePointerFinishRead = pointer;                        // przypisanie zanlezionej wartosci
        }

    }
}
