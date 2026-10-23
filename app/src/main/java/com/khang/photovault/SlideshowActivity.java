package com.khang.photovault;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình trình chiếu ảnh (slideshow).
 * TOÀN BỘ giao diện được dựng bằng mã Java (không dùng file XML layout).
 */
public class SlideshowActivity extends AppCompatActivity {

    private static final long INTERVAL = 3000; // 3 giây / ảnh

    private final Handler handler = new Handler(Looper.getMainLooper());
    private List<String> uris;
    private int index;
    private boolean playing = true;

    private ImageView img;
    private TextView tvCounter;
    private ImageButton btnPlay;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!playing) return;
            show(index + 1);
            handler.postDelayed(this, INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        ArrayList<String> list = getIntent().getStringArrayListExtra("uris");
        if (list == null || list.isEmpty()) {
            finish();
            return;
        }
        uris = list;
        index = getIntent().getIntExtra("start", 0);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(buildUi());
        show(index);
    }

    // ===================== DỰNG GIAO DIỆN BẰNG JAVA =====================
    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.setFitsSystemWindows(true);

        // Ảnh
        img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(img, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Thanh trên: nút quay lại + tiêu đề + bộ đếm
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(0x88000000);
        top.setPadding(dp(8), dp(8), dp(16), dp(8));

        ImageButton btnBack = makeButton(R.drawable.ic_back);
        btnBack.setOnClickListener(v -> finish());
        top.addView(btnBack, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = new TextView(this);
        title.setText("Trình chiếu");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setPadding(dp(8), 0, 0, 0);
        top.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        tvCounter = new TextView(this);
        tvCounter.setTextColor(Color.WHITE);
        tvCounter.setTextSize(15);
        top.addView(tvCounter);

        root.addView(top, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP));

        // Thanh dưới: trước / phát-tạm dừng / sau
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.setBackgroundColor(0x88000000);
        bottom.setPadding(0, dp(10), 0, dp(10));

        ImageButton btnPrev = makeButton(R.drawable.ic_prev);
        btnPlay = makeButton(R.drawable.ic_pause);
        ImageButton btnNext = makeButton(R.drawable.ic_next);

        btnPrev.setOnClickListener(v -> manual(index - 1));
        btnNext.setOnClickListener(v -> manual(index + 1));
        btnPlay.setOnClickListener(v -> togglePlay());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(64), dp(64));
        lp.setMargins(dp(12), 0, dp(12), 0);
        bottom.addView(btnPrev, lp);
        bottom.addView(btnPlay, lp);
        bottom.addView(btnNext, lp);

        root.addView(bottom, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        return root;
    }

    private ImageButton makeButton(int iconRes) {
        ImageButton b = new ImageButton(this);
        b.setImageResource(iconRes);
        b.setColorFilter(Color.WHITE);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setScaleType(ImageView.ScaleType.CENTER);
        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ===================== LOGIC =====================
    /** Hiện ảnh thứ i với hiệu ứng mờ chéo + phóng to nhẹ */
    private void show(int i) {
        int n = uris.size();
        index = ((i % n) + n) % n;

        Glide.with(this)
                .load(Uri.parse(uris.get(index)))
                .transition(DrawableTransitionOptions.withCrossFade(600))
                .into(img);

        img.animate().cancel();
        img.setScaleX(1f);
        img.setScaleY(1f);
        img.animate().scaleX(1.07f).scaleY(1.07f).setDuration(INTERVAL + 600).start();

        tvCounter.setText((index + 1) + " / " + n);
    }

    /** Bấm tay trước/sau: đổi ảnh rồi tính lại giờ đếm */
    private void manual(int i) {
        show(i);
        if (playing) {
            handler.removeCallbacks(ticker);
            handler.postDelayed(ticker, INTERVAL);
        }
    }

    private void togglePlay() {
        playing = !playing;
        btnPlay.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        btnPlay.setColorFilter(Color.WHITE);
        handler.removeCallbacks(ticker);
        if (playing) handler.postDelayed(ticker, INTERVAL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        if (playing) handler.postDelayed(ticker, INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }
}
