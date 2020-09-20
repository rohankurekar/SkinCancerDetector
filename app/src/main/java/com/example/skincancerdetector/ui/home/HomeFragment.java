package com.example.skincancerdetector.ui.home;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.skincancerdetector.HomeActivity;
import com.example.skincancerdetector.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeFragment extends Fragment {
    private HomeViewModel homeViewModel;

    private static final int CAMERA_REQUEST = 1888;
    ImageView imageView;
    Interpreter tflite;
    TextView malignantProbability;
    TextView benignProbability;
    TextView cancerType;
    TextView remedies;
    EditText query;
    DecimalFormat df;
    Button navPostButton;
    LinearLayout ll;
    Bitmap photo;
    StorageReference storageRef;
    StorageReference mountainsRef;
    View root;
    FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        root = inflater.inflate(R.layout.fragment_home, container, false);


        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {

            }
        });
        mAuth = FirebaseAuth.getInstance();
        // Create a storage reference from our app
        storageRef = FirebaseStorage.getInstance().getReference();

        // Access a Cloud Firestore instance from your Activity
        db = FirebaseFirestore.getInstance();

        imageView = (ImageView) root.findViewById(R.id.imageView1);
        Button photoButton = (Button) root.findViewById(R.id.capture);
        Button mapButton = (Button) root.findViewById(R.id.mapButton);
        Button postButton = (Button) root.findViewById(R.id.postButton);
        navPostButton = (Button) root.findViewById(R.id.navigation_dashboard);
        query = (EditText) root.findViewById(R.id.query);
        malignantProbability= (TextView) root.findViewById(R.id.malignantProbability);
        benignProbability = (TextView) root.findViewById(R.id.benignProbability);
        cancerType = root.findViewById(R.id.typeOfCancer);

        df = new DecimalFormat("##.##");
        photoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(root.getContext(), "Hello", Toast.LENGTH_SHORT).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        });

        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nearBySkinSpecialist();
            }
        });

        ll = (LinearLayout) root.findViewById(R.id.remedies);

        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postOnForum();
            }
        });


        return root;
    }

    public void classify(Bitmap photo){
        try {

            TensorBuffer probabilityBuffer =
                    TensorBuffer.createFixedSize(new int[]{1, 1001}, DataType.FLOAT32);
            ImageProcessor imageProcessor =
                    new ImageProcessor.Builder()
                            .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                            .build();
            TensorImage tImage = new TensorImage(DataType.FLOAT32);
            Log.e("tfliteSupport", "before photo");
            tImage.load(photo);
            tImage = imageProcessor.process(tImage);
            Log.e("tfliteSupport", "after photo");
            try{
                MappedByteBuffer tfliteModel
                        = FileUtil.loadMappedFile(root.getContext(),
                        "model.tflite");
                tflite = new Interpreter(tfliteModel);
                Log.e("tfliteSupport", "model readed");

            } catch (IOException e){
                Log.e("tfliteSupport", "Error reading model", e);
            }
            // Running inference
            if(null != tflite) {
                tflite.run(tImage.getBuffer(), probabilityBuffer.getBuffer());
                Float malignantProb=probabilityBuffer.getFloatArray()[0];
                Float benignProb=probabilityBuffer.getFloatArray()[1];
                Log.e("tfliteSupport", String.valueOf(malignantProb));
                Log.e("tfliteSupport", String.valueOf(benignProb));
                if(malignantProb>=0 && malignantProb<=1){
                    malignantProbability.setText(" "+df.format(malignantProb*100)+"%");
                    remedyFirestore("malignant");
                }
                if(benignProb>=0 && benignProb<=1) {
                    benignProbability.setText(" "+df.format(benignProb*100)+"%");
                    remedyFirestore("benign");
                }

            }

        }catch (Exception e){
            Toast.makeText(root.getContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }

    }
    /** Memory-map the model file in Assets. */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST) {
            photo = (Bitmap) data.getExtras().get("data");
            classify(photo);
            // storeImage(photo);
            imageView.setImageBitmap(photo);


        }
    }

    public void remedyFirestore(final String field){

        db.collection("remedy")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("success", document.getId() + " => " + document.getData().get(field));
                                cancerType.setText(field);

                                String []remedy=document.getData().get(field).toString().split(":");
                                for(int i=0;i<remedy.length;i++)
                                {
                                    TextView text = new TextView(root.getContext());
                                    text.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                    text.setText(""+remedy[i]);
                                    text.setTextSize(20);
                                    ll.addView(text);
                                    Log.d("length",String.valueOf(remedy.length));
                                }


                            }
                        } else {
                            Log.w("fail", "Error getting documents.", task.getException());
                        }
                    }
                });
    }
    public void nearBySkinSpecialist(){
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=skin specialist");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    public void postOnForum(){
        final ProgressDialog progressDialog = new ProgressDialog(root.getContext());
        progressDialog.setTitle("Uploading");
        progressDialog.show();



        String email=mAuth.getCurrentUser().getEmail();
        String imagename=email+UUID.randomUUID();
        //----------------------
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        //-----------------------


        mountainsRef = storageRef.child("SkinCancerDetector/"+imagename+".jpg");


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        //Log.d("check connection",mountainsRef.get);

        mountainsRef.putBytes(data)
        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
                progressDialog.dismiss();
                Toast.makeText(root.getContext(), "uploaded", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(root.getContext(), "unsuccessful", Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                //calculating progress percentage
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d("upload",String.valueOf(progress));
                //displaying percentage in progress dialog
                progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
            }
        });

        // Create a new user with a first and last name

        Map<String, Object> user = new HashMap<>();
        user.put("Query", query.getText().toString());
        user.put("ImageId",imagename+".jpg");
        user.put("benignProbability", malignantProbability.getText().toString());
        user.put("malignantProbability", benignProbability.getText().toString());




// Add a new document with a generated ID

        db.collection("forum").document(email).collection("Query")
                .document(imagename)
                .set(user, SetOptions.merge());
        user.clear();
        user.put(email, email);
        db.collection("forum").document("email")
                .set(user, SetOptions.merge());

    }



}