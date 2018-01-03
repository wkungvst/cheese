package views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import adapters.SongsRecyclerAdapter;
import adapters.ShowsRecyclerAdapter;
import interfaces.ICatalogInterface;
import interfaces.IShowClick;
import interfaces.ISongClick;
import kung.cheeseandfriends.R;
import rx.subscriptions.CompositeSubscription;
import viewmodels.MainViewModel;

/**
 * Created by wkung on 12/12/17.
 */

public class CatalogFragment extends Fragment implements ICatalogInterface, IShowClick, ISongClick {

    private View mView;
    private ListView mPrimaryListView;
    private RelativeLayout mPrimaryContainer;
    private RelativeLayout mSecondaryContainer;
    private RelativeLayout mSongsContainer;
    private MainViewModel mMainViewModel;
    private FloatingActionButton mBackButton;
    private CompositeSubscription mCompositeSubscription;
    private int mSecondaryYear;
    private SongsRecyclerAdapter mSongsRecyclerAdapter;
    private ShowsRecyclerAdapter mShowsRecyclerAdapter;
    ICatalogListener mListener;
    RecyclerView mShowsRecyclerView;
    RecyclerView mSongsRecyclerView;


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void populateList(TreeMap<Integer, Integer> yearData) {
        List<YearObject> yearObjectList = new ArrayList<>();

        for(Integer i : yearData.keySet()){
            yearObjectList.add(new YearObject(i, yearData.get(i)));
        }

        CatalogAdapter arrayAdapter = new CatalogAdapter(
                getActivity(),
                R.layout.catalog_listview_item,
                yearObjectList);

        mPrimaryListView.setNestedScrollingEnabled(true);
        mPrimaryListView.setAdapter(arrayAdapter);
    }

    @Override
    public void onBackPressed() {
        swapViews();
    }

    private void swapViews(){
        if(mSongsContainer.getVisibility() == View.VISIBLE){
            mSongsContainer.setVisibility(View.GONE);
            mSecondaryContainer.setVisibility(View.VISIBLE);
        }else if(mSecondaryContainer.getVisibility() == View.VISIBLE){
            mSecondaryContainer.setVisibility(View.GONE);
            mPrimaryContainer.setVisibility(View.VISIBLE);
            mBackButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void openShow(MainViewModel.ShowObject show) {
        mMainViewModel.openShow(show);
        mBackButton.setVisibility(View.VISIBLE);
        Log.d("@@@", " catalog fragment open show date : " + show.date);
    }

    @Override
    public void openSong(MainViewModel.SongObject song, MainViewModel.SongObject nextSong) {
        mMainViewModel.openSong(song, nextSong);
    }

    public interface ICatalogListener{

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("@@@", "CatalogFragment : onCreate");
        initialize();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_catalog, container, false);
        mPrimaryListView = mView.findViewById(R.id.catalog_primary_listview);
        mPrimaryContainer = mView.findViewById(R.id.catalog_primary_container);
        mSecondaryContainer = mView.findViewById(R.id.catalog_secondary_container);
        mSongsContainer = mView.findViewById(R.id.catalog_songs_container);
        mBackButton = mView.findViewById(R.id.back_button);

        RxView.clicks(mBackButton).subscribe(back->{
            onBackPressed();
        });
        return mView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = ((ICatalogListener)getActivity());
    }

    private void initialize(){

    }

    public void setViewModel(MainViewModel viewModel){
        mMainViewModel = viewModel;
        addSubscriptions();
    }

    private void addSubscriptions(){
        mMainViewModel.getShowObjectsForYearObservable().subscribe(shows->{
            if(shows.size() > 0){
                populateSecondaryList(shows);
            }
        });

        mMainViewModel.getSongsObservable().subscribe(songs->{
            if(songs.size() > 0){
                populateSongList(songs);
            }
        });
    }

    private void populateSongList(ArrayList<MainViewModel.SongObject> songs){
        mSongsRecyclerView = mView.findViewById(R.id.catalog_songs_recyclerview);
        mSongsRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mSongsRecyclerView.setLayoutManager(mLayoutManager);
        mSongsRecyclerAdapter = new SongsRecyclerAdapter(songs, this);
        mSongsRecyclerView.setAdapter(mSongsRecyclerAdapter);
        mPrimaryContainer.setVisibility(View.GONE);
        mSecondaryContainer.setVisibility(View.GONE);
        mSongsContainer.setVisibility(View.VISIBLE);
    }

    private void populateSecondaryList(ArrayList<MainViewModel.ShowObject> shows){
        mShowsRecyclerView = mView.findViewById(R.id.catalog_secondary_recyclerview);
        mShowsRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mShowsRecyclerView.setLayoutManager(mLayoutManager);

        mShowsRecyclerAdapter = new ShowsRecyclerAdapter(shows, this);
        mShowsRecyclerView.setAdapter(mShowsRecyclerAdapter);
        mPrimaryContainer.setVisibility(View.GONE);
        mSongsContainer.setVisibility(View.GONE);
        mSecondaryContainer.setVisibility(View.VISIBLE);
        mBackButton.setVisibility(View.VISIBLE);
    }

    private class YearObject{
        int year,shows;
        public YearObject(int year, int shows){
            this.shows = shows;
            this.year = year;
        }
    }

    private void showSecondaryView(int year){
        mSecondaryYear = year;
        mMainViewModel.getShowsOfYear(year);
    }

    private class CatalogAdapter extends ArrayAdapter<YearObject>{

        int mResource;
        Context mContext;

        public CatalogAdapter(@NonNull Context context, int resource, @NonNull List<YearObject> objects) {
            super(context, resource, objects);
            mResource = resource;
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater layoutInflater = LayoutInflater.from(mContext);

            View view = layoutInflater.inflate(mResource, null, false);

            YearObject yearObject = getItem(position);

            if (yearObject != null) {
                ((TextView)view.findViewById(R.id.catalog_year)).setText(yearObject.year + "");
                ((TextView)view.findViewById(R.id.catalog_number_shows)).setText(yearObject.shows + " SHOWS");
            }
            RxView.clicks(view).subscribe(c->{
                showSecondaryView(yearObject.year);
            });
            return view;
        }
    }

}
