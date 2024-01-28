package com.tss.myusb;


import android.support.v4.media.MediaBrowserCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MediaItemAdapter extends RecyclerView.Adapter<MediaItemAdapter.ViewHolder> {

    private List<MediaBrowserCompat.MediaItem> mediaItems;



    public MediaItemAdapter(List<MediaBrowserCompat.MediaItem> mediaItems) {
        this.mediaItems = mediaItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaBrowserCompat.MediaItem mediaItem = mediaItems.get(position);
        holder.titleTextView.setText(mediaItem.getDescription().getTitle());
        holder.bind(mediaItem);
    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView =(TextView) itemView.findViewById(R.id.titleTextView);
        }

        public void bind(MediaBrowserCompat.MediaItem mediaItem) {
            titleTextView.setText(mediaItem.getDescription().getTitle());
            // Add any additional bindings or UI updates here
        }
    }



}
