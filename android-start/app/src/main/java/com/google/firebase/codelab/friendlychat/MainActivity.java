package com.google.firebase.codelab.friendlychat;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;
import com.google.firebase.appindexing.builders.PersonBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/


public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener {

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView messengerTextView;
        CircleImageView messengerImageView;
        ImageView playAudioImageView;
        TextView incomingMessageTextView;
        ImageView incomingMessageImageView;
        TextView incomingMessengerTextView;
        CircleImageView incomingMessengerImageView;
        ImageView incomingPlayAudioImageView;
        LinearLayout outgoingLinearLayout;
        LinearLayout incomingLinearLayout;


        public MessageViewHolder(View v) {
            super(v);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
            messengerTextView = itemView.findViewById(R.id.messengerTextView);
            messengerImageView = itemView.findViewById(R.id.messengerImageView);
            playAudioImageView = itemView.findViewById(R.id.playAudioImageView);
            outgoingLinearLayout = itemView.findViewById(R.id.outgoing_model);
            incomingLinearLayout = itemView.findViewById(R.id.incoming_model);
            incomingMessageTextView = itemView.findViewById(R.id.messageTextView_incoming);
            incomingMessageImageView = itemView.findViewById(R.id.messageImageView_incoming);
            incomingMessengerTextView = itemView.findViewById(R.id.messengerTextView_incoming);
            incomingMessengerImageView = itemView.findViewById(R.id.messengerImageView_incoming);
            incomingPlayAudioImageView = itemView.findViewById(R.id.playAudioImageView_incoming);

        }
    }


    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "messages";
    private static final int REQUEST_INVITE = 1;
    private static final int REQUEST_IMAGE = 2;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 10;
    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    public String mUsername;
    private String avatarUrl;
    private SharedPreferences mSharedPreferences;
    private GoogleApiClient mGoogleApiClient;
    private static final String MESSAGE_URL = "http://friendlychat.firebase.google.com/message/";

    private ImageButton mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    private ImageView mAddMessageImageView;
    private ImageView recordAudioView;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;
    private DatabaseReference audioDatabaseReference;
    private FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>
            mFirebaseAdapter;
    // Firebase instance variables
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseAnalytics mFirebaseAnalytics;
    private AdView mAdView;
    // Record Audio Variables
    private MediaRecorder mediaRecorder;
    private String fileName = null;
    private Vibrator vibrator;
    private boolean workInProgress;
    private ProgressDialog progressDialog;
    private MediaPlayer mediaPlayer;
    private boolean isPreparing;
    private boolean isPlaying;


    private void configRemoteMsgLength() {
        // Initialize Firebase Remote Config.
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        // Define Firebase Remote Config Settings.
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setDeveloperModeEnabled(true)
                        .build();

        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put("friendly_msg_length", 10L);

        // Apply config settings and default values.
        mFirebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);

        // Fetch remote config.
        fetchConfig();
    }

    // Fetch the config to determine the allowed length of messages.
    public void fetchConfig() {
        long cacheExpiration = 3600; // 1 hour in seconds
        // If developer mode is enabled reduce cacheExpiration to 0 so that
        // each fetch goes to the server. This should not be used in release
        // builds.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings()
                .isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Make the fetched config available via
                        // FirebaseRemoteConfig get<type> calls.
                        mFirebaseRemoteConfig.activateFetched();
                        applyRetrievedLengthLimit();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // There has been an error fetching the config
                        Log.w(TAG, "Error fetching config: " +
                                e.getMessage());
                        applyRetrievedLengthLimit();
                    }
                });
    }

    /**
     * Apply retrieved length limit to edit text field.
     * This result may be fresh from the server or it may be from cached
     * values.
     */
    private void applyRetrievedLengthLimit() {
        Long friendly_msg_length =
                mFirebaseRemoteConfig.getLong("friendly_msg_length");
        mMessageEditText.setFilters(new InputFilter[]{new
                InputFilter.LengthFilter(friendly_msg_length.intValue())});
        Log.d(TAG, "FML is: " + friendly_msg_length);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.background));
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Set default username is anonymous.
        mUsername = ANONYMOUS;
        initializeFireBaseAuth();

        initiateGoogleApiClient();


        // Initialize ProgressBar and RecyclerView.
        mProgressBar = findViewById(R.id.progressBar);
        mMessageRecyclerView = findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        setupDataBaseReference();


        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        mMessageEditText = findViewById(R.id.messageEditText);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(CodelabPreferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FriendlyMessage friendlyMessage = new
                        FriendlyMessage(mMessageEditText.getText().toString(),
                        mUsername,
                        avatarUrl,
                        null /* no image */, null);
                mFirebaseDatabaseReference.child(MESSAGES_CHILD)
                        .push().setValue(friendlyMessage);
                mMessageEditText.setText("");

            }
        });

        mAddMessageImageView = findViewById(R.id.addMessageImageView);

        mAddMessageImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_IMAGE);

            }
        });
        configRemoteMsgLength();
        setupAdView();

        /*SETTING UP AUDIO FEATURES*/
        recordAudioView = findViewById(R.id.recordAudioView);
        fileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        fileName = fileName + "/recorded_audio.mp3";
        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        setupAudioRecordBtn();
