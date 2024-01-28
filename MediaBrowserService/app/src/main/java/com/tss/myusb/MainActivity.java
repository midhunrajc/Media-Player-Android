package com.tss.myusb;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.EnvironmentCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements USBEventListener  {
    receiver receiverObj;

    private static final String ACTION_USB_PERMISSION = "com.tss.USB_PERMISSION";
    private static final int REQUEST_PERMISSION_CODE = 123;
    private static final int REQUEST_CODE_OPEN_DOCUMENT_TREE = 42;
    private static final String TAG = "MainActivity";
    IntentFilter filter = new IntentFilter();
    private MediaBrowserCompat mediaBrowser;

    private MediaControllerCompat mediaController;
    private List<Uri> mp3Files;
    private RecyclerView recyclerView;
    private Button playButton;
    private Button pauseButton;
    private Button nextButton;
    private Button previousButton;
    private Button switchStorageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playButton = findViewById(R.id.playButton);
        pauseButton = findViewById(R.id.pauseButton);
        nextButton = findViewById(R.id.nextButton);
        previousButton = findViewById(R.id.previousButton);
        switchStorageButton = findViewById(R.id.storageButton);



        boolean isPermissionGranted = checkPermission();
        if(!isPermissionGranted){
            requestPermission();
        }

        // Register the receiver
        receiverObj = new receiver();
        receiverObj.registerUSBDeviceEventCallback(this);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(receiverObj, filter);

        // Create mediabrowsercompat instance
        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, MediaBrowserService.class), connectionCallback, null);
        mediaBrowser.connect();
          // Check for USB Mount on App booting
//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//        startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT_TREE);

        recyclerView = findViewById(R.id.recyclerView);
        // Set up RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  System.out.println(mediaController.getPlaybackState());
                // if (mediaController != null && mediaController.getPlaybackState() != null) {


                if (mediaController != null){
                    Log.e("onClick", "Inside OnClick");
                    //   mediaController.getTransportControls().play();
                    //   Check if media is paused or stopped, then start playback
                    int playbackState = mediaController.getPlaybackState().getState();

                    if (playbackState == PlaybackStateCompat.STATE_PAUSED ||
                            playbackState == PlaybackStateCompat.STATE_STOPPED) {
                        mediaController.getTransportControls().play();
                    }
                    Log.e("onClick", "Inside OnClick" + playbackState);
                    mediaController.getTransportControls().play();

                } else {
                    Log.e("onClick", "No playbackstate");
                }
            }

        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mediaController != null){
                    Log.e("onClick","Inside pause onClick");
                    mediaController.getTransportControls().pause();
                }

            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaController != null) {
                    Log.e("onClick","Inside nextButton onClick");
                    mediaController.getTransportControls().skipToNext();
                }
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaController != null) {
                    Log.e("onClick" , "Inside PrevButton onClick");
                    mediaController.getTransportControls().skipToPrevious();
                }
            }
        });


//        switchStorageButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//           public void onClick(View v) {
//               // Toggle between USB and internal storage
//                toggleStorage();
//            }
//        });
 }

//    private void toggleStorage() {
//        if (isUsbConnected()) {
//            // Switch to USB storage logic
//            // Example: Use UsbFile or other USB-related logic
//            // ...
//               Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//               startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT_TREE);
//
//
//            } else {
//            // Switch to internal storage logic
//            // Example: Use Environment.getExternalStorageDirectory() or other internal storage logic
//            // ...
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setMessage("USB is not connected")
//                .setTitle("USB Not Connected")
//                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//        public void onClick(DialogInterface dialog, int id) {
//            // Handle the OK button click if needed
//            dialog.dismiss();
//        }
//    });
//    AlertDialog dialog = builder.create();
//        dialog.show();
//    }
//        }

    //Check if any USB Attached
