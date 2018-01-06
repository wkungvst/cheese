package viewmodels;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.UUID;

import exceptions.NetworkException;
import rx.Observable;
import rx.subjects.BehaviorSubject;
import static viewmodels.MainViewModel.BAND.GRATEFUL_DEAD;
import static viewmodels.MainViewModel.BAND.STRING_CHEESE_INCIDENT;
import static viewmodels.MainViewModel.BAND.STS9;

/**
 * Created by wkung on 12/11/17.
 */

public class MainViewModel {

    BandObject band_1, band_2, band_3;
    private HashMap<BAND, BandObject> mBandsMap = new HashMap<>();
    private BehaviorSubject<BAND> mCurrentBand = BehaviorSubject.create();
    private BehaviorSubject<AbstractMap.SimpleEntry<Integer,ShowObject>> mMetaData = BehaviorSubject.create();
    private BehaviorSubject<TreeMap<Integer,Integer>> mYearData = BehaviorSubject.create(new TreeMap());
    private BehaviorSubject<ArrayList<ShowObject>> mShowObjectsForYear = BehaviorSubject.create();
    private BehaviorSubject<ArrayList<SongObject>> mSongs = BehaviorSubject.create();
    private BehaviorSubject<String> mAudioLink = BehaviorSubject.create();
    private BehaviorSubject<String> mNextAudioLink = BehaviorSubject.create();
    private BehaviorSubject<SongObject> mCurrentSong = BehaviorSubject.create();
    private BehaviorSubject<SongObject> mNextSong = BehaviorSubject.create();
    private BehaviorSubject<ShowObject> mCurrentShow = BehaviorSubject.create();
    private BehaviorSubject<NetworkException> mError = BehaviorSubject.create();
    public Observable<NetworkException> getErrorObservable(){ return mError.asObservable();}
    public Observable<String> getNextAudioLink(){ return mNextAudioLink.asObservable();}
    public ShowObject getCurrentShow(){return mCurrentShow.getValue();}
    private BehaviorSubject<Boolean> mIsPlaying = BehaviorSubject.create(false);
    public Observable<Boolean> getIsPlaying(){ return mIsPlaying.asObservable();}
    public Observable<SongObject> getCurrentSongObservable(){return mCurrentSong.asObservable();}
    public Observable<ShowObject> getCurrentShowObservable(){return mCurrentShow.asObservable();}
    public Observable<String> getAudioLinkObservable(){return mAudioLink.asObservable();}
    public Observable<ArrayList<SongObject>> getSongsObservable(){return mSongs.asObservable();};
    public Observable<ArrayList<ShowObject>> getShowObjectsForYearObservable(){ return mShowObjectsForYear.asObservable();}
    public Observable<AbstractMap.SimpleEntry<Integer, ShowObject>> getMetaDataObservable(){ return mMetaData.asObservable();}
    public Observable<TreeMap<Integer,Integer>> getYearDataObservable(){return mYearData.asObservable();}
    public Observable<BAND> getCurrentBandObservable(){
        return mCurrentBand.asObservable();
    }
    private static Context mContext;

    private UUID mId = UUID.randomUUID();

    public MainViewModel(Context context){
        mContext =  context;
    }

    public void setup(){
        band_1 = new BandObject(STRING_CHEESE_INCIDENT, 1996, 2017, "String Cheese Incident", "StringCheeseIncident");
        band_2 = new BandObject(GRATEFUL_DEAD, 1965, 1995, "Grateful Dead", "GratefulDead");
        band_3 = new BandObject(STS9, 1999, 2017, "Sound Tribe Sector 9", "SoundTribeSector9");
        mBandsMap.put(band_1.getBandName(), band_1);
        mBandsMap.put(band_2.getBandName(), band_2);
        mBandsMap.put(band_3.getBandName(), band_3);
        mCurrentBand.onNext(STRING_CHEESE_INCIDENT);
    }

    public void updateCatalog(){
        BandObject current = mBandsMap.get(mCurrentBand.getValue());
        if(current == null){
            Log.d("@@@", " MainViewModel updateCatalog: band object is null");
            return;
        }
        getMetaDataForBand(mContext, mCurrentBand.getValue());
        getYearDataForBand(mContext, mCurrentBand.getValue(), current.getStartYear(), current.getEndYear());
    }

    public BandObject getBandObject(BAND name){
        if(mBandsMap.containsKey(name)){
            return mBandsMap.get(name);
        }
        return null;
    }

    public UUID getId(){
        return mId;
    }



    // ** AUDIO PLAYBACK ** //

    public void toggleAudio(){
        mIsPlaying.onNext(!mIsPlaying.getValue());
    }

