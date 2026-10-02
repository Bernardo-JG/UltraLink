package org.feup.apm.cmeb_login;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

import org.feup.apm.cmeb_login.adapter.RecentChatRecyclerAdapter;
import org.feup.apm.cmeb_login.model.ChatroomModel;
import org.feup.apm.cmeb_login.util.FirebaseUtil;


public class ChatFragment extends Fragment {

RecyclerView recyclerView;

private Button search_users;
RecentChatRecyclerAdapter adapter;


    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requireActivity().getWindow().getDecorView().setSystemUiVisibility(0);
        requireActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.bars_color));

    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {




        
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        //Defining the elements:
        recyclerView = view.findViewById(R.id.recycler_view);
        search_users = view.findViewById(R.id.search_button);
        setupRecyclerView();



        search_users.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
                Intent intent = new Intent(requireActivity(), SearchUsersActivity.class);
               startActivity(intent);

            }
        });


        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // when pressing back
                requireActivity().finish(); // Finalizing the associated activity
            }
        });




        return view;
    }

//Showing the recent chats:
    void setupRecyclerView(){



       //Finding the chatrooms associated with current user and displaying by the time of the last message
        FirestoreRecyclerOptions<ChatroomModel> options = new FirestoreRecyclerOptions.Builder<ChatroomModel>()
                .setQuery(FirebaseUtil.allChatroomCollectionReference().whereArrayContains("userIds",FirebaseUtil.currentUserId())
                        .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING),ChatroomModel.class).build();

        adapter = new RecentChatRecyclerAdapter(options,getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        adapter.startListening();

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });


    }

    @Override
    public void onStart(){
        super.onStart();

        if (adapter!=null) {
            adapter.startListening();
        }

    }

    @Override
    public void onStop(){
        super.onStop();
        if (adapter!= null){
            adapter.stopListening();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume(){
        super.onResume();
        if(adapter != null){
            adapter.notifyDataSetChanged();
        }

    }
}