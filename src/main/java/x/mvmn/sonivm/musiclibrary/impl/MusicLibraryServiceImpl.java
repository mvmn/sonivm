package x.mvmn.sonivm.musiclibrary.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;

import x.mvmn.sonivm.musiclibrary.MusicLibraryChangeListener;
import x.mvmn.sonivm.musiclibrary.MusicLibraryEntry;
import x.mvmn.sonivm.musiclibrary.MusicLibraryService;

@Service
public class MusicLibraryServiceImpl implements MusicLibraryService {
    
    private final List<MusicLibraryEntry> entries = new CopyOnWriteArrayList<>();
    private final List<MusicLibraryChangeListener> changeListeners = new CopyOnWriteArrayList<>();

    @Override
    public void scanForMusicFiles(File rootDirectory) {
        // Implementation would walk the directory tree and find music files
        // This is a placeholder for actual file scanning logic
    }

    @Override
    public List<MusicLibraryEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    @Override
    public MusicLibraryEntry getEntryByIndex(int index) {
        return entries.size() > index && index >= 0 ? entries.get(index) : null;
    }

    @Override
    public int getSize() {
        return entries.size();
    }

    @Override
    public List<MusicLibraryEntry> search(Predicate<MusicLibraryEntry> criteria) {
        List<MusicLibraryEntry> result = new ArrayList<>();
        for (MusicLibraryEntry entry : entries) {
            if (criteria.test(entry)) {
                result.add(entry);
            }
        }
        return result;
    }

    @Override
    public void addChangeListener(MusicLibraryChangeListener listener) {
        changeListeners.add(listener);
    }

    @Override
    public void removeChangeListener(MusicLibraryChangeListener listener) {
        changeListeners.remove(listener);
    }

    @Override
    public void reSync() {
        // Clear current entries and rescan the library
        entries.clear();
        scanForMusicFiles(new File(System.getProperty("sonivm_home_folder", ".")));
        notifyListeners();
    }

    private void notifyListeners() {
        changeListeners.forEach(MusicLibraryChangeListener::onMusicLibraryUpdate);
    }
}