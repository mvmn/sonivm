package x.mvmn.sonivm.audio.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.springframework.stereotype.Service;

import com.tagtraum.ffsampledsp.FFAudioFileReader;
import com.tagtraum.ffsampledsp.FFAudioInputStream;

import davaguine.jeq.spi.EqualizerInputStream;
import x.mvmn.sonivm.audio.AudioFileInfo;
import x.mvmn.sonivm.audio.AudioService;
import x.mvmn.sonivm.audio.PlaybackEvent;
import x.mvmn.sonivm.audio.PlaybackEvent.ErrorType;
import x.mvmn.sonivm.audio.PlaybackEventListener;
import x.mvmn.sonivm.audio.impl.AudioServiceTask.Type;
import x.mvmn.sonivm.util.AudioFileUtil;
import x.mvmn.sonivm.audio.PlaybackState;

@Service
public class AudioServiceImpl implements AudioService, Runnable {

	private static final Logger LOGGER = Logger.getLogger(AudioServiceImpl.class.getCanonicalName());

	private final Queue<AudioServiceTask> taskQueue = new ConcurrentLinkedQueue<>();
	private final ExecutorService playbackEventListenerExecutor = Executors.newFixedThreadPool(1);

	private volatile boolean force16BitPCMConversion = true;
	private volatile boolean useEqualizer = false;

	private volatile boolean shutdownRequested = false;
	private volatile PlaybackState state = PlaybackState.STOPPED;

	private volatile FFAudioInputStream currentFFAudioInputStream;
	private volatile AudioInputStream currentNormalizedStream;
	private volatile EqualizerInputStream currentEqInputStream;
	private volatile SourceDataLine currentSourceDataLine;
	private volatile byte[] playbackBuffer;

	private volatile Mixer.Info selectedAudioDevice;

	private volatile boolean currentStreamIsSeekable;

	private volatile long previousDataLineMillisecondsPosition;
	private volatile long startingDataLineMillisecondsPosition;
	private volatile long playbackStartPositionMillisec;

	private volatile int volumePercent = 100;
	private volatile Integer requestedSeekPositionMillisec = null;

	private List<PlaybackEventListener> listeners = new CopyOnWriteArrayList<>();

	@PostConstruct
	public void startPlaybackThread() {
		Thread playbackThread = new Thread(this);
		playbackThread.setPriority(Thread.MAX_PRIORITY);
		playbackThread.setDaemon(true);
		playbackThread.start();
	}

	@Override
	public void run() {
		try {
			while (!this.shutdownRequested) {
				AudioServiceTask task = taskQueue.poll();
				if (task != null) {
					try {
						handleTask(task);
					} catch (InterruptedException interruptException) {
						throw interruptException;
					} catch (Exception e) {
						handleTaskException(e);
					}
				} else {
					try {
						if (PlaybackState.PLAYING == state) {
							int readBytes = -1;
							byte[] buffer = playbackBuffer;
							readBytes = (useEqualizer ? currentEqInputStream : currentNormalizedStream).read(buffer);
							if (readBytes < buffer.length) {
								// TODO: prepare next track
							}
							if (readBytes < 1) {
								LOGGER.info("End of track");
								doStop();
								executeListenerActions(PlaybackEvent.builder().type(PlaybackEvent.Type.FINISH).build());
							} else {
								if (LOGGER.isLoggable(Level.FINEST)) {
									LOGGER.finest("Writing bytes to source data line: " + readBytes);
								}
								currentSourceDataLine.write(buffer, 0, readBytes);
								if (requestedSeekPositionMillisec == null) {
									long dataLineMillisecondsPosition = currentSourceDataLine.getMicrosecondPosition() / 1000;
									long delta = dataLineMillisecondsPosition - this.previousDataLineMillisecondsPosition;
									if (delta > 100) { // Every 1/10th of a second (or at least not more frequent)
										long currentPlayPositionMillis = playbackStartPositionMillisec
												+ (dataLineMillisecondsPosition - startingDataLineMillisecondsPosition);
										executeListenerActions(PlaybackEvent.builder()
												.type(PlaybackEvent.Type.PROGRESS)
												.playbackPositionMilliseconds(currentPlayPositionMillis)
												.playbackDeltaMilliseconds(delta)
												.build());
										this.previousDataLineMillisecondsPosition = dataLineMillisecondsPosition;
									}
								}
							}
						} else {
							Thread.yield();
							Thread.sleep(100l);
						}
					} catch (InterruptedException interruptException) {
						throw interruptException;
					} catch (Throwable t) {
						handlePlaybackException(t);
					}
				}
			}
		} catch (InterruptedException interruptException) {
			Thread.interrupted();
			this.shutdownRequested = true;
		}
		LOGGER.info("Playback thread shutting down. Shutdown requested flag state: " + shutdownRequested);
		playbackEventListenerExecutor.shutdown();
		if (PlaybackState.STOPPED != this.state) {
			try {
				doStop();
			} catch (Exception e) {
				handleTaskException(e);
			}
		}
	}

