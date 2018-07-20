package com.artlib.letzblog;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.Iterator;

public class ProfileSettingsActivity extends AppCompatActivity {

    private ImageButton profilePictureUpdateImageButton;
    private Button profileUpdateButton;
    private EditText profileNameTextField;

    private Uri imagePath;
    private Uri oldImagePath;

    private ProgressDialog profileSettingsDialog;

    private final static int GALLERY_CODE = 1;
    private boolean profilePictureRemoved = false;

    private DatabaseReference blogUserProfileUpdateDatabaseReference;
    private DatabaseReference profileSettingsDatabaseReference;
    private FirebaseAuth currentUser;
    private StorageReference profileStorageReference;

    private UserLocalDataStore userLocalDataStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        profilePictureUpdateImageButton = (ImageButton) findViewById(R.id.update_profile_picture_image_button);
        profileUpdateButton = (Button) findViewById(R.id.profile_update_button);
        profileNameTextField = (EditText) findViewById(R.id.user_name_text);

        profileSettingsDialog = new ProgressDialog(this);

        blogUserProfileUpdateDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Blog");
        blogUserProfileUpdateDatabaseReference.keepSynced(true);

        profileSettingsDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Profile_Settings");
        profileSettingsDatabaseReference.keepSynced(true);

        currentUser = FirebaseAuth.getInstance();

        userLocalDataStore = new UserLocalDataStore(this);

        profilePictureUpdateImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopUpMenu();
            }
        });

        profileUpdateButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                updateProfileSettings(currentUser.getCurrentUser().getUid());
            }
        });

        fetchUserData(currentUser.getCurrentUser().getUid());
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
                profilePictureUpdateImageButton.setImageURI(imagePath);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(ProfileSettingsActivity.this, "Image could not be loaded", Toast.LENGTH_LONG).show();
            }
        }

    }

    private void fetchUserData(String userId) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching profile data, please wait...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(true);
        progressDialog.show();

        profileSettingsDatabaseReference.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String username = (String)dataSnapshot.child("username").getValue();
                String image = (String)dataSnapshot.child("image").getValue();

               oldImagePath = Uri.parse(image);

                profileNameTextField.setText(username);
                Picasso.with(ProfileSettingsActivity.this).load(image).into(profilePictureUpdateImageButton);

                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressDialog.dismiss();
            }
        });
    }

    private void updateProfileSettings(final String userId) {
        final String editedUsername = profileNameTextField.getText().toString().trim();
        final String[] newImage = new String[1];

        if (!TextUtils.isEmpty(editedUsername)) {
            profileSettingsDialog.setMessage("Updating profile settings, please wait...");
            profileSettingsDialog.setIndeterminate(true);
            profileSettingsDialog.setCancelable(false);
            profileSettingsDialog.show();

            blogUserProfileUpdateDatabaseReference.orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    final String[] blogId = new String[((int) dataSnapshot.getChildrenCount())];
                    Iterator<DataSnapshot> postId = dataSnapshot.getChildren().iterator();

                    int counter = 0;
                    while (postId.hasNext()) {
                        blogId[counter] = postId.next().getKey();
                        counter += 1;
                    }

                    if (imagePath != null) {
                        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(oldImagePath.toString());
                        storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                StorageReference reference = FirebaseStorage.getInstance().getReference().child("Profile_Images");
                                reference.child(imagePath.getLastPathSegment()).putFile(imagePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        userLocalDataStore.storeProfilePicture(taskSnapshot.getDownloadUrl().toString());
                                        newImage[0] = taskSnapshot.getDownloadUrl().toString();

                                        if (blogId.length > 0) {
                                            for (int i = 0; i < blogId.length; i++) {
                                                DatabaseReference ref = blogUserProfileUpdateDatabaseReference.child(blogId[i]);
                                                ref.child("profileUsername").setValue(editedUsername);
                                                ref.child("profileImage").setValue(newImage[0]);
                                            }
                                            profileSettingsDatabaseReference.child(userId).child("username").setValue(editedUsername);
                                            profileSettingsDatabaseReference.child(userId).child("image").setValue(newImage[0]).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        dismissWhenNeeded(profileSettingsDialog);
                                                        Intent mainIntent = new Intent(ProfileSettingsActivity.this, MainActivity.class);
                                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                        startActivity(mainIntent);

                                                    }
                                                }
                                            });

                                        } else {
                                            profileSettingsDatabaseReference.child(userId).child("username").setValue(editedUsername);
                                            profileSettingsDatabaseReference.child(userId).child("image").setValue(newImage[0]).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        dismissWhenNeeded(profileSettingsDialog);
                                                        Intent mainIntent = new Intent(ProfileSettingsActivity.this, MainActivity.class);
                                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                        startActivity(mainIntent);
                                                    }
                                                }
                                            });
                                        }

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });
                            }
                        });


                    } else if (imagePath == null && profilePictureRemoved == false) {

                        if (blogId.length > 0) {
                            for (int i = 0; i < blogId.length; i++) {
                                DatabaseReference ref = blogUserProfileUpdateDatabaseReference.child(blogId[i].toString());
                                ref.child("profileUsername").setValue(editedUsername);
                            }
                            profileSettingsDatabaseReference.child(userId).child("username").setValue(editedUsername).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        dismissWhenNeeded(profileSettingsDialog);
                                        Intent mainIntent = new Intent(ProfileSettingsActivity.this, MainActivity.class);
                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(mainIntent);

                                    }
                                }
                            });

                        } else {
                            profileSettingsDatabaseReference.child(userId).child("username").setValue(editedUsername).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        dismissWhenNeeded(profileSettingsDialog);
                                        Intent mainIntent = new Intent(ProfileSettingsActivity.this, MainActivity.class);
                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(mainIntent);


                                    }
                                }
                            });
                        }

                    } else if (imagePath == null && profilePictureRemoved == true) {

                        imagePath = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                                "://" + getResources().getResourcePackageName(R.mipmap.avatar) +
                                "/" + getResources().getResourceTypeName(R.mipmap.avatar) +
                                "/" + getResources().getResourceEntryName(R.mipmap.avatar));


                        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(oldImagePath.toString());
                        storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                StorageReference reference = FirebaseStorage.getInstance().getReference().child("Profile_Images");
                                reference.child(imagePath.getLastPathSegment()).putFile(imagePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        String image = taskSnapshot.getDownloadUrl().toString();

                                        if (blogId.length > 0) {
                                            for (int i = 0; i < blogId.length; i++) {
                                                DatabaseReference ref = blogUserProfileUpdateDatabaseReference.child(blogId[i].toString());
                                                ref.child("profileUsername").setValue(editedUsername);
                                                ref.child("profileImage").setValue(image);

                                            }
                                            profileSettingsDatabaseReference.child(userId).child("username").setValue(editedUsername);
                                            profileSettingsDatabaseReference.child(userId).child("image").setValue(image).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        dismissWhenNeeded(profileSettingsDialog);
                                                        Intent mainIntent = new Intent(ProfileSettingsActivity.this, MainActivity.class);
                                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                        startActivity(mainIntent);
                                                    }
                                                }
                                            });

                                        } else {
                                            profileSettingsDatabaseReference.child(userId).child("username").setValue(editedUsername);
                                            profileSettingsDatabaseReference.child(userId).child("image").setValue(image).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        dismissWhenNeeded(profileSettingsDialog);
                                                        Intent mainIntent = new Intent(ProfileSettingsActivity.this, MainActivity.class);
                                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                        startActivity(mainIntent);
                                                    }
                                                }
                                            });

                                        }
                                    }
                                });
                            }
                        });


                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        } else {
            Toast.makeText(ProfileSettingsActivity.this, "Please supply nickname", Toast.LENGTH_LONG).show();
        }

    }

    private void showPopUpMenu() {

        PopupMenu popupMenu = new PopupMenu(ProfileSettingsActivity.this, profilePictureUpdateImageButton);
        popupMenu.getMenuInflater().inflate(R.menu.profile_picture_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.remove_picture:

                        profilePictureRemoved = true;
                        profilePictureUpdateImageButton.setImageResource(R.mipmap.avatar);

                        break;

                    case R.id.change_picture:

                        openGallery();

                        break;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private void dismissDialog(ProgressDialog dialog) {
        try{
            dialog.dismiss();
        } catch (final IllegalArgumentException e) {

        } catch (final Exception e) {

        } finally {
            dialog = null;
        }
    }

    private void dismissWhenNeeded(ProgressDialog dialog) {
        if (dialog != null) {
            if (dialog.isShowing()) {
                Context context = ((ContextWrapper)dialog.getContext()).getBaseContext();
                if (context instanceof Activity) {
                    // API level 17
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
                        if (!((Activity)context).isFinishing() && !((Activity)context).isDestroyed()) {
                            dismissDialog(dialog);
                        }

                    } else {
                        // API less 17
                        if (!((Activity)context).isFinishing()) {
                            dismissDialog(dialog);
                        }
                    }

                } else {
                    dismissDialog(dialog);
                }
            }
            dialog = null;
        }
    }

}
