package org.feup.apm.cmeb_login.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.feup.apm.cmeb_login.util.FirebaseUtil;
import org.feup.apm.cmeb_login.ImageEditorActivity;
import org.feup.apm.cmeb_login.R;
import org.feup.apm.cmeb_login.VideoEditorActivity;
import org.feup.apm.cmeb_login.model.ChatMessageModel;

import java.io.File;
import java.io.IOException;

public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel,ChatRecyclerAdapter.ChatModelViewHolder> {



    Context context;
    private String chatroomId;
    private String otherUserId;
    private String otherUserUsername;
    private String myUsername;
    private String fcmToken;

    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options, Context context, String chatroomId, String otherUserId, String otherUserUsername, String myUsername, String fcmToken)
    {
        super(options);
        this.context = context;
        this.chatroomId = chatroomId;
        this.otherUserId = otherUserId;
        this.otherUserUsername = otherUserUsername;
        this.myUsername = myUsername;
        this.fcmToken = fcmToken;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatModelViewHolder holder, int position, @NonNull ChatMessageModel model) {
        // Reset visibility and content of all views to default
        holder.leftChatLayout.setVisibility(View.GONE);
        holder.rightChatLayout.setVisibility(View.GONE);
        holder.leftChatImage.setVisibility(View.GONE);
        holder.rightChatImage.setVisibility(View.GONE);
        holder.leftChatTextview.setText(null);
        holder.rightChatTextview.setText(null);

        if (model.getSenderId().equals(FirebaseUtil.currentUserId())) {
            // Right (self) message
            holder.rightChatLayout.setVisibility(View.VISIBLE);

            if (isFirebaseStorageUrl(model.getMessage())) {
                holder.rightChatImage.setVisibility(View.VISIBLE);
                loadImageOrVideo(holder.rightChatImage, holder.rightPlay, model.getMessage());
            } else {
                holder.rightChatImage.setVisibility(View.GONE);
                holder.rightChatTextview.setText(model.getMessage());
                holder.rightChatTextview.setMovementMethod(LinkMovementMethod.getInstance());
                Linkify.addLinks(holder.rightChatTextview, Linkify.ALL);
            }
        } else {
            // Left (other user) message
            holder.leftChatLayout.setVisibility(View.VISIBLE);

            if (isFirebaseStorageUrl(model.getMessage())) {
                holder.leftChatImage.setVisibility(View.VISIBLE);
                loadImageOrVideo(holder.leftChatImage, holder.leftPlay, model.getMessage());
            } else {
                holder.leftChatImage.setVisibility(View.GONE);
                holder.leftChatTextview.setText(model.getMessage());
                holder.leftChatTextview.setMovementMethod(LinkMovementMethod.getInstance());
                Linkify.addLinks(holder.leftChatTextview, Linkify.ALL);
            }
        }
    }



    @NonNull
    @Override
    public ChatModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row,parent,false);
        return new ChatModelViewHolder(view);
    }




    class ChatModelViewHolder extends RecyclerView.ViewHolder{
        public ImageView rightChatImage, leftChatImage, leftPlay, rightPlay;
        LinearLayout leftChatLayout,rightChatLayout;
        TextView leftChatTextview,rightChatTextview,readStatusTextView;




        public ChatModelViewHolder(@NonNull View itemView){
            super(itemView);

            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextview = itemView.findViewById(R.id.left_chat_tv);
            rightChatTextview = itemView.findViewById(R.id.right_chat_tv);
            leftChatImage = itemView.findViewById(R.id.left_chat_image);
            rightChatImage = itemView.findViewById(R.id.right_chat_image);
            leftPlay = itemView.findViewById(R.id.left_play);
            rightPlay = itemView.findViewById(R.id.right_play);



        }
    }
    private boolean isFirebaseStorageUrl(String url) {
        if (url.contains("audio"))
            return false;
        return url.contains("firebasestorage.googleapis.com");
    }

    private void loadImageOrVideo(ImageView imageView, ImageView play, String url) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference mediaRef = storage.getReferenceFromUrl(url);
        String cleanUrl = url.split("\\?")[0];
        Log.d("Chat Recycler", "Clean url: " + cleanUrl);

        try {
            File localFile = File.createTempFile("media", getFileExtension(cleanUrl));

            mediaRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                Log.d("Recycler", "File downloaded to: " + localFile.getAbsolutePath());

                if (url.contains(".mp4") || url.contains(".mov") || url.contains(".avi")) {
                    play.setVisibility(View.VISIBLE);
                    // Load video thumbnail
                    Glide.with(context)
                            .load(localFile) // Extract the first frame
                            .thumbnail(0.1f) // Load a low-res image first
                            .into(imageView);
                } else {
                    // Load image
                    Glide.with(context)
                            .load(localFile)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    Log.e("Recycler", "Glide failed to load image", e);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    Log.d("Recycler", "Glide successfully loaded image");

                                    //Making the image view dynamic
                                    int imageWidth = resource.getIntrinsicWidth();
                                    int imageHeight = resource.getIntrinsicHeight();
                                    float aspectRatio = (float) imageWidth / imageHeight;


                                    ViewGroup.LayoutParams params = imageView.getLayoutParams();
                                    params.width = imageView.getWidth();
                                    params.height = (int) (params.width / aspectRatio);
                                    imageView.setLayoutParams(params);
                                    return false;
                                }
                            })
                            .into(imageView);
                }

                imageView.setOnClickListener(v -> {
                    String local_url = localFile.getAbsolutePath();
                    Log.d("Chat Recycler", "Local url: " + local_url);
                    // Intent to ImageEditorActivity or VideoEditorActivity
                    Intent intent = new Intent(context, local_url.endsWith(".mp4") || local_url.endsWith(".mov") || local_url.contains(".avi") ? VideoEditorActivity.class : ImageEditorActivity.class);
                    intent.putExtra("media_path", localFile.getAbsolutePath());
                    intent.putExtra("media_url", url);
                    intent.putExtra("otherUserId", otherUserId);
                    intent.putExtra("chatroomId", chatroomId);
                    intent.putExtra("otherUserUsername", otherUserUsername);
                    intent.putExtra("myUsername", myUsername);
                    intent.putExtra("fcmToken", fcmToken);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
}
