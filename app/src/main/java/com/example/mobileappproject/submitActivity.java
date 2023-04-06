package com.example.mobileappproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.getstream.chat.android.client.ChatClient;
import io.getstream.chat.android.client.channel.ChannelClient;
import io.getstream.chat.android.client.logger.ChatLogLevel;
import io.getstream.chat.android.client.models.Channel;
import io.getstream.chat.android.client.models.User;
import io.getstream.chat.android.offline.model.message.attachments.UploadAttachmentsNetworkType;
import io.getstream.chat.android.offline.plugin.configuration.Config;
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class submitActivity extends AppCompatActivity {

    TextView event_title, dateTime, location, num_friends, friends_invited;
    String friendsList;

    Button confirm;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit);
        Intent intent = getIntent();

        sharedPref = getBaseContext().getSharedPreferences("user", Context.MODE_PRIVATE);

        event_title = (TextView) findViewById(R.id.event_title);
        num_friends = (TextView) findViewById(R.id.num_friends);
        location = (TextView) findViewById(R.id.location);
        dateTime = (TextView) findViewById(R.id.dateTime);
        friends_invited = (TextView) findViewById(R.id.friends_invited);
        confirm = (Button) findViewById(R.id.confirm);

        int number_friends = intent.getIntExtra("keyFriend", 0);
        int length = intent.getIntExtra("keyLength", 0);

        String e_title = ("Event Title: " + (intent.getStringExtra("KeyNAME")));
        String date_time = ("Date and Time: " + (intent.getStringExtra("keyDATE")));
        String loc = ("Location: " + (intent.getStringExtra("keySearch")));
        String numf = ("Number of Friends Invited: " + (number_friends));
        String[] n_friends = intent.getStringArrayExtra("keyChosen");

        StringBuilder friendsList = new StringBuilder();

        friendsList.append("Friends Invited: ");

        for(int i = 0; i < number_friends; i++){
            friendsList.append(n_friends[i]).append(", ");
        }

        event_title.setText(e_title);
        dateTime.setText(date_time);
        location.setText(loc);
        num_friends.setText(numf);
        friends_invited.setText(friendsList);

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent return_hub = new Intent(submitActivity.this, hubActivity.class);
                startActivity(return_hub);
            }
        });

        String userID = sharedPref.getString("userID", "");
        String name = sharedPref.getString("name", "");

        // Setting up chat configuration with API
        Config config = new Config(true, true, true, UploadAttachmentsNetworkType.NOT_ROAMING);
        StreamOfflinePluginFactory streamOfflinePluginFactory = new StreamOfflinePluginFactory(config, getApplicationContext());

        String jwtToken = Jwts.builder().claim("user_id", userID).signWith(SignatureAlgorithm.HS256, "secret".getBytes()).compact();

        ChatClient client = new ChatClient.Builder("jvct995yfavt", getApplicationContext())
                .withPlugin(streamOfflinePluginFactory)
                .logLevel(ChatLogLevel.ALL) // Set to NOTHING in prod
                .build();

        // Step 3 - Authenticate and connect the user
        User user = new User();
        user.setId(userID);
        user.setName(name);
        user.setImage("https://mshanken.imgix.net/cao/bolt/2019-09/devito-2-1600.jpg");

        client.connectUser(
                user,
                jwtToken
        ).enqueue();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // sets the channel name to whatever the event name is and removes all spaces as required for a channel id
        ChannelClient channelClient = client.channel("messaging", getIntent().getStringExtra("KeyNAME").replaceAll("\\s+", ""));

        Map<String, Object> extraData = new HashMap<>();
        List<String> chosenMembers = intent.getStringArrayListExtra("selectedUserIDs");
        List<String> memberIds = new ArrayList<String>(chosenMembers);
        extraData.put("members", memberIds);
        extraData.put("name", intent.getStringExtra("KeyNAME"));

        channelClient.create(memberIds, extraData).enqueue((result) -> {
            if (result.isSuccess()) {
                Channel channel = result.data();

                // Use channel by calling methods on channelClient
            } else {
                // Handle result.error()
                System.out.println("ERROR:" + result.error());
            }
        });

    }
}