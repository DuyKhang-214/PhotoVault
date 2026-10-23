package com.khang.photovault.fragments;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.khang.photovault.model.Photo;

import java.util.ArrayList;
import java.util.List;

public class GalleryFragment extends BasePhotoFragment {

    private Uri pendingPhoto; // uri ảnh đang chụp

    // Xin quyền xem ảnh
    private final ActivityResultLauncher<String> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) readPhotos();
                else {
                    tvEmpty.setText("Cần cấp quyền truy cập ảnh để sử dụng ứng dụng");
                    tvEmpty.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "Chưa được cấp quyền xem ảnh", Toast.LENGTH_LONG).show();
                }
            });

    // Mở camera và lưu ảnh vào uri đã chuẩn bị (Intent ACTION_IMAGE_CAPTURE)
    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), ok -> {
                if (ok) {
                    Toast.makeText(getContext(), "Đã lưu ảnh vào thư viện 📷", Toast.LENGTH_SHORT).show();
                    if (hasPermission()) readPhotos();
                } else if (pendingPhoto != null) {
                    // Người dùng hủy -> xóa bản ghi rỗng
                    requireContext().getContentResolver().delete(pendingPhoto, null, null);
                }
                pendingPhoto = null;
            });

    private String permission() {
        return Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), permission())
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected boolean favoritesMode() {
        return false;
    }

    @Override
    protected void loadData() {
        if (hasPermission()) readPhotos();
        else permLauncher.launch(permission());
    }

    /** Quay lại màn hình: chỉ tải lại nếu đã có quyền (tránh hỏi quyền lặp lại) */
    @Override
    protected void onRefresh() {
        if (hasPermission()) readPhotos();
    }

    @Override
    protected void onCameraClick() {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, "PV_" + System.currentTimeMillis() + ".jpg");
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= 29)
            v.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoVault");

        pendingPhoto = requireContext().getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
        if (pendingPhoto == null) {
            Toast.makeText(getContext(), "Không tạo được file ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            takePicture.launch(pendingPhoto);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Không mở được camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void readPhotos() {
        List<Photo> list = new ArrayList<>();
        String[] proj = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
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
