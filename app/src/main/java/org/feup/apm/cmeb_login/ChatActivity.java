package org.feup.apm.cmeb_login;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.Timestamp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.feup.apm.cmeb_login.adapter.ChatRecyclerAdapter;
import org.feup.apm.cmeb_login.model.ChatMessageModel;
import org.feup.apm.cmeb_login.model.ChatroomModel;
import org.feup.apm.cmeb_login.model.UserModel;
import org.feup.apm.cmeb_login.util.AndroidUtil;
import org.feup.apm.cmeb_login.util.FirebaseUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class ChatActivity extends AppCompatActivity {

    EditText message_input;
    ImageButton message_send_button;
    TextView username_text;
    RecyclerView recyclerview;
    ChatroomModel chatroomModel;
    UserModel otherUser;
    public String chatroomId;

    ImageButton stopButton;


    ImageView imageView;
    ImageButton toImageSelection;

    public String otherUserId;

    ActivityResultLauncher<Intent> imagePickLauncher;

    private ImageButton micButton;
    public String otherUserUsername;


    public String fcmToken;

    private AlertDialog progressDialog;
    private AlertDialog progressDialog2;

    public String myUsername;

    UserModel currentUserModel;

    ChatRecyclerAdapter adapter;
    private static final int REQUEST_PERMISSION_CODE = 1001;

    private MediaRecorder mediaRecorder;


    private Uri audioUri;


    private StorageReference storageReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }


        storageReference = FirebaseStorage.getInstance().getReference();


        if (getIntent() == null ) {
            Log.e("ChatActivity", "Error: Intent or UserModel null.");
            finish(); // Closes the activity to avoid crashes. In testing, this never happened, so it should probably never be used. We just keep it as a small check
            return;
        }



       //Defining the elements
        message_input = findViewById(R.id.message_input);
        message_send_button = findViewById(R.id.message_send_button);
        username_text = findViewById(R.id.username_text);
        recyclerview = findViewById(R.id.recyclerview_chat);
        imageView = findViewById(R.id.profile_pic_image_view);
        toImageSelection = findViewById(R.id.button_to_img);
        micButton = findViewById(R.id.send_audio_button);
        stopButton = findViewById(R.id.stop_button);


        //AlertDialog: used in ProfileFragment aswell
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Loading")
                .setMessage("Please wait...")
                .setCancelable(false)
                .setView(new ProgressBar(this));

        progressDialog = builder.create();







        // like in ProfileFragment:
        imagePickLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            progressDialog.show();

                            uploadImageToFirebase(imageUri, new OnImageUploadedCallback() {
                               @Override
                                public void onSuccess(String imageUrl) {
                                    progressDialog.dismiss();

                               }

                               @Override
                               public void onFailure(Exception e) {
                                    progressDialog.dismiss();

                               }
                           });
                        }
                  }
               }
        );



        //Receiving intents from other activities
        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());

        if (otherUser == null) {
            finish(); //just a check, this never happens
            return;
        }

        chatroomId = getIntent().getStringExtra("chatroomId");


        if (chatroomId == null) {
            chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());
        }


        otherUserId = getIntent().getStringExtra("otherUserId");
        if (otherUserId == null) {
            otherUserId = otherUser.getUserId();
        }


        otherUserUsername = getIntent().getStringExtra("otherUserUsername");
        if (otherUserUsername == null) {
            otherUserUsername = otherUser.getUsername();
        }

        fcmToken = getIntent().getStringExtra("fcmToken");

        if (fcmToken == null) {
            fcmToken = otherUser.getFcmToken();
        }


        myUsername = getIntent().getStringExtra("myUsername");

        if (myUsername == null) {
            FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
                currentUserModel = task.getResult().toObject(UserModel.class);
                myUsername = currentUserModel.getUsername();
            });
        }


        //Starting recording
        micButton.setOnClickListener(v->{

            if (checkPermissions()) {
                startRecording();
            } else {
                requestPermissions();
            }


        });

        //Stopping recording
        stopButton.setOnClickListener(v->{
            stopRecording();
            uploadAudioToFirebase(); //generate the link
        });

        toImageSelection.setOnClickListener(v -> {
            ImagePicker.with(this)
                    .crop()
                    .compress(512)
                    .maxResultSize(411, 800)  //user can choose the size
                    .createIntent(intent -> {
                        imagePickLauncher.launch(intent);
                        return null;
                    });

        });




        FirebaseUtil.getOtherProfilePicStorageRef(otherUserId).getDownloadUrl() //show other user's profile pic
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Uri uri = t.getResult();
                        AndroidUtil.setProfilePic(this, uri, imageView);
                    } else {
                        Log.e("ChatActivity", "Error loading profile picture: " + t.getException());
                    }
                });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // When user presses the back button
                Intent intent = new Intent(ChatActivity.this, MainActivity.class);
                intent.putExtra("targetFragment", "ChatFragment"); // We need to give an identifier to the fragment
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        username_text.setText(otherUserUsername);

        getOrCreateChatroomModel(); //setting up the chatroom

        setupChatRecyclerView(); //displaying previous messages

        //receiving intents from other activities
        String imageUrl = getIntent().getStringExtra("imageUrl");
        String videoUrl = getIntent().getStringExtra("videoUrl");
        String audioDownloadUrl = getIntent().getStringExtra("audioDownloadUrl");
        if (imageUrl != null) {
            updateMediaCollection(imageUrl,"image");
            message_input.setText(imageUrl); // Exibe o URL no EditText
        }

        if (videoUrl != null) {
            updateMediaCollection(videoUrl,"video");
            message_input.setText(videoUrl); // Exibe o URL no EditText
        }

        if (audioDownloadUrl!= null){
            message_input.setText(audioDownloadUrl);
        }

        message_send_button.setOnClickListener(v -> {
            String message = message_input.getText().toString().trim();
            if (message.isEmpty()) {
                Log.d("ChatActivity", "Empty message");
                return;
            }
            Log.d("ChatActivity", "Message sent: " + message);
            sendMessageToUser(message);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.users_chat), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),systemBars.bottom);
           return insets;
       });
    }




    public void getOrCreateChatroomModel() {
        FirebaseUtil.getChatroomReference(chatroomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                chatroomModel = task.getResult().toObject(ChatroomModel.class);
                if (chatroomModel == null) {
                    Log.d("ChatActivity", "Creating new chatroom");
                    chatroomModel = new ChatroomModel(
                            chatroomId,
                            Arrays.asList(FirebaseUtil.currentUserId(), otherUser.getUserId()),
                            Timestamp.now(),
                            "");
                }
                FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
            } else {
                Log.e("ChatActivity", "Error getting chatroom: " + task.getException());
            }
        });
    }

    public void sendMessageToUser(String message) {
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage(message);
        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel); //identifying the chat

        ChatMessageModel chatMessageModel = new ChatMessageModel(message, FirebaseUtil.currentUserId(), Timestamp.now());
        FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("ChatActivity", "Message sent to Firestore.");
                message_input.setText(""); //message text gets empty after the message is sent
                if(AndroidUtil.isLink(message) && !AndroidUtil.containsMediaTerm(message).isEmpty()) {
                    String message_multimedia = myUsername + " sent you a" + AndroidUtil.grammar(AndroidUtil.containsMediaTerm(message)) + AndroidUtil.containsMediaTerm(message);
                    sendNotification(otherUser.getFcmToken(), myUsername, message_multimedia); //to send notifications
                }
                else{
                    sendNotification(otherUser.getFcmToken(), myUsername, message);
                }
            } else {
                Log.e("ChatActivity", "Error sending message: " + task.getException());
            }
        });
    }




    void setupChatRecyclerView() { //Setting up the display of previous messages: check SearchUsersActivity or ChatFragment

        Log.d("ChatActivity", "Configurando RecyclerView...");
        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class).build();

        adapter = new ChatRecyclerAdapter(options, getApplicationContext(),
                chatroomId,
                otherUserId,
                otherUserUsername,
                myUsername,
                fcmToken);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true); //so that the messages don't appear displayed in reverse order
        recyclerview.setLayoutManager(manager);
        recyclerview.setAdapter(adapter);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerview.smoothScrollToPosition(0); //smooth scrolling
            }
        });
    }







