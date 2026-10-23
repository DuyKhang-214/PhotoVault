package com.khang.photovault;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private TextInputEditText edtEmail, edtPass;
    private MaterialButton btnLogin;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) { // đã đăng nhập thì vào thẳng Main
            goMain();
            return;
        }
        setContentView(R.layout.activity_login);

        edtEmail = findViewById(R.id.edtEmail);
        edtPass = findViewById(R.id.edtPass);
        btnLogin = findViewById(R.id.btnLogin);

        // Animation: card trượt lên + mờ dần hiện ra
        findViewById(R.id.cardLogin).startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.slide_up));

        btnLogin.setOnClickListener(v -> login());
        findViewById(R.id.tvRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.tvForgot).setOnClickListener(v -> forgotPassword());
    }

    private void login() {
        String email = edtEmail.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();
        if (email.isEmpty() || pass.length() < 6) {
            Toast.makeText(this, "Nhập email và mật khẩu (tối thiểu 6 ký tự)", Toast.LENGTH_SHORT).show();
            return;
        }
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");
        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(r -> goMain())
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng nhập");
                    Toast.makeText(this, "Đăng nhập thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void forgotPassword() {
        String email = edtEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Nhập email vào ô Email trước", Toast.LENGTH_SHORT).show();
            return;
        }
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> Toast.makeText(this, "Đã gửi email đặt lại mật khẩu", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void goMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
