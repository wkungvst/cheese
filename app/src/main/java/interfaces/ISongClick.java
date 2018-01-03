package interfaces;

import viewmodels.MainViewModel;

/**
 * Created by wkung on 12/18/17.
 */

public interface ISongClick {
    void openSong(MainViewModel.SongObject song, MainViewModel.SongObject nextSong);
}
