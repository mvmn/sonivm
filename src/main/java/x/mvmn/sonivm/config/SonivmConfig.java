package x.mvmn.sonivm.config;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.swing.ButtonGroup;
import javax.swing.JMenuBar;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import x.mvmn.sonivm.PlaybackController;
import x.mvmn.sonivm.Sonivm;
import x.mvmn.sonivm.WinAmpSkinsService;
import x.mvmn.sonivm.eq.EqualizerPresetService;
import x.mvmn.sonivm.eq.SonivmEqualizerService;
import x.mvmn.sonivm.impl.AudioDeviceOption;
import x.mvmn.sonivm.lastfm.LastFMScrobblingService;
import x.mvmn.sonivm.playqueue.PlaybackQueueService;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.ui.EqualizerWindow;
import x.mvmn.sonivm.ui.SonivmMainWindow;
import x.mvmn.sonivm.ui.SonivmTrayIconPopupMenu;
import x.mvmn.sonivm.ui.SonivmUI;
import x.mvmn.sonivm.ui.SupportedFileExtensionsDialog;
import x.mvmn.sonivm.ui.UsernamePasswordDialog;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.ui.util.swing.menu.JMenuBarBuilder;
import x.mvmn.sonivm.ui.util.swing.menu.JMenuBarBuilder.JMenuBuilder;
import x.mvmn.sonivm.util.Pair;

@Configuration
public class SonivmConfig {

	private static final Logger LOGGER = Logger.getLogger(SonivmConfig.class.getCanonicalName());

	@Value("Sonivm v${app.version:0.0.1}")
	private String appVersion = "Sonivm";

	@Bean
	@Scope("singleton")
	public SonivmUI sonivmUI(PlaybackController sonivmController, PlaybackQueueTableModel playbackQueueTableModel,
			PreferencesService preferencesService, SonivmEqualizerService eqService, EqualizerPresetService eqPresetService,
			BufferedImage sonivmIcon, SonivmTrayIconPopupMenu trayIconPopupMenu, LastFMScrobblingService lastFMScrobblingService,
			PlaybackQueueService playbackQueueService, WinAmpSkinsService winAmpSkinsService, PlaybackQueueTableModel playQueueTableModel) {

		initLookAndFeel(preferencesService);
		SwingUtil.runOnEDT(() -> SwingUtil.setTaskbarIcon(sonivmIcon), true);

		SonivmMainWindow mainWindow = new SonivmMainWindow(appVersion, playbackQueueTableModel);
		mainWindow.setIconImage(sonivmIcon);
		EqualizerWindow eqWindow = new EqualizerWindow(appVersion + " equalizer", 10, eqService, eqPresetService);

		mainWindow.setJMenuBar(sonivmWindowsMenuBar(mainWindow, eqWindow, sonivmController, preferencesService));
		eqWindow.setJMenuBar(sonivmWindowsMenuBar(mainWindow, eqWindow, sonivmController, preferencesService));

		return new SonivmUI(mainWindow, eqWindow, sonivmIcon, trayIconPopupMenu, preferencesService, sonivmController,
				lastFMScrobblingService, playbackQueueService, winAmpSkinsService, playQueueTableModel);
	}

	@Bean
	@Scope("singleton")
	public BufferedImage sonivmIcon() {
		BufferedImage sonivmIcon;
		try {
			sonivmIcon = ImageIO.read(Sonivm.class.getResourceAsStream("/sonivm_logo.png"));
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to read Sonivm icon image", e);
			sonivmIcon = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		}
		return sonivmIcon;
	}

	private void initLookAndFeel(PreferencesService preferencesService) {
		String lookAndFeelName = null;
		try {
			lookAndFeelName = preferencesService.getLookAndFeel();
			if (lookAndFeelName != null) {
				SwingUtil.setLookAndFeel(lookAndFeelName, true);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to get look and feel name from preferences", e);
		}
	}

	protected JMenuBar sonivmWindowsMenuBar(SonivmMainWindow mainWindow, EqualizerWindow eqWin, PlaybackController sonivmController,
			PreferencesService preferencesService) {
		List<AudioDeviceOption> audioDevices = getAudioDeviceOptions();
		int scrobblePercentage = preferencesService.getPercentageToScrobbleAt(70);

		JMenuBuilder<JMenuBarBuilder> menuBuilder = new JMenuBarBuilder().menu("Options");

		JMenuBuilder<JMenuBuilder<JMenuBuilder<JMenuBarBuilder>>> menuBuilderLastFMScrobblePercentage = menuBuilder.subMenu("LastFM")
				.item("Set credentials...")
				.actr(actEvent -> {
					try {
						String user = preferencesService.getUsername();
						String password = preferencesService.getPassword();
						new UsernamePasswordDialog(null, "Set credentials", true, new Pair<String, String>(user, password), unPwdPair -> {
							new Thread(() -> {
								try {
									preferencesService.setUsername(unPwdPair.getK());
									preferencesService.setPassword(new String(unPwdPair.getV()));
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
						String key = preferencesService.getApiKey();
						String secret = preferencesService.getApiSecret();
						new UsernamePasswordDialog(null, "Set API keys", true, new Pair<String, String>(key, secret), unPwdPair -> {
							new Thread(() -> {
								try {
									preferencesService.setApiKey(unPwdPair.getK());
									preferencesService.setApiSecret(new String(unPwdPair.getV()));
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

		menuBuilder.item("Supported file extensions...").actr(actEvent -> {
			Set<String> supportedFileExtensions = preferencesService.getSupportedFileExtensions();
			new SupportedFileExtensionsDialog().display(supportedFileExtensions,
					newExtensions -> preferencesService.setSupportedFileExtensions(newExtensions));
		}).build();

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

		JMenuBar result = menuBuilder.build()
				.menu("Windows")
				.item("Sonivm")
				.actr(actEvent -> SwingUtil.showAndBringToFront(mainWindow))
				.build()
				.item("Equalizer")
				.actr(actEvent -> SwingUtil.showAndBringToFront(eqWin))
				.build()
				.build()
				.build();
		return result;
	}

	private List<AudioDeviceOption> getAudioDeviceOptions() {
		return Stream
				.concat(Stream.of(AudioDeviceOption.builder().audioDeviceInfo(null).build()),
						Stream.of(AudioSystem.getMixerInfo()).map(mixerInfo -> AudioDeviceOption.of(mixerInfo)))
				.collect(Collectors.toList());
	}
}