//        storageReference = FirebaseStorage.getInstance().getReference();
        progressDialog = new ProgressDialog(this);
        isPreparing = true;


    }

    private void setupDataBaseReference() {
        // New child entries
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        audioDatabaseReference = FirebaseDatabase.getInstance().getReference();
        SnapshotParser<FriendlyMessage> parser = new SnapshotParser<FriendlyMessage>() {
            @Override
            public FriendlyMessage parseSnapshot(DataSnapshot dataSnapshot) {
                FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                if (friendlyMessage != null) {
                    friendlyMessage.setId(dataSnapshot.getKey());
                }
                return friendlyMessage;
            }
        };

        DatabaseReference messagesRef = mFirebaseDatabaseReference.child(MESSAGES_CHILD);
        FirebaseRecyclerOptions<FriendlyMessage> options =
                new FirebaseRecyclerOptions.Builder<FriendlyMessage>()
                        .setQuery(messagesRef, parser)
                        .build();
        mFirebaseAdapter = new FirebaseRecyclerAdapter<FriendlyMessage, MessageViewHolder>(options) {
            @Override
            public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new MessageViewHolder(inflater.inflate(R.layout.item_message, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(final MessageViewHolder viewHolder,
                                            int position,
                                            final FriendlyMessage friendlyMessage) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);

//                CHECKS WHETHER THE MESSAGE IS OUTGOING OR INCOMING

                if (mUsername.equals(friendlyMessage.getName())) {

//                    THIS MESSAGE IS OUTGOING

                    viewHolder.outgoingLinearLayout.setVisibility(View.VISIBLE);
                    viewHolder.incomingLinearLayout.setVisibility(View.GONE);
                    setUpOutgoingMessageViewHolder(friendlyMessage, viewHolder);

                    viewHolder.messengerTextView.setText(friendlyMessage.getName());
                    if (friendlyMessage.getAvatarUrl() == null) {
                        viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,
                                R.drawable.ic_account_circle_black_36dp));
                    } else {
                        Glide.with(MainActivity.this)
                                .load(friendlyMessage.getAvatarUrl())
                                .into(viewHolder.messengerImageView);
                    }

                } else {

//                    THIS MESSAGE IS INCOMING

                    viewHolder.outgoingLinearLayout.setVisibility(View.GONE);
                    viewHolder.incomingLinearLayout.setVisibility(View.VISIBLE);
                    setUpOutgoingIncomingMessageViewHolder(friendlyMessage, viewHolder);

                    viewHolder.incomingMessengerTextView.setText(friendlyMessage.getName());
                    if (friendlyMessage.getAvatarUrl() == null) {
                        viewHolder.incomingMessengerImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,
                                R.drawable.ic_account_circle_black_36dp));
                    } else {
                        Glide.with(MainActivity.this)
                                .load(friendlyMessage.getAvatarUrl())
                                .into(viewHolder.incomingMessengerImageView);}




                }
                if (friendlyMessage.getText() != null) {
                    // write this message to the on-device index
                    FirebaseAppIndex.getInstance()
                            .update(getMessageIndexable(friendlyMessage));
                }
                // log a view action on it
                FirebaseUserActions.getInstance().end(getMessageViewAction(friendlyMessage));

            }
        };
    }

    private void initializeFireBaseAuth() {
        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            if (mFirebaseUser.getPhotoUrl() != null) {
                avatarUrl = mFirebaseUser.getPhotoUrl().toString();
            }
        }
    }

    private void initiateGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();
    }

    private void sendInvitation() {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

    private void setupAdView() {
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void startRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(fileName);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);


        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.e("thisisatag", "prepare() failed");
        }

        mediaRecorder.start();
    }

    private void stopRecording() {
        mediaRecorder.release();
        mediaRecorder = null;
        workInProgress = false;
        uploadAudio();


    }

    private void uploadAudio() {
//        progressDialog.setMessage("Uploading Audio ..");
//        progressDialog.show();
//        StorageReference filePath = storageReference.child("Audio").child("new_audio.3gp");
        final Uri uri = Uri.fromFile(new File(fileName));

        /********TEMP CODE*********/
        FriendlyMessage tempMessage = new FriendlyMessage(null, mUsername, avatarUrl,
                null, null);
        audioDatabaseReference.child(MESSAGES_CHILD).push()
                .setValue(tempMessage, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError,
                                           DatabaseReference databaseReference) {
                        if (databaseError == null) {
                            String key = databaseReference.getKey();
                            StorageReference storageReference =
                                    FirebaseStorage.getInstance()
                                            .getReference(mFirebaseUser.getUid())
                                            .child(key)
                                            .child(uri.getLastPathSegment());

                            putAudioInStorage(storageReference, uri, key);
                        } else {
                            Log.w("thisisatag", "Unable to write message to database.",
                                    databaseError.toException());
                        }
                    }
                });

        /***********/

