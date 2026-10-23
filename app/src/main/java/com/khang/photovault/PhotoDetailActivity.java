package com.khang.photovault;

import android.content.Intent;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoDetailActivity extends AppCompatActivity {

    private boolean zoomed = false;
    private Uri currentUri;
    private ImageView detailImage;

    private final ActivityResultLauncher<Intent> cropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String croppedValue = result.getData().getStringExtra("cropped_uri");
                    if (croppedValue != null) {
                        currentUri = Uri.parse(croppedValue);
                        if (detailImage != null) {
                            detailImage.animate().cancel();
                            detailImage.setScaleX(1f);
                            detailImage.setScaleY(1f);
                            detailImage.setRotation(0f);
                            detailImage.clearColorFilter();
                            zoomed = false;
                            Glide.with(this).load(currentUri).into(detailImage);
                        }
                        ((TextView) findViewById(R.id.tvName)).setText("Ảnh đã cắt");
                        String savedAt = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
                                .format(new Date(System.currentTimeMillis()));
                        ((TextView) findViewById(R.id.tvDate)).setText("Đã cắt: " + savedAt);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_photo_detail);

        // ===== Nhận dữ liệu từ Intent =====
        currentUri = Uri.parse(getIntent().getStringExtra("uri"));
        String name = getIntent().getStringExtra("name");
        long date = getIntent().getLongExtra("date", 0);
        String transition = getIntent().getStringExtra("transition");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());

        ImageView img = findViewById(R.id.imgDetail);
        detailImage = img;
        img.setTransitionName(transition);
        Glide.with(this).load(currentUri).into(img);

        ((TextView) findViewById(R.id.tvName)).setText(name);
        String d = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(new Date(date * 1000L));
        ((TextView) findViewById(R.id.tvDate)).setText("Thêm vào: " + d);

        // ===== Sự kiện: nhấn đúp để zoom =====
        GestureDetector gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                zoomed = !zoomed;
                float s = zoomed ? 2.5f : 1f;
                img.animate().scaleX(s).scaleY(s).setDuration(250).start();
                return true;
            }
        });
        img.setOnTouchListener((v, ev) -> gd.onTouchEvent(ev));

        // ===== Bộ lọc màu ảnh: ColorMatrix + ColorFilter =====
        findViewById(R.id.btnOriginal).setOnClickListener(v -> applyFilter(img, "original"));
        findViewById(R.id.btnGray).setOnClickListener(v -> applyFilter(img, "gray"));
        findViewById(R.id.btnSepia).setOnClickListener(v -> applyFilter(img, "sepia"));
        findViewById(R.id.btnInvert).setOnClickListener(v -> applyFilter(img, "invert"));
        findViewById(R.id.btnBright).setOnClickListener(v -> applyFilter(img, "bright"));

        // ===== Cắt ảnh theo vùng người dùng tự chọn =====
        findViewById(R.id.btnCrop).setOnClickListener(v -> {
            Intent cropIntent = new Intent(this, CropActivity.class);
            cropIntent.putExtra("uri", currentUri.toString());
            cropLauncher.launch(cropIntent);
        });

        // ===== Nút xoay: animation =====
        findViewById(R.id.btnRotate).setOnClickListener(v ->
                img.animate().rotationBy(90).setDuration(300).start());

        // ===== Nút chia sẻ: Implicit Intent =====
        findViewById(R.id.btnShare).setOnClickListener(v -> {
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("image/*");
            s.putExtra(Intent.EXTRA_STREAM, currentUri);
            s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(s, "Chia sẻ ảnh"));
        });
    }

    private void applyFilter(ImageView img, String filter) {
        ColorMatrix matrix = new ColorMatrix();
        switch (filter) {
            case "gray":
                matrix.setSaturation(0f);
                break;
            case "sepia":
                matrix.set(new float[] {
                        0.393f, 0.769f, 0.189f, 0, 0,
                        0.349f, 0.686f, 0.168f, 0, 0,
                        0.272f, 0.534f, 0.131f, 0, 0,
                        0, 0, 0, 1, 0
                });
                break;
            case "invert":
                matrix.set(new float[] {
                        -1, 0, 0, 0, 255,
                        0, -1, 0, 0, 255,
                        0, 0, -1, 0, 255,
                        0, 0, 0, 1, 0
                });
                break;
            case "bright":
                matrix.set(new float[] {
                        1, 0, 0, 0, 35,
                        0, 1, 0, 0, 35,
                        0, 0, 1, 0, 35,
                        0, 0, 0, 1, 0
                });
                break;
            case "original":
            default:
                img.animate().alpha(0.55f).setDuration(100).withEndAction(() -> {
                    img.clearColorFilter();
                    img.animate().alpha(1f).setDuration(180).start();
                }).start();
                return;
        }
        img.animate().alpha(0.55f).setDuration(100).withEndAction(() -> {
            img.setColorFilter(new ColorMatrixColorFilter(matrix));
            img.animate().alpha(1f).setDuration(180).start();
        }).start();
    }

}
