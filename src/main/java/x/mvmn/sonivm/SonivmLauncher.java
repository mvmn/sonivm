package x.mvmn.sonivm;

import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.swing.ButtonGroup;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.table.TableColumnModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

import x.mvmn.sonivm.audio.AudioService;
import x.mvmn.sonivm.prefs.AppPreferencesService;
import x.mvmn.sonivm.ui.JMenuBarBuilder;
import x.mvmn.sonivm.ui.JMenuBarBuilder.JMenuBuilder;
import x.mvmn.sonivm.ui.SonivmController;
import x.mvmn.sonivm.ui.SonivmMainWindow;
import x.mvmn.sonivm.ui.model.AudioDeviceOption;
import x.mvmn.sonivm.util.ui.swing.SwingUtil;

@SpringBootApplication
@Component
public class SonivmLauncher implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(SonivmLauncher.class.getCanonicalName());

	@Autowired
	private SonivmController sonivmController;

	@Autowired
	private AudioService audioService;

	@Autowired
	private AppPreferencesService appPreferencesService;

	@Autowired
	private SonivmMainWindow mainWindow;

	public static void main(String[] args) {
		// Spring Boot makes app headless by default
		System.setProperty("java.awt.headless", "false");
		// Enable macOS native menu bar usage
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		// Make sure macOS closes all Swing windows on app quit
		System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");

		initTaskbarIcon();

		// Install FlatLaF look&feels
		SwingUtil.installLookAndFeels(true, FlatLightLaf.class, FlatIntelliJLaf.class, FlatDarkLaf.class, FlatDarculaLaf.class);

		// Run the app
		SonivmLauncher launcher = SpringApplication.run(SonivmLauncher.class, args).getBean(SonivmLauncher.class);
		SwingUtil.runOnEDT(() -> launcher.run(), false);
	}

	private static void initTaskbarIcon() {
		try {
			BufferedImage image = ImageIO.read(SonivmLauncher.class.getResourceAsStream("/sonivm_logo.png"));
			Taskbar.getTaskbar().setIconImage(image);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to load and set main window and taskbar icon.", e);
		}
	}

	public void run() {
		restorePlayQueueColumnWidths();

		mainWindow.setJMenuBar(initMenuBar(getAudioDevicesPlusDefault()));
		SwingUtil.prefSizeRatioOfScreenSize(mainWindow, 3f / 4f);
		mainWindow.pack();
		SwingUtil.moveToScreenCenter(mainWindow);

		SwingUtilities.invokeLater(() -> mainWindow.setVisible(true));
		audioService.addPlaybackEventListener(sonivmController);
	}

	private void restorePlayQueueColumnWidths() {
		try {
			int[] playQueueColumnWidths = appPreferencesService.getPlayQueueColumnWidths();
			if (playQueueColumnWidths != null) {
				SwingUtil.runOnEDT(() -> {
					TableColumnModel columnModel = mainWindow.getPlayQueueTable().getColumnModel();
					for (int i = 0; i < columnModel.getColumnCount() && i < playQueueColumnWidths.length; i++) {
						columnModel.getColumn(i).setPreferredWidth(playQueueColumnWidths[i]);
					}
				}, true);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to read+apply column width for playback queue table", e);
		}
	}

	private JMenuBar initMenuBar(List<AudioDeviceOption> audioDevices) {
		JMenuBuilder<JMenuBarBuilder> menuBuilder = new JMenuBarBuilder().menu("Options");
		JMenuBuilder<JMenuBuilder<JMenuBarBuilder>> menuBuilderLnF = menuBuilder.subMenu("Look&Feel");
		String currentLnF = SwingUtil.getLookAndFeelName(UIManager.getLookAndFeel());
		ButtonGroup rbGroupLookAndFeels = new ButtonGroup();
		Arrays.stream(UIManager.getInstalledLookAndFeels())
				.map(LookAndFeelInfo::getName)
				.forEach(lnf -> menuBuilderLnF.item(lnf)
						.radioButton()
						.group(rbGroupLookAndFeels)
						.checked(currentLnF.equals(lnf))
						.actr(e -> onSetLookAndFeel(lnf))
						.build());
		menuBuilderLnF.build();
		JMenuBuilder<JMenuBuilder<JMenuBarBuilder>> menuBuilderAudioDevice = menuBuilder.subMenu("AudioDevice");
		ButtonGroup rbGroupAudioDevices = new ButtonGroup();
		audioDevices.forEach(ad -> menuBuilderAudioDevice.item(ad.toString())
				.radioButton()
				.checked(ad.getAudioDeviceInfo() == null)
				.group(rbGroupAudioDevices)
				.actr(actEvent -> ad.selected())
				.build());
		menuBuilderAudioDevice.build();

		return menuBuilder.build().build();
	}

	private void onSetLookAndFeel(String lookAndFeelId) {
		SwingUtil.setLookAndFeel(lookAndFeelId, false);
		new Thread(() -> saveLookAndFeelPreference(lookAndFeelId)).start();
	}

	private void saveLookAndFeelPreference(String lookAndFeelId) {
		try {
			appPreferencesService.setLookAndFeel(lookAndFeelId);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to save look and feel preference", e);
		}
	}

	private List<AudioDeviceOption> getAudioDevicesPlusDefault() {
		return Stream.concat(Stream.of(AudioDeviceOption.builder().onSelect(this::setAudioDevice).audioDeviceInfo(null).build()),
				Stream.of(AudioSystem.getMixerInfo())
						.map(mixerInfo -> AudioDeviceOption.builder().audioDeviceInfo(mixerInfo).onSelect(this::setAudioDevice).build()))
				.collect(Collectors.toList());
	}

	private void setAudioDevice(AudioDeviceOption audioDevice) {
		audioService.setAudioDevice(audioDevice.getAudioDeviceInfo() != null ? audioDevice.getAudioDeviceInfo().getName() : null);
	}
}