//IMPORTANT: you can only receive notifications if you're logged in: check the logout Button in HomeFragment -> the fcmToken is deleted. You also only receive notifications if the app is closed or running in the background

    public void sendNotification(String token, String title, String body) { //this code follows the (tiresome) Firebase Cloud Messaging API V1 documentation (check https://firebase.google.com/docs/cloud-messaging/migrate-v1?hl=en )
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String BASE_URL = "https://fcm.googleapis.com";
            String FCM_SEND_ENDPOINT = "/v1/projects/ultralink-70ed3/messages:send";

            try { //performing an HTTP request
                Log.d("Testing","[INFO] Building URL for FCM endpoint...");
                URL url = new URL(BASE_URL + FCM_SEND_ENDPOINT);
                Log.d("Testing", "[INFO] FCM URL: " + url);
                AssetManager assetManager = getAssets();
                InputStream inputStream = assetManager.open("service3.json"); // in assets
                GoogleCredentials googleCredentials = GoogleCredentials // check the documentation cited above
                        .fromStream(inputStream)
                        .createScoped(Arrays.asList("https://www.googleapis.com/auth/firebase.messaging"));

                googleCredentials.refresh();
                String accessToken = googleCredentials.getAccessToken().getTokenValue();

                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                // Headers of the connection
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
                httpURLConnection.setRequestProperty("Content-Type", "application/json; UTF-8");
                httpURLConnection.setDoOutput(true);

                Log.d("Testing","[INFO] Headers configured.");

                // Building the JSON Payload
                String jsonPayload = buildJsonPayload(token, title, body);
                Log.d("Testing", "[INFO] JSON Payload: " + jsonPayload);

                // Inserting the payload in the connection
                try (OutputStream os = httpURLConnection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    System.out.println("[INFO] Payload sent to server.");
                }

                // Getting the server's response
                int responseCode = httpURLConnection.getResponseCode();
                Log.d("Testing","[INFO] Server response code: " + responseCode);

                //Notification sent?
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        Log.d("Testing","[INFO] Notification sent successfully. Response: " + response);
                    }
                } else {
                    Log.e("Testing","[ERROR] Failed to send notification. Response code: " + responseCode); //error: common codes are 404, 200 (fcmToken null), 401 and 400. Check here the meanings: https://firebase.google.com/docs/reference/fcm/rest/v1/ErrorCode?hl=en
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(httpURLConnection.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder errorResponse = new StringBuilder();
                        String errorLine;
                        while ((errorLine = br.readLine()) != null) {
                            errorResponse.append(errorLine.trim());
                        }
                        Log.e("Testing","[ERROR] Server error response: " + errorResponse);
                    }
                }

            } catch (Exception e) {
                Log.e("Testing","[ERROR] Exception occurred: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private String buildJsonPayload(String token, String title, String body) {
        Log.d("Testing","[INFO] Building JSON payload...");
        return "{" +
                "\"message\":{" +
                "\"token\":\"" + token + "\"," +
                "\"notification\":{" +
                "\"body\":\"" + body + "\"," +
                "\"title\":\"" + title + "\"" +
                "}" +
                "}" +
                "}";
    }

    //needed to send audios and images
    private boolean checkPermissions() {
        boolean recordPermission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean readPermission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;

        Log.d("Permissions", "Record: " + recordPermission + ", Read: " + readPermission);
        return recordPermission && readPermission;
    }

    //for audios
    private void requestPermissions() {
        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.READ_MEDIA_AUDIO},
                    REQUEST_PERMISSION_CODE);
        }
    }

    private void startRecording() {
        //  Prepare metadata for the audio File
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "audio_" + System.currentTimeMillis() + ".mp3");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/");


        // Insert File into MediaStore and get the URI
        audioUri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);

        if (audioUri == null) {
            Toast.makeText(this, "Failed to create audio file.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Configure the MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(getContentResolver().openFileDescriptor(audioUri, "w").getFileDescriptor()); //Set output destination

            mediaRecorder.prepare();
            mediaRecorder.start();
            micButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.VISIBLE);

            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
            toImageSelection.setEnabled(false);



        } catch (IOException e) {
            Log.e("AudioRecorder", "Error preparing MediaRecorder", e);
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
                toImageSelection.setEnabled(true);

                stopButton.setVisibility(View.GONE);
                micButton.setVisibility(View.VISIBLE);

            } catch (RuntimeException e) {
                Log.e("AudioRecorder", "Error stopping MediaRecorder", e);
                toImageSelection.setEnabled(true);
                Toast.makeText(this, "Error stopping recording", Toast.LENGTH_SHORT).show();
            }
        }


    }



    private void uploadAudioToFirebase() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Loading")
                .setMessage("Please wait...")
                .setCancelable(false)
                .setView(new ProgressBar(this));

        progressDialog2 = builder.create();


        if (audioUri != null) {
            progressDialog2.show();
            StorageReference audioRef = storageReference.child("audio/" + System.currentTimeMillis() + ".mp3");

            audioRef.putFile(audioUri) //uploading
                    .addOnSuccessListener(taskSnapshot -> audioRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String audioDownloadUrl = uri.toString();
                        message_input.setText(audioDownloadUrl);

                        progressDialog2.dismiss();

                        Log.d("Firebase", "Audio URL: " + audioDownloadUrl);
                        Toast.makeText(this, "Audio uploaded successfully!", Toast.LENGTH_SHORT).show();

                    }))
                    .addOnFailureListener(e -> {
                        Log.e("Firebase", "Error uploading audio", e);

                        progressDialog2.dismiss();
                        Toast.makeText(this, "Failed to upload audio", Toast.LENGTH_SHORT).show();
                    });
        } else {

            Toast.makeText(this, "No audio file to upload", Toast.LENGTH_SHORT).show();
        }
    }



    private void uploadImageToFirebase(Uri selectedImageUri, OnImageUploadedCallback callback) { //similar to the function above

        if (selectedImageUri != null) {
            StorageReference imageRef = storageReference.child("images/" + System.currentTimeMillis() + ".jpg");

            imageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                String imageUrl = uri.toString();

                                message_input.setText(imageUrl);
                                Log.d("Firebase", "Image's URL " + imageUrl);

                                callback.onSuccess(imageUrl);

                            }).addOnFailureListener(callback::onFailure)
                    ).addOnFailureListener(callback::onFailure);
        } else {

            callback.onFailure(new Exception("No image selected"));
        }
    }


    public interface OnImageUploadedCallback {
        void onSuccess(String imageUrl);
        void onFailure(Exception e);
    }

    private void updateMediaCollection(String mediaUrl, String type) {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        FirebaseFirestore.getInstance().collection("media")
                .whereEqualTo("url", mediaUrl)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Document with the same URL exists
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            String mediaId = document.getId();
                            List<String> chatIds = (List<String>) document.get("chatIds");

                            if (chatIds != null && !chatIds.contains(chatroomId)) {
                                // Add userId to the userIds array if not already present
                                FirebaseFirestore.getInstance().collection("media").document(mediaId)
                                        .update("userIds", FieldValue.arrayUnion(chatroomId))
                                        .addOnSuccessListener(aVoid -> Log.d("Firebase", "Chat ID added to existing media document"))
                                        .addOnFailureListener(e -> Log.e("Firebase", "Failed to update media document", e));
                            } else {
                                Log.d("Firebase", "Chat ID already present in media document");
                            }
                        }
                    } else {
                        // No document with the same URL exists, create a new one
                        Map<String, Object> mediaData = new HashMap<>();
                        mediaData.put("url", mediaUrl);
                        mediaData.put("type", type);
                        mediaData.put("chatIds", Arrays.asList(chatroomId));
                        mediaData.put("userIds", new ArrayList<>());

                        FirebaseFirestore.getInstance().collection("media").add(mediaData)
                                .addOnSuccessListener(documentReference -> Log.d("Firebase", "Media document added successfully"))
                                .addOnFailureListener(e -> Log.e("Firebase", "Failed to add media document", e));
                    }
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to fetch media document", e));
    }



}
