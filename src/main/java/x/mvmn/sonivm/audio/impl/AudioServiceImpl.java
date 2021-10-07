package x.mvmn.sonivm.audio.impl;

import java.io.File;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tagtraum.ffsampledsp.FFAudioFileReader;
import com.tagtraum.ffsampledsp.FFAudioInputStream;

import x.mvmn.sonivm.audio.AudioFileInfo;
import x.mvmn.sonivm.audio.AudioService;
import x.mvmn.sonivm.audio.PlaybackEvent;
import x.mvmn.sonivm.audio.PlaybackEvent.ErrorType;
import x.mvmn.sonivm.audio.PlaybackEventListener;
import x.mvmn.sonivm.audio.impl.AudioServiceTask.Type;

@Service
public class AudioServiceImpl implements AudioService, Runnable {

	private static final Logger LOGGER = Logger.getLogger(AudioServiceImpl.class.getCanonicalName());

	private final Queue<AudioServiceTask> taskQueue = new ConcurrentLinkedQueue<>();
	private volatile boolean shutdownRequested = false;
	private volatile State state = State.STOPPED;

	private volatile FFAudioInputStream currentFFAudioInputStream;
	private volatile AudioInputStream currentPcmStream;
	private volatile SourceDataLine currentSourceDataLine;
	private volatile byte[] playbackBuffer;
	private volatile Mixer.Info selectedAudioDevice;
	private volatile boolean currentStreamIsSeekable;
	private volatile long previousDataLineMillisecondsPosition;
	private volatile long startingDataLineMillisecondsPosition;
	private volatile long playbackStartPositionMillisec;
	private volatile int volumePercent = 100;
	private final ExecutorService playbackEventListenerExecutor = Executors.newFixedThreadPool(1);

	@Autowired(required = false)
	private List<PlaybackEventListener> listeners;

	private static enum State {
		STOPPED, PLAYING, PAUSED;
	}

	@PostConstruct
	public void startPlaybackThread() {
		new Thread(this).start();
	}

	@Override
	public void run() {
		while (!this.shutdownRequested) {
			AudioServiceTask task = taskQueue.poll();
			if (task != null) {
				try {
					handleTask(task);
				} catch (Exception e) {
					handleTaskException(e);
				}
			} else {
				try {
					if (State.PLAYING == state) {
						int readBytes = -1;
						byte[] buffer = playbackBuffer;
						readBytes = currentPcmStream.read(buffer);
						if (readBytes < 1) {
							LOGGER.info("End of track");
							doStop();
							executeListenerActions(PlaybackEvent.builder().type(PlaybackEvent.Type.FINISH).build());
						} else {
							if (LOGGER.isLoggable(Level.FINEST)) {
								LOGGER.finest("Writing bytes to source data line: " + readBytes);
							}
							currentSourceDataLine.write(buffer, 0, readBytes);
							long dataLineMillisecondsPosition = currentSourceDataLine.getMicrosecondPosition() / 1000;
							long delta = dataLineMillisecondsPosition - this.previousDataLineMillisecondsPosition;
							if (delta > 100) { // Every 1/10th of a second (or at least not more frequent)
								long currentPlayPositionMillis = playbackStartPositionMillisec
										+ (dataLineMillisecondsPosition - startingDataLineMillisecondsPosition);
								executeListenerActions(PlaybackEvent.builder()
										.type(PlaybackEvent.Type.PROGRESS)
										.playbackPositionMilliseconds(currentPlayPositionMillis)
										.build());
								this.previousDataLineMillisecondsPosition = dataLineMillisecondsPosition;
							}
						}
					} else {
						Thread.yield();
						Thread.sleep(100l);
					}
				} catch (InterruptedException interruptException) {
					Thread.interrupted();
				} catch (Exception e) {
					handlePlaybackException(e);
				}
			}
		}
		LOGGER.info("Playback thread shutting down. Shutdown requested flag state: " + shutdownRequested);
		playbackEventListenerExecutor.shutdown();
		if (State.STOPPED != this.state) {
			try {
				doStop();
			} catch (Exception e) {
				handleTaskException(e);
			}
		}
	}

