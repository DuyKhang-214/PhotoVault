package com.khang.photovault;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoDetailActivity extends AppCompatActivity {

    private boolean zoomed = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_photo_detail);

        // ===== Nhận dữ liệu từ Intent =====
        Uri uri = Uri.parse(getIntent().getStringExtra("uri"));
        String name = getIntent().getStringExtra("name");
        long date = getIntent().getLongExtra("date", 0);
        String transition = getIntent().getStringExtra("transition");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());

        ImageView img = findViewById(R.id.imgDetail);
        img.setTransitionName(transition);
        Glide.with(this).load(uri).into(img);

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

        // ===== Nút xoay: animation =====
        findViewById(R.id.btnRotate).setOnClickListener(v ->
                img.animate().rotationBy(90).setDuration(300).start());

        // ===== Nút chia sẻ: Implicit Intent =====
        findViewById(R.id.btnShare).setOnClickListener(v -> {
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("image/*");
            s.putExtra(Intent.EXTRA_STREAM, uri);
            s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(s, "Chia sẻ ảnh"));
        });
    }
}
