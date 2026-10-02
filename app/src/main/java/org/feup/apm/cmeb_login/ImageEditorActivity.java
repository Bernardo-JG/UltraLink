package org.feup.apm.cmeb_login;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.ScaleGestureDetector;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.yalantis.ucrop.UCrop;

import org.feup.apm.cmeb_login.views.DrawingView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageEditorActivity extends AppCompatActivity {

    private ImageView imageView;
    private FrameLayout frameLayout, overlayContainer;
    private ImageButton text, shape, draw, home, trash;
    private LinearLayout editLayout;
    private View selectedView;
    private DrawingView drawingView;
    private boolean isDrawingEnabled = false;
    private boolean isEraserEnabled = false;
    private File file;
    FirebaseStorage storage;
    StorageReference storageRef;
    BottomNavigationView nav;
    AlertDialog alert;
    private EditText currentDraggableEditText;
    private ConstraintLayout rootLayout;
    private int originalY, originalX;
    private ExecutorService executorService;
    private final MutableLiveData<Integer> numberViewsLiveData = new MutableLiveData<>(0);
    private SeekBar sizeSeekBar;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_editor);
        nav = findViewById(R.id.bottom_navigation);
        nav.setItemTextAppearanceActiveBoldEnabled(false);
        nav.setItemActiveIndicatorEnabled(false);
        final boolean[] saved = {false};
        final String[] firebaseUrl = new String[1];


        FirebaseApp.initializeApp(this);
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Set the status bar color to match your toolbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.bg_color));
        }

        // Optional: Ensure the text/icons are dark for better visibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        AtomicInteger j = new AtomicInteger();

        FirebaseApp.initializeApp(this);
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        imageView = findViewById(R.id.imageView);
        frameLayout = findViewById(R.id.frameLayout);
        text = findViewById(R.id.btnText);
        shape = findViewById(R.id.btnShape);
        draw = findViewById(R.id.btnDraw);
        editLayout = findViewById(R.id.editToolbar);
        editLayout.setVisibility(View.GONE);
        home = findViewById(R.id.btnLogo);
        drawingView = findViewById(R.id.drawingView);
        overlayContainer = findViewById(R.id.overlayContainer);
        ImageButton eraserButton = findViewById(R.id.btnEraser);
        eraserButton.setVisibility(View.GONE);
        eraserButton.setBackground(null);
        eraserButton.setPadding(5, 5, 5, 5);
        sizeSeekBar = findViewById(R.id.sizeSeekBar);
        sizeSeekBar.setVisibility(View.GONE);
        drawingView.setDrawingEnabled(isDrawingEnabled);
        trash = findViewById(R.id.trashCan);
        trash.setVisibility(View.GONE);

        String imagePath = getIntent().getStringExtra("image_path");
        String mediaPath = getIntent().getStringExtra("media_path");
        String mediaUrl = getIntent().getStringExtra("media_url");
        String videoPath = getIntent().getStringExtra("video_path");
        String galleryPath = getIntent().getStringExtra("media_gallery_path");
        String galleryUrl = getIntent().getStringExtra("galleryUrl");

        if (mediaPath != null)
            imagePath = mediaPath;
        if (galleryPath != null)
            imagePath = galleryPath;
        if (imagePath != null) {
            Log.d("ImageEditorActivity", "There is an image at: " + imagePath);
            // Convert URI to file path if necessary
            if (imagePath.startsWith("file://")) {
                imagePath = imagePath.substring(7); // Remove 'file://'
            }
            file = new File(imagePath);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                imageView.setImageBitmap(bitmap);


                // Adjust ImageView size based on the bitmap dimensions
                ViewGroup.LayoutParams params = imageView.getLayoutParams();
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                imageView.setLayoutParams(params);

                // Adjust Frame Layout width based on the bitmap dimensions
                overlayContainer = findViewById(R.id.overlayContainer);
                ViewGroup.LayoutParams overlayParams = overlayContainer.getLayoutParams();
                overlayParams.width = bitmap.getWidth();
                overlayContainer.setLayoutParams(overlayParams);

                Log.d("ImageEditorActivity", "OverlayContainer width: " + overlayContainer.getWidth());
                Log.d("ImageEditorActivity", "OverlayContainer height: " + overlayContainer.getHeight());

            } else {
                Log.e("ImageEditorActivity", "File does not exist: " + imagePath);
            }
        } else {
            Log.e("ImageEditorActivity", "No image path provided!");
        }

        home.setOnClickListener(v -> {
            Intent intent = new Intent(ImageEditorActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        Button save = findViewById(R.id.btnSave);
        ViewGroup.LayoutParams params = save.getLayoutParams();
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT; // Adjust width to fit the content
        save.setLayoutParams(params);

        save.setOnClickListener(v -> { // Saves the trimmed video in the case where the set duration is not the same as the video duration
            String buttonText = save.getText().toString(); // Get the current text of the button

            if (buttonText.equals("Save to Gallery")) {
                if (galleryPath != null) {
                    Toast.makeText(ImageEditorActivity.this, "Already in the gallery", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mediaPath != null) { // If image came from chat, no need to upload to firebase
                    updateMediaCollection(mediaUrl, "image");
                    Toast.makeText(ImageEditorActivity.this, "Image saved in gallery", Toast.LENGTH_SHORT).show();
                } else {
                    Bitmap bitmap = getBitmapFromImageView(imageView);
                    File file = null;
                    try {
                        file = saveBitmapToFile(bitmap);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Uri imageUri = Uri.fromFile(new File(file.getAbsolutePath())); // File with then be deleted when image is sent
                    uploadImageToFirebase(imageUri, new ImageEditorActivity.OnImageUploadedCallback() {
                        @Override
                        public void onSuccess(String imageUrl) {
                            saved[0] = true;
                            firebaseUrl[0] = imageUrl;
                            Toast.makeText(ImageEditorActivity.this, "Image saved in gallery", Toast.LENGTH_SHORT).show();
                            updateMediaCollection(imageUrl, "image"); // Media collection is used to add the image to the user's gallery
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
                            Toast.makeText(ImageEditorActivity.this, "Failed to upload video", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                saveFrameLayoutAsImage(new SaveImageCallback() { // Saves changes made on the layout
                    @Override
                    public void onImageSaved(File file) { // Interface is required because the dimensions of the frame layout are adapted to the image width before saving
                        Uri imageUri = Uri.fromFile(file);
                        Log.d("ImageEditorActivity", "File path: " + file.getAbsolutePath());
                        Intent intent = new Intent(ImageEditorActivity.this, ImageEditorActivity.class);
                        intent.putExtra("image_path", imageUri.toString());
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onSaveFailed() {
                        Log.e("ImageEditorActivity", "Save frame layout failed");
                    }
                });
            }
        });


        BottomNavigationView bottomNavigationView = nav;

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.action_back) {
                if (videoPath != null) {
                    Intent backIntent = new Intent(ImageEditorActivity.this, VideoEditorActivity.class);
                    backIntent.putExtra("video_path", videoPath); // Pass the video path back
                    Log.d("ImageEditor", "Video path provided");
                    startActivity(backIntent);
                    finish(); // Finish the current activity
                    return true;
                } else if (mediaPath != null) {
                    Intent backIntent = new Intent(ImageEditorActivity.this, ChatActivity.class);
                    backIntent.putExtra("chatroomId", getIntent().getStringExtra("chatroomId"));
                    backIntent.putExtra("otherUserId", getIntent().getStringExtra("otherUserId"));
                    backIntent.putExtra("otherUserUsername", getIntent().getStringExtra("otherUserUsername"));
                    backIntent.putExtra("myUsername", getIntent().getStringExtra("myUsername"));
                    backIntent.putExtra("fcmToken", getIntent().getStringExtra("fcmToken"));
                    backIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(backIntent);
                    finish();
                    return true;
                } else if (galleryPath != null) {
                    Intent backIntent = new Intent(ImageEditorActivity.this, GalleryActivity.class);
                    startActivity(backIntent);
                    finish();
                    return true;
                } else {
                    Intent backIntent = new Intent(ImageEditorActivity.this, MainActivity.class);
                    startActivity(backIntent);
                    finish(); // Finish the current activity
                    return true;
                }
            }

            if (item.getItemId() == R.id.action_edit) {
                // Toggle visibility of edit layout
                j.getAndIncrement();
                if (j.get() % 2 != 0) {
                    editLayout.setVisibility(View.VISIBLE);
                } else {
                    editLayout.setVisibility(View.GONE);
                }
                return true;
            }

            if (item.getItemId() == R.id.action_send) {
                //File file = saveFrameLayoutAsImage();
                if (save.getText().toString().equals("Save Changes")) {
                    Toast.makeText(this, "Save changes first!", Toast.LENGTH_LONG).show(); // Can't send if changes aren't saved
                    return true;
                }
                Bitmap bitmap = getBitmapFromImageView(imageView);
                File file = null;
                try {
                    file = saveBitmapToFile(bitmap);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Uri imageUri = Uri.fromFile(new File(file.getAbsolutePath()));
                if (mediaPath != null) {
                    deleteInternalFile(file.getAbsolutePath());
                    Intent intent = new Intent(ImageEditorActivity.this, ChatActivity.class);
                    intent.putExtra("imageUrl", mediaUrl);
                    intent.putExtra("chatroomId", getIntent().getStringExtra("chatroomId"));
                    intent.putExtra("otherUserId", getIntent().getStringExtra("otherUserId"));
                    intent.putExtra("otherUserUsername", getIntent().getStringExtra("otherUserUsername"));
                    intent.putExtra("myUsername", getIntent().getStringExtra("myUsername"));
                    intent.putExtra("fcmToken", getIntent().getStringExtra("fcmToken"));
                    startActivity(intent);

                } else if (galleryPath != null) {
                    deleteInternalFile(file.getAbsolutePath());
                    Intent intent = new Intent(ImageEditorActivity.this, SearchUsersActivity.class);
                    intent.putExtra("imageUrl", galleryUrl);
                    startActivity(intent);
                    finish();

                } else {
                    uploadImageToFirebase(imageUri, new OnImageUploadedCallback() { // Upload deletes the file
                        @Override
                        public void onSuccess(String imageUrl) {
                            if (mediaPath != null) {
                                Intent intent = new Intent(ImageEditorActivity.this, ChatActivity.class);
                                intent.putExtra("imageUrl", imageUrl);
                                intent.putExtra("chatroomId", getIntent().getStringExtra("chatroomId"));
                                intent.putExtra("otherUserId", getIntent().getStringExtra("otherUserId"));
                                intent.putExtra("otherUserUsername", getIntent().getStringExtra("otherUserUsername"));
                                intent.putExtra("myUsername", getIntent().getStringExtra("myUsername"));
                                intent.putExtra("fcmToken", getIntent().getStringExtra("fcmToken"));
                                if (alert.isShowing()) {
                                    alert.dismiss();
                                }
                                startActivity(intent);
                            } else {
                                Intent intent = new Intent(ImageEditorActivity.this, SearchUsersActivity.class);
                                intent.putExtra("imageUrl", imageUrl);
                                intent.putExtra("myUsername", getIntent().getStringExtra("myUsername"));
                                if (alert.isShowing()) {
                                    alert.dismiss();
                                }
                                startActivity(intent);
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e("ImageEditorActivity", "Error uploading image", e);
                            if (alert.isShowing()) {
                                alert.dismiss();
                            }
                            Toast.makeText(ImageEditorActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                return true;
            }
            return false;
        });

        rootLayout = findViewById(R.id.main);

// This detects if the keyboard is obscuring the added text. If it is, moves the text to middle of the screen so that the user can write and then moves it back once the keyboard is hidden
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootLayout.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootLayout.getRootView().getHeight();
                int screenWidth = rootLayout.getRootView().getWidth();
                int keypadHeight = screenHeight - r.bottom;

                Log.d("Image Editor", "Screen Height: " + screenHeight + ", Keypad Height: " + keypadHeight + ", Screen Width: " + screenWidth);

                // Check if the keyboard is shown
                if (keypadHeight > screenHeight * 0.15) {
                    if (currentDraggableEditText != null && currentDraggableEditText.hasFocus()) {
                        int[] location = new int[2];
                        currentDraggableEditText.getLocationOnScreen(location);
                        int editTextBottom = location[1] + currentDraggableEditText.getHeight();

                        // Move the EditText above the keyboard if it is obscured
                        if (editTextBottom > screenHeight - keypadHeight) {
                            int newY = screenHeight - keypadHeight - currentDraggableEditText.getHeight() - 20; // 20 is padding
                            currentDraggableEditText.setY(newY);
                            currentDraggableEditText.setX((screenWidth - currentDraggableEditText.getWidth()) / 2);
                        }
                    }
                } else {
                    // Keyboard is hidden
                    Log.d("Image Editor", "Keyboard is not shown. Resetting to original position.");
                    if (currentDraggableEditText != null) {
                        // Reset EditText to its original position
                        currentDraggableEditText.setY(originalY);
                        currentDraggableEditText.setX(originalX);
                    }
                }
            }
        });


        text.setOnClickListener(v -> {
            // Increment the number of views counter
            numberViewsLiveData.setValue(numberViewsLiveData.getValue() + 1);

            // Add a draggable TextView to the layout
            addDraggableTextView("Text");

            // Clear focus from any EditText and hide the keyboard
            View focusedView = getCurrentFocus();
            if (focusedView instanceof EditText) {
                focusedView.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
            }
        });

        shape.setOnClickListener(v -> {
            // Increment the number of views counter
            numberViewsLiveData.setValue(numberViewsLiveData.getValue() + 1);

            // Add a draggable shape to the layout
            addDraggableShape();
        });

        draw = findViewById(R.id.btnDraw);
        draw.setOnClickListener(v -> {
            // Toggle the drawing mode
            isDrawingEnabled = !isDrawingEnabled;

            // Update the button's background to indicate the state
            draw.setBackground(isDrawingEnabled ? new ColorDrawable(Color.GRAY) : null);

            // Enable or disable drawing mode and make the drawing tools visible
            drawingView.setDrawingEnabled(isDrawingEnabled);
            drawingView.setVisibility(View.VISIBLE);
            eraserButton.setVisibility(View.VISIBLE);
        });

        eraserButton.setOnClickListener(v -> {
            // Toggle eraser mode and disable drawing mode
            isEraserEnabled = !isEraserEnabled;
            isDrawingEnabled = false;

            // Update the drawing view modes
            drawingView.setDrawingEnabled(false);
            drawingView.setEraserMode(isEraserEnabled);

            // Update button backgrounds to reflect the states
            draw.setBackground(isDrawingEnabled ? new ColorDrawable(Color.GRAY) : null);
            eraserButton.setBackground(isEraserEnabled ? new ColorDrawable(Color.GRAY) : null);
        });

        drawingView.setOnDrawingListener(() -> {
            // Check if the drawing view is empty or has content
            if (!isDrawingViewEmpty()) {
                Log.d("ImageEditorActivity", "DrawingView is not empty.");
                numberViewsLiveData.setValue(numberViewsLiveData.getValue() + 1);
            } else {
                Log.d("ImageEditorActivity", "DrawingView is empty.");
                numberViewsLiveData.setValue(numberViewsLiveData.getValue() - 1);
            }
        });

        ImageButton cropButton = findViewById(R.id.btnCrop);
        cropButton.setOnClickListener(v -> {
            // Prevent cropping if unsaved changes exist
            if (save.getText().toString().equals("Save Changes")) {
                Toast.makeText(this, "Save changes first!", Toast.LENGTH_LONG).show();
                return;
            }
            // Increment the number of views and initiate cropping
            numberViewsLiveData.setValue(numberViewsLiveData.getValue() + 1);
            crop(Uri.fromFile(file));
        });

// Override the back button to handle navigation based on the current context
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mediaPath != null) {
                    // Navigate back to the ChatActivity with relevant data
                    Intent intent = new Intent(ImageEditorActivity.this, ChatActivity.class);
                    intent.putExtra("chatroomId", getIntent().getStringExtra("chatroomId"));
                    intent.putExtra("otherUserId", getIntent().getStringExtra("otherUserId"));
                    intent.putExtra("otherUserUsername", getIntent().getStringExtra("otherUserUsername"));
                    intent.putExtra("myUsername", getIntent().getStringExtra("myUsername"));
                    intent.putExtra("fcmToken", getIntent().getStringExtra("fcmToken"));
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else if (getIntent().getStringExtra("video_path") != null) {
                    // Navigate back to the VideoEditorActivity with the video path
                    Intent intent = new Intent(ImageEditorActivity.this, VideoEditorActivity.class);
                    intent.putExtra("video_path", getIntent().getStringExtra("video_path"));
                    startActivity(intent);
                    Log.d("ImageEditorActivity", "Back button pressed");
                    finish();
                } else {
                    // Navigate back to the MainActivity
                    Intent intent = new Intent(ImageEditorActivity.this, MainActivity.class);
                    startActivity(intent);
                    Log.d("ImageEditorActivity", "Back button pressed");
                    finish();
                }
            }
        });

        frameLayout.setOnClickListener(v -> {
            // Clear focus from any EditText and hide the keyboard
            View focusedView = getCurrentFocus();
            if (focusedView instanceof EditText) {
                focusedView.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
            }
        });

        numberViewsLiveData.observe(this, numberOfViews -> {
            Log.d("ViewCounter", "Number of views has changed: " + numberOfViews);

            // Update the UI based on the number of views
            if (numberOfViews > 0) {
                save.setText("Save Changes");
                ViewGroup.LayoutParams params1 = save.getLayoutParams();
                params1.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                save.setLayoutParams(params1);
            } else {
                eraserButton.setVisibility(View.GONE);
                save.setText("Save to Gallery");
                ViewGroup.LayoutParams params1 = save.getLayoutParams();
                params1.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                save.setLayoutParams(params1);
            }
        });

        trash.setOnClickListener(v -> {
            if (selectedView != null) {
                // Remove the selected view and decrement the view counter
                overlayContainer.removeView(selectedView);
                numberViewsLiveData.setValue(numberViewsLiveData.getValue() - 1);

                // Reset selection and UI states
                selectedView = null;
                trash.setVisibility(View.GONE);
                sizeSeekBar.setVisibility(View.GONE);

                Log.d("ImageEditorActivity", "Selected view deleted.");
            } else {
                Log.d("ImageEditorActivity", "No view selected to delete.");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Resize the overlayContainer to match the ImageView dimensions
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams overlayParams = overlayContainer.getLayoutParams();
                overlayParams.width = imageView.getWidth();
                overlayParams.height = imageView.getHeight();
                overlayContainer.setLayoutParams(overlayParams);

                // Log the dimensions for debugging
                Log.d("ImageEditorActivity", "OverlayContainer width: " + overlayContainer.getWidth());
                Log.d("ImageEditorActivity", "OverlayContainer height: " + overlayContainer.getHeight());
            }
        }, 100); // Delay to ensure the ImageView dimensions are set
    }

    public boolean isDrawingViewEmpty() {
        // Get the bitmap from the DrawingView
        Bitmap bitmap_draw = drawingView.getBitmap();

        // Return true if the bitmap is null
        if (bitmap_draw == null) {
            return true;
        }

        // Check each pixel in the bitmap for transparency
        for (int x = 0; x < bitmap_draw.getWidth(); x++) {
            for (int y = 0; y < bitmap_draw.getHeight(); y++) {
                if (bitmap_draw.getPixel(x, y) != Color.TRANSPARENT) {
                    return false; // Return false if a non-transparent pixel is found
                }
            }
        }
        return true; // Return true if all pixels are transparent
    }

    private void addDraggableTextView(String text) {
        // Create a new EditText view for the draggable text
        EditText editView = new EditText(this);
        editView.setText(text);
        editView.setTextSize(20);
        editView.setTextColor(Color.WHITE);
        editView.setBackground(null); // Remove default background
        editView.setBackgroundColor(Color.TRANSPARENT); // Set transparent background
        editView.setPadding(20, 20, 50, 20);
        editView.setFocusable(true);
        editView.setFocusableInTouchMode(true);
        editView.setCursorVisible(false); // Initially hide the cursor
        currentDraggableEditText = editView;

        // Handle cursor visibility based on focus changes
        editView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                editView.setCursorVisible(true); // Show cursor when focused
                currentDraggableEditText = editView;
                originalY = (int) editView.getY();
                originalX = (int) editView.getX();
            } else {
                editView.setCursorVisible(false); // Hide cursor when not focused
                currentDraggableEditText = null;
            }
        });

        // Set layout parameters to center the EditText in the container
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        editView.setLayoutParams(params);

        // Add the EditText to the container and set up interactions
        overlayContainer.addView(editView);
        setupSeekBar();
        editView.requestFocus();
        makeViewDraggable(editView);
        makeViewSelectable(editView);
    }

    private void addDraggableShape() {
        // Create a new View for the draggable shape
        View shapeView = new View(this);
        shapeView.setBackgroundResource(R.drawable.custom_oval_shape); // Set custom oval shape
        shapeView.setPadding(20, 20, 20, 20);

        // Set layout parameters for the shape
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(200, 200);
        params.gravity = Gravity.CENTER;
        shapeView.setLayoutParams(params);

        // Add the shape to the container and set up interactions
        overlayContainer.addView(shapeView);
        setupSeekBar();
        makeViewDraggable(shapeView);
        makeViewSelectable(shapeView);
    }

    private void makeViewDraggable(View view) {
        // Set up a gesture detector for scaling the view
        final ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(@NonNull ScaleGestureDetector detector) {
                        // Scale the view based on the gesture
                        float scaleFactor = detector.getScaleFactor();
                        view.setScaleX(view.getScaleX() * scaleFactor);
                        view.setScaleY(view.getScaleY() * scaleFactor);
                        return true;
                    }
                });

        // Set up a touch listener for dragging and interactions
        view.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY; // Offsets for dragging
            private float currentX, currentY; // Current coordinates
            private int lastAction;
            private boolean write = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        // Record the initial touch position
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        currentX = v.getX();
                        currentY = v.getY();
                        lastAction = MotionEvent.ACTION_DOWN;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // Move the view to the new touch position
                        if (!scaleGestureDetector.isInProgress()) {
                            v.setX(event.getRawX() + dX);
                            v.setY(event.getRawY() + dY);

                            // Detect minimal movement to determine if it's a click
                            if (lastAction == MotionEvent.ACTION_MOVE &&
                                    (Math.abs(currentX - v.getX()) < 0.1 && Math.abs(currentY - v.getY()) < 0.1)) {
                                write = true;
                            } else {
                                write = false;
                            }
                            lastAction = MotionEvent.ACTION_MOVE;
                            currentX = v.getX();
                            currentY = v.getY();
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        // Handle click behavior for EditText views
                        if (lastAction == MotionEvent.ACTION_DOWN || write) {
                            Log.d("EditText", "Performing Click");
                            v.performClick();
                            if (v instanceof EditText) {
                                EditText editText = (EditText) v;
                                editText.requestFocus();

                                // Show the keyboard and set the cursor position
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                                int offset = editText.getOffsetForPosition(event.getX(), event.getY());
                                editText.setSelection(offset);
                            }
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    private void makeViewSelectable(View view) {
        view.setOnClickListener(v -> {
            selectedView = v;

            // Show the SeekBar when a view is selected
            SeekBar sizeSeekBar = findViewById(R.id.sizeSeekBar);
            sizeSeekBar.setVisibility(View.VISIBLE);
            trash.setVisibility(View.VISIBLE);
        });

        // Handle deselection (click outside a view)
        frameLayout.setOnClickListener(v -> {
            if (selectedView != null) {
                selectedView = null;
                sizeSeekBar.setVisibility(View.GONE);
                trash.setVisibility(View.GONE);
            }
        });
    }

    private void setupSeekBar() {
        sizeSeekBar.setVisibility(View.GONE);
        sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { // Seek bar controls the size of the selected view
                if (selectedView != null) {
                    float scale = progress / 100.0f;
                    selectedView.setScaleX(scale);
                    selectedView.setScaleY(scale);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void saveFrameLayoutAsImage(SaveImageCallback callback) {
        // Handler to delay the execution
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Confirm layout changes
                Log.d("ImageEditorActivity", "OverlayContainer width after layout: " + overlayContainer.getWidth());
                Log.d("ImageEditorActivity", "OverlayContainer height after layout: " + overlayContainer.getHeight());

                // Saving the FrameLayout as an image
                if (overlayContainer.getWidth() == 0 || overlayContainer.getHeight() == 0) {
                    Log.e("ImageEditorActivity", "FrameLayout has zero dimensions.");
                    callback.onSaveFailed();
                    return;
                }

                Bitmap bitmap = Bitmap.createBitmap(overlayContainer.getWidth(), overlayContainer.getHeight(), Bitmap.Config.ARGB_8888);

                if (bitmap == null) {
                    Log.e("ImageEditorActivity", "Failed to create bitmap.");
                    callback.onSaveFailed();
                    return;
                }

                // Create a canvas to draw the FrameLayout onto the bitmap
                Canvas canvas = new Canvas(bitmap);
                overlayContainer.draw(canvas);

                // Save the bitmap to a file
                try {
                    File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    if (picturesDir == null) {
                        Log.e("ImageEditorActivity", "Unable to access external files directory.");
                        callback.onSaveFailed();
                        return;
                    }
                    File file = new File(picturesDir, "Screenshot_w_overlays_" + System.currentTimeMillis() + ".png");
                    FileOutputStream fos = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    fos.close();
                    Log.d("ImageEditorActivity", "Image saved to: " + file.getAbsolutePath());
                    callback.onImageSaved(file);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("ImageEditorActivity", "Failed to save image: " + e.getMessage());
                    callback.onSaveFailed();
                }
            }
        }, 100); // Delay of 100 milliseconds
    }


    public interface SaveImageCallback {
        void onImageSaved(File file);
        void onSaveFailed();
    }

    private void crop(Uri sourceUri){
        if(sourceUri != null){
        File outputDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File outputFile = new File(outputDir, "CroppedImage_" + System.currentTimeMillis() + ".png");
        Uri destinationUri = Uri.fromFile(outputFile);

        // Start UCrop
        UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1) // Set aspect ratio, 1:1 for a square crop.
                .withMaxResultSize(1080, 1080)
                .start(ImageEditorActivity.this);
    } else {
        Toast.makeText(ImageEditorActivity.this, "No image to crop!", Toast.LENGTH_SHORT).show();
    }}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if the result is from UCrop
        if (requestCode == UCrop.REQUEST_CROP) {
            if (resultCode == RESULT_OK && data != null) {
                // Get the cropped image URI
                Uri croppedImageUri = UCrop.getOutput(data);

                if (croppedImageUri != null) {
                    Intent intent = new Intent(this, ImageEditorActivity.class);
                    intent.putExtra("image_path", croppedImageUri.toString());
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Cropped image URI is null!", Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == UCrop.RESULT_ERROR) {
                // Handle cropping error
                Throwable cropError = UCrop.getError(data);
                if (cropError != null) {
                    Log.e("ImageEditorActivity", "Crop error: ", cropError);
                    Toast.makeText(this, "Crop failed: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    private void uploadImageToFirebase(Uri imageUri, OnImageUploadedCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false); // Prevent dialog dismissal on touch outside or back press
        builder.setView(R.layout.dialog_progress);
        builder.setTitle("Uploading Image");

        alert = builder.create();
        alert.show();

        if (imageUri != null) {
            String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            String timestamp = String.valueOf(System.currentTimeMillis());
            StorageReference imageRef = storageRef.child("images/" + userId + "/" + timestamp + ".jpg");

            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        Log.d("Firebase", "Image URL: " + imageUrl);
                        deleteInternalFile(imageUri.getPath()); // delete internal file (if exists) before starting next activity
                        callback.onSuccess(imageUrl); // Pass the URL via callback
                    }).addOnFailureListener(callback::onFailure))
                    .addOnFailureListener(callback::onFailure);
        } else {
            callback.onFailure(new Exception("No image selected"));
        }
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

    public interface OnImageUploadedCallback {
        void onSuccess(String imageUrl);
        void onFailure(Exception e);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown the ExecutorService to avoid memory leaks (could be implemented in the future)
    }

    // Method that updates the media collection if firestore and adds the media to the user's gallery (if not already there)
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
                            List<String> userIds = (List<String>) document.get("userIds");

                            if (userIds != null && !userIds.contains(userId)) {
                                // Add userId to the userIds array if not already present
                                FirebaseFirestore.getInstance().collection("media").document(mediaId)
                                        .update("userIds", FieldValue.arrayUnion(userId))
                                        .addOnSuccessListener(aVoid -> Log.d("Firebase", "User ID added to existing media document"))
                                        .addOnFailureListener(e -> Log.e("Firebase", "Failed to update media document", e));
                            } else {
                                Toast.makeText(ImageEditorActivity.this, "Already in the gallery", Toast.LENGTH_SHORT).show();
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

    private Bitmap getBitmapFromImageView(ImageView imageView) {
        imageView.setDrawingCacheEnabled(true);
        imageView.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(imageView.getDrawingCache());
        imageView.setDrawingCacheEnabled(false);
        return bitmap;
    }

    private File saveBitmapToFile(Bitmap bitmap) throws IOException {
        File file = new File(getCacheDir(), "image.png");
        FileOutputStream fos = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.close();
        return file;
    }
}