	@Override
	public void addPlaybackEventListener(PlaybackEventListener playbackEventListener) {
		this.listeners.add(playbackEventListener);
	}

	@Override
	public void removePlaybackEventListener(PlaybackEventListener playbackEventListener) {
		this.listeners.remove(playbackEventListener);
	}

	@Override
	public void removeAllPlaybackEventListeners() {
		this.listeners.clear();
	}

	private void handleTask(AudioServiceTask task) throws Exception {
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine("Got task " + task);
		}
		switch (task.getType()) {
			case UPDATE_VOLUME:
				if (PlaybackState.STOPPED != this.state) {
					doUpdateVolume();
				}
			break;
			case PAUSE:
				handlePauseRequest();
			break;
			case PLAY:
				if (PlaybackState.PAUSED == this.state) {
					this.currentSourceDataLine.start();
					this.state = PlaybackState.PLAYING;
				} else if (PlaybackState.STOPPED == this.state) {
					String filePath = task.getData();
					FFAudioFileReader ffAudioFileReader = new FFAudioFileReader();
					File file = new File(filePath);
					if (file.exists()) {
						AudioFileFormat audioFileFormat = ffAudioFileReader.getAudioFileFormat(file);
						FFAudioInputStream ffAudioInputStream = (FFAudioInputStream) ffAudioFileReader.getAudioInputStream(file);
						this.currentFFAudioInputStream = ffAudioInputStream;
						this.currentStreamIsSeekable = ffAudioInputStream.isSeekable();

						AudioFormat targetAudioFormat = new AudioFormat(44100, 16, 2, true, false);
						AudioInputStream normalizedStream;
						if (force16BitPCMConversion) {
							normalizedStream = AudioSystem.getAudioInputStream(targetAudioFormat, ffAudioInputStream);
						} else {
							normalizedStream = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ffAudioInputStream);
						}
						EqualizerInputStream eqInputStream = null;
						// if (!disableEqualizer) {
						eqInputStream = new EqualizerInputStream(normalizedStream, 10);
						// normalizedStream = eqInputStream;
						// }
						this.currentEqInputStream = eqInputStream;
						this.currentNormalizedStream = normalizedStream;

						Integer lengthInSeconds = AudioFileUtil.getAudioFileDurationInMilliseconds(file);
						if (lengthInSeconds != null) {
							lengthInSeconds /= 1000;
						} else {
							LOGGER.fine(
									"Can't determine file duration from format parameters - calculating from number of frames and framerate.");
							lengthInSeconds = (int) (ffAudioInputStream.getFrameLength() / ffAudioInputStream.getFormat().getFrameRate());
						}
						AudioFileInfo fileMetadata = AudioFileInfo.builder()
								.filePath(filePath)
								.audioFileFormat(audioFileFormat)
								.seekable(ffAudioInputStream.isSeekable())
								.durationSeconds(lengthInSeconds != null ? lengthInSeconds.intValue() : -1)
								.build();

						SourceDataLine currentSourceDataLine;
						currentSourceDataLine = selectedAudioDevice != null
								? AudioSystem.getSourceDataLine(normalizedStream.getFormat(), selectedAudioDevice)
								: AudioSystem.getSourceDataLine(normalizedStream.getFormat());
						currentSourceDataLine.open(normalizedStream.getFormat());
						currentSourceDataLine.start();
						this.currentSourceDataLine = currentSourceDataLine;

						doUpdateVolume();
						// this.playbackBuffer = new byte[Math.max(128, normalizedStream.getFormat().getFrameSize()) * 64];
						int bufferSize = currentSourceDataLine.available();
						if (bufferSize < 4096) {
							bufferSize = 4096;
						}
						if (bufferSize > 1024 * 1024) {
							bufferSize = 1024 * 1024;
						}
						this.playbackBuffer = new byte[bufferSize];
						// new byte[4410 * 16];
						if (LOGGER.isLoggable(Level.FINER)) {
							LOGGER.finer("Buffer size: " + this.playbackBuffer.length);
						}
						this.playbackStartPositionMillisec = 0;
						this.startingDataLineMillisecondsPosition = 0;
						this.previousDataLineMillisecondsPosition = 0;

						if (task.getNumericData() != null) {
							doSeek(task.getNumericData());
						}

						this.state = PlaybackState.PLAYING;
						executeListenerActions(PlaybackEvent.builder()
								.type(PlaybackEvent.Type.DATALINE_CHANGE)
								.dataLineControls(currentSourceDataLine.getControls())
								.build());
						executeListenerActions(PlaybackEvent.builder()
								.type(PlaybackEvent.Type.START)
								.eqControls(currentEqInputStream != null ? currentEqInputStream.getControls() : null)
								.audioMetadata(fileMetadata)
								.build());
					} else {
						executeListenerActions(PlaybackEvent.builder()
								.type(PlaybackEvent.Type.ERROR)
								.errorType(ErrorType.FILE_NOT_FOUND)
								.error(file.getAbsolutePath())
								.build());
					}
				}
			break;
			case SEEK:
				if (PlaybackState.PLAYING == this.state || PlaybackState.PAUSED == this.state) {
					if (this.currentStreamIsSeekable && requestedSeekPositionMillisec != null) {
						int seekPosition = requestedSeekPositionMillisec; // task.getNumericData()
						doSeek(seekPosition);
					}
				}
				requestedSeekPositionMillisec = null;
			break;
			case STOP:
				if (PlaybackState.STOPPED != this.state) {
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
				if (LOGGER.isLoggable(Level.FINE)) {
					LOGGER.fine("Switching audio device to " + audioDeviceName);
				}
				selectedAudioDevice = mixerInfo;
				if (PlaybackState.PLAYING == this.state) {
					if (LOGGER.isLoggable(Level.FINE)) {
						LOGGER.fine("On-the-fly switching audio device to " + audioDeviceName);
					}
					SourceDataLine newSourceDataLine = mixerInfo != null
							? AudioSystem.getSourceDataLine(currentNormalizedStream.getFormat(), selectedAudioDevice)
							: AudioSystem.getSourceDataLine(currentNormalizedStream.getFormat());
					newSourceDataLine.open(currentNormalizedStream.getFormat());
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

	private void doSeek(long seekPositionMillisec) throws UnsupportedOperationException, IOException {
		this.currentFFAudioInputStream.seek(seekPositionMillisec, TimeUnit.MILLISECONDS);
		this.playbackStartPositionMillisec = seekPositionMillisec;
		this.startingDataLineMillisecondsPosition = currentSourceDataLine.getMicrosecondPosition() / 1000;
	}

	protected void handlePauseRequest() {
		if (PlaybackState.PLAYING == this.state) {
			this.currentSourceDataLine.stop();
			this.state = PlaybackState.PAUSED;
		}
	}

	private void doStop() throws Exception {
		LOGGER.fine("Performing doStop()");
		this.currentSourceDataLine.stop();
		this.currentSourceDataLine.close();
		EqualizerInputStream eqInputStream = this.currentEqInputStream;
		if (eqInputStream != null) {
			eqInputStream.close();
		}
		this.currentNormalizedStream.close();
		this.currentFFAudioInputStream.close();
		this.playbackBuffer = null;
		this.previousDataLineMillisecondsPosition = 0L;
		this.state = PlaybackState.STOPPED;
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
				LOGGER.fine("Setting gain to: " + newValue);
			}
			gainControl.setValue(newValue);
		} else if (dataLine.isControlSupported(FloatControl.Type.VOLUME)) {
			FloatControl volumeControl = (FloatControl) dataLine.getControl(FloatControl.Type.VOLUME);
			float max = volumeControl.getMaximum();
			float min = volumeControl.getMinimum();
			float newValue = max;
			if (volumePercent > 0) {
				if (volumePercent == 100) {
					newValue = max;
				} else {
					newValue = min + (max - min) * volumePercent / 100;
				}
			} else {
				newValue = min;
			}
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.fine("Setting volume to: " + newValue);
			}
			volumeControl.setValue(newValue);
		} else {
			LOGGER.fine("Gain or volume control not supported - skipping set volume");
		}
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
	public void play(File file, int startFromMilliseconds) {
		enqueueTask(Type.PLAY, file.getAbsolutePath(), startFromMilliseconds);
	}

