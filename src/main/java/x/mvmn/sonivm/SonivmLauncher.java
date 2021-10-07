package x.mvmn.sonivm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import x.mvmn.sonivm.audio.AudioFileInfo;
import x.mvmn.sonivm.audio.AudioService;
import x.mvmn.sonivm.audio.PlaybackEvent;
import x.mvmn.sonivm.audio.PlaybackEventListener;

@SpringBootApplication
@Component
public class SonivmLauncher implements PlaybackEventListener {

	@Autowired
	private AudioService audioService;

	private volatile JSlider seekSlider;
	private final JSlider volumeSlider;
	private volatile boolean seekSliderIsDragged;
	private volatile boolean paused;
	private final JFrame mainWindow;
	private final JButton btnOpen;
	private final JComboBox<String> cbxAudioDeviceSelector;

	public static void main(String[] args) throws Exception {
		System.setProperty("java.awt.headless", "false");
		SpringApplication.run(SonivmLauncher.class, args).getBean(SonivmLauncher.class);
	}

	public SonivmLauncher() {
		mainWindow = new JFrame();
		mainWindow.getContentPane().setLayout(new BorderLayout());

		volumeSlider = new JSlider(JSlider.VERTICAL, 0, 100, 100);
		volumeSlider.addChangeListener(actEvent -> audioService.setVolumePercentage(volumeSlider.getValue()));

		SortedSet<String> audioDevices = Stream
				.concat(Stream.of("-- Default --"), Stream.of(AudioSystem.getMixerInfo()).map(Mixer.Info::getName))
				.collect(Collectors.toCollection(TreeSet::new));
		cbxAudioDeviceSelector = new JComboBox<String>(audioDevices.toArray(new String[audioDevices.size()]));
		cbxAudioDeviceSelector.setSelectedIndex(0);
		cbxAudioDeviceSelector.addActionListener(actEvent -> {
			String audioDeviceName = cbxAudioDeviceSelector.getSelectedItem().toString();
			if ("-- Default --".equals(audioDeviceName)) {
				audioDeviceName = null;
			}
			audioService.setAudioDevice(audioDeviceName);
		});

		btnOpen = new JButton("Open...");
		btnOpen.addActionListener(actEvent -> {
			JFileChooser jfc = new JFileChooser();
			if (JFileChooser.APPROVE_OPTION == jfc.showOpenDialog(null)) {
				File file = jfc.getSelectedFile();
				audioService.play(file);
			} else {
				audioService.shutdown();
			}
		});

		mainWindow.getContentPane().add(btnOpen, BorderLayout.CENTER);
		mainWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		mainWindow.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				audioService.shutdown();
			}
		});
		mainWindow.setMinimumSize(new Dimension(400, 300));
		mainWindow.pack();
		mainWindow.setVisible(true);
	}

	private void doSeek() {
		audioService.seek(seekSlider.getValue() * 100);
	}

	private void uiUpdateOnStop() {
		mainWindow.getContentPane().removeAll();
		mainWindow.getContentPane().add(btnOpen, BorderLayout.CENTER);
		mainWindow.invalidate();
		mainWindow.revalidate();
		mainWindow.repaint();
	}

	@Override
	public void handleEvent(PlaybackEvent event) {
		switch (event.getType()) {
			case ERROR:
				System.err.println(event.getErrorType() + ": " + event.getError());
			break;
			case FINISH:
				SwingUtilities.invokeLater(this::uiUpdateOnStop);
			break;
			case PROGRESS:
				Long delta = event.getPlaybackPositionMilliseconds();
				int sliderNewPosition = (int) (delta / 100);
				SwingUtilities.invokeLater(() -> {
					if (!seekSliderIsDragged) {
						seekSlider.getModel().setValue(sliderNewPosition);
					}
				});
			break;
			case START:
				AudioFileInfo audioInfo = event.getAudioMetadata();
				JButton btnStop = new JButton("Stop");
				btnStop.addActionListener(actEvent -> {
					audioService.stop();
					uiUpdateOnStop();
				});

				JButton btnPlayPause = new JButton("Pause");
				btnPlayPause.addActionListener(actEvent -> {
					if (paused) {
						this.audioService.resume();
					} else {
						this.audioService.pause();
					}
					paused = !paused;
					btnPlayPause.setText(paused ? "Play" : "Pause");
				});

				if (audioInfo.isSeekable()) {
					seekSlider = new JSlider(0, audioInfo.getDurationSeconds().intValue() * 10, 0);
					seekSlider.addChangeListener(actEvent -> {
						if (seekSliderIsDragged && !seekSlider.getValueIsAdjusting()) {
							doSeek();
						}
					});
					seekSlider.addMouseListener(new MouseAdapter() {
						@Override
						public void mousePressed(MouseEvent e) {
							seekSliderIsDragged = true;
						}

						@Override
						public void mouseReleased(MouseEvent e) {
							seekSliderIsDragged = false;
						}

						@Override
						public void mouseClicked(MouseEvent e) {
							doSeek();
						}
					});
					mainWindow.getContentPane().add(seekSlider, BorderLayout.NORTH);
				} else {
					seekSlider = null;
				}
				SwingUtilities.invokeLater(() -> {
					mainWindow.getContentPane().removeAll();
					JPanel btnPanel = new JPanel(new GridLayout(2, 1));
					btnPanel.add(btnPlayPause);
					btnPanel.add(btnStop);
					mainWindow.getContentPane().add(this.cbxAudioDeviceSelector, BorderLayout.NORTH);
					mainWindow.getContentPane().add(btnPanel, BorderLayout.SOUTH);
					if (seekSlider != null) {
						mainWindow.getContentPane().add(seekSlider, BorderLayout.CENTER);
					}
					mainWindow.getContentPane().add(volumeSlider, BorderLayout.EAST);
					mainWindow.invalidate();
					mainWindow.revalidate();
					mainWindow.repaint();
				});
			break;
			case DATALINE_CHANGE:
			// Control[] controls = event.getDataLineControls();
			// for (Control dataLineControl : controls) {
			// System.out.println(dataLineControl.getType() + ": " + dataLineControl);
			// }
			break;
		}
	}
}
