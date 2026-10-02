package org.feup.apm.cmeb_login;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.dhaval2404.imagepicker.ImagePicker;

import org.feup.apm.cmeb_login.model.UserModel;
import org.feup.apm.cmeb_login.util.AndroidUtil;
import org.feup.apm.cmeb_login.util.FirebaseUtil;

public class ProfileFragment extends Fragment {

    ImageView profilePic;
    EditText usernameInput;
    TextView emailInput;
    EditText yearsExperienceInput;
    EditText hospitalInput;
    EditText specialtyInput;

    Button updateProfileBtn;
    ActivityResultLauncher<Intent> imagePickLauncher;

    Uri selectedImageUri;
    UserModel currentUserModel;

    boolean activation_changes = false;

    private AlertDialog progressDialog;
    private boolean isProfileChanged = false;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

            requireActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            requireActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.bg_color));
        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null && data.getData() != null) {
                    selectedImageUri = data.getData();
                    AndroidUtil.setProfilePic(getContext(), selectedImageUri, profilePic);
                    activation_changes = true;
                    checkForChanges();
                }
            }
        });

        // Configuração do AlertDialog com ProgressBar
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Loading")
                .setMessage("Please wait...")
                .setCancelable(false)
                .setView(new ProgressBar(getContext()));

        progressDialog = builder.create();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);


        //elements definition
        profilePic = view.findViewById(R.id.profile_image_view);
        usernameInput = view.findViewById(R.id.username_profile);
        emailInput = view.findViewById(R.id.email_profile);
        yearsExperienceInput = view.findViewById(R.id.yearsExperience);
        hospitalInput = view.findViewById(R.id.hospital);
        specialtyInput = view.findViewById(R.id.specialty);
        updateProfileBtn = view.findViewById(R.id.update_profile_button);





        progressDialog.show(); //while the data is loading the user can't travel through the app
        getUserData(); //fill the data

        updateProfileBtn.setEnabled(false); //the button is only enabled
        updateProfileBtn.setAlpha(0.5f);

        TextWatcher changeWatcher = new TextWatcher() { //dynamic assessment of the inputs
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkForChanges();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        //Adding the Textwathcer
        usernameInput.addTextChangedListener(changeWatcher);
        yearsExperienceInput.addTextChangedListener(changeWatcher);
        hospitalInput.addTextChangedListener(changeWatcher);
        specialtyInput.addTextChangedListener(changeWatcher);

        updateProfileBtn.setOnClickListener(v -> updateBtnClick());

        profilePic.setOnClickListener(v -> {
            ImagePicker.with(this).cropSquare().compress(512).maxResultSize(512, 512)
                    .createIntent(intent -> {
                        imagePickLauncher.launch(intent);
                        return null;
                    });
        });

        return view;
    }

    void checkForChanges() { //allows to only allow profile updating if the user actually changed its data
        boolean usernameChanged = !usernameInput.getText().toString().equals(currentUserModel.getUsername());
        boolean hospitalChanged = !hospitalInput.getText().toString().equals(currentUserModel.getHospital());
        boolean specialtyChanged = !specialtyInput.getText().toString().equals(currentUserModel.getSpecialty());
        boolean yearsChanged = !yearsExperienceInput.getText().toString().equals(String.valueOf(currentUserModel.getYearsExperience()));
        boolean imageChanged = selectedImageUri != null && activation_changes;

        isProfileChanged = usernameChanged || hospitalChanged || specialtyChanged || yearsChanged || imageChanged;

        updateProfileBtn.setEnabled(isProfileChanged);
        updateProfileBtn.setAlpha(isProfileChanged ? 1.0f : 0.5f);
    }

    void updateBtnClick() {
        if (!isProfileChanged) return;

        progressDialog.show(); // to make sure the user doesn't travel through the app while the information is updating

        //the info to be checked is present in the textViews when the user clicks the update profile button
        String newUsername = usernameInput.getText().toString();
        String newHospital = hospitalInput.getText().toString();
        String newspecialty = specialtyInput.getText().toString();
        String newYearsExperience = yearsExperienceInput.getText().toString();

        //Verifications:

        if (newUsername.isEmpty()) {
            usernameInput.setError("Type a valid username");
            progressDialog.dismiss();
            return;
        } else {
            currentUserModel.setUsername(newUsername);
        }

        if (newHospital.isEmpty()) {
            hospitalInput.setError("Type a valid hospital name");
            progressDialog.dismiss();
            return;
        } else {
            currentUserModel.setHospital(newHospital);
        }

        if (newspecialty.isEmpty()) {
            specialtyInput.setError("Type a valid medical specialty");
            progressDialog.dismiss();
            return;
        } else {
            currentUserModel.setSpecialty(newspecialty);
        }


         if (!newYearsExperience.matches("\\d+")) {
            yearsExperienceInput.setError("You need to write an integer number");
            progressDialog.dismiss();
            return;
        }
        else if (Integer.parseInt(newYearsExperience) > 70 || Integer.parseInt(newYearsExperience) < 0) {
            yearsExperienceInput.setError("Set a valid number");
            progressDialog.dismiss();
            return;
        }  else {
            currentUserModel.setYearsExperience(Integer.parseInt(newYearsExperience));
        }

        if (selectedImageUri != null) {
            FirebaseUtil.getCurrentProfilePicStorageRef().putFile(selectedImageUri)
                    .addOnCompleteListener(task -> updateToFirestore());
        } else {
            updateToFirestore();
        }
    }

    void updateToFirestore() { //getting the info to Firebase
        FirebaseUtil.currentUserDetails().set(currentUserModel)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        progressDialog.dismiss(); //shutting down
                        activation_changes = false;
                        Toast.makeText(getContext(),"Updated Successfully!", Toast.LENGTH_LONG).show();
                        updateProfileBtn.setEnabled(false);
                        updateProfileBtn.setAlpha(0.5f);

                    } else {
                        progressDialog.dismiss(); //shutting down
                        Toast.makeText(getContext(),"Updated failed!", Toast.LENGTH_LONG).show();;
                    }
                });
    }

    void getUserData() {
        FirebaseUtil.getCurrentProfilePicStorageRef().getDownloadUrl()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Uri uri = task.getResult();
                        AndroidUtil.setProfilePic(getContext(), uri, profilePic);
                    }
                    progressDialog.dismiss(); //progressDialog shuts down
                });

        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            currentUserModel = task.getResult().toObject(UserModel.class);

            //filling the text views:
            usernameInput.setText(currentUserModel.getUsername());
            emailInput.setText(currentUserModel.getEmail());
            yearsExperienceInput.setText(String.valueOf(currentUserModel.getYearsExperience()));
            hospitalInput.setText(currentUserModel.getHospital());
            specialtyInput.setText(currentUserModel.getSpecialty());

            checkForChanges();
        });
    }


}
