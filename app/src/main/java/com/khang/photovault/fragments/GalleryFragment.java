package com.khang.photovault.fragments;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.khang.photovault.model.Photo;

import java.util.ArrayList;
import java.util.List;

public class GalleryFragment extends BasePhotoFragment {

    private final ActivityResultLauncher<String> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) readPhotos();
                else {
                    tvEmpty.setText("Cần cấp quyền truy cập ảnh để sử dụng ứng dụng");
                    tvEmpty.setVisibility(android.view.View.VISIBLE);
                    Toast.makeText(getContext(), "Chưa được cấp quyền xem ảnh", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected boolean favoritesMode() {
        return false;
    }

    @Override
    protected void loadData() {
        String perm = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), perm) == PackageManager.PERMISSION_GRANTED)
            readPhotos();
        else
            permLauncher.launch(perm);
    }

    private void readPhotos() {
        List<Photo> list = new ArrayList<>();
        String[] proj = {
                android.provider.MediaStore.Images.Media._ID,
                android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                android.provider.MediaStore.Images.Media.DATE_ADDED
        };
        try (Cursor c = requireContext().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj, null, null,
                MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            while (c != null && c.moveToNext()) {
                long id = c.getLong(0);
                Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                list.add(new Photo(id, uri, c.getString(1), c.getLong(2)));
            }
        }
        showData(list);
    }
}
