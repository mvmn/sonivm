package x.mvmn.sonivm.musiclibrary.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import x.mvmn.sonivm.musiclibrary.MusicLibraryChangeListener;
import x.mvmn.sonivm.musiclibrary.MusicLibraryEntry;
import x.mvmn.sonivm.musiclibrary.MusicLibraryPersistenceService;
import x.mvmn.sonivm.musiclibrary.MusicLibraryService;
import x.mvmn.sonivm.playqueue.PlaybackQueueEntry.TrackMetadata;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.tag.TagRetrievalService;

@Service
public class MusicLibraryServiceImpl implements MusicLibraryService {

	private static final Logger LOGGER = Logger.getLogger(MusicLibraryServiceImpl.class.getCanonicalName());

	@Autowired
	private MusicLibraryPersistenceService musicLibraryPersistenceService;

	@Autowired
	private TagRetrievalService tagRetrievalService;

	@Autowired
	private PreferencesService preferencesService;

	private final ExecutorService tagReadingTaskExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	private final AtomicInteger activeTagReadingTasks = new AtomicInteger(0);

    private final List<MusicLibraryEntry> entries = new ArrayList<>();
    private final List<MusicLibraryChangeListener> changeListeners = new CopyOnWriteArrayList<>();

    @Override
    public void scanForMusicFiles(File rootDirectory) {
        if (!rootDirectory.exists() || !rootDirectory.isDirectory()) {
            LOGGER.warning("Root directory does not exist or is not a directory: " + rootDirectory.getAbsolutePath());
            return;
        }

        Set<String> supportedExtensions = preferencesService.getSupportedFileExtensions();
        Set<String> seenPaths = new HashSet<>();
        List<File> musicFiles = new ArrayList<>();

        try {
            Path rootPath = rootDirectory.toPath();
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String extension = FilenameUtils.getExtension(file.getFileName().toString());
                    if (supportedExtensions.contains(extension.toLowerCase())) {
                        String fullPath = file.toAbsolutePath().toString();
                        if (seenPaths.add(fullPath)) {
                            musicFiles.add(file.toFile());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    LOGGER.log(Level.WARNING, "Failed to visit file: " + file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to walk directory tree: " + rootDirectory.getAbsolutePath(), e);
            return;
        }

        musicFiles.sort((File a, File b) -> a.getName().compareToIgnoreCase(b.getName()));

        if (musicFiles.isEmpty()) {
            return;
        }

        AtomicInteger completedCount = new AtomicInteger(0);
        for (File file : musicFiles) {
            submitTagReadingTask(file, musicFiles.size(), completedCount);
        }
    }

    private void submitTagReadingTask(File file, int totalFiles, AtomicInteger completedCount) {
        activeTagReadingTasks.incrementAndGet();
        tagReadingTaskExecutor.submit(() -> {
            try {
                TrackMetadata tagMetadata = tagRetrievalService.getAudioFileMetadata(file);

                MusicLibraryEntry.TrackMetadata libraryMetadata = null;
                if (tagMetadata != null) {
                    libraryMetadata = MusicLibraryEntry.TrackMetadata.builder()
                            .trackNumber(tagMetadata.getTrackNumber())
                            .artist(tagMetadata.getArtist())
                            .album(tagMetadata.getAlbum())
                            .title(tagMetadata.getTitle())
                            .date(tagMetadata.getDate())
                            .genre(tagMetadata.getGenre())
                            .duration(tagMetadata.getDuration())
                            .build();
                }

                MusicLibraryEntry entry = MusicLibraryEntry.builder()
                        .targetFileFullPath(file.getAbsolutePath())
                        .targetFileName(file.getName())
                        .trackMetadata(libraryMetadata)
                        .duration(tagMetadata != null && tagMetadata.getDuration() != null ? tagMetadata.getDuration() : null)
                        .build();

                synchronized (entries) {
                    entries.add(entry);
                }

                LOGGER.fine("Scanned: " + file.getAbsolutePath());
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Failed to read tags for file: " + file.getAbsolutePath(), t);
            } finally {
                int completed = completedCount.incrementAndGet();
                if (activeTagReadingTasks.decrementAndGet() == 0) {
                    synchronized (entries) {
                        entries.sort((MusicLibraryEntry a, MusicLibraryEntry b) -> {
                            int c = compareNulls(a.getArtist(), b.getArtist());
                            if (c != 0) return c;
                            c = compareNulls(a.getAlbum(), b.getAlbum());
                            if (c != 0) return c;
                            int trackA = parseTrackNumber(a.getTrackNumber());
                            int trackB = parseTrackNumber(b.getTrackNumber());
                            return Integer.compare(trackA, trackB);
                        });
                    }
                    List<MusicLibraryEntry> snapshot;
                    synchronized (entries) {
                        snapshot = new ArrayList<>(entries);
                    }
                    musicLibraryPersistenceService.saveMusicLibrary(snapshot);
                    SwingUtilities.invokeLater(() -> notifyListeners());
                }
            }
        });
    }

    @Override
    public List<MusicLibraryEntry> getEntries() {
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }

    @Override
    public MusicLibraryEntry getEntryByIndex(int index) {
        synchronized (entries) {
            return entries.size() > index && index >= 0 ? entries.get(index) : null;
        }
    }

    @Override
    public int getSize() {
        synchronized (entries) {
            return entries.size();
        }
    }

    @Override
    public List<MusicLibraryEntry> search(Predicate<MusicLibraryEntry> criteria) {
        synchronized (entries) {
            List<MusicLibraryEntry> result = new ArrayList<>();
            for (MusicLibraryEntry entry : entries) {
                if (criteria.test(entry)) {
                    result.add(entry);
                }
            }
            return result;
        }
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
        List<MusicLibraryEntry> loadedEntries = musicLibraryPersistenceService.loadMusicLibrary();
        synchronized (entries) {
            entries.clear();
            if (loadedEntries != null && !loadedEntries.isEmpty()) {
                entries.addAll(loadedEntries);
            }
        }
        notifyListenersOnEDT();

        synchronized (entries) {
            entries.clear();
        }

        List<String> folders = preferencesService.getMusicLibraryFolders();
        if (folders != null && !folders.isEmpty()) {
            for (String folderPath : folders) {
                scanForMusicFiles(new File(folderPath));
            }
        }
    }

    private void notifyListenersOnEDT() {
        if (SwingUtilities.isEventDispatchThread()) {
            notifyListeners();
        } else {
            SwingUtilities.invokeLater(() -> notifyListeners());
        }
    }

    private void notifyListeners() {
        changeListeners.forEach(MusicLibraryChangeListener::onMusicLibraryUpdate);
    }

    public void shutdown() {
        LOGGER.info("Shutting down music library tag reading executor.");
        tagReadingTaskExecutor.shutdownNow();
    }

    private static int compareNulls(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareToIgnoreCase(b);
    }

    private static int parseTrackNumber(String trackNumber) {
        if (trackNumber == null) return 999999;
        try {
            return Integer.parseInt(trackNumber);
        } catch (NumberFormatException e) {
            return 999999;
        }
    }
}