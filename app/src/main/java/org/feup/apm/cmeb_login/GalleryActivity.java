package org.feup.apm.cmeb_login;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.feup.apm.cmeb_login.adapter.GalleryAdapter;
import org.feup.apm.cmeb_login.util.FirebaseUtil;

import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    private GalleryAdapter adapter;
    private List<String> urls;
    private String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gallery);

        // Set the status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.bars_color));
        }
        // Optional: Ensure the text/icons are dark for better visibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        RecyclerView recyclerView = findViewById(R.id.imagegallery);
        recyclerView.setHasFixedSize(true);

        userID = FirebaseUtil.currentUserId();

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), 3);
        recyclerView.setLayoutManager(layoutManager);

        urls = new ArrayList<>();
        adapter = new GalleryAdapter(this, getApplicationContext(), urls);
        recyclerView.setAdapter(adapter);

        // Retrieve URLs from Firebase Storage
        prepareData();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(GalleryActivity.this, MainActivity.class);
                startActivity(intent);
                Log.d("GalleryActivity", "Back button pressed");
                finish(); // Finish the activity
            }
        });
    }

    // Only retrieves urls that were saved to the user's gallery
    private void prepareData() {
        FirebaseFirestore.getInstance().collection("media")
                .whereArrayContains("userIds", userID)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String url = document.getString("url");
                        urls.add(url);
                        adapter.notifyItemInserted(urls.size() - 1);
                    }
                })
                .addOnFailureListener(exception -> Log.e("GalleryActivity", "Failed to fetch media", exception));
    }
}