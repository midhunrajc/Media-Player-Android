package com.tss.myusb;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.MediaBrowserServiceCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;



public class MediaBrowserService extends MediaBrowserServiceCompat {
    private List<Uri> mp3Files = new ArrayList<>();
    List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
    private static final String TAG = "receiver";

    private MediaSessionCompat mediaSession;
    private UsbDevice gUsbDevice;
    public static final String ACTION_USB_PERMISSION = "com.tss.USB_PERMISSION";

    private MediaPlayer mediaPlayer = new MediaPlayer();
    private int currentQueueIndex = 0; // Initialize to 0 to indicate 1st  item


    public MediaBrowserService() {
    }




    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // Trigger onLoadChildren in your MediaBrowserService
          //  handleUsbMounted();
            if ("MP3_FILES_UPDATED".equals(intent.getAction())) {
                Log.d("MediaBrowserService","onRecieve 1");
                mp3Files = intent.getParcelableArrayListExtra("mp3Files");
                // Notify clients that the list of MP3 files has changed
//                notifyChildrenChanged("USB_media_id");
                handleUsbMounted();
            } else if ("ACTION_USB_PERMISSION".equals(intent.getAction())) {
                handleUsbMounted();

            } else if ("ACTION_USB_DEVICE_DETACHED".equals(intent.getAction())) {
                handleUsbUnMounted();
            }
        }
    };

    private void handleUsbMounted() {
        // Notify all connected clients that the children of "media_items" have changed
        notifyChildrenChanged("USB_media_id");
        Log.e("MediaBrowserService", "notifyChildrenChanged called");

    }
    private void handleUsbUnMounted() {
        // Notify all connected clients that the children of "media_items" have changed
        notifyChildrenChanged("media_id");
        Log.e("MediaBrowserService", "notifyChildrenChanged called");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister USB mounted receiver
        //unregisterReceiver(mp3FilesReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Create a MediaSessionCompat
        mediaSession = new MediaSessionCompat(this, "MusicSession");
        // Set the media session's callback
        mediaSession.setCallback(mediaSessionCallback);

        // Set flags to indicate that the media session will handle media buttons
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Build the initial playback state
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE);
        mediaSession.setPlaybackState(stateBuilder.build());


        // Set the active state and token for the MediaSessionCompat
        mediaSession.setActive(true);
        setSessionToken(mediaSession.getSessionToken());


        IntentFilter filter = new IntentFilter();
        filter.addAction("MP3_FILES_UPDATED");
        //filter.addAction("ACTION_USB_PERMISSION");
        filter.addAction("ACTION_USB_DEVICE_DETACHED");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

    }


    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        // Implement logic to check if the client is allowed to connect
        // and return the root ID and optional extras
        // For example, you can create a BrowserRoot object and return it
        Log.e("MediaBrowserService", "Success onGetRoot");
        return new BrowserRoot("media_id", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.e("MediaBrowserService", "Success onLoadChildern");

        if (parentId.equals("media_id")) {
            // Return the list of media items when the root ID is requested
            result.sendResult(getMediaItemsFromStorage());

        } else if (parentId.equals("USB_media_id")) {
            Log.d("MediaBrowserService", "onLoadChildren: getUsbPath(): " + getUsbDirectory());
            // Return the list of media items when the root ID is requested
            Log.d("MediaBrowserService", "onLoadChildren: triggered");
           // Convert your list of Uri to a list of MediaItem

            //List<MediaBrowserCompat.MediaItem> mediaItems = convertUriListToMediaItems(mp3Files);
            mediaItems =  convertUriListToMediaItems(mp3Files);
              //may reload mediaItems ,as it declared globally
            result.sendResult(mediaItems);
        } else {
            // Handle other cases, such as browsing by artist, album, etc.
            result.sendResult(null);
        }
    }


    private File getUsbDirectory() {
        StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);

        if (storageManager != null) {
            // Get the list of all storage volumes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                StorageVolume[] storageVolumes = storageManager.getStorageVolumes().toArray(new StorageVolume[0]);
                Log.d("MediaBrowserService", "getUsbPath:All storageVolumes= " + Arrays.toString(storageVolumes));
                // Iterate through the volumes to find the USB volume
                for (StorageVolume volume : storageVolumes) {
                    Log.d("MediaBrowserService", "getUsbPath: storageVolume: " + volume.isRemovable());
                    if (volume.isRemovable()) {
                        // This is a removable (potentially USB) volume
                        return volume.getDirectory();
                    }
                }
            }
        }
        // If no USB volume is found, you may handle it accordingly
        return null;
    }


    private MediaSessionCompat.Callback mediaSessionCallback =
            new MediaSessionCompat.Callback() {
                @Override
                public void onPlay() {
                    // Handle play event
                    Log.e("MediaBrowserService", "onPlay callback");

                    if(!mediaItems.isEmpty() && currentQueueIndex >=0 && currentQueueIndex < mediaItems.size()) {
                        MediaBrowserCompat.MediaItem firstSong = mediaItems.get(currentQueueIndex);

                        Log.e("BrowserService","Total Songs " + mediaItems.size());

                        // Use MediaDescriptionCompat to get the media information
                        MediaDescriptionCompat description = firstSong.getDescription();
                        // Retrieve the media URI from the metadata
                        Uri mediaUri = description.getMediaUri();

                        try {
                            if(mediaPlayer != null){
                                if(mediaPlayer.isPlaying()){
                                    mediaPlayer.pause();
                                }
                                mediaPlayer.reset();
                            }else{
                                mediaPlayer = new MediaPlayer();
                            }

                            mediaPlayer.setDataSource(getApplicationContext(),mediaUri);
                            mediaPlayer.prepare();
                        } catch (IOException  | IllegalStateException e) {
                            e.printStackTrace();
                        }
                        mediaPlayer.start();
                    }

                }
                @Override
                public void onPause() {
                    // Handle pause event

                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        Log.e("Inside onPlay","mediaPlayer Started");
                        // Update playback state, UI, etc.
                        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);

                    }
                }
                @Override
                public void onSkipToNext() {

                    Log.e("onClick","onClick onSkiptoNext");
                    currentQueueIndex = (currentQueueIndex +1) % mediaItems.size();
                    onPlay();

                }

                @Override
                public void onSkipToPrevious() {
                    Log.e("onClick", "onClick onSkiptoNext");
                    currentQueueIndex = (currentQueueIndex - 1 + mediaItems.size())% mediaItems.size();
                    onPlay();
                }


            };

    // Custom method to set the playback state
    private void setPlaybackState(int playbackState) {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setState(playbackState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0)
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE);
        mediaSession.setPlaybackState(stateBuilder.build());
        // Notify any listeners or update UI based on the playback state change
        // For example, you might send a broadcast, update a notification, etc.
    }

    private List<MediaBrowserCompat.MediaItem> convertUriListToMediaItems(List<Uri> uriList) {
      //  List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        for (Uri uri : uriList) {
            String title = getFileNameFromUri(uri);
            String artist = getArtistFromUri(uri);

            Log.d("MediaItemConversion", "Title: " + title + ", Artist: " + artist);

            MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(uri.toString())
                            .setTitle(title)
                            .setSubtitle(artist)
                            .setMediaUri(uri)
                            .build(),
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            );

            mediaItems.add(mediaItem);
        }

        return mediaItems;
    }

    private String getFileNameFromUri(Uri uri) {
        Log.d("MediaBrowserService",uri.getLastPathSegment());
        return uri.getLastPathSegment(); // Get the file name from the Uri
    }

    private String getArtistFromUri(Uri uri) {
        String fileName = uri.getLastPathSegment(); // Get the file name from the Uri
        // Assuming the file name follows the pattern "Title - Artist.mp3"
        String[] parts = fileName.split(" - ");
        if (parts.length >= 2) {
            return parts[1].replace(".mp3", ""); // Extract artist and remove file extension
        } else {
            return "Default Artist";
        }
    }

    private List<MediaBrowserCompat.MediaItem> getMediaItemsFromStorage() {
        Log.d("MediaBrowserService", "Media Items From Internal Storage");
      // List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        // Retrieve songs from the device's media store
        Cursor cursor = contentResolver.query(
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                null,
                null,
                null,
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String mediaId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                // Create a URI from the file path
                Uri mediaUri = Uri.parse("file://" + data);

                // Create a MediaDescriptionCompat for each song
                MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                        .setMediaId(mediaId)
                        .setTitle(title)
                        .setSubtitle(artist)
                        .setMediaUri(mediaUri)
                        .build();

                // Create a MediaItem and add it to the list
                MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                        description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
                mediaItems.add(mediaItem);
            }
            cursor.close();
        }
        return mediaItems;
    }


}