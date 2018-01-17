package interfaces;

import viewmodels.MainViewModel;

/**
 * Created by wkung on 12/18/17.
 */

public interface IFavoriteSongClick {
    void openSong(MainViewModel.SongObject song, MainViewModel.SongObject nextSong);
    void deleteFavorite(MainViewModel.SongObject song);
}
