package com.example.mobileappproject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobileappproject.databinding.ActivityChannelBinding;
import com.getstream.sdk.chat.viewmodel.MessageInputViewModel;
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel;
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Mode.Normal;
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Mode.Thread;
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.State.NavigateUp;

import io.getstream.chat.android.client.ChatClient;
import io.getstream.chat.android.client.extensions.FlowExtensions;
import io.getstream.chat.android.client.logger.ChatLogLevel;
import io.getstream.chat.android.client.models.Channel;
import io.getstream.chat.android.client.models.Message;
import io.getstream.chat.android.client.models.User;
import io.getstream.chat.android.offline.extensions.ChatClientExtensions;
import io.getstream.chat.android.offline.model.message.attachments.UploadAttachmentsNetworkType;
import io.getstream.chat.android.offline.plugin.configuration.Config;
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory;
import io.getstream.chat.android.offline.plugin.state.channel.ChannelState;
import io.getstream.chat.android.ui.message.input.viewmodel.MessageInputViewModelBinding;
import io.getstream.chat.android.ui.message.list.header.MessageListHeaderView;
import io.getstream.chat.android.ui.message.list.header.viewmodel.MessageListHeaderViewModel;
import io.getstream.chat.android.ui.message.list.header.viewmodel.MessageListHeaderViewModelBinding;
import io.getstream.chat.android.ui.message.list.viewmodel.MessageListViewModelBinding;
import io.getstream.chat.android.ui.message.list.viewmodel.factory.MessageListViewModelFactory;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import kotlinx.coroutines.flow.StateFlow;


public class ChannelActivity extends AppCompatActivity {

    private final static String CID_KEY = "key:cid";

    public static Intent newIntent(Context context, Channel channel, String name, String userID) {
        final Intent intent = new Intent(context, ChannelActivity.class);
        intent.putExtra(CID_KEY, channel.getCid());
        intent.putExtra("userID", userID);
        intent.putExtra("name", name);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent userIntent = getIntent();
        String userID = userIntent.getStringExtra("userID");
        String name = userIntent.getStringExtra("name");

        ActivityChannelBinding binding = ActivityChannelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setting up chat configuration with API
        Config config = new Config(true, true, true, UploadAttachmentsNetworkType.NOT_ROAMING);
        StreamOfflinePluginFactory offlinePluginFactory = new StreamOfflinePluginFactory(config, getBaseContext());

        ChatClient client = new ChatClient.Builder("jvct995yfavt", getBaseContext())
                .logLevel(ChatLogLevel.ALL)
                .withPlugin(offlinePluginFactory)
                .build();

        User user = new User();
        user.setId(userID);
        user.setName(name);

        String jwtToken = Jwts.builder().claim("user_id", userID).signWith(SignatureAlgorithm.HS256, "secret".getBytes()).compact();

        client.connectUser(user, jwtToken).enqueue((result) -> {
            if(result.isSuccess()){
                Toast.makeText(this, "Connected user!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Couldn't connect user!", Toast.LENGTH_LONG).show();
            }
        });

        // Watching a channel's state using the offline library
        StateFlow<ChannelState> channelStateFlow = ChatClientExtensions.watchChannelAsState(client, "messaging:TestChannel", 0);
        LiveData<ChannelState> channelStateLiveData = FlowExtensions.asLiveData(channelStateFlow);

        channelStateLiveData.observe(this, channelState -> {
            if (channelState != null) {
                // StateFlow objects to observe. Use FlowExtensions.asLiveData(stateFlow); to LiveData conversion.
                channelState.getMessages();
                channelState.getReads();
                channelState.getTyping();
            } else {
                // User not connected yet.
            }
        });

        ViewModelProvider.Factory factory = new MessageListViewModelFactory.Builder()
                .cid(userIntent.getStringExtra("key:cid"))
                .build();

        ViewModelProvider provider = new ViewModelProvider(this, factory);
        MessageListHeaderViewModel messageListHeaderViewModel = provider.get(MessageListHeaderViewModel.class);
        MessageListViewModel messageListViewModel = provider.get(MessageListViewModel.class);
        MessageInputViewModel messageInputViewModel = provider.get(MessageInputViewModel.class);

        MessageListHeaderViewModelBinding.bind(messageListHeaderViewModel, binding.messageListHeaderView, this);
        MessageListViewModelBinding.bind(messageListViewModel, binding.messageListView, this, true);
        MessageInputViewModelBinding.bind(messageInputViewModel, binding.messageInputView, this);

        // Step 3 - Let both MessageListHeaderView and MessageInputView know when we open a thread
        messageListViewModel.getMode().observe(this, mode -> {
            if (mode instanceof Thread) {
                Message parentMessage = ((Thread) mode).getParentMessage();
                messageListHeaderViewModel.setActiveThread(parentMessage);
                messageInputViewModel.setActiveThread(parentMessage);
            } else if (mode instanceof Normal) {
                messageListHeaderViewModel.resetThread();
                messageInputViewModel.resetThread();
            }
        });

        // Step 4 - Let the message input know when we are editing a message
        binding.messageListView.setMessageEditHandler(messageInputViewModel::postMessageToEdit);

        // Step 5 - Handle navigate up state
        messageListViewModel.getState().observe(this, state -> {
            if (state instanceof NavigateUp) {
                finish();
            }
        });

        // Step 6 - Handle back button behaviour correctly when you're in a thread
        MessageListHeaderView.OnClickListener backHandler = () -> {
            messageListViewModel.onEvent(MessageListViewModel.Event.BackButtonPressed.INSTANCE);
        };

        binding.messageListHeaderView.setBackButtonClickListener(backHandler);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backHandler.onClick();
            }
        });

    }
}
