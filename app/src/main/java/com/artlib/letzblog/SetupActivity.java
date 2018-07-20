package com.artlib.letzblog;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

public class SetupActivity extends AppCompatActivity {

    private ImageButton profileImageButton;
    private Button setupButton;
    private EditText displayName;
    private Uri imagePath = null;
    private ProgressDialog progressDialog;

    private static final int GALLERY_CODE = 120;

    private DatabaseReference userDatabase;
    private FirebaseAuth firebaseAuth;
    private StorageReference userImageStorage;

    private UserLocalDataStore userLocalDataStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        profileImageButton = (ImageButton) findViewById(R.id.user_profile_picture);
        setupButton = (Button)findViewById(R.id.setup_button);
        displayName = (EditText) findViewById(R.id.user_name);
        progressDialog = new ProgressDialog(this);

        userDatabase = FirebaseDatabase.getInstance().getReference().child("Profile_Settings");
        firebaseAuth = FirebaseAuth.getInstance();
        userImageStorage = FirebaseStorage.getInstance().getReference();

        userLocalDataStore = new UserLocalDataStore(this);

        profileImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        setupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupAccount();
            }
        });
    }

    private void setupAccount() {
        final String name = displayName.getText().toString().trim();

        if (!TextUtils.isEmpty(name)) {
            progressDialog.setMessage("Setting up account...");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();

            final String userId = firebaseAuth.getCurrentUser().getUid();
            final Uri checkedImage;

            if (imagePath != null) {
                checkedImage = imagePath;
            } else {
                checkedImage = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + getResources().getResourcePackageName(R.mipmap.avatar) +
                "/" + getResources().getResourceTypeName(R.mipmap.avatar) +
                "/" + getResources().getResourceEntryName(R.mipmap.avatar));
            }

            StorageReference filePath = userImageStorage.child("Profile_Images").child(checkedImage.getLastPathSegment());
            filePath.putFile(checkedImage).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    String downloadedUri = taskSnapshot.getDownloadUrl().toString();

                    userDatabase.child(userId).child("username").setValue(name);
                    userDatabase.child(userId).child("image").setValue(downloadedUri);

                    userLocalDataStore.storeProfilePicture(downloadedUri);
                    userLocalDataStore.storeProfileUsername(name);

                    progressDialog.dismiss();
                    Intent mainIntent = new Intent(SetupActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(mainIntent);

                }
            });

        } else {
            Toast.makeText(SetupActivity.this, "Name field must not be empty!", Toast.LENGTH_LONG).show();
        }
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
            CropImage.activity(imagePath)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setCropShape(CropImageView.CropShape.OVAL)
                    .setAspectRatio(1, 1)
                    .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                imagePath = result.getUri();
                profileImageButton.setImageURI(imagePath);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

    }
}
