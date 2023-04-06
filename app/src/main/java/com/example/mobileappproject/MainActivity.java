package com.example.mobileappproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobileappproject.databinding.ActivityMainBinding;

import org.jetbrains.annotations.Nullable;

import io.getstream.chat.android.client.ChatClient;
import io.getstream.chat.android.client.logger.ChatLogLevel;
import io.getstream.chat.android.client.models.User;
import io.getstream.chat.android.offline.model.message.attachments.UploadAttachmentsNetworkType;
import io.getstream.chat.android.offline.plugin.configuration.Config;
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory;
import io.getstream.chat.android.ui.channel.list.viewmodel.ChannelListViewModel;
import io.getstream.chat.android.ui.channel.list.viewmodel.ChannelListViewModelBinding;
import io.getstream.chat.android.ui.channel.list.viewmodel.factory.ChannelListViewModelFactory;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public final class MainActivity extends AppCompatActivity {

    SharedPreferences sharedPref;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = getBaseContext().getSharedPreferences("user", Context.MODE_PRIVATE);

        String userID = sharedPref.getString("userID", "");
        String name = sharedPref.getString("name", "");

        String jwtToken = Jwts.builder()
                .claim("user_id", userID)
                .signWith(SignatureAlgorithm.HS256, "secret".getBytes())
                .compact();

        // Step 0 - inflate binding
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Step 1 - Set up the OfflinePlugin for offline storage
        StreamOfflinePluginFactory streamOfflinePluginFactory = new StreamOfflinePluginFactory(
                new Config(
                        true,
                        true,
                        true,
                        UploadAttachmentsNetworkType.NOT_ROAMING
                ),
                getApplicationContext()
        );

        // Step 2 - Set up the client for API calls with the plugin for offline storage
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

        ViewModelProvider.Factory factory = new ChannelListViewModelFactory.Builder()
                .sort(ChannelListViewModel.DEFAULT_SORT)
                .build();

        ChannelListViewModel channelsViewModel =
                new ViewModelProvider(this, factory).get(ChannelListViewModel.class);

        // Step 5 - Connect the ChannelListViewModel to the ChannelListView, loose
        //          coupling makes it easy to customize
        ChannelListViewModelBinding.bind(channelsViewModel, binding.channelListView, this);
        binding.channelListView.setChannelItemClickListener(
                channel -> startActivity(ChannelActivity.newIntent(this, channel, name, userID))
        );
    }
}