//        filePath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//            @Override
//            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                progressDialog.setMessage("Uploading Complete");
//                progressDialog.dismiss();
//            }
//        });
    }

    private void startPlaying(Uri downloadUri) throws IOException {
        isPlaying = true;
//        Uri myUri = Uri.parse(url);
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(this, downloadUri);
            setOnMediaPlayerPreparedListener();
            setOnMediaCompletionListener();
            setOnMediaPlayerErrorListener();
            mediaPlayer.prepareAsync();
            isPreparing = true;
            Log.v("startplayingtag", "startplaying method is called");

        }


    }

    private void stopPlaying() {
        mediaPlayer.release();
        mediaPlayer = null;
        isPlaying = false;
    }

    private void putAudioInStorage(StorageReference storageReference, Uri uri, final String key) {

        Log.v("thisisatag", "putAudioInStorage is called");
        storageReference.putFile(uri).addOnCompleteListener(MainActivity.this,
                new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            FriendlyMessage friendlyMessage =
                                    new FriendlyMessage(null, mUsername, avatarUrl, null, task.getResult().getMetadata().getDownloadUrl()
                                            .toString());
                            audioDatabaseReference.child(MESSAGES_CHILD).child(key)
                                    .setValue(friendlyMessage);
                            Log.v("thisisatag", "Audio upload succeded");
                            deleteRecorderFileOnSD();
                        } else {
                            Log.v("thisisatag", "Audio upload task was not successful.",
                                    task.getException());
                        }
                    }
                });
    }

    private void putImageInStorage(StorageReference storageReference, Uri uri, final String key) {
        Log.v("thisisatag", "putImageInStorage is called");
        storageReference.putFile(uri).addOnCompleteListener(MainActivity.this,
                new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            FriendlyMessage friendlyMessage =
                                    new FriendlyMessage(null, mUsername, avatarUrl,
                                            task.getResult().getMetadata().getDownloadUrl()
                                                    .toString(), null);
                            mFirebaseDatabaseReference.child(MESSAGES_CHILD).child(key)
                                    .setValue(friendlyMessage);
                            Log.v("thisisatag", "image upload succeded");
                        } else {
                            Log.v("thisisatag", "Image upload task was not successful.",
                                    task.getException());
                        }
                    }
                });
    }

    private void checkIsPrepared(MessageViewHolder viewHolder) {
        if (!isPreparing) {
            viewHolder.playAudioImageView.setImageDrawable(getResources().getDrawable(R.drawable.play));
        }

    }

    private void checkIsPlaying(MessageViewHolder viewHolder) {
        if (!isPlaying) {
            viewHolder.playAudioImageView.setClickable(true);
        }
    }

    private void setUpOutgoingMessageViewHolder(final FriendlyMessage friendlyMessage, final MessageViewHolder viewHolder) {
        if (friendlyMessage.getText() != null) {
            viewHolder.messageTextView.setText(friendlyMessage.getText());
            viewHolder.messageTextView.setVisibility(TextView.VISIBLE);
            viewHolder.messageImageView.setVisibility(ImageView.GONE);
            viewHolder.playAudioImageView.setVisibility(View.GONE);
        }
        if (friendlyMessage.getImageUrl() != null && friendlyMessage.getAudioUrl() == null) {

            String imageUrl = friendlyMessage.getImageUrl();
            if (imageUrl.startsWith("gs://")) {
                StorageReference storageReference = FirebaseStorage.getInstance()
                        .getReferenceFromUrl(imageUrl);
                storageReference.getDownloadUrl().addOnCompleteListener(
                        new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    String downloadUrl = task.getResult().toString();
                                    Glide.with(viewHolder.messageImageView.getContext())
                                            .load(downloadUrl)
                                            .into(viewHolder.messageImageView);
                                } else {
                                    Log.w(TAG, "Getting download url was not successful.",
                                            task.getException());
                                }
                            }
                        });
            } else {
                Glide.with(viewHolder.messageImageView.getContext())
                        .load(friendlyMessage.getImageUrl())
                        .into(viewHolder.messageImageView);
            }
            viewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
            viewHolder.messageTextView.setVisibility(TextView.GONE);
            viewHolder.playAudioImageView.setVisibility(View.GONE);

        }
        if (friendlyMessage.getAudioUrl() != null) {
            viewHolder.playAudioImageView.setVisibility(View.VISIBLE);
            viewHolder.messageTextView.setVisibility(TextView.GONE);
            viewHolder.messageImageView.setVisibility(ImageView.GONE);
            viewHolder.playAudioImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!isPlaying) {
                        viewHolder.playAudioImageView.setClickable(false);
                        Glide.with(getApplicationContext()).load(R.drawable.spinner_loader).into(viewHolder.playAudioImageView);
//                                viewHolder.playAudioImageView.setImageDrawable(getResources().getDrawable(R.drawable.common_google_signin_btn_icon_light));
                        StorageReference storageReference = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(friendlyMessage.getAudioUrl());

                        //getting download url to start playing
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                try {
                                    startPlaying(uri);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        //checking for statuses every one second (preparing and playing audio)

                        final Handler handler = new Handler();
                        final int delay = 1000; //milliseconds
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                checkIsPlaying(viewHolder);
                                checkIsPrepared(viewHolder);
                                handler.postDelayed(this, delay);
                            }
                        }, delay);


                    }
                }
            });


        }
    }

    private void setUpOutgoingIncomingMessageViewHolder(final FriendlyMessage friendlyMessage, final MessageViewHolder viewHolder) {
        if (friendlyMessage.getText() != null) {
            viewHolder.incomingMessageTextView.setText(friendlyMessage.getText());
            viewHolder.incomingMessageTextView.setVisibility(TextView.VISIBLE);
            viewHolder.incomingMessageImageView.setVisibility(ImageView.GONE);
            viewHolder.incomingPlayAudioImageView.setVisibility(View.GONE);
        }
        if (friendlyMessage.getImageUrl() != null && friendlyMessage.getAudioUrl() == null) {

            String imageUrl = friendlyMessage.getImageUrl();
            if (imageUrl.startsWith("gs://")) {
                StorageReference storageReference = FirebaseStorage.getInstance()
                        .getReferenceFromUrl(imageUrl);
                storageReference.getDownloadUrl().addOnCompleteListener(
                        new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    String downloadUrl = task.getResult().toString();
                                    Glide.with(viewHolder.incomingMessageImageView.getContext())
                                            .load(downloadUrl)
                                            .into(viewHolder.incomingMessageImageView);
                                } else {
                                    Log.w(TAG, "Getting download url was not successful.",
                                            task.getException());
                                }
                            }
                        });
            } else {
                Glide.with(viewHolder.incomingMessageImageView.getContext())
                        .load(friendlyMessage.getImageUrl())
                        .into(viewHolder.incomingMessageImageView);
            }
            viewHolder.incomingMessageImageView.setVisibility(ImageView.VISIBLE);
            viewHolder.incomingMessageTextView.setVisibility(TextView.GONE);
            viewHolder.incomingPlayAudioImageView.setVisibility(View.GONE);

        }
        if (friendlyMessage.getAudioUrl() != null) {
            viewHolder.incomingPlayAudioImageView.setVisibility(View.VISIBLE);
            viewHolder.incomingMessageTextView.setVisibility(TextView.GONE);
            viewHolder.incomingMessageImageView.setVisibility(ImageView.GONE);
            viewHolder.incomingPlayAudioImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!isPlaying) {
                        viewHolder.incomingPlayAudioImageView.setClickable(false);
                        Glide.with(getApplicationContext()).load(R.drawable.spinner_loader).into(viewHolder.incomingPlayAudioImageView);
//                                viewHolder.playAudioImageView.setImageDrawable(getResources().getDrawable(R.drawable.common_google_signin_btn_icon_light));
                        StorageReference storageReference = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(friendlyMessage.getAudioUrl());

                        //getting download url to start playing
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                try {
                                    startPlaying(uri);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        //checking for statuses every one second (preparing and playing audio)

                        final Handler handler = new Handler();
                        final int delay = 1000; //milliseconds
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                checkIsPlaying(viewHolder);
                                checkIsPrepared(viewHolder);
                                handler.postDelayed(this, delay);
                            }
                        }, delay);


                    }
                }
            });


        }
    }

    private void setOnMediaPlayerPreparedListener() {
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                isPreparing = false;
                mediaPlayer.start();
                Toast.makeText(MainActivity.this, "playback started", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setOnMediaCompletionListener() {
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlaying();
            }
        });
    }

    private void setOnMediaPlayerErrorListener() {
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                Log.v("tagg", "an error has occured");
                mediaPlayer.reset();
                return false;
            }
        });
    }

    private void deleteRecorderFileOnSD() {
        File file = new File(fileName);
        file.delete();
    }




    @SuppressLint("ClickableViewAccessibility")
    private void setupAudioRecordBtn() {
        final Handler handler = new Handler();
        recordAudioView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, final MotionEvent motionEvent) {
                recordAudioView.setImageDrawable(getResources().getDrawable(R.drawable.microphone_red));
                if (!workInProgress) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (mediaRecorder == null) {
                                    recordAudioView.setClickable(false);
                                    startRecording();
                                    Toast.makeText(getApplicationContext(), "recording started",
                                            Toast.LENGTH_SHORT).show();
                                    vibrator.vibrate(50);
                                }

                            }
                        }, 500);


                    } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        handler.removeCallbacksAndMessages(null);
                        if (mediaRecorder != null) {
                            workInProgress = true;
                            vibrator.vibrate(50);
                            stopRecording();
                            recordAudioView.setImageDrawable(getResources().getDrawable(R.drawable.microphone));
                            Toast.makeText(getApplicationContext(), "recording stopped",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            vibrator.vibrate(1000);
                            Toast.makeText(getApplicationContext(),
                                    "please press and hold for at least one second",
                                    Toast.LENGTH_LONG).show();
                            recordAudioView.setImageDrawable(getResources().getDrawable(R.drawable.microphone));
                        }
                    }
                }
                return true;
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();


