package com.khang.photovault;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText edtName, edtEmail, edtPass, edtConfirm;
    private MaterialButton btnRegister;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_register);

        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPass = findViewById(R.id.edtPass);
        edtConfirm = findViewById(R.id.edtConfirm);
        btnRegister = findViewById(R.id.btnRegister);

        findViewById(R.id.cardRegister).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.slide_up));

        btnRegister.setOnClickListener(v -> register());
        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
    }

    private void register() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();
        String confirm = edtConfirm.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            toast("Vui lòng nhập đầy đủ họ tên và email");
            return;
        }
        if (pass.length() < 6) {
            toast("Mật khẩu tối thiểu 6 ký tự");
            return;
        }
        if (!pass.equals(confirm)) {
            toast("Mật khẩu nhập lại không khớp");
            return;
        }

        btnRegister.setEnabled(false);
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) return;

                    // Lưu thêm vào Firestore
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", name);
                    data.put("email", email);
                    data.put("createdAt", FieldValue.serverTimestamp());
                    FirebaseFirestore.getInstance().collection("users").document(user.getUid()).set(data);

                    // Lưu tên hiển thị rồi vào Main
                    user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build())
                            .addOnCompleteListener(t -> {
                                toast("Đăng ký thành công!");
                                Intent i = new Intent(this, MainActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(i);
                            });
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "Đăng ký thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
