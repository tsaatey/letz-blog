package com.artlib.letzblog;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class UpdatePostActivity extends AppCompatActivity {

    private ImageButton imageButton;
    private Button updateButton;
    private EditText postTitle;
    private EditText postDescription;

    private final static int GALLERY_CODE = 112;
    private Uri imagePath = null;

    private DatabaseReference updatePostDatabaseReference;
    private StorageReference firebaseStorage;

    private UserLocalDataStore userLocalDataStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_post);

        updatePostDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Blog");
        updatePostDatabaseReference.keepSynced(true);

        firebaseStorage = FirebaseStorage.getInstance().getReference();

        userLocalDataStore = new UserLocalDataStore(this);

        imageButton = (ImageButton) findViewById(R.id.image_button);
        updateButton = (Button) findViewById(R.id.update_blog_button);
        postTitle = (EditText) findViewById(R.id.title);
        postDescription = (EditText) findViewById(R.id.description);

        // Call method to fetch blog data
        fetchBlogData(userLocalDataStore.getPostKey());

        // Open gallery to select image
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        // Set OnclickListener for update button
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateBlog(userLocalDataStore.getPostKey());
            }
        });

    }

    private void fetchBlogData(String postKey) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching blog data, please wait...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(true);
        progressDialog.show();

        updatePostDatabaseReference.child(postKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String title = (String) dataSnapshot.child("blogTitle").getValue();
                String description = (String) dataSnapshot.child("blogDescription").getValue();
                String image = (String) dataSnapshot.child("blogImage").getValue();

                userLocalDataStore.storeBlogImage(image);

                postTitle.setText(title);
                postDescription.setText(description);
                Picasso.with(UpdatePostActivity.this).load(image).into(imageButton);

                progressDialog.dismiss();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressDialog.dismiss();
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

    private void updateBlog(String postKey) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating post, please wait...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);

        final DatabaseReference updatePost = updatePostDatabaseReference.child(postKey);
        final String post_title = postTitle.getText().toString().trim();
        final String post_desc = postDescription.getText().toString().trim();

        if (!TextUtils.isEmpty(post_title) && !TextUtils.isEmpty(post_desc)) {
            progressDialog.show();

            if (imagePath != null) {
                StorageReference storageReference = firebaseStorage.child("Blog_Images").child(Uri.parse(userLocalDataStore.getBlogImage()).getLastPathSegment());
                storageReference.putFile(imagePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        final Uri downloadUri = taskSnapshot.getDownloadUrl();
                        updatePost.child("blogTitle").setValue(post_title);
                        updatePost.child("blogDescription").setValue(post_desc);
                        updatePost.child("blogImage").setValue(downloadUri.toString());

                        progressDialog.dismiss();

                        Intent mainIntent = new Intent(UpdatePostActivity.this, MainActivity.class);
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(mainIntent);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(UpdatePostActivity.this, "Post update failed!", Toast.LENGTH_LONG).show();
                    }
                });


            } else {
                updatePost.child("blogTitle").setValue(post_title);
                updatePost.child("blogDescription").setValue(post_desc);

                progressDialog.dismiss();

                Intent mainIntent = new Intent(UpdatePostActivity.this, MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(mainIntent);
            }
        } else {
            Toast.makeText(UpdatePostActivity.this, "Make sure fields are not empty", Toast.LENGTH_LONG).show();
        }
    }
}
