package org.feup.apm.cmeb_login.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;

import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.feup.apm.cmeb_login.ImageEditorActivity;
import org.feup.apm.cmeb_login.R;
import org.feup.apm.cmeb_login.VideoEditorActivity;
import org.feup.apm.cmeb_login.util.FirebaseUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    private List<String> urls;
    private Activity activity;
    private Context context;
    private static Map<String, String> downloadedFiles = new HashMap<>();

    public GalleryAdapter(Activity activity, Context context, List<String> urls) {
        this.activity = activity;
        this.context = context;
        this.urls = urls;
        Log.d("Gallery Adapter", "Gallery adapter being used");
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_gallery, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String url = urls.get(position);
        // Use Glide or Picasso to load the image
        loadImageOrVideo(holder.imageView, url);

        if (url.contains(".mp4") || url.contains(".mov") || url.contains(".avi")) {
            Log.d("Gallery", "Should have play");
            holder.play.setVisibility(View.VISIBLE);
        } else {
            Log.d("Gallery", "Should not have play");
            holder.play.setVisibility(View.GONE);
        }

        holder.deleteButton.setOnClickListener(v -> {
            // Implement delete functionality
            showDeleteConfirmationDialog(url, position);
        });
    }

    @Override
    public int getItemCount() {
        return urls.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView, play;
        ImageButton deleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            play = itemView.findViewById(R.id.playButton);
        }
    }

    private void loadImageOrVideo(ImageView imageView, String url) {
        if (downloadedFiles.containsKey(url)) {
            String localPath = downloadedFiles.get(url);
            Log.d("GalleryAdapter", "Using cached file: " + localPath);

            // Load from local path
            loadMediaFromFile(imageView, new File(localPath), url);
            File localFile = new File(localPath);
            // Set the OnClickListener for imageView
            imageView.setOnClickListener(v -> {
                String localUrl = localFile.getAbsolutePath();
                Log.d("GalleryAdapter", "Local URL: " + localUrl);

                // Intent to either ImageEditorActivity or VideoEditorActivity
                Intent intent = new Intent(context, localUrl.endsWith(".mp4") || localUrl.endsWith(".mov") || localUrl.contains(".avi")
                        ? VideoEditorActivity.class : ImageEditorActivity.class);

                intent.putExtra("media_gallery_path", localFile.getAbsolutePath());
                intent.putExtra("galleryUrl", url);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Add this flag for context startActivity

                context.startActivity(intent);
            });
            return;
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference mediaRef = storage.getReferenceFromUrl(url);
        String cleanUrl = url.split("\\?")[0];
        Log.d("Chat Recycler", "Clean url: " + cleanUrl);

        try {
            File localFile = File.createTempFile("media", getFileExtension(cleanUrl));

            mediaRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                Log.d("Recycler", "File downloaded to: " + localFile.getAbsolutePath());

                // Cache the downloaded file
                downloadedFiles.put(url, localFile.getAbsolutePath());

                // Load the media
                loadMediaFromFile(imageView, localFile, url);

                // Set the OnClickListener for imageView
                imageView.setOnClickListener(v -> {
                    String localUrl = localFile.getAbsolutePath();
                    Log.d("GalleryAdapter", "Local URL: " + localUrl);

                    // Intent to either ImageEditorActivity or VideoEditorActivity
                    Intent intent = new Intent(context, localUrl.endsWith(".mp4") || localUrl.endsWith(".mov") || localUrl.contains(".avi")
                            ? VideoEditorActivity.class : ImageEditorActivity.class);

                    intent.putExtra("media_gallery_path", localFile.getAbsolutePath());
                    intent.putExtra("galleryUrl", url);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Add this flag for context startActivity

                    context.startActivity(intent);
                });
            });
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileExtension(String url) {
        if (url.contains(".")) {
            return url.substring(url.lastIndexOf("."));
        } else {
            return "";
        }
    }

    private void showDeleteConfirmationDialog(String url, int position) {
        new AlertDialog.Builder(activity)
                .setTitle("Delete Confirmation")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("OK", (dialog, which) -> deleteImage(url, FirebaseUtil.currentUserId(),position))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void loadMediaFromFile(ImageView imageView, File localFile, String url) {
        if (url.contains(".mp4") || url.contains(".mov") || url.contains(".avi")) {
            // Load video thumbnail
            Glide.with(context)
                    .load(localFile)
                    .placeholder(R.drawable.ic_upload)
                    .error(R.drawable.ic_trash_can)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .thumbnail(0.1f)
                    .into(imageView);
        } else {
            // Load image
            Glide.with(context)
                    .load(localFile)
                    .placeholder(R.drawable.ic_upload)
                    .error(R.drawable.ic_trash_can)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageView);
        }
    }

    private void deleteImage(String mediaUrl, String userId, int position) {
        FirebaseFirestore.getInstance().collection("media")
                .whereEqualTo("url", mediaUrl)
                .whereArrayContains("userIds", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String mediaId = document.getId();
                        FirebaseFirestore.getInstance().collection("media").document(mediaId)
                                .update("userIds", FieldValue.arrayRemove(userId))
                                .addOnSuccessListener(aVoid -> {
                                    urls.remove(position);
                                    notifyItemRemoved(position);
                                    notifyItemRangeChanged(position, urls.size());
                                    Log.d("Gallery", "User ID removed from media document");

                                    // Check if the userIds and chatIds arrays are empty
                                    FirebaseFirestore.getInstance().collection("media").document(mediaId).get()
                                            .addOnSuccessListener(doc -> {
                                                List<String> userIds = (List<String>) doc.get("userIds");
                                                List<String> chatIds = (List<String>) doc.get("chatIds");
                                                if ((userIds == null || userIds.isEmpty()) && (chatIds == null || chatIds.isEmpty())) {
                                                    // Delete from Firebase Storage
                                                    StorageReference mediaRef = FirebaseStorage.getInstance().getReferenceFromUrl(mediaUrl);
                                                    mediaRef.delete().addOnSuccessListener(aVoid1 -> {
                                                        // Delete document from Firestore
                                                        FirebaseFirestore.getInstance().collection("media").document(mediaId).delete()
                                                                .addOnSuccessListener(aVoid2 -> Log.d("Gallery", "Media deleted successfully"))
                                                                .addOnFailureListener(e -> Log.e("Gallery", "Failed to delete media document", e));
                                                    }).addOnFailureListener(e -> Log.e("Gallery", "Failed to delete media from storage", e));
                                                }
                                            })
                                            .addOnFailureListener(e -> Log.e("Gallery", "Failed to fetch media document", e));
                                })
                                .addOnFailureListener(e -> Log.e("Gallery", "Failed to update media document", e));
                    }
                })
                .addOnFailureListener(e -> Log.e("Gallery", "Failed to fetch media document", e));
    }
}
