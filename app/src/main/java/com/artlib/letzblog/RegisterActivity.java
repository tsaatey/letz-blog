package com.artlib.letzblog;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText personName;
    private EditText email;
    private EditText password;
    private Button signUpButton;
    private ProgressDialog progressDialog;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        firebaseAuth = FirebaseAuth.getInstance();

        personName = (EditText) findViewById(R.id.txt_person_name);
        email = (EditText) findViewById(R.id.txt_person_email);
        password = (EditText) findViewById(R.id.txt_person_password);
        signUpButton = (Button) findViewById(R.id.register_button);
        progressDialog = new ProgressDialog(this);

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void registerUser() {
        final String name = personName.getText().toString().trim();
        final String userMail = email.getText().toString().trim();
        final String uPassword = password.getText().toString();

        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(userMail) && !TextUtils.isEmpty(uPassword)) {
            progressDialog.setMessage("Signing up, please wait...");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();

            firebaseAuth.createUserWithEmailAndPassword(userMail, uPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        String userId = firebaseAuth.getCurrentUser().getUid();
                        DatabaseReference currentUser = databaseReference.child(userId);
                        currentUser.child("name").setValue(name);
                        progressDialog.dismiss();

                        Intent setupIntent = new Intent(RegisterActivity.this, SetupActivity.class);
                        setupIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(setupIntent);
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Error creating account", Toast.LENGTH_LONG).show();
                    }

                }
            });
        }
    }
}
