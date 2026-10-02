package org.feup.apm.cmeb_login;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import org.feup.apm.cmeb_login.model.UserModel;
import org.feup.apm.cmeb_login.util.AndroidUtil;
import org.feup.apm.cmeb_login.util.FirebaseUtil;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    UserModel userModel; //user to create

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //EdgeToEdge.enable(this);

        setContentView(R.layout.activity_register);


        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(getResources().getColor(R.color.bg_color));


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // For when user presses the back button
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                intent.putExtra("targetFragment", "ChatFragment"); // Adiciona uma identificação para o fragmento
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish(); // Finishing the Search Users Activity
            }
        });


        auth = FirebaseAuth.getInstance();  //to allow the authentication with email and password from Firebase (Firebase Authentication)


        //Definition of the elements:
        Button register_button = findViewById(R.id.settings);
        EditText etUsername = findViewById(R.id.etUsername);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword1 = findViewById(R.id.etPassword1);
        EditText etPassword2 = findViewById(R.id.etPassword2);
        EditText yearsExperience = findViewById(R.id.etYears);
        EditText etHospital = findViewById(R.id.etHospital);
        EditText etSpecialty = findViewById(R.id.etSpecialty);

        //While the button is disabled:
        register_button.setAlpha(0.5f);




        //same as before
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
             //not necessary
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isEnabled =
                        !etUsername.getText().toString().isEmpty() &&
                                !etPassword1.getText().toString().trim().isEmpty() &&
                                !etPassword2.getText().toString().trim().isEmpty() &&
                        !yearsExperience.getText().toString().trim().isEmpty() &&
                                !etSpecialty.getText().toString().trim().isEmpty() &&
                                !etHospital.getText().toString().trim().isEmpty();

                // if every box is registered, button is enabled
                register_button.setEnabled(isEnabled);

                // Update opacity
                if (isEnabled) {
                    register_button.setAlpha(1.0f);
                } else {
                    register_button.setAlpha(0.5f);
                }

                if (etPassword1.getText().length() > 0 && etPassword1.getText().length() < 8) {
                    etPassword1.setError("Password must be at least 8 characters long.");
                } else {
                    etPassword1.setError(null); // Remove when the input is valid
                }

                if (etPassword2.getText().length() > 0) {
                    if (etPassword1.getText().length() == 0) {
                        etPassword2.setError("Start by typing the password in the field above.");
                    } else if (!etPassword2.getText().toString().trim().equals(etPassword1.getText().toString().trim())) {
                        etPassword2.setError("Passwords must be equal.");
                    } else {
                        etPassword2.setError(null); // Remove the error when valid
                    }
                } else {
                    etPassword2.setError(null); // Clear error if field is empty
                }


                if (yearsExperience.getText().length() > 0  && !yearsExperience.getText().toString().trim().matches("\\d+")) {
                    yearsExperience.setError("You must write an integer number");  //making sure the typed number is integer
                }

                else if( yearsExperience.getText().length() > 0  && AndroidUtil.isInteger(yearsExperience.getText().toString().trim()) && (Integer.parseInt(yearsExperience.getText().toString().trim()) > 70 || Integer.parseInt(yearsExperience.getText().toString().trim()) < 0)){
                    yearsExperience.setError("Write a valid number"); // the number must be between 0 and 70
                }
                else{
                    yearsExperience.setError(null);
                }


                if(etUsername.getText().length() > 20){
                    etUsername.setError("Your username can't be so long");
                }
                else{
                    etUsername.setError(null);
                }




            }

            @Override
            public void afterTextChanged(Editable s) {
                // not necessary
            }
        };


        //Adding the text watcher to the fields
        etPassword1.addTextChangedListener(watcher);
        etPassword2.addTextChangedListener(watcher);
        yearsExperience.addTextChangedListener(watcher);
        etUsername.addTextChangedListener(watcher);



        //User clicks register
        register_button.setOnClickListener(v -> {

            //Getting the text from the EditTexts to strings:
            String email = etEmail.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password1 = etPassword1.getText().toString().trim();
            String password2 = etPassword2.getText().toString().trim();
            String hospital = etHospital.getText().toString().trim();
            String years_Experience = yearsExperience.getText().toString().trim();
            String specialty = etSpecialty.getText().toString().trim();

            try {


                // Verify the typed passwords
                if (!(password1.equals(password2))) {
                    throw new Exception("Passwords do not match.");
                }

                if (password1.length() < 8) {
                    throw new Exception("Password must be at least 8 characters long.");
                }

                if (!years_Experience.isEmpty() && !AndroidUtil.isInteger(years_Experience)) {
                    throw new Exception("You must write an integer number of years of experience.");
                }


                if( !years_Experience.isEmpty() && years_Experience.matches("\\d+") && (Integer.parseInt(years_Experience) > 70 || Integer.parseInt(years_Experience) < 0)){
                    throw new Exception("Years of experience must be between 0 and 70.");
                }

                if(etUsername.getText().length() > 20){
                    throw new Exception("Write a shorter username");
                }

                //Calling the register function
                registerUser(username,email, password1, Integer.parseInt(years_Experience), hospital, specialty);


            } catch (Exception e) {
                // Display error message
                Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }


        });


       // ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
    //        Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      //      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
        //    return insets;
       // });
    }

    private void registerUser(String username, String email, String password, int yearsExperience, String hospital, String specialty) {
        // Verifying if username already exists
        FirebaseUtil.allUserCollectionReference()
                .whereEqualTo("username", username) // Filter by username
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // If the result isn't empty, username already exists
                        Toast.makeText(RegisterActivity.this, "Username already exists. Please choose another one.", Toast.LENGTH_SHORT).show();
                    } else if (task.isSuccessful()) {
                        // If username doesn't exist
                        auth.createUserWithEmailAndPassword(email, password) //calling the Firebase Authentication
                                .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(RegisterActivity.this, "Registering Successful", Toast.LENGTH_SHORT).show(); //success message
                                            userModel = new UserModel(username, email, Timestamp.now(),FirebaseUtil.currentUserId(), yearsExperience, hospital, specialty); //check UserModel to see the attributes
                                            FirebaseUtil.currentUserDetails().set(userModel);

                                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class); //navigating to Main Activity
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(RegisterActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    } else {
                        // In case the search for the user goes wrong
                        Toast.makeText(RegisterActivity.this, "Error checking username: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }






}