//        if (mPlayer != null) {
//            mPlayer.release();
//            mPlayer = null;
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("thisisatag", "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    final Uri uri = data.getData();
                    Log.d("thisisatag", "Uri: " + uri.toString());

                    FriendlyMessage tempMessage = new FriendlyMessage(null, mUsername, avatarUrl,
                            null, null);
                    mFirebaseDatabaseReference.child(MESSAGES_CHILD).push()
                            .setValue(tempMessage, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError,
                                                       DatabaseReference databaseReference) {
                                    if (databaseError == null) {
                                        String key = databaseReference.getKey();
                                        StorageReference storageReference =
                                                FirebaseStorage.getInstance()
                                                        .getReference(mFirebaseUser.getUid())
                                                        .child(key)
                                                        .child(uri.getLastPathSegment());

                                        putImageInStorage(storageReference, uri, key);
                                    } else {
                                        Log.w("thisisatag", "Unable to write message to database.",
                                                databaseError.toException());
                                    }
                                }
                            });
                }
            }
        } else if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Check how many invitations were sent and log.
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                Log.d(TAG, "Invitations sent: " + ids.length);
            } else {
                // Sending failed or it was canceled, show failure message to the user
                Log.d(TAG, "Failed to send invitation.");
            }
        } else {
            Bundle payload = new Bundle();
            payload.putString(FirebaseAnalytics.Param.VALUE, "not sent");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE,
                    payload);
            // Sending failed or it was canceled, show failure message to
            // the user
            Log.d(TAG, "Failed to send invitation.");
        }

    }


    private Action getMessageViewAction(FriendlyMessage friendlyMessage) {
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(friendlyMessage.getName(), MESSAGE_URL.concat(friendlyMessage.getId()))
                .setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in.
        // TODO: Add code to check if user is signed in.
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        mFirebaseAdapter.stopListening();
        mGoogleApiClient.stopAutoManage(this);
        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();
        mFirebaseAdapter.startListening();
        if (mAdView != null) {
            mAdView.resume();
        }


    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mUsername = ANONYMOUS;
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                return true;
            case R.id.fresh_config_menu:
                fetchConfig();
                return true;
            case R.id.invite_menu:
                sendInvitation();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Indexable getMessageIndexable(FriendlyMessage friendlyMessage) {
        PersonBuilder sender = Indexables.personBuilder()
                .setIsSelf(mUsername.equals(friendlyMessage.getName()))
                .setName(friendlyMessage.getName())
                .setUrl(MESSAGE_URL.concat(friendlyMessage.getId() + "/sender"));

        PersonBuilder recipient = Indexables.personBuilder()
                .setName(mUsername)
                .setUrl(MESSAGE_URL.concat(friendlyMessage.getId() + "/recipient"));

        Indexable messageToIndex = Indexables.messageBuilder()
                .setName(friendlyMessage.getText())
                .setUrl(MESSAGE_URL.concat(friendlyMessage.getId()))
                .setSender(sender)
                .setRecipient(recipient)
                .build();

        return messageToIndex;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

}