	@Override
	public void pause() {
		handlePauseRequest();
		// enqueueTask(Type.PAUSE);
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
		// Throttle by setting requested seek position to single variable,
		// so that multiple quickly made seek requests will all override it.
		// The var will be reset on processing seek request,
		// so for multiple requests in short time only first one will actually
		// be executed - to avoid repeated seek to the same position.
		requestedSeekPositionMillisec = milliseconds;
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

	private void enqueueTask(Type taskType, String data, long numData) {
		taskQueue.add(new AudioServiceTask(taskType, data, numData));
	}

	private Mixer.Info getMixerInfoByName(String name) {
		return Stream.of(AudioSystem.getMixerInfo()).filter(mixerInfo -> name.equalsIgnoreCase(mixerInfo.getName())).findAny().orElse(null);
	}

	private void handlePlaybackException(Throwable t) {
		LOGGER.log(Level.SEVERE, "Playback error", t);
		executeListenerActions(PlaybackEvent.builder()
				.type(PlaybackEvent.Type.ERROR)
				.errorType(t instanceof UnsupportedAudioFileException ? ErrorType.FILE_FORMAT_ERROR : ErrorType.PLAYBACK_ERROR)
				.error(t.getClass().getSimpleName() + " " + t.getMessage())
				.build());
	}

	private void handleTaskException(Throwable t) {
		handlePlaybackException(t);
	}

	private void executeListenerActions(PlaybackEvent event) {
		if (listeners != null) {
			for (PlaybackEventListener playbackEventListener : listeners) {
				playbackEventListenerExecutor.execute(() -> {
					try {
						playbackEventListener.handleEvent(event);
					} catch (Throwable t) {
						handleEventListenerException(t);
					}
				});
			}
		}
	}

	private void handleEventListenerException(Throwable t) {
		LOGGER.log(Level.SEVERE, "Playback event listener error", t);
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

	@Override
	public boolean isPaused() {
		return PlaybackState.PAUSED == this.state;
	}

	@Override
	public boolean isPlaying() {
		return PlaybackState.PLAYING == this.state;
	}

	@Override
	public boolean isStopped() {
		return PlaybackState.STOPPED == this.state;
	}

	@Override
	public boolean isUseEqualizer() {
		return useEqualizer;
	}

	@Override
	public void setUseEqualizer(boolean useEqualizer) {
		this.useEqualizer = useEqualizer;
	}
}
