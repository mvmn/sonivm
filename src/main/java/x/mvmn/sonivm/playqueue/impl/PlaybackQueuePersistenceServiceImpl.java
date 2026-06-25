package x.mvmn.sonivm.playqueue.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import x.mvmn.sonivm.playqueue.PlaybackQueueEntry;
import x.mvmn.sonivm.playqueue.PlaybackQueuePersistenceService;
import x.mvmn.sonivm.playqueue.PlaybackQueueService;

@Component
@RequiredArgsConstructor
public class PlaybackQueuePersistenceServiceImpl implements PlaybackQueuePersistenceService {

	private static final Logger LOGGER = Logger.getLogger(PlaybackQueuePersistenceServiceImpl.class.getSimpleName());

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final PlaybackQueueService playbackQueueService;

	@Override
	public void savePlayQueuesContents() {
		try {
			LOGGER.info("Storing play queues.");
			saveQueueNames();
			int queueCount = this.playbackQueueService.getQueuesCount();
			for (int i = 0; i < queueCount; i++) {
				OBJECT_MAPPER.writeValue(getPlayQueueStorageFile(i), this.playbackQueueService.getCopyOfQueue(i));
			}
			LOGGER.info("Storing play queues succeeded.");
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to store the playback queue", e);
		}
	}

	@Override
	public void savePlayQueueContents(int queueIndex) {
		try {
			LOGGER.info("Storing play queue " + queueIndex);
			OBJECT_MAPPER.writeValue(getPlayQueueStorageFile(queueIndex), this.playbackQueueService.getCopyOfQueue(queueIndex));
			LOGGER.info("Storing play queue " + queueIndex + " succeeded.");
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to store the playback queue " + queueIndex, e);
		}
	}

	@Override
	public void saveQueueNames() {
		try {
			List<String> names = new ArrayList<>();
			for (int i = 1; i < this.playbackQueueService.getQueuesCount(); i++) {
				names.add(this.playbackQueueService.getQueueName(i));
			}

			OBJECT_MAPPER.writeValue(getPlayQueueNamesFile(), names);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to store queue names", e);
		}
	}

	private List<String> loadQueueNamesList() throws JsonParseException, JsonMappingException, IOException {
		File queueNamesFile = getPlayQueueNamesFile();

		List<String> queueNamesList = new ArrayList<>();
		queueNamesList.add("Default");
		if (queueNamesFile.exists()) {
			String[] queueNames = OBJECT_MAPPER.readValue(queueNamesFile, String[].class);
			queueNamesList.addAll(Arrays.asList(queueNames));
		}
		return queueNamesList;
	}

	@Override
	public void restorePlayQueues() {
		try {
			LOGGER.info("Restoring play queues.");
			List<String> queueNamesList = loadQueueNamesList();

			for (int i = 0; i < queueNamesList.size(); i++) {
				File queueFile = getPlayQueueStorageFile(i);
				if (queueFile.exists()) {
					List<PlaybackQueueEntry> queueEntries = OBJECT_MAPPER.readValue(queueFile,
							new TypeReference<List<PlaybackQueueEntry>>() {});
					if (i > 0) {
						this.playbackQueueService.addQueue(queueNamesList.get(i));
					}
					this.playbackQueueService.setCurrentlyViewedQueue(i);
					this.playbackQueueService.clearQueue();
					this.playbackQueueService.addRows(queueEntries);
					LOGGER.info("Restoring play queue " + i + " succeeded.");
				} else {
					LOGGER.info("Restoring play queue " + i + " not needed - no queue stored yet.");
				}
			}
			this.playbackQueueService.setCurrentlyViewedQueue(0);
			LOGGER.info("Restoring play queues completed.");
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to retrieve and restore the playback queues", e);
		}
	}

	private File getPlayQueueStorageFile(int queueIndex) {
		return new File(new File(System.getProperty("sonivm_home_folder")),
				queueIndex > 0 ? ("queue_" + queueIndex + ".json") : "queue.json");
	}

	private File getPlayQueueNamesFile() {
		return new File(new File(System.getProperty("sonivm_home_folder")), "queue_names.json");
	}
}
