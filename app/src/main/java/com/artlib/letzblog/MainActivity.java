package com.artlib.letzblog;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mBlogList;
    private DatabaseReference databaseReference;
    private DatabaseReference userLikeDatabase;
    private DatabaseReference userProfileDatabaseReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    private UserLocalDataStore userLocalDataStore;

    private boolean processLike = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBlogList = (RecyclerView) findViewById(R.id.blog_list);
        mBlogList.setHasFixedSize(true);
        mBlogList.setLayoutManager(new LinearLayoutManager(this));
        firebaseAuth = FirebaseAuth.getInstance();

        authStateListener = new FirebaseAuth.AuthStateListener(){
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser()== null) {
                    Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(loginIntent);
                }
            }
        };

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Blog");
        databaseReference.keepSynced(true);

        userProfileDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Profile_Settings");
        userProfileDatabaseReference.keepSynced(true);

        userLikeDatabase = FirebaseDatabase.getInstance().getReference().child("Likes");
        userLikeDatabase.keepSynced(true);

        userLocalDataStore = new UserLocalDataStore(this);

        checkUserExist();


    }

    @Override
    protected void onStart() {
        super.onStart();

        firebaseAuth.addAuthStateListener(authStateListener);

        FirebaseRecyclerAdapter<Blog, BlogViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Blog, BlogViewHolder>(
                Blog.class,
                R.layout.blog_row,
                BlogViewHolder.class,
                databaseReference
        ) {
            @Override
            protected void populateViewHolder(final BlogViewHolder viewHolder, final Blog model, int position) {
                // Get id of post
                final String postKey = getRef(position).getKey();

                // Set post details to RecyclerView
                viewHolder.setBlogTitle(model.getBlogTitle());
                viewHolder.setBlogDescription(model.getBlogDescription());
                viewHolder.setBlogImage(getApplicationContext(), model.getBlogImage());
                viewHolder.setProfileImage(getApplicationContext(), model.getProfileImage());
                viewHolder.setBlogDate(model.getBlogDate());
                viewHolder.setProfileUsername(model.getProfileUsername());

                viewHolder.setLikeButtonIcon(postKey);

                viewHolder.hide_show_more_button(postKey);

//                viewHolder.setLikeCount(postKey);

                // Set OnclickListener for RecyclerView
                viewHolder.view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });

                viewHolder.likeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        processLike = true;

                        userLikeDatabase.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (processLike) {

                                    if (dataSnapshot.child(postKey).hasChild(firebaseAuth.getCurrentUser().getUid())) {

                                        userLikeDatabase.child(postKey).child(firebaseAuth.getCurrentUser().getUid()).removeValue();
                                        processLike = false;

                                    } else {

                                        userLikeDatabase.child(postKey).child(firebaseAuth.getCurrentUser().getUid()).setValue(model.getProfileUsername());
                                        processLike = false;
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                });

                viewHolder.commentButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, "This feature is not implemented", Toast.LENGTH_LONG).show();

                    }
                });

                viewHolder.shareButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, "This feature is not implemented", Toast.LENGTH_LONG).show();
                    }
                });

                viewHolder.more_image_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popupMenu = new PopupMenu(v.getContext(), viewHolder.more_image_button);
                        popupMenu.inflate(R.menu.more_menu);

                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case R.id.update_post:
                                        userLocalDataStore.storePostKey(postKey);
                                        startActivity(new Intent(MainActivity.this, UpdatePostActivity.class));

                                        break;

                                    case R.id.delete_post:
                                        final DatabaseReference imageReference = FirebaseDatabase.getInstance().getReference().child("Blog");
                                        final String[] imageUrl = new String[1];

                                        imageReference.child(postKey).child("blogImage").addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                imageUrl[0] = (String) dataSnapshot.getValue();
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });

                                        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                                        alert.setMessage("Delete this post?")
                                                .setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {

                                                        final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                                                        progressDialog.setMessage("Deleting post, please wait...");
                                                        progressDialog.setIndeterminate(true);
                                                        progressDialog.setCancelable(false);
                                                        progressDialog.show();

                                                        StorageReference deleteImage = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl[0]);
                                                        deleteImage.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                databaseReference.child(postKey).removeValue();
                                                                userLikeDatabase.child(postKey).removeValue();
                                                                progressDialog.dismiss();
                                                            }
                                                        }).addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                progressDialog.dismiss();
                                                                Toast.makeText(MainActivity.this, "Error"+e.getMessage(), Toast.LENGTH_LONG).show();
                                                            }
                                                        });
                                                    }

                                                })
                                                .create()
                                                .show();

                                        break;
                                }
                                return false;
                            }
                        });
                        popupMenu.show();
                    }
                });

            }
        };

        mBlogList.setAdapter(firebaseRecyclerAdapter);

    }

    private void checkUserExist() {
        if  (firebaseAuth.getCurrentUser() != null) {
            final String userId = firebaseAuth.getCurrentUser().getUid();
            userProfileDatabaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.hasChild(userId)) {
                        Intent setupIntent = new Intent(MainActivity.this, SetupActivity.class);
                        setupIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(setupIntent);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_menu:
                startActivity(new Intent(MainActivity.this, PostActivity.class));
                break;
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, ProfileSettingsActivity.class));
                break;
            case R.id.action_logout:
                logOut();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // BlogViewHolder class
    public static class BlogViewHolder extends RecyclerView.ViewHolder {
        View view;
        ImageButton likeButton;
        ImageButton commentButton;
        ImageButton shareButton;

        DatabaseReference likesDatabaseReference;
        FirebaseAuth firebaseAuth;
        DatabaseReference blogIdDatabaseReference;

        ImageButton more_image_button;

        public BlogViewHolder(View itemView) {
            super(itemView);
            view = itemView;

            likeButton = (ImageButton)view.findViewById(R.id.like_button);

            likesDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Likes");
            likesDatabaseReference.keepSynced(true);
            firebaseAuth = FirebaseAuth.getInstance();

            blogIdDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Blog");
            blogIdDatabaseReference.keepSynced(true);

            more_image_button = (ImageButton)view.findViewById(R.id.more_image_button);

            commentButton = (ImageButton)view.findViewById(R.id.comment_button);
            shareButton = (ImageButton)view.findViewById(R.id.share_button);

        }

        public void setBlogTitle(String title) {
            TextView post_title = (TextView)view.findViewById(R.id.post_title);
            post_title.setText(title);
        }

        public void setBlogDescription(String description){
            TextView post_description = (TextView)view.findViewById(R.id.post_description);
            post_description.setText(description);
        }

        public void setBlogImage(final Context context, final String image) {
            final ImageView post_image = (ImageView)view.findViewById(R.id.post_image);
            Picasso.with(context).load(image).into(post_image);

        }

        public void setBlogDate (String postDate) {
            TextView postDateView = (TextView)view.findViewById(R.id.post_date);
            postDateView.setText(postDate);
        }

        public void setProfileUsername(String username) {
            TextView usernameView = (TextView)view.findViewById(R.id.username);
            usernameView.setText(username);
        }

        public void setProfileImage (final Context context, String profileImage) {
            CircleImageView profileImageView = (CircleImageView)view.findViewById(R.id.profile_display);
            Picasso.with(context).load(profileImage).placeholder(R.mipmap.avatar).into(profileImageView);
        }

        public void setLikeButtonIcon(final String postKey) {
            final TextView likeTextView = (TextView)view.findViewById(R.id.like_text_view);
            likesDatabaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (firebaseAuth.getCurrentUser() != null) {
                        if (dataSnapshot.child(postKey).hasChild(firebaseAuth.getCurrentUser().getUid())) {
                            likeButton.setImageResource(R.mipmap.ic_thumb_up_black_24dp);
                            likeTextView.setText("Liked");
                            likeTextView.setTypeface(Typeface.DEFAULT_BOLD);
                        } else {
                            likeButton.setImageResource(R.mipmap.ic_thumb_up_gray_24dp);
                            likeTextView.setTypeface(Typeface.DEFAULT);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        public void setLikeCount(final String postKey) {
            final TextView likeTextView = (TextView)view.findViewById(R.id.like_text_view);
            likesDatabaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    long numberOfLikes = dataSnapshot.child(postKey).getChildrenCount();

                    if (numberOfLikes == 0) {
                        likeTextView.setText("");
                    } else if (numberOfLikes == 1) {
                        likeTextView.setText(numberOfLikes +" Like");
                    } else if (numberOfLikes > 1) {
                        likeTextView.setText(numberOfLikes +" Likes");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        public void hide_show_more_button(String key) {
            final ImageButton imageButton = (ImageButton)view.findViewById(R.id.more_image_button);

            blogIdDatabaseReference.child(key).child("userId").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (firebaseAuth.getCurrentUser() != null) {
                        String userId = (String)dataSnapshot.getValue();

                        if (!TextUtils.isEmpty(userId)) {
                            if (userId.equals(firebaseAuth.getCurrentUser().getUid())) {
                                imageButton.setVisibility(View.VISIBLE);

                            } else {
                                imageButton.setVisibility(View.INVISIBLE);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }

    }

    private void logOut() {
        firebaseAuth.signOut();
        userLocalDataStore.clearUserData();
    }


}
