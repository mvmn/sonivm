package x.mvmn.sonivm.playqueue.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import x.mvmn.sonivm.SonivmShutdownListener;
import x.mvmn.sonivm.cue.CueData;
import x.mvmn.sonivm.cue.CueData.CueDataFileData;
import x.mvmn.sonivm.cue.CueData.CueDataTrackData;
import x.mvmn.sonivm.cue.CueSheetParser;
import x.mvmn.sonivm.playqueue.PlaybackQueueEntry;
import x.mvmn.sonivm.playqueue.PlaybackQueueEntry.TrackMetadata;
import x.mvmn.sonivm.playqueue.PlaybackQueueFileImportService;
import x.mvmn.sonivm.playqueue.PlaybackQueueService;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.tag.TagRetrievalService;
import x.mvmn.sonivm.ui.SonivmUI;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.util.AudioFileUtil;
import x.mvmn.sonivm.util.TimeDateUtil;

@Service
public class PlaybackQueueFileImportServiceImpl implements PlaybackQueueFileImportService, SonivmShutdownListener {

	private static final Logger LOGGER = Logger.getLogger(PlaybackQueueFileImportServiceImpl.class.getCanonicalName());

	@Autowired
	private PlaybackQueueService playbackQueueService;

	@Autowired
	private TagRetrievalService tagRetrievalService;

	@Autowired
	private SonivmUI sonivmUI;

	@Autowired
	private PreferencesService preferencesService;

	private final ExecutorService tagReadingTaskExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	private volatile boolean shutdownRequested = false;

	@Override
	public void importFilesIntoPlayQueue(int queuePosition, List<File> filesToImport, Consumer<String> importProgressListener) {
		filesToImport.sort(Comparator.comparing(File::getName));
		if (queuePosition < 0) {
			queuePosition = playbackQueueService.getQueueSize();
		}
		for (File file : filesToImport) {
			queuePosition += addFileToQueue(queuePosition, file, importProgressListener);
			if (this.shutdownRequested) {
				break;
			}
		}
	}

	private int addFileToQueue(int queuePosition, File file, Consumer<String> importProgressListener) {
		if (this.shutdownRequested) {
			return 0;
		}
		try {
			if (!file.exists()) {
				return 0;
			}

			Set<String> supportedExtensions = preferencesService.getSupportedFileExtensions();
			if (file.isDirectory()) {
				File[] filesInDirectory = file.listFiles();
				Map<String, File> filesInDirByName = Stream.of(filesInDirectory)
						.collect(Collectors.toMap(File::getName, f -> f, (a, b) -> a, HashMap::new));

				Set<String> cueFileNames = filesInDirByName.keySet()
						.stream()
						.filter(aFileName -> aFileName.toLowerCase().endsWith(".cue"))
						.collect(Collectors.toSet());

				int countOfTracksAddedFromAllCueFiles = 0;
				for (String cueFileName : cueFileNames) {
					List<PlaybackQueueEntry> tracksFromCueFileToAdd = new ArrayList<>();
					File cueFile = filesInDirByName.get(cueFileName);
					List<File> cueTargetFiles = processCueFile(file, cueFile, tracksFromCueFileToAdd);
					if (!tracksFromCueFileToAdd.isEmpty()) {
						playbackQueueService.addRows(queuePosition, tracksFromCueFileToAdd);
						importProgressListener.accept("tracks from CUE file " + file.getName());
						countOfTracksAddedFromAllCueFiles += tracksFromCueFileToAdd.size();
						// Skip file that was added as individual tracks by CUE sheet
						cueTargetFiles.stream().map(File::getName).forEach(filesInDirByName::remove);
					}
				}
				queuePosition += countOfTracksAddedFromAllCueFiles;

				int addedFromRemainingFiles = 0;
				List<PlaybackQueueEntry> entriesToAdd = new ArrayList<>();
				for (File remainingFile : filesInDirByName.values()
						.stream()
						.sorted(Comparator.comparing(File::getName))
						.collect(Collectors.toList())) {
					String fileName = remainingFile.getName();
					String extension = FilenameUtils.getExtension(fileName);
					if (remainingFile.isDirectory()) {
						addedFromRemainingFiles += addFileToQueue(queuePosition + addedFromRemainingFiles, remainingFile,
								importProgressListener);
					} else if (supportedExtensions.contains(extension.toLowerCase())) {
						entriesToAdd.add(fileToQueueEntry(remainingFile));
					}
				}

				playbackQueueService.addRows(queuePosition + addedFromRemainingFiles, entriesToAdd);
				importProgressListener.accept("files from directory " + file.getName());
				entriesToAdd.forEach(this::createTagReadingTask);

				return addedFromRemainingFiles + countOfTracksAddedFromAllCueFiles + entriesToAdd.size();
			}

			String fileName = file.getName();
			String extension = FilenameUtils.getExtension(fileName);

			if ("cue".equalsIgnoreCase(extension) && supportedExtensions.contains("cue")) {
				List<PlaybackQueueEntry> tracksFromCueToAdd = new ArrayList<>();
				processCueFile(file.getParentFile(), file, tracksFromCueToAdd);
				if (!tracksFromCueToAdd.isEmpty()) {
					playbackQueueService.addRows(queuePosition, tracksFromCueToAdd);
					tracksFromCueToAdd.size();
				}

				importProgressListener.accept("tracks from CUE file " + file.getName());

				return tracksFromCueToAdd.size();
			}

			if (supportedExtensions.contains(extension.toLowerCase())) {
				PlaybackQueueEntry queueEntry = fileToQueueEntry(file);
				List<PlaybackQueueEntry> newEntries = Arrays.asList(queueEntry);
				if (queuePosition >= 0) {
					playbackQueueService.addRows(queuePosition, newEntries);
				} else {
					playbackQueueService.addRows(newEntries);
				}
				importProgressListener.accept(file.getName());
				createTagReadingTask(queueEntry);
				return 1;
			} else {
				return 0;
			}
		} catch (Throwable t) {
			LOGGER.log(Level.SEVERE, "Failed to add file to play queue: " + file.getAbsolutePath(), t);
		}
		return 0;
	}

