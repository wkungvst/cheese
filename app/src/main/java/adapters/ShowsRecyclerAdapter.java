package adapters;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import kung.cheeseandfriends.R;
import viewmodels.MainViewModel;
import interfaces.IShowClick;

/**
 * Created by wkung on 12/16/17.
 */

public class ShowsRecyclerAdapter extends RecyclerView.Adapter<ShowsRecyclerAdapter.ViewHolder>{

    private ArrayList<MainViewModel.ShowObject> dataSet;
    Activity context;
    IShowClick clicker;

    public ShowsRecyclerAdapter(ArrayList<MainViewModel.ShowObject> data, IShowClick click ){
        clicker = click;
        dataSet = data;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private TextView location;
        private TextView venue;
        private TextView description;
        private TextView date;
        private View layout;

        public ViewHolder(final View v) {
            super(v);
            layout = v;
            layout.setClickable(true);
            layout.setFocusable(true);
            title = (TextView)v.findViewById(R.id.show_title);
            location = (TextView)v.findViewById(R.id.show_location);
            venue = (TextView)v.findViewById(R.id.show_venue);
            date = (TextView)v.findViewById(R.id.show_date);

        }
    }

    @Override
    public ShowsRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
        // create a new view
        context = (Activity)parent.getContext();
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.show_widget, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.title.setText(Html.fromHtml(dataSet.get(position).title));
        holder.date.setText(dataSet.get(position).date);
        holder.venue.setText(dataSet.get(position).venue + ", ");
        holder.location.setText(dataSet.get(position).location);
        holder.layout.setOnClickListener(view -> {
            clicker.openShow(dataSet.get(position));
        });
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

}