    public void startAudio(){
        mIsPlaying.onNext(true);
    }

    public void prepareNextSong(){
        SongObject song = mCurrentSong.getValue();
        Iterator<SongObject> i = mSongs.getValue().iterator();
        while(i.hasNext()){
            if(song == i.next()){
                if(i.hasNext()){
                    SongObject nextSong = i.next();
                    Log.d("@@@", " reset next audio link! " + nextSong.title);
                    String nextLink = "https://www.archive.org/download/" + nextSong.rootname + "/" + nextSong.name;
                    openSong(nextSong,null);
                }
            }
        }
    }

    public void getShowsOfYear(int year){
        RequestQueue queue = Volley.newRequestQueue(mContext);
        String band = mBandsMap.get(mCurrentBand.getValue()).getIdentifier();
        String location = constructShowsOfYearString(year, band);
        StringRequest req = new StringRequest(Request.Method.GET, location,
                response -> {
                    response = response.substring(9, response.length() - 1);
                    try {
                        ArrayList<ShowObject> showsForYear = parseShowsOfYear(response);
                        mShowObjectsForYear.onNext(showsForYear);
                    } catch (JSONException e) {
                        mError.onNext(new NetworkException(NetworkException.TYPE.JSON,e.getMessage()));
                        e.printStackTrace();
                    }
                    //Log.d("@@@, " , " result: " + response);
                },
                error -> Log.d("@@@", " error response " + error));
        queue.add(req);
    }

    private ArrayList<ShowObject> parseShowsOfYear(String response) throws JSONException{
        ArrayList<ShowObject> list = new ArrayList<>();
        JSONObject responseJSON = new JSONObject(response);
        responseJSON = new JSONObject(String.valueOf(responseJSON.getJSONObject("response")));
        JSONArray array = responseJSON.getJSONArray("docs");
        String description ="";
        String coverage = "";
        String venue = "";
        String date = "";
        String title = "";
        for(int i=0;i<array.length();i++){
            JSONObject obj = (JSONObject)array.get(i);
            try{
                description = obj.getString("description");
                coverage = obj.getString("coverage");
                venue = obj.getString("venue");
                date = obj.getString("date");
                if(date.length() > 15){
                    date = date.substring(0,date.length() - 10);
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    Date newDate = format.parse(date);
                    format = new SimpleDateFormat("MM-dd-yyyy");
                    date = format.format(newDate);
                    if(date.substring(0,1).equals("0")){
                        date = date.substring(1,date.length());
                    }
                }
                title = obj.getString("title").replace(mBandsMap.get(mCurrentBand.getValue()).canonical, "");
                if(title.length() > 15){ title = title.substring(0, title.length() - 14);}
            }catch(Exception e){
                mError.onNext(new NetworkException(NetworkException.TYPE.JSON," view model: parseShowsOfYear " + e.getMessage()));
            }
            list.add(new ShowObject(
                    title,
                    coverage,
                    venue,
                    date,
                    description,
                    obj.getString("identifier")));
        }
        Collections.sort(list, new DateComparator());
        return list;
    }

    private void getMetaDataForBand(Context context, BAND band){
        getMDTotalShows(context,band);
    }

    private void getMDTotalShows(Context context, BAND band){
        RequestQueue queue = Volley.newRequestQueue(context);
        String location = constructMDTotalShowsString(mBandsMap.get(band).getIdentifier());
        StringRequest req = new StringRequest(Request.Method.GET, location,
                response -> {
                    response = response.substring(9, response.length() - 1);
                    try {
                        AbstractMap.SimpleEntry<Integer,String> metaData = parseMDTotalShows(response);
                        if(metaData.getValue().length() > 0){
                            // we have the data. now create show object.
                            getLatestShowData(metaData.getKey(), metaData.getValue());
                        }
                    } catch (JSONException e) {
                        mError.onNext(new NetworkException(NetworkException.TYPE.JSON," view model: getMDTotalShows " + e.getMessage()));
                        e.printStackTrace();
                    }
                },
                error -> Log.d("@@@", " error response " + error));
        queue.add(req);
    }

    private void getLatestShowData(int totalShows, String show){
        RequestQueue queue = Volley.newRequestQueue(mContext);
        String location = "http://www.archive.org/download/" + show + "/" + show + "_meta.xml";
        Log.d("@@@, ", " get latest show: " + location);
        StringRequest req = new StringRequest(Request.Method.GET, location,
                response -> {
                    try {
                        ShowObject showObject = parseLatestShowData(show, response);
                        mMetaData.onNext(new AbstractMap.SimpleEntry<>(totalShows, showObject));
                    } catch (JSONException e) {
                        mError.onNext(new NetworkException(NetworkException.TYPE.JSON,"view model: getLatestShowData " + e.getMessage()));
                        e.printStackTrace();
                    }
                },
                error -> Log.d("@@@, ", " error response " + error));
        queue.add(req);
    }

