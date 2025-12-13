package x.mvmn.sonivm.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.swing.ButtonGroup;
import javax.swing.JMenuBar;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import lombok.Getter;
import x.mvmn.sonivm.PlaybackController;
import x.mvmn.sonivm.impl.AudioDeviceOption;
import x.mvmn.sonivm.playqueue.PlaybackQueueService;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.ui.guessgame.GuessMusicGameUI;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.ui.util.swing.menu.JMenuBarBuilder;
import x.mvmn.sonivm.ui.util.swing.menu.JMenuBarBuilder.JMenuBuilder;
import x.mvmn.sonivm.util.Pair;

public class SonivmMenuBar {
	private static final Logger LOGGER = Logger.getLogger(SonivmMenuBar.class.getCanonicalName());
	@Getter
	protected final JMenuBar jMenuBar;

	private AtomicReference<GuessMusicGameUI> gameUIRef = new AtomicReference<>();

	public SonivmMenuBar(SonivmUIController ui,
			PlaybackController sonivmController,
			PreferencesService preferencesService,
			PlaybackQueueService playbackQueueService) {
		List<AudioDeviceOption> audioDevices = SonivmUI.getAudioDeviceOptions();
		int scrobblePercentage = preferencesService.getPercentageToScrobbleAt(70);

		JMenuBuilder<JMenuBarBuilder> menuBuilder = new JMenuBarBuilder().menu("Options");
		menuBuilder.subMenu("Popup notifications")
				.item("Enable")
				.actr(e -> preferencesService.setNotificationsEnabled(true))
				.build()
				.item("Disable")
				.actr(e -> preferencesService.setNotificationsEnabled(false))
				.build().build();

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
				.actr(actEvent -> ui.onShowMainWindow())
				.build()
				.item("Equalizer")
				.actr(actEvent -> ui.onShowEQWindow())
				.build()
				.separator()
				.item("Retro UI main window")
				.actr(actEvent -> ui.onShowRetroUIMainWindow())
				.build()
				.item("Retro UI equalizer")
				.actr(actEvent -> ui.onShowRetroUIEQWindow())
				.build()
				.item("Retro UI playlist")
				.actr(actEvent -> ui.onShowRetroUIPlaylistWindow())
				.build()
				.separator()
				.item("Guess the music game")
				.actr(e -> SonivmMenuBar.this.showGame(playbackQueueService, sonivmController))
				.build()
				.build()
				.build();

		this.jMenuBar = result;
	}

	private void showGame(PlaybackQueueService playbackQueueService, PlaybackController playbackController) {
		GuessMusicGameUI ui = gameUIRef.updateAndGet(v -> {
			return v != null ? v : new GuessMusicGameUI(playbackQueueService, playbackController);
		});
		ui.pack();
		SwingUtil.moveToScreenCenter(ui);
		ui.setVisible(true);
	}
}