	private PlaybackQueueEntry fileToQueueEntry(File file) {
		return PlaybackQueueEntry.builder().targetFileFullPath(file.getAbsolutePath()).targetFileName(file.getName()).build();
	}

	private void createTagReadingTask(PlaybackQueueEntry queueEntry) {
		tagReadingTaskExecutor.submit(() -> {
			try {
				TrackMetadata meta = tagRetrievalService.getAudioFileMetadata(new File(queueEntry.getTargetFileFullPath()));
				queueEntry.setTrackMetadata(meta);
				playbackQueueService.signalUpdateInTrackInfo(queueEntry);
				SwingUtil.runOnEDT(() -> sonivmUI.getMainWindow().updateStatus("Loaded tags for " + queueEntry.getTargetFileFullPath()),
						false);
			} catch (Throwable t) {
				String message = "Failed to read tags for file " + queueEntry.getTargetFileFullPath();
				LOGGER.log(Level.WARNING, message, t);
				SwingUtil.runOnEDT(() -> sonivmUI.getMainWindow().updateStatus(message), false);
			}
		});
	}

	private List<File> processCueFile(File directory, File cueFile, List<PlaybackQueueEntry> tracksFromCueToAdd) {
		List<File> cueTargetFiles = new ArrayList<>();
		try {
			CueData cueData = CueSheetParser.parseCueFile(cueFile);
			if (cueData.getFileData() != null && cueData.getFileData() != null && !cueData.getFileData().isEmpty()) {
				for (CueDataFileData cueFileTargetFile : cueData.getFileData()) {
					String targetFilePath = cueFileTargetFile.getFile();
					if (targetFilePath == null || cueFileTargetFile.getTracks() == null || cueFileTargetFile.getTracks().isEmpty()) {
						continue;
					}
					File cueTargetFile = new File(directory, targetFilePath);
					if (!cueTargetFile.exists()) {
						continue;
					}
					cueTargetFiles.add(cueTargetFile);
					Integer targetFileDurationMilliseconds = AudioFileUtil.getAudioFileDurationInMilliseconds(cueTargetFile);
					if (targetFileDurationMilliseconds != null) {
						PlaybackQueueEntry previousTrack = null;
						for (CueDataTrackData cueTrack : cueFileTargetFile.getTracks()) {
							if (cueTrack.getIndexes() != null && !cueTrack.getIndexes().isEmpty()
									&& "audio".equalsIgnoreCase(cueTrack.getDataType())) {
								Integer startTime = cueIndexTimeToMillisec(cueTrack.getIndexes().get(0).getIndexValue());

								PlaybackQueueEntry track = PlaybackQueueEntry.builder()
										.cueSheetTrack(true)
										.targetFileFullPath(cueTargetFile.getAbsolutePath())
										.targetFileName(cueTrack.getTitle())
										.cueSheetTrackStartTimeMillis(startTime)
										.trackMetadata(TrackMetadata.builder()
												.title(cueTrack.getTitle())
												.artist(cueTrack.getPerformer() != null && !cueTrack.getPerformer().trim().isEmpty()
														? cueTrack.getPerformer()
														: cueData.getPerformer())
												.album(cueData.getTitle())
												.trackNumber(cueTrack.getNumber())
												.date(TimeDateUtil.yearFromDateTagValue(cueData.getRems().get("DATE")))
												.build())
										.build();

								cueTrack.getIndexes().get(0);
								if (previousTrack != null) {
									int prevTrackDuration = startTime - previousTrack.getCueSheetTrackStartTimeMillis();
									previousTrack.setCueSheetTrackFinishTimeMillis(startTime);
									previousTrack.setDuration(prevTrackDuration);
								}
								tracksFromCueToAdd.add(track);

								previousTrack = track;
							}
						}

						if (previousTrack != null) {
							int prevTrackDuration = targetFileDurationMilliseconds - previousTrack.getCueSheetTrackStartTimeMillis();
							previousTrack.setCueSheetTrackFinishTimeMillis(targetFileDurationMilliseconds);
							previousTrack.setDuration(prevTrackDuration);
						}
					} else {
						LOGGER.warning("Unable to determine cue file target file audio duration " + cueFile.getAbsolutePath());
					}
				}
			} else {
				LOGGER.warning("Missing file data in cue file " + cueFile.getAbsolutePath());
			}
		} catch (Throwable t) {
			LOGGER.log(Level.SEVERE, "Failed to process cue file " + cueFile.getAbsolutePath(), t);
		}
		return cueTargetFiles;
	}

	private int cueIndexTimeToMillisec(String indexValue) {
		// https://www.gnu.org/software/ccd2cue/manual/html_node/INDEX-_0028CUE-Command_0029.html#INDEX-_0028CUE-Command_0029
		// mm minutes plus ss seconds plus ff frames
		// The time mm:ss:ff is an offset relative to the beginning of the file specified by the current FILE command context
		// There are 75 frames per second
		int[] parts = Stream.of(indexValue.split(":")).mapToInt(Integer::parseInt).toArray();
		return (parts[0] * 60 + parts[1]) * 1000 + parts[2] * 1000 / 75;
	}

	@Override
	public void onSonivmShutdown() {
		LOGGER.info("Shutting down tag reading task executor.");
		tagReadingTaskExecutor.shutdownNow();
		shutdownRequested = true;
	}
}
