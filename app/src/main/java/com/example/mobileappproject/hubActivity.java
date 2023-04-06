package com.example.mobileappproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class hubActivity extends AppCompatActivity{

    FirebaseAuth mAuth;
    TextView userNameTV;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);

        sharedPref = getBaseContext().getSharedPreferences("user", Context.MODE_PRIVATE);

        Button chatsButton = (Button) findViewById(R.id.chats_button);
        Button eventButton = (Button) findViewById(R.id.event_button);

        mAuth = FirebaseAuth.getInstance();
        userNameTV = findViewById(R.id.hub_welcome_user);

        FirebaseUser user = mAuth.getCurrentUser();

        String userID = sharedPref.getString("userID", "");
        String userName = sharedPref.getString("name", "");

        // Return user to register if the userID is not present once in the hub
        if(userID.isEmpty()){
            Intent retIntent = new Intent(getBaseContext(), registerActivity.class);
            startActivity(retIntent);
        }

        userNameTV.append(" " + userName);

        chatsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chatPage = new Intent(hubActivity.this, MainActivity.class);
                startActivity(chatPage);
            }
        });

        eventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent eventPage = new Intent(hubActivity.this, eventActivity.class);
                startActivity(eventPage);
            }
        });

    }

}