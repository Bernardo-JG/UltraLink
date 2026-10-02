package org.feup.apm.cmeb_login.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import org.feup.apm.cmeb_login.util.AndroidUtil;
import org.feup.apm.cmeb_login.ChatActivity;
import org.feup.apm.cmeb_login.util.FirebaseUtil;
import org.feup.apm.cmeb_login.R;
import org.feup.apm.cmeb_login.model.UserModel;

public class SearchUserRecyclerAdapter extends FirestoreRecyclerAdapter<UserModel,SearchUserRecyclerAdapter.UserModelViewHolder> {



    Context context;
    private String imageUrl = null;

    public SearchUserRecyclerAdapter(@NonNull FirestoreRecyclerOptions<UserModel> options, Context applicationContext) {
        super(options);
        this.context = applicationContext;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    protected void onBindViewHolder(@NonNull UserModelViewHolder holder, int position, @NonNull UserModel model) {
        holder.usernameText.setText(model.getUsername());
        holder.hospitalText.setText("Specialist in " + model.getSpecialty() +" in " + model.getHospital());
        holder.yearsText.setText("Years of Experience: "+ model.getYearsExperience());

        FirebaseUtil.getOtherProfilePicStorageRef(model.getUserId()).getDownloadUrl()
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()){
                        Uri uri = t.getResult();
                        AndroidUtil.setProfilePic(context,uri,holder.profilePic);
                    }
                });
        holder.itemView.setOnClickListener(v->{
            Intent intent = new Intent(context,ChatActivity.class);
            AndroidUtil.passUserModelAsIntent(intent,model);
            if(imageUrl != null)
                intent.putExtra("imageUrl", imageUrl);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @NonNull
    @Override
    public UserModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.search_user_recycler_row,parent,false);
        return new UserModelViewHolder(view);
    }

    class UserModelViewHolder extends RecyclerView.ViewHolder{
        TextView usernameText;
        TextView hospitalText;
        ImageView profilePic;

        TextView yearsText;
        
        
        public UserModelViewHolder(@NonNull View itemView){
            super(itemView);
            usernameText=itemView.findViewById(R.id.user_name_text);
            hospitalText = itemView.findViewById(R.id.hospital_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
            yearsText = itemView.findViewById(R.id.years_text);

        }
    }
}