//    private boolean isUsbConnected() {
//        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
//        if (usbManager != null) {
//            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
//            return !deviceList.isEmpty();
//        }
//        return false;
//    }
    private MediaBrowserCompat.ConnectionCallback connectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    // Get the MediaControllerCompat
                    Log.e("MyActivity", "onConnected");
                    mediaController = new MediaControllerCompat(
                            MainActivity.this, mediaBrowser.getSessionToken());
                    mediaController.registerCallback(controllerCallback);
                    mediaBrowser.subscribe("USB_media_id", subscriptionCallback);
                    mediaBrowser.subscribe("media_id", subscriptionCallback);


                }

                @Override
                public void onConnectionSuspended() {

                    Log.e("MainActivity", "onConnectionSuspended");
                    // Handle connection suspension
                }

                @Override
                public void onConnectionFailed() {
                    // Handle connection failure
                    Log.e("MainActivity", "onConnectionFailed");
                }
            };


    private MediaControllerCompat.Callback controllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    // Handle metadata change
                    // Handle metadata change
                    Log.d("MyMediaActivity", "Metadata changed: " + metadata);
                }

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    super.onPlaybackStateChanged(state);
                    int playbackState = state.getState();
                    // Handle playback state changes
                    Log.d("MediaSessionCallback", "Playback state changed: " + playbackState);
                    switch (playbackState){

                        case PlaybackStateCompat.STATE_PLAYING:
                            Log.e("PlaybackState","PLaying");
                            break;

                        case PlaybackStateCompat.STATE_PAUSED:
                            Log.e("PlaybackState" ,"Paused");
                            break;
                    }


                }
            };


    private MediaBrowserCompat.SubscriptionCallback subscriptionCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
            // Handle loaded media items
            Log.d("MainActivity", "SubscriptionCallback onChildrenLoaded");
            Log.d("MediaBrowserExample", "Number of media items: " + children.size());

         /*   for (MediaBrowserCompat.MediaItem mediaItem : children) {
                Log.d("MediaBrowserExample", "MediaItem ID: " + mediaItem.getMediaId());
                Log.d("MediaBrowserExample", "MediaItem Title: " + mediaItem.getDescription().getTitle());
                Log.d("MediaBrowserExample", "MediaItem Artist: " + mediaItem.getDescription().getSubtitle());
            }

          */

            // Create an instance of MediaItemAdapter and pass the list of media items
            MediaItemAdapter mediaItemAdapter = new MediaItemAdapter(children);

            // Set the adapter to the RecyclerView
            recyclerView.setAdapter(mediaItemAdapter);


        }

        @Override
        public void onError(@NonNull String parentId) {
            // Handle error
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the USB receiver
        unregisterReceiver(receiverObj);
    }
    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED ;
        }
        return true;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                REQUEST_PERMISSION_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG," EXTERNAL STORAGE PERMISSION GRANTED");

            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                Log.d(TAG,"EXTERNAL STORAGE PERMISSION DENIED");
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_OPEN_DOCUMENT_TREE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri treeUri = data.getData();

               mp3Files = new ArrayList<>();

                // Traverse the USB drive and find all MP3 files
                findMp3Files(treeUri, mp3Files);

                Uri initialMp3FileUri = mp3Files.get(0);

                // Process the list of MP3 files as needed
                for (Uri mp3FileUri : mp3Files) {
                    // Do something with each MP3 file URI
                    // e.g., add it to a list, play it, etc.
                    Log.d("MP3 Files", "MP3 file found: " + mp3FileUri.toString());
                    Toast.makeText(this, "MP3 file found: " + mp3FileUri.toString(), Toast.LENGTH_SHORT).show();
                }
                Log.d("MP3 Files", " 1st MP3 file found: " + mp3Files.get(0));
            }
        }
        // Send broadcast with the mp3Files list
        sendMp3FilesBroadcast(mp3Files);
    }
    private void findMp3Files(Uri rootUri, List<Uri> mp3Files) {
        DocumentFile root = DocumentFile.fromTreeUri(this, rootUri);

        if (root != null && root.exists() && root.isDirectory()) {
            traverseDirectory(root, mp3Files);
        }
    }

    private void traverseDirectory(DocumentFile directory, List<Uri> mp3Files) {
        DocumentFile[] files = directory.listFiles();

        if (files != null) {
            for (DocumentFile file : files) {
                if (file.isDirectory()) {
                    // Recursively traverse subdirectories
                    traverseDirectory(file, mp3Files);
                } else if (file.getType().startsWith("audio/") && file.getName().toLowerCase().endsWith(".mp3")) {
                    // Check if the file is an MP3 file
                    mp3Files.add(file.getUri());
                }
            }
        }
    }
    private void sendMp3FilesBroadcast(List<Uri> mp3Files) {
        Log.d("sendMp3","MP3_FILES_UPDATED");
        Intent intent = new Intent("MP3_FILES_UPDATED");
        intent.putParcelableArrayListExtra("mp3Files", new ArrayList<>(mp3Files));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    @Override
    public void onUSBDeviceAttached() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT_TREE);

    }

    @Override
    public void onUSBDeviceDetached() {
    Log.d(TAG,"OnUSBDeviceDetached");
    }
}