package com.khang.photovault.adapter;

import android.app.Activity;
import android.content.Intent;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.khang.photovault.PhotoDetailActivity;
import com.khang.photovault.R;
import com.khang.photovault.model.Photo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.VH> {

    private final Activity act;
    private final boolean favMode;
    private final Runnable onFavChanged;

    private final List<Photo> all = new ArrayList<>();
    private final List<Photo> shown = new ArrayList<>();
    private String query = "";
    private int sortType = 0;

    public PhotoAdapter(Activity act, boolean favMode, Runnable onFavChanged) {
        this.act = act;
        this.favMode = favMode;
        this.onFavChanged = onFavChanged;
    }

    // ===== Dữ liệu / tìm kiếm / sắp xếp =====
    public void setData(List<Photo> list) {
        all.clear();
        all.addAll(list);
        apply();
    }

    public void setQuery(String q) {
        query = q == null ? "" : q.trim().toLowerCase();
        apply();
    }

    public void setSort(int type) {
        sortType = type;
        apply();
    }

    private void apply() {
        shown.clear();
        for (Photo p : all)
            if (query.isEmpty() || p.name.toLowerCase().contains(query)) shown.add(p);

        if (sortType == 0) shown.sort((a, b) -> Long.compare(b.dateAdded, a.dateAdded));
        else if (sortType == 1) shown.sort((a, b) -> Long.compare(a.dateAdded, b.dateAdded));
        else shown.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        notifyDataSetChanged();
    }

    // ===== ViewHolder + ContextMenu =====
    class VH extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        ImageView img;

        VH(View v) {
            super(v);
            img = v.findViewById(R.id.imgPhoto);
            v.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo info) {
            int pos = getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Photo p = shown.get(pos);

            menu.setHeaderTitle(p.name);
            act.getMenuInflater().inflate(R.menu.menu_photo_context, menu);
            menu.findItem(R.id.ctx_favorite).setTitle(favMode ? "Bỏ yêu thích" : "❤ Thêm vào yêu thích");
            for (int i = 0; i < menu.size(); i++)
                menu.getItem(i).setOnMenuItemClickListener(it -> onContextClick(it, p));
        }
    }

    private boolean onContextClick(MenuItem it, Photo p) {
        int id = it.getItemId();
        if (id == R.id.ctx_favorite) {
            if (favMode) removeFavorite(p);
            else addFavorite(p);
        } else if (id == R.id.ctx_share) {
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("image/*");
            s.putExtra(Intent.EXTRA_STREAM, p.uri);
            s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            act.startActivity(Intent.createChooser(s, "Chia sẻ ảnh"));
        } else if (id == R.id.ctx_info) {
            String d = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
                    .format(new Date(p.dateAdded * 1000L));
            new MaterialAlertDialogBuilder(act)
                    .setTitle("Thông tin ảnh")
                    .setMessage("Tên: " + p.name + "\nNgày thêm: " + d + "\nID: " + p.id)
                    .setPositiveButton("Đóng", null)
                    .show();
        }
        return true;
    }

    private void addFavorite(Photo p) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        Map<String, Object> m = new HashMap<>();
        m.put("uri", p.uri.toString());
        m.put("name", p.name);
        m.put("dateAdded", p.dateAdded);
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("favorites").document(String.valueOf(p.id)).set(m)
                .addOnSuccessListener(v -> Toast.makeText(act, "Đã thêm vào yêu thích ❤", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(act, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void removeFavorite(Photo p) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("favorites").document(String.valueOf(p.id)).delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(act, "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
                    if (onFavChanged != null) onFavChanged.run();
                });
    }

    // ===== Adapter =====
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(act).inflate(R.layout.item_photo, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Photo p = shown.get(pos);
        Glide.with(act).load(p.uri).centerCrop().into(h.img);

        String tn = "photo_" + p.id;
        h.img.setTransitionName(tn);

        h.itemView.setOnClickListener(v -> {
            // Explicit Intent + extras + Shared Element Transition
            Intent i = new Intent(act, PhotoDetailActivity.class);
            i.putExtra("uri", p.uri.toString());
            i.putExtra("name", p.name);
            i.putExtra("date", p.dateAdded);
            i.putExtra("transition", tn);
            ActivityOptionsCompat opt = ActivityOptionsCompat.makeSceneTransitionAnimation(act, h.img, tn);
            act.startActivity(i, opt.toBundle());
        });
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }
}
