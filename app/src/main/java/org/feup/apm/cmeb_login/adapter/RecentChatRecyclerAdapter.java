package org.feup.apm.cmeb_login.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.google.firebase.Timestamp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.feup.apm.cmeb_login.BluetoothDeviceInfo;
import org.feup.apm.cmeb_login.ImageEditorActivity;
import org.feup.apm.cmeb_login.VideoEditorActivity;
import org.feup.apm.cmeb_login.util.AndroidUtil;
import org.feup.apm.cmeb_login.ChatActivity;
import org.feup.apm.cmeb_login.util.FirebaseUtil;
import org.feup.apm.cmeb_login.R;
import org.feup.apm.cmeb_login.model.ChatroomModel;
import org.feup.apm.cmeb_login.model.UserModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class RecentChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatroomModel,RecentChatRecyclerAdapter.ChatroomModelViewHolder> {



    Context context;

    public RecentChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatroomModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatroomModelViewHolder holder, int position, @NonNull ChatroomModel model) {
        // Clean profile picture
        holder.profilePic.setImageResource(R.drawable.profile);

        FirebaseUtil.getOtherUserFromChatroom(model.getUserIds())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean lastMessageSentByMe = model.getLastMessageSenderId().equals(FirebaseUtil.currentUserId());

                        UserModel otherUserModel = task.getResult().toObject(UserModel.class);

                        // Search and load user's profile pic
                        FirebaseUtil.getOtherProfilePicStorageRef(otherUserModel.getUserId()).getDownloadUrl()
                                .addOnCompleteListener(t -> {
                                    if (t.isSuccessful()) {
                                        Uri uri = t.getResult();
                                        AndroidUtil.setProfilePic(context, uri, holder.profilePic);
                                    } else {
                                        // In case image is not found, get the original one
                                        holder.profilePic.setImageResource(R.drawable.profile);
                                    }
                                });

                        holder.usernameText.setText(otherUserModel.getUsername());

                        //assessing if the display should be in the left or right
                        if (lastMessageSentByMe) { //checks if a link was sent and if so, if it contains multimedia (and what)
                            if (model.getLastMessage() != null) {
                                if (AndroidUtil.isLink(model.getLastMessage()) && !model.getLastMessage().isEmpty()) {
                                    if (!AndroidUtil.containsMediaTerm(model.getLastMessage()).isEmpty()) {
                                        holder.lastMessageText.setText("You sent " + otherUserModel.getUsername() + " a" + AndroidUtil.grammar(AndroidUtil.containsMediaTerm(model.getLastMessage())) + AndroidUtil.containsMediaTerm(model.getLastMessage()));
                                    } else {
                                        holder.lastMessageText.setText("You: " + model.getLastMessage());

                                    }
                                } else {
                                    holder.lastMessageText.setText("You: " + model.getLastMessage());
                                }

                            }
                            else {
                                holder.lastMessageText.setText("You: " + model.getLastMessage());
                            }
                        } else {
                            if (model.getLastMessage() != null) {
                                if (AndroidUtil.isLink(model.getLastMessage())) { //same procedure
                                    if (!AndroidUtil.containsMediaTerm(model.getLastMessage()).isEmpty()) {
                                        holder.lastMessageText.setText(otherUserModel.getUsername() + " sent you a" + AndroidUtil.grammar(AndroidUtil.containsMediaTerm(model.getLastMessage())) + AndroidUtil.containsMediaTerm(model.getLastMessage()));
                                    } else {
                                        holder.lastMessageText.setText(model.getLastMessage());

                                    }
                                } else {
                                    holder.lastMessageText.setText(model.getLastMessage());
                                }
                            }

                            else {
                                holder.lastMessageText.setText(model.getLastMessage());
                            }

                        }

                        Timestamp timestamp = model.getLastMessageTimestamp();
                        holder.lastMessageTime.setText(FirebaseUtil.timestampToString(timestamp));
                        holder.lastMessageDay.setText(FirebaseUtil.timestampToDay(timestamp));

                        holder.itemView.setOnClickListener(v -> {
                            Intent intent = new Intent(context, ChatActivity.class);
                            AndroidUtil.passUserModelAsIntent(intent, otherUserModel);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        });
                    }
                });
    }


    @NonNull
    @Override
    public ChatroomModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_chat_recycler_row,parent,false);
        return new ChatroomModelViewHolder(view);
    }

    class ChatroomModelViewHolder extends RecyclerView.ViewHolder{
        TextView usernameText;
        TextView lastMessageText;
        TextView lastMessageTime;

        TextView lastMessageDay;
        ImageView profilePic;


        public ChatroomModelViewHolder(@NonNull View itemView){
            super(itemView);
            usernameText=itemView.findViewById(R.id.user_name_text);
            lastMessageText = itemView.findViewById(R.id.last_message_text);
            lastMessageTime = itemView.findViewById(R.id.last_message_time_text);
            lastMessageDay = itemView.findViewById(R.id.last_message_day);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);

        }
    }





}
