package com.example.mobileappproject;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobileappproject.models.User;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.getstream.chat.android.client.ChatClient;
import io.getstream.chat.android.client.logger.ChatLogLevel;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class registerActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    FirebaseFirestore db;
    Button btnRegister;
    Button btnLogin;
    AutocompleteSupportFragment autocompleteSupportFragment;
    User user = User.builder().build();
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        btnRegister = findViewById(R.id.submitButton);
        btnLogin = findViewById(R.id.loginButton);

        // Access the shared preferences to set credentials
        sharedPref = getBaseContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        // Init the autocomplete places search
        autocompleteSupportFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Init the places autocomplete
        if(!Places.isInitialized()){
            Places.initialize(getApplicationContext(), getString(R.string.API_KEY), Locale.CANADA);
        }

        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        // init firebase auth & db
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();



        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // Used to get the lat and lang of place selected
                LatLng placeLatLng = place.getLatLng();
                user.setLocation(placeLatLng);
            }

            @Override
            public void onError(@NonNull Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerUser();
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginIntent = new Intent(getBaseContext(), loginActivity.class);
                startActivity(loginIntent);
            }
        });

    }

    private void registerUser(){
        EditText nameEt = findViewById(R.id.editTextName);
        EditText emailEt = findViewById(R.id.editTextEmail);
        EditText passwordEt = findViewById(R.id.editTextPassword);

        String name = nameEt.getText().toString();
        String email = emailEt.getText().toString();
        String password = passwordEt.getText().toString();

        if(name.isEmpty()  || email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Please fill in all fields to register!", Toast.LENGTH_LONG).show();
            return;
        }

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email.trim(), password.trim()).addOnCompleteListener( this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d( TAG, "createUserWithEmail:success" );
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    assert firebaseUser != null;
                    String userID = firebaseUser.getUid();
                    user.setName(name);
                    Toast.makeText(registerActivity.this, "Authentication success!", Toast.LENGTH_SHORT).show();

                    // Create userDoc using method
                    createUserDoc(user, userID);

                    // Send current userID to hub
                    Intent hubIntent = new Intent(getBaseContext(), hubActivity.class);

                    // Initialize the chat client
                    ChatClient client = new ChatClient.Builder("jvct995yfavt", getApplicationContext())
                            .logLevel(ChatLogLevel.ALL) // Set to NOTHING in prod
                            .build();

                    String jwtToken = Jwts.builder().claim("user_id", userID).signWith(SignatureAlgorithm.HS256, "secret".getBytes()).compact();

                    io.getstream.chat.android.client.models.User user = new io.getstream.chat.android.client.models.User();
                    user.setId(userID);
                    user.setName(name);
                    user.setImage("https://mshanken.imgix.net/cao/bolt/2019-09/devito-2-1600.jpg");

                    client.connectUser(
                            user,
                            jwtToken
                    ).enqueue();

                    editor.putString("userID", userID);
                    editor.putString("name", name);
                    editor.commit();
                    startActivity(hubIntent);

                } else {
                    Log.w(TAG, "createUserWithEmail:failed");
                    Toast.makeText(registerActivity.this, "Authentication failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


    // Create method that takes in user ID and sends intent to hub activity with the userid to determine the user with firestore
    private void createUserDoc(User user, String uid){
        CollectionReference userCollection = db.collection("users");

        Map<String, Object> userRef = new HashMap<>();
        userRef.put("name", user.getName());
        userRef.put("latitude", user.getLocation().latitude);
        userRef.put("longitude", user.getLocation().longitude);

        userCollection.document(uid).set(userRef);
    }
}
