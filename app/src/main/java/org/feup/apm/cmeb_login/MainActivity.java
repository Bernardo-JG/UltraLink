package org.feup.apm.cmeb_login;

import android.app.NotificationManager;

import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import android.view.MenuItem;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.ContextCompat;



import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;



public class MainActivity extends AppCompatActivity {
    private Button logout;
    HomeFragment homeFragment;
    ProfileFragment profileFragment;
    ChatFragment chatFragment;

    BottomNavigationView bottomNavigationView;

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);







            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermission();
        }



        homeFragment = new HomeFragment();
        profileFragment = new ProfileFragment();
        chatFragment = new ChatFragment();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
       
        bottomNavigationView.setItemActiveIndicatorColor(ColorStateList.valueOf(Color.parseColor("#3300BCD4")));

        //when pressing back from the ChatActivity for example (going directly to the ChatFragment)
        String targetFragment = getIntent().getStringExtra("targetFragment"); //getting intent
        if ("ChatFragment".equals(targetFragment)) {
            getSupportFragmentManager().beginTransaction().replace(R.id.main, chatFragment).commit();
            bottomNavigationView.setSelectedItemId(R.id.menu_chat);

        }
        if ("HomeFragment".equals(targetFragment)) {
            getSupportFragmentManager().beginTransaction().replace(R.id.main, homeFragment).commit();
            bottomNavigationView.setSelectedItemId(R.id.menu_home);
        }else {
            getSupportFragmentManager().beginTransaction().replace(R.id.main, homeFragment).commit();
            bottomNavigationView.setSelectedItemId(R.id.menu_home);
        }


        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                if (item.getItemId() == R.id.menu_home){
                    getSupportFragmentManager().beginTransaction().replace(R.id.main,homeFragment).commit();

                }
                if (item.getItemId() == R.id.menu_profile){
                    getSupportFragmentManager().beginTransaction().replace(R.id.main,profileFragment).commit();

                }
                if (item.getItemId() == R.id.menu_chat){
                    getSupportFragmentManager().beginTransaction().replace(R.id.main,chatFragment).commit();

                }


                return true;
            }
        });





    }



    //Notification permissions : shown if the user doesn't have the permission already accepted
    private void checkNotificationPermission() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted
          //  Toast.makeText(this, "Permission for notifications already granted", Toast.LENGTH_SHORT).show();
        } else {
            // Ask for permission
            requestNotificationPermission();
        }
    }

    // Asking for permission
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    // Processes the user's response
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Toast.makeText(this, "Permission for notifications granted", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied
                Toast.makeText(this, "Permission for notifications denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }










}