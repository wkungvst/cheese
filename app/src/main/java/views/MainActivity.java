package views;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Timer;
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

    private static final int TIMEOUT_MS = 10000;
    private CompositeSubscription mCompositeSubscription;
    private MainViewModel mMainViewModel;
    private MainPagerAdapter mMainPagerAdapter;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private MainViewModel.BAND mCurrentBand;
    private ICatalogInterface mCatalogInterface;
    private MainViewModel.ShowObject mLatestShowObject;
    private MediaPlayer mMediaPlayer;
    private SeekBar mSeekbar;
    private String mBandName = "";
    private EditText mSearchEdit;
    private Boolean mSongHasPlayed = false;
    private FloatingActionButton mBandsButton;
    private CountDownTimer mCountDownTimer;
    private Snackbar mSnackbar;

    private static String[] TABS = {"Jam Of The Day", "Catalog"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startTimeoutCounter();

        mMainViewModel = new MainViewModel(this);
        mMainPagerAdapter = new MainPagerAdapter(this, getSupportFragmentManager());
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getString("BAND") != null){
            mMainViewModel.setup(extras.getString("BAND"));
        }else{
            mMainViewModel.setup();
        }
        ((TextView)findViewById(R.id.bottom_tag)).setText(Html.fromHtml("&#9834; &#9835; Let Your Love Light Shine &#9835; &#9834;"));
        mTabLayout = (TabLayout)findViewById(R.id.tab_layout);
        mViewPager = (ViewPager)findViewById(R.id.pager);
        mSeekbar = findViewById(R.id.player_seekbar);
        mSearchEdit = findViewById(R.id.search_edittext);
        mBandsButton = findViewById(R.id.bands_button);
        mBandsButton.setOnClickListener(view -> openBandsPopup());

        RxView.clicks(findViewById(R.id.bands_popup_cancel)).subscribe(
            click->{
                closeBandsPopup();
            }
        );

        mMediaPlayer = new MediaPlayer();

        mSeekbar.getProgressDrawable().setColorFilter(Color.parseColor("#ffffff"), PorterDuff.Mode.SRC_IN);
        mSeekbar.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

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
        initialize();
        addSubscriptions();
    }

    private void startTimeoutCounter(){
        mCountDownTimer = new CountDownTimer(TIMEOUT_MS, 1000) {

            public void onTick(long millisUntilFinished) {}

            public void onFinish() {
                showTimeoutError();
                Log.d("@#@@,  ", " NETWORK TIMER@!!");
            }
        }.start();
    }

    private void showTimeoutError(){
        clearTimeoutCount();
        mSnackbar = Snackbar
                .make(findViewById(R.id.parent), "No Network Connection.", Snackbar.LENGTH_INDEFINITE);
        mSnackbar.setAction("TRY AGAIN", view -> {
            Intent i = new Intent(MainActivity.this, MainActivity.class);
            startActivity(i);
        });
        mSnackbar.setActionTextColor(Color.parseColor("#4e6182"));
        mSnackbar.show();
    }

    private void clearTimeoutCount(){
        mCountDownTimer.cancel();
        if(mSnackbar != null){ mSnackbar.dismiss();}
    }

    private void initialize(){
        mSearchEdit.setHint(Html.fromHtml("<b>search </b>song names <b>&#9834; &#9835; </b>"));
    }

    private void addSubscriptions(){
        mCompositeSubscription = new CompositeSubscription();

        RxView.clicks(findViewById(R.id.show_widget)).subscribe(c->{
            mMainViewModel.openCurrentShow();
        });

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
                expandHeader(false);
            }else{
                Log.d("@@@", "songs observable. size is 0");
            }
        }));

        mCompositeSubscription.add(mMainViewModel.getAudioLinkObservable().subscribe(audio->{
            Log.d("@@@", " get audio link observable. play audio");
            playAudio(audio);
            if(mMainViewModel.getCurrentShow() != null){
                populateNowPlaying(mMainViewModel.getCurrentShow());
            }
        }));

        mCompositeSubscription.add(mMainViewModel.getCurrentSongObservable().subscribe(songObject->{
            if(songObject != null){
                mMainViewModel.openPlayer();
                populatePlayer(songObject);
            }
        }));

        mCompositeSubscription.add(mMainViewModel.getCurrentSongFavoriteObservable().subscribe(favorite->{
            Log.d("@@@, ", " current song favorite observable updated! favorite? " + favorite);
            if(favorite){
                ((ImageButton)findViewById(R.id.player_favorite)).setImageResource(R.drawable.favorite);
            }else{
                ((ImageButton)findViewById(R.id.player_favorite)).setImageResource(R.drawable.favorite_empty);
            }
        }));

        mCompositeSubscription.add(mMainViewModel.getIsPlayingFavorite().subscribe(isFavorite->{
            if(isFavorite){
                ((TextView)findViewById(R.id.latest_show_tag)).setText("PLAYING FROM FAVORITES");
                findViewById(R.id.show_widget).setVisibility(View.GONE);
            }else{
                ((TextView)findViewById(R.id.latest_show_tag)).setText("NOW PLAYING");
                findViewById(R.id.show_widget).setVisibility(View.VISIBLE);
            }
        }));

        mCompositeSubscription.add(mMainViewModel.getIsPlaying().subscribe(
            playing->{
                ((ImageButton)findViewById(R.id.play_pause)).setImageResource(playing? R.drawable.pause : R.drawable.play);
                if(playing){
                    if(!mMediaPlayer.isPlaying()){
                        Log.d("@@@", "mediaPlayer start");
                        mMediaPlayer.start();
                    }
                }else{
                    Log.d("@@@", "mediaPlayer pause");
                    mMediaPlayer.pause();
                }
            }
        ));

        mCompositeSubscription.add(rx.Observable.combineLatest(mMainViewModel.getYearDataObservable(), mMainViewModel.getMetaDataObservable(), new Func2<TreeMap<Integer,Integer>, AbstractMap.SimpleEntry<Integer,MainViewModel.ShowObject>, Boolean>() {
            @Override
            public Boolean call(TreeMap<Integer, Integer> yearData, AbstractMap.SimpleEntry<Integer, MainViewModel.ShowObject> metaData) {
                if(metaData != null && yearData.size() > 0){
                    findViewById(R.id.splash).setVisibility(View.GONE);
                    findViewById(R.id.bands_button).setVisibility(View.VISIBLE);
                    clearTimeoutCount();
                }
                return false;
            }
        }).subscribe());

        mCompositeSubscription.add(mMainViewModel.getErrorObservable().subscribe(error->{
            Log.d("@@@", " error: " + error.getMessage());
        }));

        mCompositeSubscription.add(mMainViewModel.getIsSearchingObservable().subscribe(searching->{
            if(searching){
                findViewById(R.id.search_progress).setVisibility(View.VISIBLE);
                findViewById(R.id.search_button).setVisibility(View.GONE);
            }else{
                 findViewById(R.id.search_progress).setVisibility(View.GONE);
            }
        }));

        mCompositeSubscription.add(mMainViewModel.getNextAudioLink().subscribe(nextAudio->{
            prepareNextSong(nextAudio);
        }));

        // search song - show result count
        mCompositeSubscription.add(mMainViewModel.getSearchResultCountObservable().subscribe(count->{
            if(count > 0){
                showSearchResultCount(count);
                mMainViewModel.setSearchModeOn(true);
            }else{
                showSearchResultCount(0);
                // TODO: show no results screen
            }
        }));

        mCompositeSubscription.add(mMainViewModel.getPlayerVisibleObservable().subscribe(visible->{
            Log.d("@@@, ", " should be visible? " + visible);
            findViewById(R.id.player).setVisibility(visible? View.VISIBLE : View.GONE);
        }));

        mCompositeSubscription.add(mMainViewModel.getSearchModeOnObservable().subscribe(searchOn->{
            if(searchOn){
                mTabLayout.getTabAt(0).setText("SEARCH RESULTS");
                TabLayout.Tab tab = mTabLayout.getTabAt(0);
                if (tab != null){
                    tab.select();
                }
            }else{
                mTabLayout.getTabAt(0).setText("CATALOG");
            }
        }));

        findViewById(R.id.player_close).setOnClickListener(click->{
            mMainViewModel.closePlayer();
        });

        RxView.clicks(findViewById(R.id.play_pause)).subscribe(click->{
            toggleAudio();
        });

        RxView.clicks(findViewById(R.id.play_prev)).subscribe(click->{
            seekToPrevious();
        });

        RxView.clicks(findViewById(R.id.play_next)).subscribe(click->{
            seekToNext();
        });

        RxView.clicks(findViewById(R.id.player_favorite)).subscribe(click->{
            mMainViewModel.addToFavorites();
        });

        RxView.clicks(findViewById(R.id.search_result_container)).subscribe(click->{
            showSearchButton(true);
            clearSearch();
            mMainViewModel.setSearchModeOn(false);
        });

        RxView.clicks(findViewById(R.id.search_button)).subscribe(click->{
            performSearch();
        });

        ((EditText)findViewById(R.id.search_edittext)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch();
                    return true;
                }
                return false;
            }
        });
    }

    private void closeBandsPopup(){
        findViewById(R.id.bands_popup).setVisibility(View.GONE);
    }

    private void clearSearch(){
        ((EditText)findViewById(R.id.search_edittext)).setText("");
    }

    private void showSearchButton(boolean show){
        findViewById(R.id.search_button).setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.search_result_container).setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showSearchResultCount(int count){
        findViewById(R.id.search_button).setVisibility(View.GONE);
        findViewById(R.id.search_result_container).setVisibility(View.VISIBLE);
        if(count == 0){
            //clearSearch();
            ((TextView)findViewById(R.id.search_result_count)).setText("NO SEARCH RESULTS.");
        }else if (count == 1){
            ((TextView)findViewById(R.id.search_result_count)).setText(count + " RESULT");
        }else{
            ((TextView)findViewById(R.id.search_result_count)).setText(count + " RESULTS");
        }

    }

    private void performSearch(){
        closeKeyboard();
        String term = ((EditText)findViewById(R.id.search_edittext)).getText().toString();
        if(term.length()  == 0 )return;
        performSearch(term);
    }

    private void performSearch(String term){
        mMainViewModel.performSearch(term);
    }

    private void closeKeyboard(){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(((EditText)findViewById(R.id.search_edittext)).getWindowToken(), 0);
    }

    private void seekToNext(){
        mMainViewModel.prepareNextSong();
    }

    private void seekToPrevious(){
        if(mMediaPlayer.isPlaying()){
            mMediaPlayer.seekTo(0);
        }
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
        expandHeader(false);
    }

    private void openBandsPopup() {
        if(mCurrentBand == MainViewModel.BAND.STRING_CHEESE_INCIDENT){
            openNewBand("GratefulDead");
        }else{
            openNewBand("StringCheeseIncident");
        }

    //    findViewById(R.id.bands_popup).setVisibility(View.VISIBLE);
    }

    private void populateNowPlaying(MainViewModel.ShowObject show){
        findViewById(R.id.total_shows).setVisibility(View.GONE);
        String title = show.title;
        String date = show.date;
        String location = show.location;
        String venue = show.venue;
        ((TextView)findViewById(R.id.show_location)).setText(show.location);
        ((TextView)findViewById(R.id.show_title)).setText(show.title.trim());
        ((TextView)findViewById(R.id.show_date)).setText(show.date);
        ((TextView)findViewById(R.id.show_venue)).setText(show.venue + ", ");
    }

    private void populateMetaData(AbstractMap.SimpleEntry<Integer, MainViewModel.ShowObject> data){
        mBandName = mMainViewModel.getBandObject(mCurrentBand).getCanonical();
        mLatestShowObject = data.getValue();
        ((TextView)findViewById(R.id.total_shows)).setText("SHOWS: " + data.getKey());
        ((TextView)findViewById(R.id.band_name)).setText("" +mBandName);
        ((TextView)findViewById(R.id.show_title)).setText("" + mLatestShowObject.title);
        ((TextView)findViewById(R.id.show_date)).setText("" + mLatestShowObject.date);
        ((TextView)findViewById(R.id.show_location)).setText("" + mLatestShowObject.location);
        ((TextView)findViewById(R.id.show_venue)).setText("" + mLatestShowObject.venue + ", ");
    }

    private void playAudio(String audio){

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
            mSongHasPlayed = false;
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(audio);
            mMediaPlayer.setOnPreparedListener(mediaPlayer -> {
                // start audio playback
                mMainViewModel.startAudio();
                mMediaPlayer.setOnCompletionListener(player -> {
                    Log.d("@@@", " media player on complete. prepare next song");
                    if(mSongHasPlayed){
                        mMainViewModel.prepareNextSong();
                    }else{
                        Log.d("@@@", " song hasn't even played yet. don't prepare next song");
                    }
                   // prepareNextSong();
                });
                mSongHasPlayed = true;
                mSeekbar.setMax(mMediaPlayer.getDuration());
                mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if(mMediaPlayer != null && fromUser){
                            //Log.d("@@@", " seek to: " + (progress*1000));
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
    }

    private void prepareNextSong(String audioLink){
        MediaPlayer nextMediaPlayer = new MediaPlayer();
        nextMediaPlayer.setVolume(10.0f,10.0f);
        try {
            nextMediaPlayer.setDataSource(audioLink);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void updateCatalog(){
        mMainViewModel.updateCatalog();
    }

    private void openNewBand(String band){
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        intent.putExtra("BAND", band);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if(mCatalogInterface != null){
            mCatalogInterface.onBackPressed();
        }
        mMainViewModel.setSearchModeOn(false);
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
                     CatalogFragment fragment = new CatalogFragment();
                     fragment.setViewModel(mMainViewModel);
                     mPager.setCatalogInterface(fragment);
                     return fragment;
                 }
                 case 1:
                     FavoritesFragment fragment = new FavoritesFragment();
                     fragment.setViewModel(mMainViewModel);
                     return fragment;

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
