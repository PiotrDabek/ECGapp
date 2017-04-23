package com.example.szopen.ecgapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Aktywność umożliwiająca wybranie dowolnego pliku z rozszerzeniem .ecg w celu dalszej obsługi.
 * Klasa pozwala na swobodne przemieszzanie się w katalogach dostępnych w urządzeniu jednak nie pozwala
 * na operowanie na plikach innych niż te z rozszerzeniem .ecg.
 */
public class FileSelectActivity extends AppCompatActivity {

    private File currDir;                           // sciezka folderu do przeszukania
    private FileArrayAdapter adapter;               // adaper listView
    ListView listView;                              // uchwyt od listView

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;   // request code dla pozwolen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_select);

        listView = (ListView) findViewById(R.id.lista);
        currDir = new File(String.valueOf(Environment.getExternalStorageDirectory())+ "/ECGApp/" ); // przypisanie folderu z zapisanymi pomiarami

        populateListView(currDir);                                                                  // zapelanienie listView plikami

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {                     // listener klikniecia w element listView
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                FileItem item = adapter.getItem(i);                                                 // obiekt pobrany z listView
                if(item.getImage().equalsIgnoreCase("folder_icon"))                                 // jesli folder
                {
                    currDir = new File(item.getPath());                                             // przypisz nowa sciezke
                    populateListView(currDir);                                                      // zapelnij liste nowymi wynikami
                }
                else
                {
                    String extension = item.getName().substring(item.getName().lastIndexOf(".")+1,item.getName().length());
                    if (extension.equalsIgnoreCase("ecg"))                                          // czy plik ma format .ecg
                    {
                                                                                                   //otwarcie nowej aktywnosci
                        Intent intent = new Intent(getBaseContext(), ECGPlayerActivity.class);
                        intent.putExtra("filePath", item.getPath());
                        startActivity(intent);

                    }else Toast.makeText(getApplicationContext(), "Wybrany plik nie jest w formacie .ecg",      // info o wyborze zlego pliku
                            Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    /**
     * Metoda służąca do wyświetlenia katalogów i plików obecnie znajdujących się w katalogu.
     * Przed próbą uzyskania dostępu do pamięci sprawdzane są stosowne uprawnienia.
     * Pliki w formacie .ecg oznaczone sa specjalna ikona.
     *
     * @param startDirectory katalog startowy
     */
    private void populateListView(File startDirectory) {
        if (ContextCompat.checkSelfPermission(FileSelectActivity.this,                                      // sprawdzenie przypisanych uprawnien
                Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(FileSelectActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }
        else {
            File[]allFiles = startDirectory.listFiles();                                            // pobranie informacji o zawartosci folderu

            List<FileItem> directories = new ArrayList<FileItem>();                                        // listy folderow i plikow
            List<FileItem> files = new ArrayList<FileItem>();

            if(allFiles != null) {                                                                  // jesli folder jest pusty
                try {
                    for (File item : allFiles) {
                        String date_modify = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date(item.lastModified())); // data edycji

                        if (item.isDirectory()) {                                                   // sprawdzenie typu pliku  i dodanie do listy
                            directories.add(new FileItem(item.getName(), "", date_modify, item.getAbsolutePath(), "folder_icon"));  // dodaj nowy element folderu
                        } else {
                            //String extension = item.getName().substring(item.getName().lastIndexOf(".")); // powoduje blad
                            String extension = item.getName().substring(item.getName().lastIndexOf(".") + 1, item.getName().length());
                            if (extension.equalsIgnoreCase("ecg")) {
                                files.add(new FileItem(item.getName(), "", date_modify, item.getAbsolutePath(), "ic_launcher"));
                            } else files.add(new FileItem(item.getName(), "", date_modify, item.getAbsolutePath(), "file_icon"));
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Blad dodawania plikow do listView",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }

            Collections.sort(directories);                                                          // sortowanie elementow
            Collections.sort(files);
            directories.addAll(files);                                                              // dodanie wszystkiego w kolejnosci do jednej listy

            if(startDirectory.getParent()!= null)                                                   // dodanie opcji powrotu do folderu wyzej, jesli jest mozliwosc
                directories.add(0,new FileItem("..","","Parent Directory",startDirectory.getParent(),"folder_icon"));
            adapter = new FileArrayAdapter(FileSelectActivity.this,R.layout.layout_file_select_listview,directories);
            listView.setAdapter(adapter);                                                           // dodaj elementy do listView
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,                                         // po przyznaniu uprawnien wywolaj funkcje
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    populateListView(currDir);                                                       // funkcja do wywolania

                } else {
                    Toast.makeText(getApplicationContext(), "Nie uzyskano zezwolenia na zapis do " +
                            "pamieci zewnetrznej", Toast.LENGTH_LONG);
                }
                return;
            }
        }
    }
}
