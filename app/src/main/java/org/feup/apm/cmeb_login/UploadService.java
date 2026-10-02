package org.feup.apm.cmeb_login;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UploadService extends Service {
    private ExecutorService executorService;

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Uri fileUri = intent.getParcelableExtra("fileUri");
        String userId = intent.getStringExtra("userId");
        String timestamp = intent.getStringExtra("timestamp");
        String fileType = intent.getStringExtra("fileType");
        Log.d("UploadService", "File being uploaded");

        if (fileUri != null && userId != null && timestamp != null && fileType != null) {
            if (fileType.equals("image")) {
                StorageReference galleryImageRef = FirebaseStorage.getInstance().getReference().child("gallery/images/" + userId + "/" + timestamp + ".jpg");
                executorService.execute(() -> uploadToGalleryFolder(fileUri, galleryImageRef));
            } else if (fileType.equals("video")) {
                StorageReference galleryVideoRef = FirebaseStorage.getInstance().getReference().child("gallery/videos/" + userId + "/" + timestamp + ".mp4");
                executorService.execute(() -> uploadToGalleryFolder(fileUri, galleryVideoRef));
            }
        }

        return START_NOT_STICKY;
    }

    private void uploadToGalleryFolder(Uri fileUri, StorageReference galleryRef) {
        galleryRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d("UploadService", "File uploaded to gallery folder successfully");
                    stopSelf();
                }).addOnFailureListener(e -> {
                    Log.e("UploadService", "Failed to upload file to gallery folder", e);
                    stopSelf();
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
