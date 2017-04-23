package com.example.szopen.ecgapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Aktywność DiagnosticModeActivity słuzy do komunikacji i wyświetlania danych pochodzacych z urządzenia pomiarowego.
 * Klasa ustanawia połączenie, odbiera dane a następnie wyświwietla je na ekranie użytkownika jako wykres.
 * Odebrane dane mogą być następnie zapisane do pliku w formacie .ecg .
 *
 * Dane wysyłane są do urzadzenia jako ciag bajtow.
 */
public class DiagnosticModeActivity extends AppCompatActivity {

    //elementy gui
    ImageButton buttonConnect, buttonSave, buttonSettings, buttonStats;
    TextView textViev;
    ListView listViev1;
    ProgressBar progressBar;

    boolean buttonConnectPressed = false;

    //elementy grafu
    private LineGraphSeries<DataPoint> mSeries;             //seria danych do wyswietlenia
    private double graph2LastXValue = 5d;                   // os x
    GraphView graph;                                        //obiekt UI


    // zapis do pliku
    String fileName;                                        //nazwa pliku
    String filePath;                                        //sciezka do pliku
    String fileDir;                                         //sciezka do folderu
    File file;                                              //uchwyt do pliku
    Writer writer;                                          //writer
    boolean writeToFileFlag = false;                        //flaga czy trwa zapis
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;   // request code dla pozwolen

    //obiekty Bluetooth
    private final static int REQUEST_ENABLE_BT = 1;         // request code
    private UUID ECGAppUUID;                                // UUID połączenia

    boolean bluetoothConnectionFlag = false;                //flaga nawiazanego polaczenia
    AlertDialog dialog;

    ThreadConnectBTdevice btThreadConnectWithDevice;        //watek odpowiadający za polaczenie z urzadzeniem bluetooth
    ThreadConnected btThreadConnected;                      //watek odpowiadajacy za obsługę polaczenia
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    /**
     * W metodzie onCreate ustawiany jest layout aktywności. DiagnosticModeActivity działa w widoku horyzontalnym
     * co zapewnia bardziej przejrzystą reprezentcję danych pomiarowych. Ustawiane są elementy GUI jak również parametry grafu.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.diagnostic_mode2);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);     // landscape mode

        //inicjalizacja grafu
        graph= (GraphView)findViewById(R.id.graph);
        mSeries = new LineGraphSeries<>();
        graph.addSeries(mSeries);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(2000);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-50);
        graph.getViewport().setMaxY(260);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);

        guiSetup();                                                            //inicjalizacja GUI

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    /**
     * Metoda służąca do przypisania elementów z XML do obiektów oraz ustawienia niektórych podstawowych własności
     * obiektów. W metodzie ustawiene są także OnClickListenery dla buttonów.
     */
    void guiSetup() {

        buttonConnect = (ImageButton) findViewById(R.id.buttonConnect);
        buttonSave = (ImageButton) findViewById(R.id.buttonSave);
        buttonSettings = (ImageButton) findViewById(R.id.buttonSettings);
        buttonStats = (ImageButton) findViewById(R.id.buttonStats);
        textViev = (TextView) findViewById(R.id.textView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        textViev.setVisibility(View.INVISIBLE);                                 // elementy niewidoczne dla zapiu
        progressBar.setVisibility(View.INVISIBLE);
        buttonSave.setEnabled(false);                                           // przycisk dostepny po nawiazaniu polaczenia



        //obsługa buttonow


        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!buttonConnectPressed)
                {
                    bluetoothInit();                                                    // zainicjalizowanie polaczenia bluetooth
                    buttonConnectPressed = true;
                }
                else                                                                    // zamkniecie polaczenia bluetooth
                {
                    if (btThreadConnected != null) btThreadConnected.cancel();          // zakonczenie watkow bluetooth
                    if (btThreadConnectWithDevice != null) btThreadConnectWithDevice.cancel();
                    bluetoothConnectionFlag = false;                                    // ustawienie flagu polaczenia
                    writeToFileFlag = false;                                            // ustawnienie flagi zapisu
                    buttonSave.setEnabled(false);                                       // przycisk dostepny tylko po nawiazaniu polaczenia

                    if(writer != null) {                                                // zamkniecie writera
                        try {
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if(file != null) {
                        textViev.setVisibility(View.INVISIBLE);
                        progressBar.setVisibility(View.INVISIBLE);

                        //zapewnieine widoczności pliku w eksploratorze plikow
                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri uri = Uri.fromFile(file);
                        intent.setData(uri);
                        sendBroadcast(intent);
                    }
                    buttonConnectPressed = false;
                }
            }
        });

        buttonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(DiagnosticModeActivity.this, "buttonSettings nie jest okodzony", Toast.LENGTH_SHORT).show();
            }
        });

        buttonStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(DiagnosticModeActivity.this, "buttonStats nie jest okodzony", Toast.LENGTH_SHORT).show();
            }
        });

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
           buttonSaveExec();                                                    // zapis do pliku
            }
        });
    }

    /*************************************************************************************
     Procedura wcisniecia buttonSave
     *************************************************************************************

     Dla telefonow z systemem 6.0 wprowadzona zostala obsluga run-time permissions

     kod obslugi przycisku zostal rozbity na 3 czesci:
     1) setOnClickListener sprawdzajcy czy aplikacja posiada pozwolenie
     - jesli tak -> wykonuje buttonSaveExec()
     - prosi o zezwolenie
     2) onRequestPermissionsResult() wykonywany, gdy aplikacja pozwolenia nie miala
     - gdy przydzielono -> wywolanie buttonSaveExec()
     - gdy nie -> informacja dla uzytkownika
     3) buttonSaveExec() - faktyczny kod obslugi przycisku
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    buttonSaveExec();

                } else {
                    Toast.makeText(getApplicationContext(), "Nie uzyskano zezwolenia na zapis do " +
                            "pamieci zewnetrznej", Toast.LENGTH_LONG);
                }
                return;
            }
        }
    }

    /**
     * Metoda słuzaca do możliwienia zapisu danych do pliku. Przycisk sluzacy do jej aktywacji jest
     * aktywny tylko wtedy gdy ustanowione jest polaczenie bluetooth.
     *
     * W przypadku gdy zapis juz trwa, metoda kończy zapis.
     *
     * Przed umozliwieniem zapisu do pliku sprawdzane jest, czy aplikacja posiada stosowne uprawnienia.
     * Każdy plik zapisany jets z rozszerzeniem .ecg a w nazwa zawiera date zapisu. W przypadku
     * gdy w katalogu znajduje sie wiele pilkow o tej samej dacie nazwa rozszerzana jest o liczbe.
     */
    void buttonSaveExec()
    {
        if (ContextCompat.checkSelfPermission(DiagnosticModeActivity.this,                                      // sprawdzenie uprawnien
                Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(DiagnosticModeActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_ASK_PERMISSIONS);
        }
        else {
            if(writeToFileFlag == false) {                                                                      // sprawdzenie flagi zapisu

                if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {                  //Sprawdzenie dostepnosci zewnetrzenj pamieci

                    Log.d("pamiec dostepna", "Dostepna pamiec zewnetrzna");
                    fileName = "/ECG-" + (new SimpleDateFormat("yyyyMMddhhmm").format(new Date())) + ".ecg";    // tworzenie nazwy pliku
                    fileDir = Environment.getExternalStorageDirectory() +"/ECGApp/";                            // uzyskanie sciezki do folderu w zewnetrzej pamieci
                    filePath = fileDir + fileName;                                                              // sciezka do pliku
                    file = new File(fileDir);
                    file.mkdirs();                                                                              // utworzenie folderow
                    file = new File(filePath);                                                                  // tworzenie uchwytu do pliku

                    Integer i = 0;
                    while (file.exists())                                                                       // kontrola dla wielu plikow zapisywanych o tej samej godzinie
                    {
                        i++;
                        fileName = "/ECG-" + (new SimpleDateFormat("yyyyMMddhhmm").format(new Date())) + "-"+ i.toString() + ".ecg";
                        filePath = Environment.getExternalStorageDirectory() + "/ECGApp/" + fileName;
                        file = new File(filePath);
                    }
                    try {

                        file.createNewFile();                                                                   // proba utworzenia pliku
                        Log.v("File","file crated");
                    } catch (IOException e) {                                                                   // sprawdzenie czy plik zostal utworzony
                        e.printStackTrace();
                        Log.v("File", "file not created");
                        Toast.makeText(DiagnosticModeActivity.this, "Plik nie zostal utworzony, sprawdz zezwolenia aplikacji!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    try {
                        writer = new FileWriter(filePath);                                                      // proba utworzenie writera do pliku
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.v("writer", "writer not created");
                        Toast.makeText(DiagnosticModeActivity.this, "Writer nie zostal utworzony!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    textViev.setVisibility(View.VISIBLE);                                                       // ustawienie animacji o trwajacym zapisie
                    progressBar.setVisibility(View.VISIBLE);
                    writeToFileFlag = true;
                }
                else
                {
                    Log.d("pamiec niedostepna", "brak pamieci zewnetrznej!");
                    Toast.makeText(DiagnosticModeActivity.this, "Brak pamieci zewnetrznej!", Toast.LENGTH_LONG).show();
                    return;
                }


            }else                                                                                               // zakonczenie zapisu
            {
                textViev.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.INVISIBLE);

                writeToFileFlag = false;
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //zapewnieine widoczności pliku w eksploratorze plikow zaraz po zakonczeniu zapisu

                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri uri = Uri.fromFile(file);
                intent.setData(uri);
                sendBroadcast(intent);
            }
        }
    }

//*********************************************************************

    /**
     * Metoda sluzaca do zainicjalizowania polaczenia bluetooth. Sprawdza, czy
     * urzadzenie posiada modul bluetooth i czy jest on wlaczony. Nastepnie pobiera liste
     * powiazanych urzadzen i nawiazuje polaczenie z jednym z nich za pomoca osobnego watku
     * @see ThreadConnectBTdevice
     * @return
     */
    int bluetoothInit() {
        listViev1 = new ListView(this);//(ListView)findViewById( R.id.listView);
        final String UUID_STRING_WELL_KNOWN_SPP =
                "00001101-0000-1000-8000-00805F9B34FB";                                         //well-known SPP UUID(szczegoly w dokumentacji)
        ECGAppUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);                               //przypisanie wygenerowanego UUID do obiektu
                                                                                                //adapter urzadzenia bluetooth w telefonie
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();              //pobranie informacji o Bluetooth w urządzeniu

        if (mBluetoothAdapter == null) {                                                        //sprawdzenie czy urzadzenie posiada bbluetooth
            Toast.makeText(DiagnosticModeActivity.this, "To urzadzenie nie wspiera Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (!mBluetoothAdapter.isEnabled()) {                                                   //sprawdzenie czy bluetooth jest wlaczony
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);         //jeśli nie, odpalenie aktywności w celu wlaczenia
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return 0 ;
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();              //set sparowanych urzadzen bluetooth
        //jeśli dostępne są sparowane urzadzenia
        if (pairedDevices.size() > 0) {

            ArrayList<BluetoothDevice> pairedDevicesArrayList = new ArrayList<BluetoothDevice>();   //tablica sparowanych urzadzen

            for (BluetoothDevice device : pairedDevices) pairedDevicesArrayList.add(device);     // dodaj do listy sparowanych urzadzen
            Toast.makeText(getApplicationContext(), "Wybierz urzadzenie do sparowania", Toast.LENGTH_LONG).show();


            //stworzenie adaptera urzywajacego layoutu custom.xml jako swojego(mozna w nim zmieniac kolory,czcionki itp.
            ArrayAdapter<BluetoothDevice> pairedDevicesAdapter = new ArrayAdapter<BluetoothDevice>(this, R.layout.custom, R.id.txtItem, pairedDevicesArrayList);
            //pairedDevicesAdapter = new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1, pairedDevicesArrayList);

            listViev1.setAdapter(pairedDevicesAdapter);                                          //przypisanie adaptera do listView

            // wyswietlamy liste sparowanych urzadzen jako AlertDialog

            AlertDialog.Builder builder = new AlertDialog.Builder(DiagnosticModeActivity.this);
            builder.setCancelable(true);
            builder.setPositiveButton("OK", null);

            builder.setView(listViev1);
            dialog = builder.create();


            //efekt wcisniecia elementu listView

            listViev1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);  //wydobadz wybrane urzadzenie
                    Toast.makeText(DiagnosticModeActivity.this,                                             //info o urzadzeniu
                            "Nazwa: " + device.getName() + "\n"
                                    + "Adres: " + device.getAddress() + "\n",
                            Toast.LENGTH_LONG).show();
                    dialog.dismiss();                                                               //wylacz AlertDialog

                    btThreadConnectWithDevice = new ThreadConnectBTdevice(device);                  //przekazanie urzadzenia do watku
                    btThreadConnectWithDevice.start();                                              //rozpoczecie watku polaczenia
                }
            });

            dialog.show();                                                                          //wyswietl dialog
        }
        return 0;
    }

    /**
     * Metoda wywolywana w momencie wlaczenia modulu bluetooth
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK){
                bluetoothInit();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(DiagnosticModeActivity.this, "Nie udało się włączyć Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Watek odpowiedzialny za ustanowienie polaczenia
     * Jeśli połaczenie zostalo poprawieni azinicjowane załaczany jest nowy watek obsługi tego połączenia
     * @see ThreadConnected
     */

    private class ThreadConnectBTdevice extends Thread {

        private BluetoothSocket bluetoothSocket = null;                                             //socket do zainicjalizowania
        private final BluetoothDevice bluetoothDevice;                                              //urzadzenie do polaczenia


        private ThreadConnectBTdevice(BluetoothDevice device) {
            bluetoothDevice = device;                                                               // przypisanie urzadzenia

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(ECGAppUUID);             //proba stworzenia socketa dla danego urzadzenia

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                bluetoothSocket.connect();                                                          //proba polaczenia
                success = true;
            } catch (IOException e) {
                e.printStackTrace();

                try {
                    bluetoothSocket.close();                                                        //jesli sie nie udalo-zamknij socket
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            if (success) {                                                                          // jesli ustanowiono polaczenie
                final String ifconnectedString = "Nawiazano polaczenie:\n"
                        + "BluetoothSocket: " + bluetoothSocket + "\n"
                        + "BluetoothDevice: " + bluetoothDevice;

                Log.d("polaczono", ifconnectedString);
                //Toast.makeText(getApplicationContext(), ifconnectedString, Toast.LENGTH_SHORT).show();

                btThreadConnected = new ThreadConnected(bluetoothSocket);                           //watek obslugujacy polaczenie, przekazanie socketa
                btThreadConnected.start();
            }
        }

        public void cancel() {                                                                      // procedura obslugi zakonczenia watku

            Toast.makeText(getApplicationContext(),
                    "close bluetoothSocket",
                    Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();                                                            // proba zamkniecia socketa
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Wątak służacy do obsługi połączenia i wymiany danych po ustanowieniu połączenia
     */
    private class ThreadConnected extends Thread {

        private final BluetoothSocket connectedBluetoothSocket;                                       // socket
        private final InputStream connectedInputStream;                                                 // strumien wejsciowy
        private final OutputStream connectedOutputStream;                                               // strumien wyjsciowy

        public ThreadConnected(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream in = null;
            OutputStream out = null;
            bluetoothConnectionFlag = true;                                                             // ustawienie flagi polaczenia

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    buttonSave.setEnabled(true);
                }
            });                                      // aktywowanie przycisku buttonSave

            try {
                in = socket.getInputStream();                                                           // przypisanie strumieni
                out = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() {
            while (bluetoothConnectionFlag) {                                                       // dzialanie watku az do zminy stanu flagi
                try {

                    final byte[] buffer = new byte[256];
                    final int[] filebuffer = new int[256];


                    final int bytes = connectedInputStream.read(buffer);                            // sczytanie zawartosci bufora strumienia wejsciowego
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            for (int i = 0; i < bytes; i++) {                                      //dodanie nowyach wartosci do grafu
                                graph2LastXValue += 1d;                                            // kolejny element wykresu
                                filebuffer[i] = (int) byte2short(buffer[i]);                       // zamiana bitu na liczbe w zakresie 0-255


                                if (writeToFileFlag) {                                              // jesli zapis do pliku
                                    try {
                                        writer.write(String.valueOf(filebuffer[i]) + System.getProperty("line.separator"));       // "\n" dla RandomAccessFile aby mozna bylo uzywac readLine()
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                                mSeries.appendData(new DataPoint(graph2LastXValue, filebuffer[i]), true, 2000);       //Usuniecie starych probek i dodanie nowych
                            }
                        }
                    });
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        /**
         * Metoda służaca do wysłania danych do powiazanego urzadzenia bluetooth
         * @param bytes tablica bajtow do wyslania
         */
        public void sendDataByte (byte[] bytes) {
            try {
                connectedOutputStream.write(bytes);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Zakonczenie watku
         */
        public void cancel() {
            try {
                connectedBluetoothSocket.close();
                connectedInputStream.close();
            } catch (IOException e) {
            }
        }

    }

    /**
     * Metoda służaca do zamiany bajtu na liczbe short bez znaku tnz. w zakresie 0-255
     * @param value dana do zamiany
     * @return  short z przedzialu 0-255
     */
    short byte2short(short value) {
        return (short) (0x000000FF & ((int) value));
    }


    /**
     * Metoda onDestroy zakańcza działające wątki oraz zamyka writery do plików
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (btThreadConnected != null) btThreadConnected.cancel();
        if (btThreadConnectWithDevice != null) btThreadConnectWithDevice.cancel();
        bluetoothConnectionFlag = false;

        if(writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(file != null) {
            textViev.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.INVISIBLE);

            //zapewnieine widoczności pliku w eksploratorze plikow
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri = Uri.fromFile(file);
            intent.setData(uri);
            sendBroadcast(intent);
        }

    }
}