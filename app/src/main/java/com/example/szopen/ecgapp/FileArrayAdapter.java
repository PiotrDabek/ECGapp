package com.example.szopen.ecgapp;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

/**
 * Klasa implementująca adapter dla Custom ListView
 */

public class FileArrayAdapter extends ArrayAdapter<FileItem> implements AdapterView.OnItemClickListener {

    private Context context;
    private int id;
    private List<FileItem> listOfItems;

    public FileArrayAdapter(@NonNull Context context, @LayoutRes int textViewResource, @NonNull List<FileItem> objects) {
        super(context, textViewResource, objects);
        this.context = context;
        this.id = textViewResource;
        this.listOfItems = objects;
    }

    public FileItem getItem(int i)
    {
        return listOfItems.get(i);
    }

    public View getView(int position, View convertView, ViewGroup parent){
        View v = convertView;
        if(v==null){
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id,null);
        }
       final FileItem item = listOfItems.get(position);
        if (item!=null){
            TextView textViewName = (TextView) v.findViewById(R.id.fileName);                                               // przypisanie elementow GUI
            TextView textViewDate = (TextView) v.findViewById(R.id.fileDate);
            ImageView imageViewIcon = (ImageView) v.findViewById(R.id.imageView);
            ImageButton buttonDelete = (ImageButton) v.findViewById(R.id.buttonDelete);

            if(textViewName != null) textViewName.setText(item.getName());                                                  // pobranie info do FileItemu
            if(textViewDate != null) textViewDate.setText(item.getDate());
            if(imageViewIcon != null)
            {
                int resID = context.getResources().getIdentifier("mipmap/"+item.getImage(),null,context.getPackageName());  // ustawnienie obrazka
                imageViewIcon.setImageResource(resID);
            }

            String extension = item.getName().substring(item.getName().lastIndexOf(".")+1,item.getName().length());         // jesli element .ecg dodaj opcje usuniecia
            if (extension.equalsIgnoreCase("ecg"))
            {
                buttonDelete.setVisibility(View.VISIBLE);
                buttonDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        File file = new File(item.getPath());
                        boolean deleted = file.delete();
                        if(deleted)                                                                                         // czy usunieto
                            Toast.makeText(context, "Plik zniknie po odswiezeniu ekranu",
                                    Toast.LENGTH_SHORT).show();
                        else Toast.makeText(context, "Nie udalo sie usunac pliku",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            else buttonDelete.setVisibility(View.INVISIBLE);                                                                // ikona kosza niewidoczna dla innych elementow niz .ecg
        }

        return v;
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }
}
