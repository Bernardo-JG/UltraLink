package org.feup.apm.cmeb_login;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.media3.exoplayer.ExoPlayer;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.slider.RangeSlider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VideoEditorActivity extends AppCompatActivity {
    private static final String TAG = "VideoEditor";
    private static final int REQUEST_WRITE_STORAGE = 1001;
    private RangeSlider rangeSlider;
    private Uri videoUri;
    private ExoPlayer exoPlayer;
    private int initialDurationInSeconds;
    boolean firstTime = true;
    private String outputPath, mediaPath, videoPath;
    AlertDialog alert;
    private Button save;


    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_editor);
        save = findViewById(R.id.btnSave);
        final boolean[] saved = {false};
        final String[] firebaseUrl = new String[1];

        // Set the status bar color to match your toolbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.bg_color));
        }

        // Optional: Ensure the text/icons are dark for better visibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        videoPath = getIntent().getStringExtra("video_path"); // Comes from Upload or from AcquireFiles
        mediaPath = getIntent().getStringExtra("media_path"); // Comes from Chat Activity
        String originalVideo = getIntent().getStringExtra("original_path"); // Used to know if te video has been trimmed
        String galleryPath = getIntent().getStringExtra("media_gallery_path"); // Comes from Gallery
        String mediaUrl = getIntent().getStringExtra("media_url"); // Comes from Chat Activity
        String galleryUrl = getIntent().getStringExtra("galleryUrl"); // Comes from Chat Activity
        if(mediaPath != null)
            videoPath = mediaPath;
        if(galleryPath != null)
            videoPath = galleryPath;
        if (videoPath != null) {
            videoUri = Uri.fromFile(new File(videoPath)); // Create a Uri from the file path
        } else {
            Log.e("VideoEditorActivity", "No video path provided!");
            return; // Exit if no video path is provided
        }

        rangeSlider = findViewById(R.id.RangeSlider);
        PlayerView playerView = findViewById(R.id.playerView);

        exoPlayer = new ExoPlayer.Builder(this).build(); // Build the exo player to play the video
        playerView.setPlayer(exoPlayer);
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri));
        exoPlayer.prepare();
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT); // Resize Mode Fit to maintain video dimensions

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    long duration = exoPlayer.getDuration();
                    if (duration != C.TIME_UNSET) {
                        if (firstTime) {
                            initialDurationInSeconds = (int) (duration / 1000); // Set initial duration of the video

                            // Set the range slider values
                            rangeSlider.setValueFrom(0);
                            rangeSlider.setValueTo(initialDurationInSeconds); // Set the maximum value
                            rangeSlider.setValues(0f, (float) initialDurationInSeconds); // Set initial values
                            firstTime = false;
                        }
                    }
                }
            }
        });

        rangeSlider.addOnChangeListener((slider, value, fromUser) -> updateClipping()); // updates video start and finish
                                                                    // based on what the user chooses in the range slider

        save.setOnClickListener(v -> { // Saves the trimmed video in the case where the set duration is not the same as the video duration
            String buttonText = save.getText().toString(); // Get the current text of the button

            if (buttonText.equals("Save to Gallery")) {
                if(galleryPath != null)
                {
                    Toast.makeText(VideoEditorActivity.this, "Already in the gallery", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(mediaPath != null && originalVideo == null){ // If video wasn't trimmed and it came from chat, no need to upload to firebase
                    updateMediaCollection(mediaUrl, "video");
                    Toast.makeText(VideoEditorActivity.this, "Video saved in gallery", Toast.LENGTH_SHORT).show();
                }
                else{
                    uploadVideoToFirebase(new OnVideoUploadedCallback() {
                        @Override
                        public void onSuccess(String videoUrl) {
                            saved[0] = true;
                            firebaseUrl[0] = videoUrl;
                            updateMediaCollection(videoUrl, "video"); // Media collection is used to add the video to the user's gallery
                            Toast.makeText(VideoEditorActivity.this, "Video saved in gallery", Toast.LENGTH_SHORT).show();
                            if (alert.isShowing()) {
                                alert.dismiss();
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e("VideoEditorActivity", "Error uploading video", e);
                            if (alert.isShowing()) {
                                alert.dismiss();
                            }
                            Toast.makeText(VideoEditorActivity.this, "Failed to upload video", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                // Existing logic for trimming the video
                List<Float> positions = rangeSlider.getValues();
                int startPosition = positions.get(0).intValue();
                int endPosition = positions.get(1).intValue();
                trimVideo(videoUri, startPosition, endPosition);
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setItemTextAppearanceActiveBoldEnabled(false);
        bottomNavigationView.setItemActiveIndicatorEnabled(false); // Since, in this case, bottom navigation is used for better UI and not to navigate, set indicator to false
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if(item.getItemId() == R.id.action_back){
                // Back button logic
                if(mediaPath != null){ // In case mediaPath in not null, redirect to the same chat
                    Intent intent = new Intent(VideoEditorActivity.this, ChatActivity.class);
                    intent.putExtra("chatroomId", getIntent().getStringExtra("chatroomId"));
                    intent.putExtra("otherUserId", getIntent().getStringExtra("otherUserId"));
                    intent.putExtra("otherUserUsername", getIntent().getStringExtra("otherUserUsername"));
                    intent.putExtra("myUsername", getIntent().getStringExtra("myUsername"));
                    intent.putExtra("fcmToken", getIntent().getStringExtra("fcmToken"));
                    startActivity(intent);
                    finish(); // Finish the current activity
                    return true;
                } else if(galleryPath != null){ // In case galleryPath in not null, redirect to the gallery
                    Intent backIntent = new Intent(VideoEditorActivity.this, GalleryActivity.class);
                    startActivity(backIntent);
                    finish(); // Finish the current activity
                    return true;
                }
                else{
                    Intent backIntent = new Intent(VideoEditorActivity.this, MainActivity.class);
                    startActivity(backIntent);
                    finish(); // Finish the current activity
                    return true;
                }
            }

            if (item.getItemId() == R.id.action_screenshot) {
                try {
                    if (exoPlayer != null) {
                        exoPlayer.setPlayWhenReady(false); // Pause the video
                    }
                    captureScreenshot();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Toast.makeText(VideoEditorActivity.this, "Screenshot taken!", Toast.LENGTH_SHORT).show();
                return true;
            }

            if(item.getItemId() ==  R.id.action_send) {
                if(mediaPath != null && originalVideo == null){ // If video wasn't trimmed and it came from chat, no need to upload to firebase
                    Intent intent = new Intent(VideoEditorActivity.this, ChatActivity.class);
                    intent.putExtra("videoUrl", mediaUrl);
                    intent.putExtra("chatroomId", getIntent().getStringExtra("chatroomId"));
                    intent.putExtra("otherUserId", getIntent().getStringExtra("otherUserId"));
                    intent.putExtra("otherUserUsername", getIntent().getStringExtra("otherUserUsername"));
                    intent.putExtra("myUsername", getIntent().getStringExtra("myUsername"));
                    intent.putExtra("fcmToken", getIntent().getStringExtra("fcmToken"));
                    startActivity(intent);
                    finish();
                }
                else if(galleryPath != null && originalVideo == null){ // If video wasn't trimmed and it came from chat, no need to upload to firebase
                    Intent intent = new Intent(VideoEditorActivity.this, SearchUsersActivity.class);
                    intent.putExtra("videoUrl", galleryUrl);
                    startActivity(intent);
                    finish();
                }
                else {
                    if (!saved[0]) {
                        // Upload video to firebase and then send it to another user

                        uploadVideoToFirebase(new OnVideoUploadedCallback() {
                            @Override
                            public void onSuccess(String videoUrl) {
                                if (mediaPath != null) {
                                    Intent intent = new Intent(VideoEditorActivity.this, ChatActivity.class);
                                    intent.putExtra("videoUrl", videoUrl);
                                    intent.putExtra("chatroomId", getIntent().getStringExtra("chatroomId"));
                                    intent.putExtra("otherUserId", getIntent().getStringExtra("otherUserId"));
                                    intent.putExtra("otherUserUsername", getIntent().getStringExtra("otherUserUsername"));
                                    intent.putExtra("myUsername", getIntent().getStringExtra("myUsername"));
                                    intent.putExtra("fcmToken", getIntent().getStringExtra("fcmToken"));
                                    if (alert.isShowing()) {
                                        alert.dismiss();
                                    }
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Intent intent = new Intent(VideoEditorActivity.this, SearchUsersActivity.class);
                                    intent.putExtra("videoUrl", videoUrl);
                                    intent.putExtra("myUsername", getIntent().getStringExtra("myUsername"));
                                    if (alert.isShowing()) {
                                        alert.dismiss();
                                    }
                                    startActivity(intent);
                                    finish();
                                }
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e("VideoEditorActivity", "Error uploading video", e);
                                if (alert.isShowing()) {
                                    alert.dismiss();
                                }
                                Toast.makeText(VideoEditorActivity.this, "Failed to upload video", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    else{
                        if (mediaPath != null) {
                            Intent intent = new Intent(VideoEditorActivity.this, ChatActivity.class);
                            intent.putExtra("videoUrl", firebaseUrl[0]);
                            intent.putExtra("chatroomId", getIntent().getStringExtra("chatroomId"));
                            intent.putExtra("otherUserId", getIntent().getStringExtra("otherUserId"));
                            intent.putExtra("otherUserUsername", getIntent().getStringExtra("otherUserUsername"));
                            intent.putExtra("myUsername", getIntent().getStringExtra("myUsername"));
                            intent.putExtra("fcmToken", getIntent().getStringExtra("fcmToken"));
                            startActivity(intent);
                            finish();
                        } else {
                            Intent intent = new Intent(VideoEditorActivity.this, SearchUsersActivity.class);
                            intent.putExtra("videoUrl", firebaseUrl[0]);
                            intent.putExtra("myUsername", getIntent().getStringExtra("myUsername"));
                            startActivity(intent);
                            finish();
                        }
                    }
                }
                return true;
            }
            return false;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) { // Dynamically handle the back button
            @Override
            public void handleOnBackPressed() {
                if(mediaPath != null){
                    Intent intent = new Intent(VideoEditorActivity.this, ChatActivity.class);
                    intent.putExtra("chatroomId", getIntent().getStringExtra("chatroomId"));
                    intent.putExtra("otherUserId", getIntent().getStringExtra("otherUserId"));
                    intent.putExtra("otherUserUsername", getIntent().getStringExtra("otherUserUsername"));
                    intent.putExtra("myUsername", getIntent().getStringExtra("myUsername"));
                    intent.putExtra("fcmToken", getIntent().getStringExtra("fcmToken"));
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else if(galleryPath != null){
                    Intent backIntent = new Intent(VideoEditorActivity.this, GalleryActivity.class);
                    startActivity(backIntent);
                    finish();
                } else{
                    Intent backIntent = new Intent(VideoEditorActivity.this, MainActivity.class);
                    startActivity(backIntent);
                    finish(); // Finish the current activity
                }
            }
        });

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (exoPlayer != null) {
            outState.putLong("video_position", exoPlayer.getCurrentPosition());
            outState.putBoolean("video_playing", exoPlayer.getPlayWhenReady());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        long position = savedInstanceState.getLong("video_position");
        boolean playWhenReady = savedInstanceState.getBoolean("video_playing");

        exoPlayer.seekTo(position);
        exoPlayer.setPlayWhenReady(playWhenReady);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release(); // Release the player when the activity is destroyed
        }
    }

    private void updateClipping() { // Update star and finish of media item based on the values in the range slider
        List<Float> positions = rangeSlider.getValues();
        int startPosition = positions.get(0).intValue();
        int endPosition = positions.get(1).intValue();
        boolean isEnabled = startPosition != 0 || endPosition != initialDurationInSeconds; // Users can only trim if the values don't have the same duration as the video
        if(!isEnabled) {
            save.setText("Save to Gallery");
            // Ensure the width is sufficient for the text
            ViewGroup.LayoutParams params = save.getLayoutParams();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT; // Adjust width to fit the content
            save.setLayoutParams(params);
        }
        else{
            save.setText("Trim");
            // Ensure the width is sufficient for the text
            ViewGroup.LayoutParams params = save.getLayoutParams();
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT; // Adjust width to fit the content
            save.setLayoutParams(params);
        }

        MediaItem.ClippingConfiguration clippingConfiguration = new MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startPosition * 1000L) // Convert to milliseconds
                .setEndPositionMs(endPosition * 1000L) // Convert to milliseconds
                .build();

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(videoUri)
                .setClippingConfiguration(clippingConfiguration)
                .build();

        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
    }

    public String getMimeType(String inputPath) { // Get mime type of the video used to get the extension
        String extension = MimeTypeMap.getFileExtensionFromUrl(inputPath);
        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return null; // Default if type cannot be determined
    }


    private String getFileExtensionFromMimeType(String mimeType) { // Get extension of the video to create new path after trimming
        if (mimeType == null) {
            return "";
        }
        switch (mimeType) {
            case "video/mp4":
                return ".mp4";
            case "video/x-msvideo":
            case "video/avi":
                return ".avi";
            case "video/mpeg":
                return ".mpeg";
            case "video/quicktime":
                return ".mov";
            // Add more MIME types and their corresponding extensions as needed
            default:
                return "";
        }
    }

    private void trimVideo(Uri videoUri, int startMs, int endMs) { // Trim the video based on the values of the range slider using FFMPeg
        if(startMs==0 && endMs == initialDurationInSeconds){
            Log.d("Video Editor", "Doesn't make sense to trim");
            return;
        }
        String inputPath = videoUri.getPath();
        String mimeType = getMimeType(inputPath);
        Log.d(TAG, "Myme: " + mimeType);
        String fileExtension = getFileExtensionFromMimeType(mimeType);
        outputPath = getExternalFilesDir(null) + "/trimmed_video" + System.currentTimeMillis() + fileExtension;

        Log.d("FFmpeg", "Input Path: " + inputPath);
        Log.d("FFmpeg", "Output Path: " + outputPath);
        Log.d("FFmpeg", "Start Time (ms): " + startMs);
        Log.d("FFmpeg", "End Time (ms): " + endMs);

        if (startMs >= endMs) {
            Log.e("FFmpeg", "Invalid trim times: start time is greater than or equal to end time.");
            return;
        }

        String[] cmd = {
                "-i", inputPath,
                "-ss", String.valueOf(startMs),
                "-to", String.valueOf(endMs),
                "-c", "copy",
                outputPath
        };

        FFmpeg.executeAsync(cmd, (executionId, returnCode) -> {
            if (returnCode == Config.RETURN_CODE_SUCCESS) {
                Log.d("FFmpeg", "Trimming successful!");
                Toast.makeText(this, "Trimmed video saved", Toast.LENGTH_LONG).show();
                //uploadVideoToFirebase(outputPath);
                if (mediaPath != null && inputPath.equals(mediaPath)) {
                    deleteOriginalFile(inputPath);
                }

                // Restart the activity with the new trimmed video
                Intent intent = new Intent(this, VideoEditorActivity.class); // After trimming restart the activity with the trimmed video
                intent.putExtra("video_path", outputPath);
                intent.putExtra("original_video", videoPath);
                finish(); // Finish the current activity to avoid overlapping
                startActivity(intent);
            } else {
                Log.e("FFmpeg", "Trimming failed!");
            }
        });
    }

    @SuppressLint("NewApi")
    @OptIn(markerClass = UnstableApi.class)
    private void captureScreenshot() throws IOException {
        if (exoPlayer != null) {
            long currentPosition = exoPlayer.getCurrentPosition(); // Get the current playback position in milliseconds
            captureFrameAtTime(currentPosition);
        }
    }

    private void captureFrameAtTime(long timeMs) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(videoUri.getPath());

            // Extract the frame at the specified time (in microseconds)
            Bitmap frameBitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);

            if (frameBitmap != null) {
                // Save the bitmap to a file
                File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File frameFile = new File(picturesDir, "frame_" + System.currentTimeMillis() + ".png");
                FileOutputStream fos = new FileOutputStream(frameFile);

                frameBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();

                exoPlayer.setPlayWhenReady(false);

                Intent intent = new Intent(VideoEditorActivity.this, ImageEditorActivity.class);
                intent.putExtra("image_path", frameFile.getAbsolutePath());
                intent.putExtra("video_path", videoUri.getPath());
                startActivity(intent);

                Log.d("FrameCapture", "Frame saved to: " + frameFile.getAbsolutePath());
            } else {
                Log.e("FrameCapture", "Failed to capture frame.");
            }
        } catch (IOException e) {
            Log.e("FrameCapture", "Error saving frame: " + e.getMessage());
        } finally {
            retriever.release();
        }
    }

    private void uploadVideoToFirebase(OnVideoUploadedCallback callback) { // uploads video to firebase on send button clicked
        AlertDialog.Builder builder = new AlertDialog.Builder(this); // Since it takes some time, there is the need to display an alert dialog
        builder.setCancelable(false); // Prevent dialog dismissal on touch outside or back press
        builder.setView(R.layout.dialog_progress);
        builder.setTitle("Uploading Video");

        alert = builder.create();
        alert.show();
        String filePath;

        filePath = videoUri.getPath();
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        String timestamp = String.valueOf(System.currentTimeMillis());
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("videos/" + userId + "/" + timestamp + ".mp4");

        storageRef.putFile(videoUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d("Firebase", "Upload successful! URL: " + uri.toString());
                    saveVideoMetadataToFirestore(uri.toString()); // associate video data with user
                    deleteInternalFile(filePath); // delete file if the file exists and is in internal storage
                    callback.onSuccess(uri.toString()); // pass the firebase storage url
                }))
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Upload failed: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    private void saveVideoMetadataToFirestore(String videoUrl) {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> videoData = new HashMap<>();
        videoData.put("url", videoUrl);
        videoData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("users").document(userId).collection("videos")
                .add(videoData)
                .addOnSuccessListener(documentReference -> Log.d("Firestore", "Video metadata saved successfully!"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error saving video metadata: " + e.getMessage()));
    }

    private void deleteInternalFile(String filePath) {
        File file = new File(filePath);
        File internalStorageDir = getFilesDir();

        if (file.exists() && file.getAbsolutePath().startsWith(internalStorageDir.getAbsolutePath())) {
            if (file.delete()) {
                Log.d("File", "File deleted successfully");
            } else {
                Log.e("File", "Failed to delete file");
            }
        } else {
            Log.e("File", "File is not in internal storage or does not exist");
        }
    }

    public interface OnVideoUploadedCallback {
        void onSuccess(String videoUrl);
        void onFailure(Exception e);
    }

    private void deleteOriginalFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            if (file.delete()) {
                Log.d("VideoEditorActivity", "Original video deleted successfully.");
                Toast.makeText(this,"Original video deleted successfully.",Toast.LENGTH_SHORT).show();
            } else {
                Log.e("VideoEditorActivity", "Failed to delete the original video.");
            }
        } else {
            Log.e("VideoEditorActivity", "File not found: " + path);
        }
    }

    private void updateMediaCollection(String mediaUrl, String type) { // add new document (or update) with video url and userID to the media collection. This will be used in the gallery and chat
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        FirebaseFirestore.getInstance().collection("media")
                .whereEqualTo("url", mediaUrl)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Document with the same URL exists
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            String mediaId = document.getId();
                            List<String> userIds = (List<String>) document.get("userIds");

                            if (userIds != null && !userIds.contains(userId)) {
                                // Add userId to the userIds array if not already present
                                FirebaseFirestore.getInstance().collection("media").document(mediaId)
                                        .update("userIds", FieldValue.arrayUnion(userId))
                                        .addOnSuccessListener(aVoid -> Log.d("Firebase", "User ID added to existing media document"))
                                        .addOnFailureListener(e -> Log.e("Firebase", "Failed to update media document", e));
                            } else {
                                Toast.makeText(VideoEditorActivity.this, "Already in the gallery", Toast.LENGTH_SHORT).show();
                                Log.d("Firebase", "User ID already present in media document");
                            }
                        }
                    } else {
                        // No document with the same URL exists, create a new one
                        Map<String, Object> mediaData = new HashMap<>();
                        mediaData.put("url", mediaUrl);
                        mediaData.put("type", type);
                        mediaData.put("userIds", Arrays.asList(userId));
                        mediaData.put("chatIds", new ArrayList<>());

                        FirebaseFirestore.getInstance().collection("media").add(mediaData)
                                .addOnSuccessListener(documentReference -> Log.d("Firebase", "Media document added successfully"))
                                .addOnFailureListener(e -> Log.e("Firebase", "Failed to add media document", e));
                    }
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to fetch media document", e));
    }
}