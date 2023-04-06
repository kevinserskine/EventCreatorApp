package com.example.mobileappproject;

import static com.android.volley.VolleyLog.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import io.getstream.chat.android.client.ChatClient;
import io.getstream.chat.android.client.logger.ChatLogLevel;
import io.getstream.chat.android.client.models.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class loginActivity extends AppCompatActivity {
    EditText emailET,passET;
    Button loginButton, registerButton;
    FirebaseAuth mAuth;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        sharedPref = getBaseContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        emailET = findViewById(R.id.editTextEmail);
        passET = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Button to login
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailET.getText().toString();
                String pass = passET.getText().toString();
                if(email.isEmpty() || pass.isEmpty()){
                    Toast.makeText(getBaseContext(), "Please fill in all fields to login!", Toast.LENGTH_LONG).show();
                }else{
                    loginUser(email, pass);
                }

            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registerIntent = new Intent(getBaseContext(), registerActivity.class);
                startActivity(registerIntent);
            }
        });

    }

    private void loginUser(String email, String pass){
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(getBaseContext(), "Successful login!", Toast.LENGTH_LONG).show();

                            // Gets current user
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            assert firebaseUser != null;

                            // Gets username to apply to preferences
                            DocumentReference userDoc = db.collection("users").document(firebaseUser.getUid());
                            userDoc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@lombok.NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document.exists()) {
                                            String name = document.getData().get("name").toString();
                                            String userID = firebaseUser.getUid();

                                            editor.putString("name", name);
                                            editor.putString("userID", userID);
                                            editor.commit();

                                            // Initialize the chat client
                                            ChatClient client = new ChatClient.Builder("jvct995yfavt", getApplicationContext())
                                                    .logLevel(ChatLogLevel.ALL) // Set to NOTHING in prod
                                                    .build();

                                            String jwtToken = Jwts.builder().claim("user_id", userID).signWith(SignatureAlgorithm.HS256, "secret".getBytes()).compact();

                                            User user = new User();
                                            user.setId(userID);
                                            user.setName(name);
                                            user.setImage("https://mshanken.imgix.net/cao/bolt/2019-09/devito-2-1600.jpg");

                                            client.connectUser(
                                                    user,
                                                    jwtToken
                                            ).enqueue();

                                        } else {
                                            Log.d(TAG, "No such document");
                                        }
                                    } else {
                                        Log.d(TAG, "get failed with ", task.getException());
                                    }

                                }
                            });

                            // Send user to hub activity
                            Intent hubIntent = new Intent(getBaseContext(), hubActivity.class);
                            startActivity(hubIntent);

                        }else{
                            Toast.makeText(getBaseContext(), "Unsuccessful login!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

}