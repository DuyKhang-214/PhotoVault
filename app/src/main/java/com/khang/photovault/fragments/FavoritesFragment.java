package com.khang.photovault.fragments;

import android.net.Uri;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.khang.photovault.model.Photo;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends BasePhotoFragment {

    @Override
    protected boolean favoritesMode() {
        return true;
    }

    @Override
    protected void loadData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("favorites").get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    List<Photo> list = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        Long date = d.getLong("dateAdded");
                        list.add(new Photo(
                                Long.parseLong(d.getId()),
                                Uri.parse(d.getString("uri")),
                                d.getString("name"),
                                date == null ? 0 : date));
                    }
                    showData(list);
                })
                .addOnFailureListener(e -> {
                    if (isAdded())
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
