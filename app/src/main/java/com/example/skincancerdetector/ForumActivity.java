package com.example.skincancerdetector;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ForumActivity extends AppCompatActivity {
    Map<String, Object> forumData = new HashMap<>();
    FirebaseFirestore db;
    ArrayList listItemMessages=new ArrayList<String>();
    String documentId;
    String email;
    Button send;
    String ImageId;
    String Query;
    String malignantProbability;
    String benignProbability;
    StorageReference storageRef;
    StorageReference mountainsRef;
    EditText answer;
    ListView listView;
    ImageView image;
    TextView textQuery;
    TextView textMalignantProbability;
    TextView textBenignProbability;
    ArrayAdapter<String> adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum);
        listView=findViewById(R.id.mobile_list);
        image=findViewById(R.id.image);
        send=findViewById(R.id.send);
        answer=findViewById(R.id.answer);
        textQuery=findViewById(R.id.Query);
        textQuery.setMovementMethod(new ScrollingMovementMethod());
        textBenignProbability=findViewById(R.id.benignProbability);
        textMalignantProbability=findViewById(R.id.malignantProbability);
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        Intent intent = getIntent();
        documentId = intent.getStringExtra("documentId");
        String sp[]=documentId.split("@");
        email=sp[0]+"@gmail.com";
        Log.d("email",email);
        getQueryForumData();
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // TODO Auto-generated method stub
                String value=adapter.getItem(position);
                Toast.makeText(ForumActivity.this,value+position,Toast.LENGTH_SHORT).show();


            }
        });
    }

    public void getQueryForumData(){
        listItemMessages.clear();
        db.collection("forum").document(email).collection("Query")
                .whereEqualTo("ImageId", documentId+".jpg")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("success", document.getId() + " => " + document.getData());
                                forumData=document.getData();
                                Query=forumData.get("Query").toString();
                                ImageId=forumData.get("ImageId").toString();
                                malignantProbability=forumData.get("malignantProbability").toString();
                                benignProbability=forumData.get("benignProbability").toString();
                                forumData.remove("ImageId");
                                forumData.remove("malignantProbability");
                                forumData.remove("benignProbability");
                                forumData.remove("Query");
                                textQuery.setText(Query);
                                textBenignProbability.setText(benignProbability);
                                textMalignantProbability.setText(malignantProbability);
                                for(Object s:forumData.values()){
                                    listItemMessages.add(s.toString());
                                }


                                Log.d("forum",listItemMessages.toString());
                                adapter = new ArrayAdapter<String>(ForumActivity.this,
                                        android.R.layout.simple_list_item_1, android.R.id.text1, listItemMessages);
                                listView.setAdapter(adapter);

                                addImage();


                            }
                        } else {
                            Log.w("fail", "Error getting documents.", task.getException());
                        }
                    }
                });

    }
    public void sendMessage(){
        db.collection("forum").document(email).collection("Query")
                .document(documentId)
                .update(UUID.randomUUID().toString(), answer.getText().toString())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("send", "DocumentSnapshot successfully updated!");
                        listItemMessages.add(answer.getText().toString());
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("failed", "Error updating document", e);
                    }
                });
    }
    public void addImage(){
        mountainsRef = storageRef.child("SkinCancerDetector/"+ImageId);


        final long ONE_MEGABYTE = 1024 * 1024;
        mountainsRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // use this as needed
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                image.setImageBitmap(bitmap);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
    }


}