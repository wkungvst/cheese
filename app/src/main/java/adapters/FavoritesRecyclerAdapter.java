package adapters;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import interfaces.IFavoriteSongClick;
import kung.cheeseandfriends.R;
import viewmodels.MainViewModel;

/**
 * Created by user on 1/13/18.
 */

public class FavoritesRecyclerAdapter extends RecyclerView.Adapter<FavoritesRecyclerAdapter.ViewHolder>{

private ArrayList<MainViewModel.SongObject> dataSet;
        Activity context;
        IFavoriteSongClick clicker;
        int row_index = -1;

public FavoritesRecyclerAdapter(ArrayList<MainViewModel.SongObject> data, IFavoriteSongClick click ){
        clicker = click;
        dataSet = data;
        }

public static class ViewHolder extends RecyclerView.ViewHolder {

    private TextView title;
    private ImageButton delete;
    private TextView album;
    private TextView date;
    private TextView location;
    private View layout;
    private ImageView animation;

    public ViewHolder(final View v) {
        super(v);
        layout = v;
        title = v.findViewById(R.id.song_title);
        delete = v.findViewById(R.id.delete_favorite);
        album = v.findViewById(R.id.song_album);
        date = v.findViewById(R.id.show_date);
        location = v.findViewById(R.id.show_location);
        animation = v.findViewById(R.id.play_animation);
    }
}

    @Override
    public FavoritesRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
        // create a new view
        context = (Activity)parent.getContext();
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.favorites_widget, parent, false);
        // set the view's size, margins, paddings and layout parameters
        FavoritesRecyclerAdapter.ViewHolder vh = new FavoritesRecyclerAdapter.ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(FavoritesRecyclerAdapter.ViewHolder holder, int position) {
        holder.title.setText(Html.fromHtml(dataSet.get(position).title));
        holder.date.setText(dataSet.get(position).show.date);
        holder.location.setText(dataSet.get(position).show.location);
        if(row_index == position){
            holder.animation.setVisibility(View.VISIBLE);
            holder.animation.setBackgroundResource(R.drawable.playing_animation);
            AnimationDrawable animation = (AnimationDrawable)holder.animation.getBackground();
            animation.start();
            holder.delete.setVisibility(View.GONE);
        }
        else
        {
            holder.delete.setVisibility(View.VISIBLE);
            holder.animation.setVisibility(View.GONE);
            //    holder.layout.setBackgroundColor(Color.parseColor("#ffffff"));
        }

        holder.delete.setOnClickListener(click->{
            Log.d("@@@"," delete this song: " + dataSet.get(position).title);
            clicker.deleteFavorite(dataSet.get(position));
        });

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
        Log.d("@@@", " update current song");
        for(MainViewModel.SongObject s : dataSet){
            if(s == song){
                row_index = dataSet.indexOf(s);
                notifyDataSetChanged();
                return;
            }
        }
        row_index = -1;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

}
