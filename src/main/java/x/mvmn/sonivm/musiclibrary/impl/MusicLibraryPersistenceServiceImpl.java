package x.mvmn.sonivm.musiclibrary.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import x.mvmn.sonivm.musiclibrary.MusicLibraryEntry;
import x.mvmn.sonivm.musiclibrary.MusicLibraryPersistenceService;

@Component
@RequiredArgsConstructor
public class MusicLibraryPersistenceServiceImpl implements MusicLibraryPersistenceService {
    
    private static final Logger LOGGER = Logger.getLogger(MusicLibraryPersistenceServiceImpl.class.getSimpleName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @Override
    public void saveMusicLibrary(List<MusicLibraryEntry> entries) {
        try {
            LOGGER.info("Storing music library.");
            File storageFile = getStorageFile();
            OBJECT_MAPPER.writeValue(storageFile, entries);
            LOGGER.info("Storing music library succeeded.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to store the music library", e);
        }
    }

    @Override
    public List<MusicLibraryEntry> loadMusicLibrary() {
        try {
            LOGGER.info("Loading music library.");
            File storageFile = getStorageFile();
            
            if (storageFile.exists()) {
                List<MusicLibraryEntry> entries = OBJECT_MAPPER.readValue(storageFile, 
                    new com.fasterxml.jackson.core.type.TypeReference<List<MusicLibraryEntry>>() {});
                LOGGER.info("Loading music library succeeded.");
                return entries;
            } else {
                LOGGER.info("No existing music library found to load.");
                return new ArrayList<>();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load the music library", e);
            return new ArrayList<>();
        }
    }

    @Override
    public File getStorageFile() {
        return new File(new File(System.getProperty("sonivm_home_folder")), "music_library.json");
    }
}