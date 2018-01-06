package adapters;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import interfaces.ISongClick;
import kung.cheeseandfriends.R;
import viewmodels.MainViewModel;

/**
 * Created by wkung on 12/16/17.
 */

public class SongsRecyclerAdapter extends RecyclerView.Adapter<SongsRecyclerAdapter.ViewHolder>{

    private ArrayList<MainViewModel.SongObject> dataSet;
    Activity context;
    ISongClick clicker;
    int row_index = -1;

    public SongsRecyclerAdapter(ArrayList<MainViewModel.SongObject> data, ISongClick click ){
        clicker = click;
        dataSet = data;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private TextView track;
        private TextView album;
        private TextView length;
        private View layout;
        private ImageView animation;

        public ViewHolder(final View v) {
            super(v);
            layout = v;
            title = v.findViewById(R.id.song_title);
            track = v.findViewById(R.id.song_track);
            album = v.findViewById(R.id.song_album);
            length = v.findViewById(R.id.song_length);
            animation = v.findViewById(R.id.play_animation);
        }
    }

    @Override
    public SongsRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
        // create a new view
        context = (Activity)parent.getContext();
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song_widget, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.title.setText(Html.fromHtml(dataSet.get(position).title));
        holder.track.setText(dataSet.get(position).track + "");
        holder.length.setText(dataSet.get(position).length);
        if(row_index == position){
            holder.animation.setVisibility(View.VISIBLE);
            holder.animation.setBackgroundResource(R.drawable.playing_animation);
            AnimationDrawable animation = (AnimationDrawable)holder.animation.getBackground();
            animation.start();
            holder.track.setVisibility(View.GONE);
        //    holder.layout.setBackgroundColor(Color.parseColor("#eeeeee"));
        }
        else
        {
            holder.track.setVisibility(View.VISIBLE);
            holder.animation.setVisibility(View.GONE);
        //    holder.layout.setBackgroundColor(Color.parseColor("#ffffff"));
           }
        holder.layout.setOnClickListener(view -> {
            row_index = position;
            notifyDataSetChanged();
            Handler handler = new Handler();
            handler.post(()->{
                clicker.openSong(dataSet.get(position), (position + 1 >= dataSet.size()) ? null : dataSet.get(position + 1));
            });
        });
    }

    public void updateCurrentSong(MainViewModel.SongObject song){
        for(MainViewModel.SongObject s : dataSet){
            if(song == s){
                row_index = dataSet.indexOf(s);
            }
        }
        notifyDataSetChanged();

    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

}