package x.mvmn.sonivm;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sound.sampled.AudioSystem;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.springframework.beans.factory.annotation.Value;
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
import x.mvmn.sonivm.ui.SonivmController;
import x.mvmn.sonivm.ui.SonivmMainWindow;
import x.mvmn.sonivm.ui.model.AudioDeviceOption;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;
import x.mvmn.sonivm.util.ui.swing.SwingUtil;

@SpringBootApplication
@Component
public class SonivmLauncher implements PlaybackEventListener {

	private static final Logger LOGGER = Logger.getLogger(SonivmLauncher.class.getCanonicalName());

	private final AudioService audioService;
	private final AppPreferencesService appPreferencesService;

	private volatile JSlider seekSlider;
	private final JSlider volumeSlider;
	private volatile boolean seekSliderIsDragged;
	private volatile boolean paused;
	private final SonivmMainWindow mainWindow;

	public static void main(String[] args) {
		// Spring Boot makes app headless by default
		System.setProperty("java.awt.headless", "false");
		// Enable macOS native menu bar usage
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		// Install FlatLaF look&feels
		SwingUtil.installLookAndFeels(true, FlatLightLaf.class, FlatIntelliJLaf.class, FlatDarkLaf.class, FlatDarculaLaf.class);
		// Run the app
		SpringApplication.run(SonivmLauncher.class, args).getBean(SonivmLauncher.class);
	}

	public SonivmLauncher(@Value("${app.version:0.0.1}") String appVersion, AppPreferencesService appPreferencesService,
			SonivmController sonivmController, AudioService audioService, PlaybackQueueTableModel playbackQueueTableModel) {
		this.appPreferencesService = appPreferencesService;
		this.audioService = audioService;

		// Pre-init Look&Feel based on preferences
		initLookAndFeel();

		mainWindow = new SonivmMainWindow("Sonivm v" + appVersion, sonivmController, playbackQueueTableModel);

		volumeSlider = new JSlider(JSlider.VERTICAL, 0, 100, 100);
		volumeSlider.addChangeListener(actEvent -> audioService.setVolumePercentage(volumeSlider.getValue()));
		//
		// btnOpen = new JButton("Open...");
		// btnOpen.addActionListener(actEvent -> {
		// JFileChooser jfc = new JFileChooser();
		// if (JFileChooser.APPROVE_OPTION == jfc.showOpenDialog(null)) {
		// File file = jfc.getSelectedFile();
		// audioService.play(file);
		// } else {
		// audioService.shutdown();
		// }
		// });

		mainWindow.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				audioService.stop();
				audioService.shutdown();
			}
		});

		mainWindow.setJMenuBar(initMenuBar(getAudioDevicesWithDefault()));
		SwingUtil.prefSizeRatioOfScreenSize(mainWindow, 3f / 4f);
		mainWindow.pack();
		SwingUtil.moveToScreenCenter(mainWindow);

		SwingUtilities.invokeLater(() -> mainWindow.setVisible(true));
		audioService.addPlaybackEventListener(this);
	}

	private void initLookAndFeel() {
		String lookAndFeelName = null;
		try {
			lookAndFeelName = appPreferencesService.getLookAndFeel();
			if (lookAndFeelName != null) {
				SwingUtil.setLookAndFeel(lookAndFeelName, true);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to get look and feel name from preferences", e);
		}
	}

	private JMenuBar initMenuBar(List<AudioDeviceOption> audioDevices) {
		JMenuBuilder<JMenuBarBuilder> menuBuilder = new JMenuBarBuilder().menu("Options");
		JMenuBuilder<JMenuBuilder<JMenuBarBuilder>> menuBuilderLnF = menuBuilder.subMenu("Look&Feel");
		String currentLnF = SwingUtil.getLookAndFeelName(UIManager.getLookAndFeel());
		// List<JCheckBoxMenuItem> lnfOptions = new ArrayList<>();
		ButtonGroup rbGroupLookAndFeels = new ButtonGroup();
		Arrays.stream(UIManager.getInstalledLookAndFeels())
				.map(LookAndFeelInfo::getName)
				.forEach(lnf -> menuBuilderLnF.item(lnf)
						.radioButton()
						.group(rbGroupLookAndFeels)
						.checked(currentLnF.equals(lnf))
						.actr(e -> onSetLookAndFeel(lnf))
						// .process(mi -> lnfOptions.add((JCheckBoxMenuItem) mi))
						.build());
		menuBuilderLnF.build();
		JMenuBuilder<JMenuBuilder<JMenuBarBuilder>> menuBuilderAudioDevice = menuBuilder.subMenu("AudioDevice");
		ButtonGroup rbGroupAudioDevices = new ButtonGroup();
		audioDevices.forEach(ad -> menuBuilderAudioDevice.item(ad.toString())
				.radioButton()
				.checked(ad.getAudioDeviceInfo() == null)
				.group(rbGroupAudioDevices)
				.actr(actEvent -> ad.selected()));
		menuBuilderAudioDevice.build();

		return menuBuilder.build().build();
	}

	private void onSetLookAndFeel(String lookAndFeelId) {
		SwingUtil.setLookAndFeel(lookAndFeelId, false);
		// lnfOptions.forEach(mi -> mi.setState(lnf.equals(mi.getText())));
		new Thread(() -> saveLookAndFeelPreference(lookAndFeelId)).start();
	}

	private void saveLookAndFeelPreference(String lookAndFeelId) {
		try {
			appPreferencesService.setLookAndFeel(lookAndFeelId);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to save look and feel preference", e);
		}
	}

	private List<AudioDeviceOption> getAudioDevicesWithDefault() {
		return Stream.concat(Stream.of(AudioDeviceOption.builder().onSelect(this::setAudioDevice).audioDeviceInfo(null).build()),
				Stream.of(AudioSystem.getMixerInfo())
						.map(mixerInfo -> AudioDeviceOption.builder().audioDeviceInfo(mixerInfo).onSelect(this::setAudioDevice).build()))
				.collect(Collectors.toList());
	}

	private void setAudioDevice(AudioDeviceOption audioDevice) {
		audioService.setAudioDevice(audioDevice.getAudioDeviceInfo() != null ? audioDevice.getAudioDeviceInfo().getName() : null);
	}

	private void doSeek() {
		audioService.seek(seekSlider.getValue() * 100);
	}

	@Override
	public void handleEvent(PlaybackEvent event) {
		switch (event.getType()) {
			case ERROR:
				System.err.println(event.getErrorType() + ": " + event.getError());
			break;
			case FINISH:
			// SwingUtilities.invokeLater(this::uiUpdateOnStop);
			break;
			case PROGRESS:
				Long playbackPositionMillis = event.getPlaybackPositionMilliseconds();
				int seekSliderNewPosition = (int) (playbackPositionMillis / 100);
				SwingUtilities.invokeLater(() -> mainWindow.updateSeekSliderPosition(seekSliderNewPosition));
			break;
			case START:
				AudioFileInfo audioInfo = event.getAudioMetadata();
				JButton btnStop = new JButton("Stop");
				btnStop.addActionListener(actEvent -> {
					audioService.stop();
					// uiUpdateOnStop();
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
					if (audioInfo.isSeekable()) {
						mainWindow.allowSeek(audioInfo.getDurationSeconds().intValue() * 10);
					} else {
						mainWindow.disallowSeek();
					}
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
