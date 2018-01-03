package views;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.net.URI;
import java.util.AbstractMap;
import java.util.Observable;
import java.util.TreeMap;

import interfaces.ICatalogInterface;
import interfaces.IPager;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;
import viewmodels.MainViewModel;
import kung.cheeseandfriends.R;

/**
 * Created by wkung on 12/11/17.
 */

public class MainActivity extends FragmentActivity implements ActionBar.TabListener, CatalogFragment.ICatalogListener, IPager {

    private CompositeSubscription mCompositeSubscription;
    private MainViewModel mMainViewModel;
    private MainPagerAdapter mMainPagerAdapter;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private MainViewModel.BAND mCurrentBand;
    private ICatalogInterface mCatalogInterface;
    private MainViewModel.ShowObject mLatestShowObject;
    private MediaPlayer mMediaPlayer;
    private MediaPlayer mMediaPlayerNext;
    private SeekBar mSeekbar;
    private TextView mPlayerSong;
    private String mBandName = "";

    private static String[] TABS = {"Jam Of The Day", "Catalog"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMainViewModel = new MainViewModel(this);
        mMainPagerAdapter = new MainPagerAdapter(this, getSupportFragmentManager());
        mMainViewModel.setup();
        mTabLayout = (TabLayout)findViewById(R.id.tab_layout);
        mViewPager = (ViewPager)findViewById(R.id.pager);
        mSeekbar = findViewById(R.id.player_seekbar);
        mPlayerSong = findViewById(R.id.player_song);
        mMediaPlayer = new MediaPlayer();
        mMediaPlayerNext = new MediaPlayer();

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override
            public void onPageScrollStateChanged(int state) {}
            @Override
            public void onPageSelected(int position) {;}
        });

        mViewPager.setAdapter(new MainPagerAdapter(this, getSupportFragmentManager()));
        mTabLayout.setupWithViewPager(mViewPager);

        addSubscriptions();
    }

    private void addSubscriptions(){
        mCompositeSubscription = new CompositeSubscription();

        mCompositeSubscription.add(mMainViewModel.getMetaDataObservable().subscribe(data->{
            Log.d("@@@", " from activity number of shows: " + data.getKey());
            Log.d("@@@", " from activity latest shows : " + data.getValue());
            populateMetaData(data);
        }));

        mCompositeSubscription.add(mMainViewModel.getCurrentBandObservable().subscribe(band->{
            mCurrentBand = band;
            updateCatalog();
        }));

        mCompositeSubscription.add(mMainViewModel.getYearDataObservable().subscribe(yearData->{
            if(yearData.size() > 0){
                mCatalogInterface.populateList(yearData);
            }
        }));

        mCompositeSubscription.add(mMainViewModel.getShowObjectsForYearObservable().subscribe(shows->{
            if(shows.size() > 0){
                expandHeader(false);
            }
        }));

        mCompositeSubscription.add(mMainViewModel.getSongsObservable().subscribe(songs->{
            if(songs.size() > 0){
                for(MainViewModel.SongObject s : songs){
               //     Log.d("@@@, ", " song name" + s.title + " track # " + s.track);
                }
                expandHeader(false);
            }
        }));

        mCompositeSubscription.add(mMainViewModel.getAudioLinkObservable().subscribe(audio->{
            Log.d("@@@", " get audio link observable. play audio");
            playAudio(audio);
            populateNowPlaying(mMainViewModel.getCurrentShow());
        }));

        mCompositeSubscription.add(mMainViewModel.getCurrentSongObservable().subscribe(songObject->{
            findViewById(R.id.player).setVisibility(View.VISIBLE);
            populatePlayer(songObject);
        }));

        mCompositeSubscription.add(mMainViewModel.getIsPlaying().subscribe(
            playing->{
                ((ImageButton)findViewById(R.id.play_pause)).setImageResource(playing? R.drawable.pause : R.drawable.play);
                if(playing){
                    if(!mMediaPlayer.isPlaying()){
                        Log.d("@@@", " not already playing, start playing");
                        mMediaPlayer.start();
                    }else{
                        Log.d("@@@", " already playing, dont start again");
                    }
                }else{
                    mMediaPlayer.pause();
                }
            }
        ));

        mCompositeSubscription.add(rx.Observable.combineLatest(mMainViewModel.getYearDataObservable(), mMainViewModel.getMetaDataObservable(), new Func2<TreeMap<Integer,Integer>, AbstractMap.SimpleEntry<Integer,MainViewModel.ShowObject>, Boolean>() {
            @Override
            public Boolean call(TreeMap<Integer, Integer> yearData, AbstractMap.SimpleEntry<Integer, MainViewModel.ShowObject> metaData) {
                if(metaData != null && yearData.size() > 0){
                    findViewById(R.id.splash).setVisibility(View.GONE);
                }
                return false;
            }
        }).subscribe());

        RxView.clicks(findViewById(R.id.play_pause)).subscribe(click->{
            toggleAudio();
        });
    }

    private void expandHeader(boolean expand){
        ((AppBarLayout)findViewById(R.id.appbar_layout)).setExpanded(expand);
    }

    private void toggleAudio(){
        mMainViewModel.toggleAudio();
    }

    private void populatePlayer(MainViewModel.SongObject songObject){
        TextView marquee = ((TextView)findViewById(R.id.player_song));
        marquee.setText(songObject.title);
    //    marquee.setHorizontallyScrolling(true);
    //    marquee.setSelected(true);
        expandHeader(false);
    }

    private void populateNowPlaying(MainViewModel.ShowObject show){
        findViewById(R.id.total_shows).setVisibility(View.GONE);
        ((TextView)findViewById(R.id.latest_show_tag)).setText("NOW PLAYING");
        String title = show.title;
        String date = show.date;
        String location = show.location;
        String venue = show.venue;
        Log.d("@@@", " date: " + date);
        ((TextView)findViewById(R.id.show_location)).setText(show.location);
        ((TextView)findViewById(R.id.show_title)).setText(show.title);
        ((TextView)findViewById(R.id.show_date)).setText(show.date);
        ((TextView)findViewById(R.id.show_venue)).setText(show.venue + ", ");
    }

    private void populateMetaData(AbstractMap.SimpleEntry<Integer, MainViewModel.ShowObject> data){
        mBandName = mMainViewModel.getBandObject(mCurrentBand).getCanonical();
        mLatestShowObject = data.getValue();
        Log.d("@@@, ", " title iss: " +mLatestShowObject.title + ".");
        ((TextView)findViewById(R.id.total_shows)).setText("SHOWS: " + data.getKey());
        ((TextView)findViewById(R.id.band_name)).setText("" +mBandName);
        ((TextView)findViewById(R.id.show_title)).setText("" + mLatestShowObject.title);
        ((TextView)findViewById(R.id.show_date)).setText("" + mLatestShowObject.date);
        ((TextView)findViewById(R.id.show_location)).setText("" + mLatestShowObject.location);
        ((TextView)findViewById(R.id.show_venue)).setText("" + mLatestShowObject.venue + ", ");
    }

    private void playAudio(String audio){
        Log.d("@@@", " playAudio. audio link " + URI.create(audio));
        Uri uri = Uri.parse(audio);
        // Log.d("@@@, ", " track length: " + Integer.parseInt(audio))

        if(mMediaPlayer == null){
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setVolume(10.0f,10.0f);
        }

        if(mMediaPlayer.isPlaying()){
            Log.d("@@@, ","media player stop");
            mMediaPlayer.stop();
        }

        try {
            Log.d("@@@, "," media player prepare");
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(audio);
            mMediaPlayer.setOnPreparedListener(mediaPlayer -> {
                mMediaPlayer.start();
                mMediaPlayer.setOnCompletionListener(player -> {
                   // get next song
                   // mMainViewModel.prepareNextSong();
                });
                mSeekbar.setMax(mMediaPlayer.getDuration());
                mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if(mMediaPlayer != null && fromUser){
                            Log.d("@@@", " seek to: " + (progress*1000));
                            mMediaPlayer.seekTo(progress);
                        }
                    }
                });
                Handler mHandler = new Handler();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mMediaPlayer != null){
                            int mCurrentPosition = mMediaPlayer.getCurrentPosition();
                            //Log.d("@@@", " seekbar set progress: " + mCurrentPosition);
                            mSeekbar.setProgress(mCurrentPosition);
                        }
                        mHandler.postDelayed(this, 1000);
                    }
                });
            });
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("@@@", " something went wrong creating media player");
        }
        mMainViewModel.startAudio();
    }

    private void updateCatalog(){
        mMainViewModel.updateCatalog();
    }

    @Override
    public void onBackPressed() {
        if(mCatalogInterface != null){
            mCatalogInterface.onBackPressed();
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void setCatalogInterface(ICatalogInterface catalog) {
        mCatalogInterface = catalog;
    }

    public class MainPagerAdapter extends FragmentPagerAdapter{

        private IPager mPager;

        public MainPagerAdapter(IPager pager, FragmentManager fm) {
            super(fm);
            mPager = pager;
        }

        @Override
        public Fragment getItem(int position) {
             switch(position){
                 case 0:{
                     Log.d("@@", " new catalog!");
                     CatalogFragment fragment = new CatalogFragment();
                     fragment.setViewModel(mMainViewModel);
                     mPager.setCatalogInterface(fragment);
                     return fragment;
                 }
                 case 1:
                 return new JamFragment();

             }
             return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "CATALOG";
                case 1:
                default:
                    return "WHAT'S GOUDA";
            }
        }
    }
}
