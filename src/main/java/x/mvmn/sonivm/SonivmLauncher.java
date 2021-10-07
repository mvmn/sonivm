package x.mvmn.sonivm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sound.sampled.AudioSystem;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

import x.mvmn.sonivm.audio.AudioFileInfo;
import x.mvmn.sonivm.audio.AudioService;
import x.mvmn.sonivm.audio.PlaybackEvent;
import x.mvmn.sonivm.audio.PlaybackEventListener;
import x.mvmn.sonivm.prefs.AppPreferencesService;
import x.mvmn.sonivm.ui.JMenuBarBuilder;
import x.mvmn.sonivm.ui.JMenuBarBuilder.JMenuBuilder;
import x.mvmn.sonivm.ui.SwingUtil;
import x.mvmn.sonivm.ui.model.AudioDeviceOption;

@SpringBootApplication
@Component
public class SonivmLauncher implements PlaybackEventListener {

	private static final Logger LOGGER = Logger.getLogger(SonivmLauncher.class.getCanonicalName());

	@Autowired
	private AudioService audioService;

	// @Autowired
	private final AppPreferencesService appPreferencesService;

	private volatile JSlider seekSlider;
	private final JSlider volumeSlider;
	private volatile boolean seekSliderIsDragged;
	private volatile boolean paused;
	private final JFrame mainWindow;
	private final JButton btnOpen;
	private final JComboBox<AudioDeviceOption> cbxAudioDeviceSelector;

	public static void main(String[] args) throws Exception {
		System.setProperty("java.awt.headless", "false");
		SwingUtilities.invokeAndWait(() -> {
			try {
				Stream.of(FlatLightLaf.class, FlatIntelliJLaf.class, FlatDarkLaf.class, FlatDarculaLaf.class)
						.forEach(lafClass -> UIManager.installLookAndFeel(lafClass.getSimpleName(), lafClass.getCanonicalName()));
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to install FlatLAF look and feels", e);
			}
		});
		SpringApplication.run(SonivmLauncher.class, args).getBean(SonivmLauncher.class);
	}

	public SonivmLauncher(final AppPreferencesService appPreferencesService) {
		this.appPreferencesService = appPreferencesService;

		String lookAndFeelName = null;
		try {
			lookAndFeelName = appPreferencesService.getLookAndFeel();
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to get look and feel name from preferences", e);
		}

		// Set Look&Feel before constructing any UI components
		final String finalLnFName = lookAndFeelName;
		try {
			SwingUtilities.invokeAndWait(() -> {
				try {
					if (finalLnFName != null) {
						SwingUtil.setLookAndFeel(finalLnFName);
					}
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Failed to init UI and set look and feel to " + finalLnFName, e);
				}
			});
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			Thread.interrupted();
			throw new RuntimeException(e);
		}

		mainWindow = new JFrame();
		mainWindow.getContentPane().setLayout(new BorderLayout());

		volumeSlider = new JSlider(JSlider.VERTICAL, 0, 100, 100);
		volumeSlider.addChangeListener(actEvent -> audioService.setVolumePercentage(volumeSlider.getValue()));

		List<AudioDeviceOption> audioDevices = Stream
				.concat(Stream.of(AudioDeviceOption.builder().audioDeviceInfo(null).build()),
						Stream.of(AudioSystem.getMixerInfo())
								.map(mixerInfo -> AudioDeviceOption.builder().audioDeviceInfo(mixerInfo).build()))
				.collect(Collectors.toList());
		cbxAudioDeviceSelector = new JComboBox<AudioDeviceOption>(audioDevices.toArray(new AudioDeviceOption[audioDevices.size()]));
		cbxAudioDeviceSelector.setSelectedIndex(0);
		cbxAudioDeviceSelector.addActionListener(actEvent -> {
			AudioDeviceOption audioDevice = (AudioDeviceOption) cbxAudioDeviceSelector.getSelectedItem();
			audioService.setAudioDevice(audioDevice.getAudioDeviceInfo() != null ? audioDevice.getAudioDeviceInfo().getName() : null);
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
				audioService.stop();
				audioService.shutdown();
			}
		});

		JMenuBuilder<JMenuBarBuilder> menuBuilder = new JMenuBarBuilder().menu("Options");
		JMenuBuilder<JMenuBuilder<JMenuBarBuilder>> menuBuilderLnF = menuBuilder.subMenu("Look&Feel");
		String currentLnF = SwingUtil.getLookAndFeelName(UIManager.getLookAndFeel());
		List<JCheckBoxMenuItem> lnfOptions = new ArrayList<>();
		Arrays.stream(UIManager.getInstalledLookAndFeels())
				.map(LookAndFeelInfo::getName)
				.forEach(lnf -> menuBuilderLnF.item(lnf).checkbox().checked(currentLnF.equals(lnf)).actr(e -> {
					SwingUtil.setLookAndFeel(lnf);
					lnfOptions.forEach(mi -> mi.setState(lnf.equals(mi.getText())));
					new Thread(() -> saveLaFConfig(lnf)).start();
				}).process(mi -> lnfOptions.add((JCheckBoxMenuItem) mi)).build());

		mainWindow.setJMenuBar(menuBuilderLnF.build().build().build());

		mainWindow.setMinimumSize(new Dimension(400, 200));
		mainWindow.pack();
		SwingUtil.moveToScreenCenter(mainWindow);

		SwingUtilities.invokeLater(() -> {
			try {
				mainWindow.setVisible(true);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to init UI and set look and feel to " + finalLnFName, e);
			}
		});
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

	private void saveLaFConfig(String lookAndFeelPrefValue) {
		try {
			appPreferencesService.setLookAndFeel(lookAndFeelPrefValue);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to save look and feel preference", e);
		}
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
						if (seekSliderIsDragged) {
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
							// doSeek();
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
