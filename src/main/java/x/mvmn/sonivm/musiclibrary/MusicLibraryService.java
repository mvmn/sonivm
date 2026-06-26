package x.mvmn.sonivm.musiclibrary;

import java.io.File;
import java.util.List;
import java.util.function.Predicate;

public interface MusicLibraryService {
    void scanForMusicFiles(File rootDirectory);
    
    List<MusicLibraryEntry> getEntries();
    
    MusicLibraryEntry getEntryByIndex(int index);
    
    int getSize();
    
    List<MusicLibraryEntry> search(Predicate<MusicLibraryEntry> criteria);
    
    void addChangeListener(MusicLibraryChangeListener listener);
    
    void removeChangeListener(MusicLibraryChangeListener listener);

    void reSync();
}