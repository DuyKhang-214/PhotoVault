package com.khang.photovault;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.OutputStream;

/** Màn hình chọn vùng cắt ảnh và lưu kết quả thành ảnh mới trong thư viện. */
public class CropActivity extends AppCompatActivity {
    private CropImageView cropImageView;
    private Bitmap loadedBitmap;
    private View saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        MaterialToolbar toolbar = findViewById(R.id.cropToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        cropImageView = findViewById(R.id.cropImageView);
        saveButton = findViewById(R.id.btnCropSave);
        saveButton.setEnabled(false);

        String rawUri = getIntent().getStringExtra("uri");
        if (rawUri == null || rawUri.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy ảnh để cắt", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Uri sourceUri = Uri.parse(rawUri);
        Glide.with(this).asBitmap().load(sourceUri).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                loadedBitmap = resource;
                cropImageView.setBitmap(resource);
                saveButton.setEnabled(true);
            }

            @Override
            public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                // Glide quản lý vòng đời Bitmap theo Activity.
            }

            @Override
            public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                Toast.makeText(CropActivity.this, "Không tải được ảnh. Hãy thử ảnh khác.", Toast.LENGTH_LONG).show();
                finish();
            }
        });

        findViewById(R.id.btnCropCancel).setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> cropAndSave());
    }

    private void cropAndSave() {
        Bitmap cropped = cropImageView.getCroppedBitmap();
        if (cropped == null) {
            Toast.makeText(this, "Vui lòng chọn vùng ảnh cần cắt", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri outputUri = null;
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "PhotoVault_Crop_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoVault");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            outputUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (outputUri == null) throw new IllegalStateException("Không tạo được file ảnh mới");
            try (OutputStream stream = getContentResolver().openOutputStream(outputUri)) {
                if (stream == null || !cropped.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw new IllegalStateException("Không ghi được ảnh đã cắt");
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues ready = new ContentValues();
                ready.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(outputUri, ready, null, null);
            }

            Intent result = new Intent();
            result.putExtra("cropped_uri", outputUri.toString());
            setResult(RESULT_OK, result);
            Toast.makeText(this, "Đã cắt và lưu ảnh vào thư viện", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            if (outputUri != null) getContentResolver().delete(outputUri, null, null);
            Toast.makeText(this, "Không thể lưu ảnh đã cắt: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (cropped != loadedBitmap && !cropped.isRecycled()) cropped.recycle();
        }
    }
}