    private void getYearDataForBand(Context context, BAND band, int startYear, int endYear) {

        RequestQueue queue = Volley.newRequestQueue(context);
        TreeMap<Integer,Integer> results = new TreeMap<>();
        for (int i = startYear; i <= endYear; i++) {
            String location = constructYearString(i, mBandsMap.get(band).getIdentifier());
            int finalI = i;
            StringRequest req = new StringRequest(Request.Method.GET, location,
                    response -> {
                        // response from archive.org is wrapped with a 'response()', so remove this to get the JSON object
                        response = response.substring(9, response.length() - 1);
                        try {
                            int numberOfShows = parseYearData(response);
                            results.put(finalI, numberOfShows);
                            if(finalI == endYear){
                                BandObject bandObject = mBandsMap.get(band);
                                bandObject.setYearData(results);
                                mBandsMap.put(band, bandObject);
                                mYearData.onNext(bandObject.getYearData());
                            }
                        } catch (JSONException e) {
                            mError.onNext(new NetworkException(NetworkException.TYPE.JSON," view model: getYearDataForBand " + e.getMessage()));
                            e.printStackTrace();
                        }
                    },
                    error -> Log.d("@@@, ", " error response"));
            queue.add(req);
        }
    }

    public void openSong(SongObject song, SongObject nextSong){
//        Log.d("@@@, ", " open this song: " + song.title + " next song: " + nextSong.title);
        if(song == mCurrentSong.getValue()) return;
        mCurrentSong.onNext(song);
        String link = "https://www.archive.org/download/" + song.rootname + "/" + song.name;
        if(nextSong != null){
            String nextLink = "https://www.archive.org/download/" + nextSong.rootname + "/" + nextSong.name;
            mNextAudioLink.onNext(nextLink);
        }else{
            Log.d("@@@, ", " view model. no next song");
        }
        mAudioLink.onNext(link);
    }

    public void openShow(ShowObject show){
        mCurrentShow.onNext(show);
        String id = show.file;
        RequestQueue queue = Volley.newRequestQueue(mContext);
        ArrayList<SongObject> results = new ArrayList<>();

        String location = "http://www.archive.org/download/" + id + "/" + id + "_files.xml";
        Log.d("@@@, ", " location: " + location);
        StringRequest req = new StringRequest(Request.Method.GET, location,
                response -> {
                    try {
                        mSongs.onNext(parseSongsForShow(id, response));
                    } catch (JSONException e) {
                        mError.onNext(new NetworkException(NetworkException.TYPE.JSON,"view model: openShow " + e.getMessage()));
                        e.printStackTrace();
                    }
                },
                error -> Log.d("@@@, ", " error response"));
        queue.add(req);
    }

    private AbstractMap.SimpleEntry<Integer, String> parseMDTotalShows(String response) throws JSONException{
        JSONObject responseJSON = new JSONObject(response);
        // get total shows
        responseJSON = new JSONObject(String.valueOf(responseJSON.getJSONObject("response")));
        int totalShows = Integer.valueOf(responseJSON.getString("numFound"));
        // get most recent show
        JSONArray recentShowJSON = responseJSON.getJSONArray("docs");
        String latestShow = recentShowJSON.getJSONObject(0).getString("identifier");
        return new AbstractMap.SimpleEntry<>(totalShows, latestShow);
    }

    private ShowObject parseLatestShowData(String file, String response) throws JSONException{
        JSONObject json = XML.toJSONObject(response).getJSONObject("metadata");
        String title = json.getString("title").replace(mBandsMap.get(mCurrentBand.getValue()).canonical, "");
        if(title.length() > 15){ title = title.substring(1, title.length() - 14);}
        String location = json.getString("coverage");
        String venue = json.getString("venue");
        String date = json.getString("date");
        Log.d("@@@", " latest show date: " + date);
        String description = json.getString("description");
        return new ShowObject(title,location,venue,date,description,file);
    }

