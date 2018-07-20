package com.artlib.letzblog;

import android.app.ProgressDialog;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Date;
import java.util.TimeZone;

public class PostActivity extends AppCompatActivity {

    private ImageButton imageButton;
    private EditText titleText;
    private EditText descriptionText;
    private Button submitBlogButton;
    private Uri imagePath = null;
    private ProgressDialog progressDialog;

    private StorageReference firebaseStorage;
    private DatabaseReference databaseReference;
    private DatabaseReference userData;
    private DatabaseReference profileDatabaseReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    private final static int GALLERY_CODE = 100;
    private String username;
    private String profileImagePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        imageButton = (ImageButton) findViewById(R.id.image_button);
        titleText = (EditText) findViewById(R.id.title);
        descriptionText = (EditText) findViewById(R.id.description);
        submitBlogButton = (Button) findViewById(R.id.submit_blog_button);

        progressDialog = new ProgressDialog(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser  = firebaseAuth.getCurrentUser();

        firebaseStorage = FirebaseStorage.getInstance().getReference();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Blog");
        userData = FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUser.getUid());
        profileDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Profile_Settings");

        // Code image button to call openGallery method
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        // Call upload method to save new post to database
        submitBlogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadPost();
            }
        });
    }

    private void openGallery() {
        Intent openGalleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        openGalleryIntent.setType("image/*");
        startActivityForResult(openGalleryIntent, GALLERY_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_CODE && resultCode == RESULT_OK) {
            imagePath = data.getData();
            imageButton.setImageURI(imagePath);
        }
    }

    private void uploadPost() {
        final String title = titleText.getText().toString().trim();
        final String description = descriptionText.getText().toString().trim();

        final java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("E MMM dd yyy @ HH:mm");
        simpleDateFormat.setTimeZone(TimeZone.getDefault());
        final String date = simpleDateFormat.format(new Date()).toString();

        if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(description) && imagePath != null) {
            progressDialog.setMessage("Posting blog...");
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();

            StorageReference storageReference = firebaseStorage.child("Blog_Images").child(imagePath.getLastPathSegment());
            storageReference.putFile(imagePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    final Uri downloadUri = taskSnapshot.getDownloadUrl();
                    final DatabaseReference newPost = databaseReference.push();

                    userData.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            profileDatabaseReference.child(firebaseAuth.getCurrentUser().getUid()).child("username").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    username = dataSnapshot.getValue(String.class);

                                    profileDatabaseReference.child(firebaseAuth.getCurrentUser().getUid()).child("image").addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            profileImagePath = dataSnapshot.getValue(String.class);

                                            newPost.child("blogTitle").setValue(title);
                                            newPost.child("blogDescription").setValue(description);
                                            newPost.child("blogImage").setValue(downloadUri.toString());
                                            newPost.child("userId").setValue(firebaseUser.getUid());
                                            newPost.child("blogDate").setValue(date);
                                            newPost.child("blogTime").setValue(getPostTime());
                                            newPost.child("profileImage").setValue(profileImagePath);
                                            newPost.child("profileUsername").setValue(username);

                                            newPost.child("username").setValue(dataSnapshot.child("name").getValue()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful()) {
                                                        Intent mainIntent = new Intent(PostActivity.this, MainActivity.class);
                                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                        startActivity(mainIntent);
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                    progressDialog.dismiss();

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(PostActivity.this, "Error uploading image", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(PostActivity.this, "Make sure to add an image, post title and description", Toast.LENGTH_LONG).show();
        }
    }

    private String getPostTime() {
        final java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getDefault());
        final String date = simpleDateFormat.format(new Date()).toString();
        return date;
    }
}
