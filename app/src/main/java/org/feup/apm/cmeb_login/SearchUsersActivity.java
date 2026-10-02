package org.feup.apm.cmeb_login;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.feup.apm.cmeb_login.adapter.SearchUserRecyclerAdapter;
import org.feup.apm.cmeb_login.model.UserModel;
import org.feup.apm.cmeb_login.util.FirebaseUtil;

public class SearchUsersActivity extends AppCompatActivity {

    RadioGroup radioGroup;
    RadioButton rbHospital, rbSpecialty, rbUsername, rbExperience;
    SearchUserRecyclerAdapter adapter;
    EditText etSearchUsername;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CollectionReference usersCollection = db.collection("users");




    RecyclerView recyclerview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search_users);



        //Definition of the elements:
        etSearchUsername = findViewById(R.id.etSearchUsername);
        recyclerview = findViewById(R.id.recyclerview);
        radioGroup = findViewById(R.id.radioGroup);
        rbUsername = findViewById(R.id.rbUsername);
        rbHospital = findViewById(R.id.rbHospital);
        rbSpecialty = findViewById(R.id.rbSpecialty);
        rbExperience = findViewById(R.id.rbExperience);
        etSearchUsername.setHint("Type the username");


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // For when user presses the back button
                Intent intent = new Intent(SearchUsersActivity.this, MainActivity.class);
                intent.putExtra("targetFragment", "ChatFragment"); // Adiciona uma identificação para o fragmento
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish(); // Finishing the Search Users Activity
            }
        });

        //Updating the hint dynamically
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {

                hintToShow(checkedId);
            }
        });


        etSearchUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed

            }



            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Showing dynamically the results everytime the user types
                String searchTerm = s.toString();
                if (!searchTerm.isEmpty()) {
                    setupSearchRecyclerView(searchTerm);
                }
                else{
                    usersCollection.get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null) {
                                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                    UserModel user = document.toObject(UserModel.class);
                                    // Updating
                                }
                            }
                        } else {
                            Log.w("Firestore", "Error getting documents.", task.getException());
                        }
                    });
                    adapter.stopListening();
                    recyclerview.setAdapter(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

    }


    void setupSearchRecyclerView(String searchTerm){

        usersCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        UserModel user = document.toObject(UserModel.class);
                        // Updating
                    }
                }
            } else {
                Log.w("Firestore", "Error getting documents.", task.getException());
            }
        });




        FirestoreRecyclerOptions<UserModel>options = getFirestoreRecyclerOptions(radioGroup,searchTerm);

        adapter = new SearchUserRecyclerAdapter(options,getApplicationContext());


        //When sending images or videos:
        if(getIntent().getStringExtra("imageUrl") != null)
            adapter.setImageUrl(getIntent().getStringExtra("imageUrl"));
        if(getIntent().getStringExtra("videoUrl") != null)
            adapter.setImageUrl(getIntent().getStringExtra("videoUrl"));


        recyclerview.setLayoutManager(new LinearLayoutManager(this));
        recyclerview.setAdapter(adapter);
        adapter.startListening();

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerview.smoothScrollToPosition(0); // to allow scroll
            }
        });
    }

    @Override
    protected void onStart(){
        super.onStart();

        if (adapter!=null) {
            adapter.startListening();
        }

    }

    @Override
    protected void onStop(){
        super.onStop();
        if (adapter!= null){
            adapter.stopListening();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(adapter != null){
            adapter.startListening();
        }
    }

    public FirestoreRecyclerOptions<UserModel> getFirestoreRecyclerOptions(RadioGroup radioGroup, String searchTerm) {
        // Adapting the query to the selected radio button
        String orderByField = "username"; // Valor padrão

        int selectedId = radioGroup.getCheckedRadioButtonId();



        if (selectedId == R.id.rbHospital) {
            orderByField = "hospital";
            etSearchUsername.setHint("Type the desired hospital");

        }

        if(selectedId == R.id.rbSpecialty){
            orderByField = "specialty";
            etSearchUsername.setHint("Type the desired medical specialty");

        }


        else if (selectedId == R.id.rbExperience) {

            orderByField = "yearsExperience";
            etSearchUsername.setHint("Type the minimum years of experience");

        }

        // Building the query
        Query query;
        if ("yearsExperience".equals(orderByField)) {

            int minYearsExperience = 0; // in case input is empty
            try {
                minYearsExperience = Integer.parseInt(searchTerm);// Passing to int
            } catch (NumberFormatException e) {
                //Invalid input
                e.printStackTrace();
            }

            // For the years of experience
            query = usersCollection
                    .whereGreaterThanOrEqualTo(orderByField, minYearsExperience)
                    .whereNotEqualTo("userId", FirebaseUtil.currentUserId()) // Excluding current user
                    .orderBy(orderByField); // Ordering by years of experience
        } else {
            // For the other fields
            query = usersCollection
                    .whereGreaterThanOrEqualTo(orderByField, searchTerm)
                    .whereLessThan(orderByField, searchTerm + "\uf8ff")
                    .whereNotEqualTo("userId", FirebaseUtil.currentUserId())
                    .orderBy(orderByField);
        }



        return new FirestoreRecyclerOptions.Builder<UserModel>()
                .setQuery(query, UserModel.class)
                .build();
    }


    //Selecting the hint to show
    void hintToShow(int selectedId){
        etSearchUsername.setHint("Type the username"); //default


        if (selectedId == R.id.rbHospital) {

            etSearchUsername.setHint("Type the desired hospital");

        } else if (selectedId == R.id.rbExperience) {
            etSearchUsername.setHint("Type the minimum years of experience");

        }
        else if(selectedId == R.id.rbSpecialty){
            etSearchUsername.setHint("Type the desired medical specialty");

        }
    }


}