	private void handleTask(AudioServiceTask task) throws Exception {
		LOGGER.fine("Got task " + task);
		switch (task.getType()) {
			case UPDATE_VOLUME:
				if (State.STOPPED != this.state) {
					doUpdateVolume();
				}
			break;
			case PAUSE:
				handlePauseRequest();
			break;
			case PLAY:
				if (State.PAUSED == this.state) {
					this.state = State.PLAYING;
				} else if (State.STOPPED == this.state) {
					String filePath = task.getData();
					FFAudioFileReader ffAudioFileReader = new FFAudioFileReader();
					File file = new File(filePath);
					if (file.exists()) {
						AudioFileFormat format = ffAudioFileReader.getAudioFileFormat(file);

						FFAudioInputStream currentFFAudioInputStream = (FFAudioInputStream) ffAudioFileReader.getAudioInputStream(file);
						this.currentFFAudioInputStream = currentFFAudioInputStream;
						this.currentStreamIsSeekable = currentFFAudioInputStream.isSeekable();

						AudioInputStream currentPcmStream = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED,
								currentFFAudioInputStream);
						this.currentPcmStream = currentPcmStream;

						long lengthInSeconds = -1;
						if (format.properties() != null && format.properties().get("duration") != null) {
							Object duration = format.properties().get("duration");
							if (duration instanceof Number) {
								lengthInSeconds = ((Number) duration).longValue() / 1000000;
							} else {
								lengthInSeconds = Long.parseLong(duration.toString()) / 1000000;
							}
						} else {
							lengthInSeconds = (long) (currentFFAudioInputStream.getFrameLength()
									/ currentFFAudioInputStream.getFormat().getFrameRate());
						}
						AudioFileInfo fileMetadata = AudioFileInfo.builder()
								.filePath(filePath)
								.seekable(currentFFAudioInputStream.isSeekable())
								.durationSeconds(lengthInSeconds)
								.build();

						SourceDataLine currentSourceDataLine;
						currentSourceDataLine = selectedAudioDevice != null
								? AudioSystem.getSourceDataLine(currentPcmStream.getFormat(), selectedAudioDevice)
								: AudioSystem.getSourceDataLine(currentPcmStream.getFormat());
						currentSourceDataLine.open(currentPcmStream.getFormat());
						currentSourceDataLine.start();
						this.currentSourceDataLine = currentSourceDataLine;
						doUpdateVolume();
						this.playbackBuffer = new byte[Math.max(128, currentPcmStream.getFormat().getFrameSize()) * 64];
						this.playbackStartPositionMillisec = 0;
						this.startingDataLineMillisecondsPosition = 0;
						this.previousDataLineMillisecondsPosition = 0;
						this.state = State.PLAYING;
						executeListenerActions(PlaybackEvent.builder()
								.type(PlaybackEvent.Type.DATALINE_CHANGE)
								.dataLineControls(currentSourceDataLine.getControls())
								.build());
						executeListenerActions(PlaybackEvent.builder().type(PlaybackEvent.Type.START).audioMetadata(fileMetadata).build());
					} else {
						executeListenerActions(
								PlaybackEvent.builder().type(PlaybackEvent.Type.ERROR).errorType(ErrorType.FILE_NOT_FOUND).build());
					}
				}
			break;
			case SEEK:
				if (State.PLAYING == this.state || State.PAUSED == this.state) {
					if (this.currentStreamIsSeekable) {
						this.currentFFAudioInputStream.seek(task.getNumericData(), TimeUnit.MILLISECONDS);
						this.playbackStartPositionMillisec = task.getNumericData();
						this.startingDataLineMillisecondsPosition = currentSourceDataLine.getMicrosecondPosition() / 1000;
					}
				}
			break;
			case STOP:
				if (State.STOPPED != this.state) {
					doStop();
				}
			break;
			case SET_AUDIODEVICE:
				String audioDeviceName = task.getData();
				Mixer.Info mixerInfo = null;
				if (audioDeviceName != null) {
					mixerInfo = getMixerInfoByName(audioDeviceName);
					if (mixerInfo == null) {
						executeListenerActions(PlaybackEvent.builder()
								.type(PlaybackEvent.Type.ERROR)
								.errorType(ErrorType.AUDIODEVICE_ERROR)
								.error("Did not find audio device " + audioDeviceName)
								.build());
						break;
					}
				}
				LOGGER.fine("Switching audio device to " + audioDeviceName);
				selectedAudioDevice = mixerInfo;
				if (State.PLAYING == this.state) {
					LOGGER.fine("On-the-fly switching audio device to " + audioDeviceName);
					SourceDataLine newSourceDataLine = mixerInfo != null
							? AudioSystem.getSourceDataLine(currentPcmStream.getFormat(), selectedAudioDevice)
							: AudioSystem.getSourceDataLine(currentPcmStream.getFormat());
					newSourceDataLine.open(currentPcmStream.getFormat());
					newSourceDataLine.start();
					this.currentSourceDataLine.close();
					this.currentSourceDataLine = newSourceDataLine;
					doUpdateVolume();
					executeListenerActions(PlaybackEvent.builder()
							.type(PlaybackEvent.Type.DATALINE_CHANGE)
							.dataLineControls(newSourceDataLine.getControls())
							.build());
				}
			break;
		}
	}

	protected void handlePauseRequest() {
		if (State.PLAYING == this.state) {
			this.state = State.PAUSED;
		}
	}

	private void doStop() throws Exception {
		LOGGER.fine("Performing doStop()");
		this.currentSourceDataLine.close();
		this.currentPcmStream.close();
		this.currentFFAudioInputStream.close();
		this.playbackBuffer = null;
		this.previousDataLineMillisecondsPosition = 0L;
		this.state = State.STOPPED;
	}

	private void doUpdateVolume() {
		DataLine dataLine = this.currentSourceDataLine;
		if (dataLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
			FloatControl gainControl = (FloatControl) dataLine.getControl(FloatControl.Type.MASTER_GAIN);
			float minimum = gainControl.getMinimum();
			float newValue;
			if (volumePercent > 0) {
				if (volumePercent == 100) {
					newValue = 0.0f;
				} else {
					newValue = minimum - minimum * volumePercent / 100;
				}
			} else {
				newValue = minimum;
			}
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.info("Setting gain to: " + newValue);
			}
			gainControl.setValue(newValue);
		} else {
			LOGGER.info("Gain control not supported - skipping set volume");
		}
	}

	@Override
	public Set<String> listAudioDevices() {
		return Stream.of(AudioSystem.getMixerInfo()).map(Info::getName).collect(Collectors.toCollection(TreeSet::new));
	}

	@Override
	public void setAudioDevice(String audioDeviceName) {
		enqueueTask(Type.SET_AUDIODEVICE, audioDeviceName);
	}

	@Override
	public void play(File file) {
		enqueueTask(Type.PLAY, file.getAbsolutePath());
	}

	@Override
	public void pause() {
		enqueueTask(Type.PAUSE);
	}

	@Override
	public void resume() {
		enqueueTask(Type.PLAY);
	}

	@Override
	public void stop() {
		enqueueTask(Type.STOP);
	}

	@Override
	public void seek(int milliseconds) {
		enqueueTask(Type.SEEK, milliseconds);
	}

	@Override
	public void shutdown() {
		this.shutdownRequested = true;
	}

	private void enqueueTask(Type taskType) {
		enqueueTask(taskType, null);
	}

	private void enqueueTask(Type taskType, long data) {
		taskQueue.add(new AudioServiceTask(taskType, null, data));
	}

	private void enqueueTask(Type taskType, String data) {
		taskQueue.add(new AudioServiceTask(taskType, data, null));
	}

	private Mixer.Info getMixerInfoByName(String name) {
		return Stream.of(AudioSystem.getMixerInfo()).filter(mixerInfo -> name.equalsIgnoreCase(mixerInfo.getName())).findAny().orElse(null);
	}

	private void handlePlaybackException(Exception e) {
		LOGGER.log(Level.SEVERE, "Playback error", e);
		executeListenerActions(PlaybackEvent.builder()
				.type(PlaybackEvent.Type.ERROR)
				.errorType(ErrorType.PLAYBACK_ERROR)
				.error(e.getClass().getSimpleName() + " " + e.getMessage())
				.build());
	}

	private void handleTaskException(Exception e) {
		handlePlaybackException(e);
	}

	private void executeListenerActions(PlaybackEvent event) {
		if (listeners != null) {
			for (PlaybackEventListener playbackEventListener : listeners) {
				playbackEventListenerExecutor.execute(() -> {
					try {
						playbackEventListener.handleEvent(event);
					} catch (Exception e) {
						handleEventListenerException(e);
					}
				});
			}
		}
	}

	private void handleEventListenerException(Exception e) {
		LOGGER.log(Level.SEVERE, "Playback event listener error", e);
	}

	@Override
	public void setVolumePercentage(int volumePercent) {
		if (volumePercent > 100) {
			volumePercent = 100;
		} else if (volumePercent < 0) {
			volumePercent = 0;
		}
		this.volumePercent = volumePercent;
		enqueueTask(Type.UPDATE_VOLUME);
	}
}
