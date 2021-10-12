package x.mvmn.sonivm.lastfm.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import de.umass.lastfm.scrobble.ScrobbleData;
import x.mvmn.sonivm.lastfm.LastFMQueueService;
import x.mvmn.sonivm.lastfm.ScrobbleDataHelper;
import x.mvmn.sonivm.util.FileHelper;

@Service
public class LastFMQueueServiceImpl implements LastFMQueueService {

	private static final Logger LOGGER = Logger.getLogger(LastFMQueueServiceImpl.class.getCanonicalName());

	private static final int END_OF_ENTRY_MARKER = '\n';

	protected static final Object QUEUE_LOCK = new Object();
	protected File queueFile;

	@PostConstruct
	public void init() {
		this.queueFile = new File(new File(System.getProperty("sonivm_home_folder")), "lastfm_submission_queue.txt");
	}

	@Override
	public void queueTrack(ScrobbleData track) {
		synchronized (QUEUE_LOCK) {
			try {
				FileHelper.appendToFile(queueFile, ScrobbleDataHelper.serialize(track, END_OF_ENTRY_MARKER));
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Failed to store LastFM submission queue", e);
			}
		}
	}

	@Override
	public void processQueuedTracks(Consumer<List<ScrobbleData>> processor, int max) {
		synchronized (QUEUE_LOCK) {
			if (queueFile.exists()) {
				try {
					List<ScrobbleData> entries = new ArrayList<>();
					long lastOffset = -1;
					long fileSize = FileUtils.sizeOf(queueFile);
					if (fileSize > 0) {
						lastOffset = fileSize;
						while (max-- > 0 && lastOffset > 0) {
							long offset = lastOffset - 2;
							if (offset < 0) {
								offset = 0;
							}
							byte[] data;
							try (RandomAccessFile raf = new RandomAccessFile(queueFile, "r")) {
								raf.seek(offset);
								while (raf.read() != END_OF_ENTRY_MARKER && offset > 0) {
									offset--;
									raf.seek(offset);
								}
								int entrySize = (int) (lastOffset - offset - 1);
								if (offset == 0) {
									raf.seek(0);
									entrySize = (int) lastOffset;
								}
								data = new byte[entrySize];
								raf.read(data, 0, data.length);
							}
							ScrobbleData tle = ScrobbleDataHelper.deserialize(data);
							entries.add(tle);
							lastOffset = offset;
						}
					}
					if (!entries.isEmpty()) {
						processor.accept(entries);
						FileHelper.truncateFile(queueFile, lastOffset);
					}
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Failed to process LastFM submission queue", e);
				}
			}
		}
	}
}
