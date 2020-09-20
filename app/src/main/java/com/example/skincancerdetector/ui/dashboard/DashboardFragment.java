package com.example.skincancerdetector.ui.dashboard;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.skincancerdetector.ForumActivity;
import com.example.skincancerdetector.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {
    Map<String, Object> emails;
    ListView listView;
    FirebaseFirestore db;
    private FirebaseAuth mAuth;
    String email;
    View root;
    RadioButton userForumData;
    RadioButton publicForumData;
    Button load;
    ArrayAdapter<String> adapter;
    ProgressDialog progressDialog;
    ArrayList listItemUser=new ArrayList<String>();
    ArrayList listItemPublic=new ArrayList<String>();
    ArrayList listItemUserId=new ArrayList<String>();
    ArrayList listItemPublicId=new ArrayList<String>();
    private DashboardViewModel dashboardViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                ViewModelProviders.of(this).get(DashboardViewModel.class);
        root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        dashboardViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {

            }
        });

        mAuth = FirebaseAuth.getInstance();
        email=mAuth.getCurrentUser().getEmail();
        progressDialog = new ProgressDialog(root.getContext());
        // Access a Cloud Firestore instance from your Activity
        db = FirebaseFirestore.getInstance();

        listView=(ListView)root.findViewById(R.id.mobile_list);
        userForumData = root.findViewById(R.id.userForumData);
        publicForumData = root.findViewById(R.id.publicForumData);
        load = root.findViewById(R.id.load);
        load.setEnabled(false);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // TODO Auto-generated method stub
                String value=adapter.getItem(position);
                Toast.makeText(root.getContext(),value+position,Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(root.getContext(), ForumActivity.class);
                String documentId="";
                if(userForumData.isChecked()){
                    documentId=listItemUserId.get(position).toString();
                }
                if(publicForumData.isChecked()){
                    documentId=listItemPublicId.get(position).toString();
                }
                intent.putExtra("documentId", documentId);
                startActivity(intent);

            }
        });
        userForumData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                load.setEnabled(false);
                progressDialog.setTitle("Uploading");
                progressDialog.show();
                getForumUserData();
                adapter.notifyDataSetChanged();
                publicForumData.setChecked(false);
                progressDialog.dismiss();
                load.setEnabled(true);
            }
        });
        publicForumData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                load.setEnabled(false);
                progressDialog.setTitle("Uploading");
                progressDialog.show();
                getForumPublicData();
                adapter.notifyDataSetChanged();
                userForumData.setChecked(false);
                progressDialog.dismiss();
                load.setEnabled(true);
            }
        });

        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                load();
            }
        });

        return root;
    }



    public void getForumUserData(){



        listItemUser.clear();
        db.collection("forum").document(email).collection("Query")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("success", document.getId() + " => " + document.getData().get("Query"));
                                listItemUser.add(document.getData().get("Query").toString());
                                listItemUserId.add(document.getId());
                                Log.d("forum",listItemUser.toString());

                            }

                        } else {
                            Log.w("fail", "Error getting documents.", task.getException());

                        }
                    }
                });

        adapter = new ArrayAdapter<String>(root.getContext(),
                android.R.layout.simple_list_item_1, android.R.id.text1, listItemUser);
        listView.setAdapter(adapter);



    }
    public void getForumPublicData(){



        listItemPublic.clear();

        emails = new HashMap<>();
        db.collection("forum")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("success email", document.getId() + " => " + document.getData());
                                emails=document.getData();
                                //listItemPublic.add(document.getId());
                                Log.d("forum",listItemPublic.toString());
                                Log.d("id",emails.toString());
                                for(String emailId:emails.keySet()) {
                                    Log.d("email",emailId);
                                    db.collection("forum").document(emailId).collection("Query")
                                            .get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                                            Log.d("success", document.getId() + " => " + document.getData().get("Query"));
                                                            listItemPublic.add(document.getData().get("Query").toString());
                                                            listItemPublicId.add(document.getId());
                                                            Log.d("forum", listItemPublic.toString());


                                                        }

                                                    } else {
                                                        Log.w("fail", "Error getting documents.", task.getException());
                                                    }
                                                }
                                            });

                                }
                            }

                        } else {
                            Log.w("fail", "Error getting documents.", task.getException());

                        }
                    }
                });





        adapter = new ArrayAdapter<String>(root.getContext(),
                android.R.layout.simple_list_item_1, android.R.id.text1, listItemPublic);
        listView.setAdapter(adapter);





    }
    public void load(){
        adapter.notifyDataSetChanged();
    }

}