package x.mvmn.sonivm;

import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.swing.ButtonGroup;
import javax.swing.JMenuBar;
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

import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.ui.JMenuBarBuilder;
import x.mvmn.sonivm.ui.JMenuBarBuilder.JMenuBuilder;
import x.mvmn.sonivm.ui.SonivmController;
import x.mvmn.sonivm.ui.SonivmMainWindow;
import x.mvmn.sonivm.ui.UsernamePasswordDialog;
import x.mvmn.sonivm.ui.model.AudioDeviceOption;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.util.Pair;

@SpringBootApplication
@Component
public class SonivmLauncher implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(SonivmLauncher.class.getCanonicalName());

	@Autowired
	private SonivmController sonivmController;

	@Autowired
	private SonivmMainWindow mainWindow;

	@Autowired
	private PreferencesService preferencesService;

	public static void main(String[] args) {
		initConsoleLogging();

		// Spring Boot makes app headless by default
		System.setProperty("java.awt.headless", "false");
		// Enable macOS native menu bar usage
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		// Make sure macOS closes all Swing windows on app quit
		System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");

		initTaskbarIcon();

		// Install FlatLaF look&feels
		SwingUtil.installLookAndFeels(true, FlatLightLaf.class, FlatIntelliJLaf.class, FlatDarkLaf.class, FlatDarculaLaf.class);

		// Ensure data folder exists
		File userHome = new File(System.getProperty("user.home"));
		File appHomeFolder = new File(userHome, ".sonivm");
		if (!appHomeFolder.exists()) {
			appHomeFolder.mkdir();
		}

		System.setProperty("sonivm_home_folder", appHomeFolder.getAbsolutePath());

		// Run the app
		SonivmLauncher launcher = SpringApplication.run(SonivmLauncher.class, args).getBean(SonivmLauncher.class);
		SwingUtil.runOnEDT(() -> launcher.run(), false);
	}

	private static void initConsoleLogging() {
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter());
		Logger rootLogger = Logger.getLogger("x.mvmn.sonivm");
		rootLogger.setLevel(Level.INFO);
		rootLogger.addHandler(handler);
		handler.setLevel(Level.INFO);
	}

	private static void initTaskbarIcon() {
		try {
			BufferedImage image = ImageIO.read(SonivmLauncher.class.getResourceAsStream("/sonivm_logo.png"));
			Taskbar.getTaskbar().setIconImage(image);
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Failed to load and set main window and taskbar icon.", t);
		}
	}

	public void run() {
		mainWindow.setJMenuBar(initMenuBar(getAudioDevicesPlusDefault()));
		SwingUtil.prefSizeRatioOfScreenSize(mainWindow, 3f / 4f);
		sonivmController.onBeforeUiPack();
		mainWindow.pack();
		SwingUtil.moveToScreenCenter(mainWindow);
		sonivmController.onBeforeUiSetVisible();

		SwingUtilities.invokeLater(() -> mainWindow.setVisible(true));
	}

	private JMenuBar initMenuBar(List<AudioDeviceOption> audioDevices) {
		int scrobblePercentage = preferencesService.getPercentageToScrobbleAt(70);

		JMenuBuilder<JMenuBarBuilder> menuBuilder = new JMenuBarBuilder().menu("Options");

		JMenuBuilder<JMenuBuilder<JMenuBuilder<JMenuBarBuilder>>> menuBuilderLastFMScrobblePercentage = menuBuilder.subMenu("LastFM")
				.item("Set credentials...")
				.actr(actEvent -> {
					try {
						String user = this.preferencesService.getUsername();
						String password = this.preferencesService.getPassword();
						new UsernamePasswordDialog(null, "Set credentials", true, new Pair<String, String>(user, password), unPwdPair -> {
							new Thread(() -> {
								try {
									this.preferencesService.setUsername(unPwdPair.getK());
									this.preferencesService.setPassword(new String(unPwdPair.getV()));
									sonivmController.onLastFMCredsOrKeysUpdate();
								} catch (Exception e) {
									LOGGER.log(Level.WARNING, "Failed to store LastFM credentials to prefs", e);
								}
							}).start();
						}).setVisible(true);
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Failed to obtain LastFM credentials from prefs", e);
					}
				})
				.build()
				.item("Set API keys...")
				.actr(actEvent -> {
					try {
						String key = this.preferencesService.getApiKey();
						String secret = this.preferencesService.getApiSecret();
						new UsernamePasswordDialog(null, "Set API keys", true, new Pair<String, String>(key, secret), unPwdPair -> {
							new Thread(() -> {
								try {
									this.preferencesService.setApiKey(unPwdPair.getK());
									this.preferencesService.setApiSecret(new String(unPwdPair.getV()));
									sonivmController.onLastFMCredsOrKeysUpdate();
								} catch (Exception e) {
									LOGGER.log(Level.WARNING, "Failed to store LastFM API keys to prefs", e);
								}
							}).start();
						}, "API Key", "API Secret").setVisible(true);
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Failed to obtain LastFM API keys from prefs", e);
					}
				})
				.build()
				.subMenu("Scrobble at");
		ButtonGroup rbGroupLastFMScrobblePercentage = new ButtonGroup();
		Stream.of(10, 30, 50, 70, 90)
				.forEach(scrobblePercentageOption -> menuBuilderLastFMScrobblePercentage.item("" + scrobblePercentageOption + "%")
						.radioButton()
						.group(rbGroupLastFMScrobblePercentage)
						.checked(scrobblePercentageOption == scrobblePercentage)
						.actr(e -> sonivmController.onLastFMScrobblePercentageChange(scrobblePercentageOption))
						.build());
		menuBuilderLastFMScrobblePercentage.build().build();

		JMenuBuilder<JMenuBuilder<JMenuBarBuilder>> menuBuilderAudioDevice = menuBuilder.subMenu("AudioDevice");
		ButtonGroup rbGroupAudioDevices = new ButtonGroup();
		audioDevices.forEach(ad -> menuBuilderAudioDevice.item(ad.toString())
				.radioButton()
				.checked(ad.getAudioDeviceInfo() == null)
				.group(rbGroupAudioDevices)
				.actr(actEvent -> sonivmController.onSetAudioDevice(ad))
				.build());
		menuBuilderAudioDevice.build();

		String currentLnF = SwingUtil.getLookAndFeelName(UIManager.getLookAndFeel());
		JMenuBuilder<JMenuBuilder<JMenuBarBuilder>> menuBuilderLnF = menuBuilder.subMenu("Look&Feel");
		ButtonGroup rbGroupLookAndFeels = new ButtonGroup();
		Arrays.stream(UIManager.getInstalledLookAndFeels())
				.map(LookAndFeelInfo::getName)
				.forEach(lnf -> menuBuilderLnF.item(lnf)
						.radioButton()
						.group(rbGroupLookAndFeels)
						.checked(currentLnF.equals(lnf))
						.actr(e -> sonivmController.onSetLookAndFeel(lnf))
						.build());
		menuBuilderLnF.build();

		Logger rootLogger = Logger.getLogger("x.mvmn.sonivm");
		Level currentLogLevel = rootLogger.getLevel();

		JMenuBuilder<JMenuBuilder<JMenuBarBuilder>> menuBuilderLogLEvel = menuBuilder.subMenu("Log level");
		ButtonGroup rbGroupLogLevels = new ButtonGroup();
		Stream.of(Level.INFO, Level.WARNING, Level.SEVERE, Level.FINE, Level.FINER, Level.FINEST, Level.ALL, Level.CONFIG, Level.OFF)
				.forEach(level -> menuBuilderLogLEvel.item(level.getName())
						.radioButton()
						.checked(level.equals(currentLogLevel))
						.group(rbGroupLogLevels)
						.actr(actEvent -> {
							rootLogger.setLevel(level);
							Stream.of(rootLogger.getHandlers()).forEach(handler -> handler.setLevel(level));
						})
						.build());
		menuBuilderLogLEvel.build();

		return menuBuilder.build().build();
	}

	private List<AudioDeviceOption> getAudioDevicesPlusDefault() {
		return Stream
				.concat(Stream.of(AudioDeviceOption.builder().audioDeviceInfo(null).build()),
						Stream.of(AudioSystem.getMixerInfo())
								.map(mixerInfo -> AudioDeviceOption.builder().audioDeviceInfo(mixerInfo).build()))
				.collect(Collectors.toList());
	}
}
