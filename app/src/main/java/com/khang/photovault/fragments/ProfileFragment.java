package com.khang.photovault.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.khang.photovault.LoginActivity;
import com.khang.photovault.R;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvAvatar, tvFavCount;
    private ImageView imgAvatar;
    private FirebaseUser user;

    // Chọn ảnh đại diện từ máy (Intent ACTION_OPEN_DOCUMENT)
    private final ActivityResultLauncher<String[]> pickAvatar =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null || user == null) return;
                try {
                    requireContext().getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {
                }
                requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                        .edit().putString("avatar_" + user.getUid(), uri.toString()).apply();
                showAvatar(uri);
            });

    private void showAvatar(Uri uri) {
        imgAvatar.setVisibility(View.VISIBLE);
        Glide.with(this).load(uri).circleCrop().into(imgAvatar);
    }

    /** Đăng xuất và quay về màn hình đăng nhập (dùng chung cho Toolbar + nút trong Hồ sơ) */
    public static void logout(Activity a) {
        FirebaseAuth.getInstance().signOut();
        Intent i = new Intent(a, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_profile, c, false);
        user = FirebaseAuth.getInstance().getCurrentUser();

        tvName = v.findViewById(R.id.tvName);
        tvEmail = v.findViewById(R.id.tvEmail);
        tvAvatar = v.findViewById(R.id.tvAvatar);
        tvFavCount = v.findViewById(R.id.tvFavCount);
        if (user == null) return v;

        bindUser();
        loadFavCount();

        // Ảnh đại diện
        imgAvatar = v.findViewById(R.id.imgAvatar);
        String saved = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                .getString("avatar_" + user.getUid(), null);
        if (saved != null) showAvatar(Uri.parse(saved));
        ((View) tvAvatar.getParent()).setOnClickListener(x -> pickAvatar.launch(new String[]{"image/*"}));

        // Dark mode
        SharedPreferences sp = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
        MaterialSwitch sw = v.findViewById(R.id.swDark);
        sw.setChecked(sp.getBoolean("dark", false));
        sw.setOnCheckedChangeListener((btn, checked) -> {
            sp.edit().putBoolean("dark", checked).apply();
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        v.findViewById(R.id.btnEdit).setOnClickListener(x -> editName());

        v.findViewById(R.id.btnReset).setOnClickListener(x ->
                FirebaseAuth.getInstance().sendPasswordResetEmail(user.getEmail())
                        .addOnSuccessListener(r -> Toast.makeText(getContext(),
                                "Đã gửi email đặt lại mật khẩu tới " + user.getEmail(), Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show()));

        v.findViewById(R.id.btnLogout).setOnClickListener(x -> logout(requireActivity()));
        return v;
    }

    private void bindUser() {
        String email = user.getEmail() == null ? "" : user.getEmail();
        String name = user.getDisplayName();
        if (name == null || name.isEmpty())
            name = email.contains("@") ? email.substring(0, email.indexOf('@')) : "Người dùng";

        tvName.setText(name);
        tvEmail.setText(email);
        tvAvatar.setText(name.substring(0, 1).toUpperCase());
    }

    private void loadFavCount() {
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .collection("favorites").get()
                .addOnSuccessListener(s -> {
                    if (isAdded()) tvFavCount.setText(String.valueOf(s.size()));
                });
    }

    private void editName() {
        View dlg = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_name, null);
        TextInputEditText edt = dlg.findViewById(R.id.edtNewName);
        edt.setText(user.getDisplayName());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đổi tên hiển thị")
                .setView(dlg)
                .setPositiveButton("Lưu", (d, w) -> {
                    String n = edt.getText().toString().trim();
                    if (n.isEmpty()) return;
                    user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(n).build())
                            .addOnSuccessListener(x -> {
                                FirebaseFirestore.getInstance().collection("users")
                                        .document(user.getUid()).update("name", n);
                                if (isAdded()) bindUser();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
