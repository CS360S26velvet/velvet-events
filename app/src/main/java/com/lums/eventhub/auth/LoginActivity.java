package com.lums.eventhub.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.lums.eventhub.R;
import com.lums.eventhub.admin.dashboard.AdminDashboardActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String user = etUsername.getText().toString();
            if (user.startsWith("#AD")) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
            } else {
                Toast.makeText(this, "Use #AD prefix for Admin", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
