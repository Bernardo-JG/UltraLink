package org.feup.apm.cmeb_login;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.Manifest;


import android.provider.MediaStore;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

import org.feup.apm.cmeb_login.util.FirebaseUtil;

public class HomeFragment extends Fragment {

    private Button logout, acquire,settings;
    private Button gallery;
    private Button uploadButton;
    private ActivityResultLauncher<Intent> resultLauncher;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

            requireActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            requireActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.bg_color));

        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        // All permissions granted, navigate to AcquireFile activity
                        navigateToAcquireFile();
                    } else {
                        // Check if permissions were denied permanently
                        if (shouldShowPermissionRationale()) {
                            showPermissionRationaleDialog();
                        } else {
                            showSettingsRedirectDialog();
                        }
                    }
                });
    }

    private void checkAndRequestPermissions() {
        if (hasRequiredPermissions()) {
            navigateToAcquireFile();
        } else {
            requestPermissionsLauncher.launch(new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
        }
    }

    private boolean hasRequiredPermissions() {
        Context context = requireContext();
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean shouldShowPermissionRationale() {
        Activity activity = requireActivity();
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.BLUETOOTH) ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.BLUETOOTH_ADMIN) ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.BLUETOOTH_CONNECT);
    }

    private void navigateToAcquireFile() {
        Intent intent = new Intent(requireContext(), AcquireFile.class);
        startActivity(intent);
    }

    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Permissions Required")
                .setMessage("Permissions are required to access Bluetooth features. Please grant them to proceed.")
                .setPositiveButton("Grant Permissions", (dialog, which) -> checkAndRequestPermissions())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showSettingsRedirectDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Permissions Required")
                .setMessage("Permissions are required to access Bluetooth features. Please enable them in the app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // Defining the elements
        logout = rootView.findViewById(R.id.Logout);
        acquire = rootView.findViewById(R.id.acquire_images);
        gallery = rootView.findViewById(R.id.gallery);
        settings = rootView.findViewById(R.id.settings);
        uploadButton = rootView.findViewById(R.id.upload);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requireActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        getFCMToken();




        // Logout button
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Deleting the fcmToken (you can't receive notifications when logged out)
                FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            FirebaseUtil.currentUserDetails().update("fcmToken",null); //updating the token info in Firebase

                            FirebaseAuth.getInstance().signOut();
                            Toast.makeText(getActivity(), "You have signed out successfully.", Toast.LENGTH_SHORT).show();

                            // Going to LoginActivity
                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            startActivity(intent);
                            getActivity().finish();
                        }
                    }
                });

            }
        });

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), GalleryActivity.class);
                startActivity(intent);

                // Finalizar a atividade atual, se necessário
                getActivity().finish();
            }
        });



        acquire.setOnClickListener(v -> checkAndRequestPermissions());

        uploadButton.setOnClickListener(v -> pickMedia());

        // Register result launcher
        registerResult();

 settings.setOnClickListener(new View.OnClickListener() {
     @Override
     public void onClick(View view) {
         Intent intent = new Intent(getActivity(), SettingsActivity.class);
         startActivity(intent);
         getActivity().finish();
     }
 });
        return rootView;
    }


    //generating a token when the user enters the HomeFragment
    void getFCMToken(){
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()){
                    String token = task.getResult();
                    FirebaseUtil.currentUserDetails().update("fcmToken",token);


                }
            }
        });
    }

    private void pickMedia() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Allow all types
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multiple selection
        resultLauncher.launch(intent);
    }

    private void registerResult() {
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    try {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();

                            // Handle multiple media items
                            if (data.getClipData() != null) {
                                int count = data.getClipData().getItemCount();
                                for (int i = 0; i < count; i++) {
                                    Uri mediaUri = data.getClipData().getItemAt(i).getUri();
                                    handleMediaUri(mediaUri);
                                }
                            }
                            // Handle single media item
                            else if (data.getData() != null) {
                                Uri mediaUri = data.getData();
                                handleMediaUri(mediaUri);
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), "Error processing media selection", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void handleMediaUri(Uri mediaUri) {
        if (mediaUri != null) {
            String mimeType = requireActivity().getContentResolver().getType(mediaUri);
            if (mimeType != null) {
                if (mimeType.startsWith("image")) {
                    String imagePath = getPathFromUri(mediaUri);
                    if (imagePath != null) {
                        Intent intent = new Intent(getActivity(), ImageEditorActivity.class);
                        intent.putExtra("image_path", imagePath);
                        startActivity(intent);
                    }
                } else if (mimeType.startsWith("video")) {
                    String videoPath = getPathFromUri(mediaUri);
                    if (videoPath != null) {
                        Intent intent = new Intent(getActivity(), VideoEditorActivity.class);
                        intent.putExtra("video_path", videoPath);
                        startActivity(intent);
                    }
                }
            }
        }
    }

    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Video.Media.DATA};
        Cursor cursor = requireActivity().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(columnIndex);
            cursor.close();
            return path;
        }
        return null;
    }




}