    private ArrayList<SongObject> parseSongsForShow(String root, String response) throws JSONException {
        ArrayList<SongObject> results = new ArrayList<>();
        JSONObject json = XML.toJSONObject(response);
        json = json.getJSONObject("files");
        JSONArray array = json.getJSONArray("file");
        String name = "" , album= "", title = "", length = "", link ="";
        int track = 0;
        for(int i=0;i<array.length();i++){
            // mp3 format
            JSONObject song = array.getJSONObject(i);
            if(array.getString(i).contains(".mp3")){
                try{
                    name = song.getString("name");
                    title = song.getString("title");
                    length = song.getString("length");
                    track = Integer.parseInt(song.getString("track"));
                    SongObject songObject = new SongObject(root, name, title, length, track);
                    results.add(songObject);
                }catch (Exception e){
                    mError.onNext(new NetworkException(NetworkException.TYPE.JSON,"view model: parseSongsForShow " + e.getMessage()));
                }
            }
        }
        // maybe it's a flac
        if(results.size() == 0){
            for(int i=0;i<array.length();i++){
                // mp3 format
                JSONObject song = array.getJSONObject(i);
                Log.d("@@@", " we have a song " + song.getString("name"));
                if(song.getString("name").contains(".flac")){
                    try{
                        name = song.getString("name");
                        title = song.getString("title");
                        length = song.getString("length");
                        track = Integer.parseInt(song.getString("track"));
                        SongObject songObject = new SongObject(root, name, title, length, track);
                        results.add(songObject);
                    }catch (Exception e){
                        mError.onNext(new NetworkException(NetworkException.TYPE.JSON,"view model: parseSongsForShow " + e.getMessage()));
                    }
                }
            }
        }
        Collections.sort(results, (song1, song2) -> {
            if(song1.track > song2.track) return 1;
            return -1;
        });
        return results;
    }

    private int parseYearData(String response) throws JSONException{
        JSONObject responseJSON = new JSONObject(response);
        responseJSON = new JSONObject(String.valueOf(responseJSON.getJSONObject("response")));
        return Integer.valueOf(responseJSON.getString("numFound"));
    }

    private String constructShowsOfYearString(int year, String artist){
        return "https://archive.org/advancedsearch.php?q=collection%3A%28" + artist + "%29+AND+date%3A%5B" + year + "-01-01+TO+" + year + "-12-31%5D&fl%5B%5D=coverage&fl%5B%5D=date&fl%5B%5D=description&fl%5B%5D=venue&fl%5B%5D=identifier&fl%5B%5D=title&sort%5B%5D=&sort%5B%5D=&sort%5B%5D=&rows=1000&page=1&callback=callback&save=yes&output=json";
    }

    private String constructMDTotalShowsString(String artist){
        String url ="https://archive.org/advancedsearch.php?q=collection%3A%28" + artist;
        url += "%29&fl%5B%5D=identifier&sort%5B%5D=date+desc&sort%5B%5D=&sort%5B%5D=&rows=1&page=1&callback=callback&save=yes&output=json";
        return url;
    }

    private String constructYearString(int year, String artist){
        String url = "https://archive.org/advancedsearch.php?q=collection%3A%28" + artist;
        url += "%29+AND+date%3A%5B";
        url += year + "-01-01+TO+" + year + "-12-31 %5D&fl%5B%5D=identifier&sort%5B%5D=addeddate+desc&sort%5B%5D=&sort%5B%5D=&rows=10000&page=1&callback=callback&save=yes&output=json";
        return url;
    }

    public enum BAND{
        STRING_CHEESE_INCIDENT,
        GRATEFUL_DEAD,
        STS9
    }

    public class ShowObject{

        public String title;
        public String location;
        public String venue;
        public String date;
        public String file;

        public ShowObject(String title, String location, String venue, String date, String description, String file){
            this.title = title;
            this.location = location;
            this.venue = venue;
            this.date = date;
            this.file = file;
        }
    }

    public class SongObject{
        public String rootname;
        public String name;
        public String title;
        public String length;
        public int track;

        public SongObject(String rootname, String name, String title, String length, int track){
            this.rootname = rootname;
            this.name = name;
            this.title = title;
            this.length = length;
            this.track = track;
        }
    }

    public class BandObject{

        int startYear;
        int endYear;
        String canonical;
        String identifier;
        BAND band;
        TreeMap<Integer,Integer> yearData;

        public BandObject(BAND band, int startYear, int endYear, String cononical, String identifier){
            this.band = band;
            this.startYear = startYear;
            this.endYear = endYear;
            this.canonical = cononical;
            this.identifier = identifier;
        }

        public int getStartYear(){return startYear;}
        public int getEndYear(){return endYear;}
        public BAND getBandName(){return band;}
        public String getCanonical(){return canonical;}
        public String getIdentifier(){return identifier;}
        public TreeMap<Integer,Integer> getYearData(){return yearData;}

        public void setYearData(TreeMap<Integer, Integer> yearData){
            this.yearData = yearData;
        }
    }

    class DateComparator implements Comparator<ShowObject>{
        @Override
        public int compare(ShowObject show1, ShowObject show2) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            try {
                if(df.parse(show1.date).after(df.parse(show2.date))){
                    return 1;
                }
                return -1;
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }
}
