package com.khang.photovault.model;

import android.net.Uri;

public class Photo {
    public final long id;
    public final Uri uri;
    public final String name;
    public final long dateAdded; // giây

    public Photo(long id, Uri uri, String name, long dateAdded) {
        this.id = id;
        this.uri = uri;
        this.name = name == null ? "" : name;
        this.dateAdded = dateAdded;
    }
}
