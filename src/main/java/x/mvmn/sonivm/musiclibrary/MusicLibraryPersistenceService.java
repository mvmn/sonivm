package x.mvmn.sonivm.musiclibrary;

import java.io.File;
import java.util.List;

public interface MusicLibraryPersistenceService {
    void saveMusicLibrary(List<MusicLibraryEntry> entries);
    
    List<MusicLibraryEntry> loadMusicLibrary();
    
    File getStorageFile();
}