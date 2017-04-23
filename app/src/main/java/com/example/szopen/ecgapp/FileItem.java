package com.example.szopen.ecgapp;

import android.support.annotation.NonNull;

/**
 * Klasa reprezentująca plik wyświetlany w
 * @see FileSelectActivity
 */

public class FileItem implements Comparable<FileItem> {


    private String name, data,date,path,image;

    public FileItem(String name, String data, String date, String path, String image) {
        this.name = name;
        this.data = data;
        this.date = date;
        this.path = path;
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public String getData() {
        return data;
    }

    public String getDate() {
        return date;
    }

    public String getPath() {
        return path;
    }

    public String getImage() {
        return image;
    }

    @Override
    public int compareTo(@NonNull FileItem item) {
        if(this.name!= null) return this.name.toLowerCase().compareTo(item.getName().toLowerCase());        //kolejnosc alfabetyczna
        else throw new IllegalArgumentException();
    }
}
