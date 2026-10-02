package org.feup.apm.cmeb_login;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
Button login_button;
EditText etEmail,etPassword;
TextView start_register;


public FirebaseAuth auth = FirebaseAuth.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(getResources().getColor(R.color.bg_color));


        //Views definition
        login_button = findViewById(R.id.login_button);
         etEmail = findViewById(R.id.etEmail);
         etPassword = findViewById(R.id.etPassword);
         start_register = findViewById(R.id.start_register);
        login_button.setEnabled(false);



        // Textwatcher to verify the inputs dynamically
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
             //not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isEnabled =    //only enabled when both fields are filled
                        !etEmail.getText().toString().trim().isEmpty() &&
                                !etPassword.getText().toString().trim().isEmpty();

                // Updating button state
                login_button.setEnabled(isEnabled);


                if (isEnabled) {
                    login_button.setAlpha(1.0f);
                } else {
                    login_button.setAlpha(0.5f);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        };

        // Adding the text watcher
        etEmail.addTextChangedListener(watcher);
        etPassword.addTextChangedListener(watcher);


        // User clicks the login button
        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                String txt_email = etEmail.getText().toString().trim();
                String txt_password = etPassword.getText().toString().trim();
                loginUser(txt_email,txt_password);
            }

        });


         //Going to the register activity
        start_register.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(i);
            finish();
        });






        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


    }


    private void loginUser(String email, String password){
        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener(this,new OnSuccessListener<AuthResult>(){ //using the Firebase Authentication service
            @Override
                    public void onSuccess(AuthResult authResult){
                Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent (LoginActivity.this, MainActivity.class));  //going to the main activity
                finish();
            }
        });

        auth.signInWithEmailAndPassword(email, password).addOnFailureListener(this, e -> {  //Displaying errors
            String errorMessage;
            if (e.getMessage().contains("There is no user record")) {
                errorMessage = "Email not registered. Please sign up.";
            } else if (e.getMessage().contains("The password is invalid")) {
                errorMessage = "Incorrect password. Please try again.";
            } else {
                errorMessage = "Login failed: please insert valid credentials.";
            }
            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
        });
    }
}
