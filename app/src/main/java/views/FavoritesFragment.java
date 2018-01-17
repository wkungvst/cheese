package views;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import adapters.FavoritesRecyclerAdapter;
import adapters.SongsRecyclerAdapter;
import interfaces.IFavoriteSongClick;
import interfaces.ISongClick;
import kung.cheeseandfriends.R;
import viewmodels.MainViewModel;

/**
 * Created by wkung on 12/12/17.
 */

public class FavoritesFragment extends Fragment implements IFavoriteSongClick {

    View mView;
    MainViewModel mMainViewModel;
    private RecyclerView mSongsRecyclerView;
    private FavoritesRecyclerAdapter mSongsRecyclerAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_favorites, container, false);
        ((TextView)mView.findViewById(R.id.no_favorites_text)).setText(Html.fromHtml("Click the &#10084; icon to add songs to this list!"));
        addSubscriptions();

        return mView;
    }

    public void setViewModel(MainViewModel viewModel){
        mMainViewModel = viewModel;

    }

    private void addSubscriptions(){
        mMainViewModel.getFavoritesObservable().subscribe(list->{
            Log.d("@@@, ", " list size: " + list.size());
            populateFavorites(list);
        });
    }

    private void populateFavorites(ArrayList<MainViewModel.SongObject> songs){
        mSongsRecyclerView = mView.findViewById(R.id.favorite_songs_recyclerview);
        mSongsRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mSongsRecyclerView.setLayoutManager(mLayoutManager);
        mSongsRecyclerAdapter = new FavoritesRecyclerAdapter(songs, this);
        mSongsRecyclerView.setAdapter(mSongsRecyclerAdapter);
    }

    @Override
    public void openSong(MainViewModel.SongObject song, MainViewModel.SongObject nextSong) {
        mMainViewModel.openSong(song, nextSong, true);
    }

    @Override
    public void deleteFavorite(MainViewModel.SongObject song) {
        mMainViewModel.deleteFavorite(song);
    }
}
