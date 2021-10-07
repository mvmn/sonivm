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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
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

	private volatile boolean shutdownRequested = false;
	private final Queue<AudioServiceTask> taskQueue = new ConcurrentLinkedQueue<>();
	private volatile State state = State.STOPPED;

	private volatile FFAudioInputStream currentFFAudioInputStream;
	private volatile AudioInputStream currentPcmStream;
	private volatile SourceDataLine currentSourceDataLine;
	private volatile byte[] playbackBuffer;
	private volatile Mixer.Info selectedAudioDevice;
	private volatile boolean currentStreamIsSeekable;
	private volatile long previousDataLineMicrosecondsPosition;
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
		while (!shutdownRequested) {
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
							doStop();
							executeListenerActions(PlaybackEvent.builder().type(PlaybackEvent.Type.FINISH).build());
						} else {
							currentSourceDataLine.write(buffer, 0, readBytes);
							long dataLineMicrosecondsPosition = currentSourceDataLine.getMicrosecondPosition();
							long delta = dataLineMicrosecondsPosition - this.previousDataLineMicrosecondsPosition;
							if (delta > 100000) {
								executeListenerActions(
										PlaybackEvent.builder().type(PlaybackEvent.Type.PROGRESS).playbackTimeDelta(delta).build());
								this.previousDataLineMicrosecondsPosition = dataLineMicrosecondsPosition;
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
		System.out.println("Shutdown req " + shutdownRequested);
		playbackEventListenerExecutor.shutdown();
		if (State.PLAYING == this.state) {
			try {
				doStop();
			} catch (Exception e) {
				handleTaskException(e);
			}
		}
	}

	private void handleTask(AudioServiceTask task) throws Exception {
		System.out.println("Got task " + task);
		switch (task.getType()) {
			case PAUSE:
				if (State.PLAYING == this.state) {
					this.state = State.PAUSED;
				}
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
						if (selectedAudioDevice != null) {
							currentSourceDataLine = AudioSystem.getSourceDataLine(currentPcmStream.getFormat(), selectedAudioDevice);
						} else {
							currentSourceDataLine = AudioSystem.getSourceDataLine(currentPcmStream.getFormat());
						}
						currentSourceDataLine.open(currentPcmStream.getFormat());
						currentSourceDataLine.start();
						this.currentSourceDataLine = currentSourceDataLine;
						this.playbackBuffer = new byte[Math.max(128, currentPcmStream.getFormat().getFrameSize()) * (int) 256];
						this.state = State.PLAYING;
						this.previousDataLineMicrosecondsPosition = 0;
						executeListenerActions(PlaybackEvent.builder().type(PlaybackEvent.Type.START).audioMetadata(fileMetadata).build());
					} else {
						executeListenerActions(
								PlaybackEvent.builder().type(PlaybackEvent.Type.ERROR).errorType(ErrorType.FILE_NOT_FOUND).build());
					}
				}
			break;
			case SEEK:
				if (State.PLAYING == this.state) {
					if (currentStreamIsSeekable) {
						currentFFAudioInputStream.seek(task.getNumericData(), TimeUnit.MILLISECONDS);
					}
				}
			break;
			case STOP:
				if (State.PLAYING == this.state) {
					doStop();
				}
			break;
			case SET_AUDIODEVICE:
				Mixer.Info mixerInfo = getMixerInfoByName(task.getData());
				if (mixerInfo != null) {
					selectedAudioDevice = mixerInfo;
					if (State.PLAYING == this.state) {
						SourceDataLine newSourceDataLine = AudioSystem.getSourceDataLine(currentPcmStream.getFormat(), selectedAudioDevice);
						currentSourceDataLine.close();
						currentSourceDataLine = newSourceDataLine;
						currentSourceDataLine.open(currentPcmStream.getFormat());
						currentSourceDataLine.start();
					}
				}
			break;
		}
	}

	private void doStop() throws Exception {
		System.out.println("doStop");
		this.currentSourceDataLine.close();
		this.currentPcmStream.close();
		this.currentFFAudioInputStream.close();
		this.playbackBuffer = null;
		this.previousDataLineMicrosecondsPosition = 0L;
		this.state = State.STOPPED;
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
		e.printStackTrace();
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
		e.printStackTrace();
	}
}
