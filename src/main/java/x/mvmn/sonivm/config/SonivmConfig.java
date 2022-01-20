package x.mvmn.sonivm.config;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import x.mvmn.sonivm.PlaybackController;
import x.mvmn.sonivm.Sonivm;
import x.mvmn.sonivm.WinAmpSkinsService;
import x.mvmn.sonivm.eq.EqualizerPresetService;
import x.mvmn.sonivm.eq.SonivmEqualizerService;
import x.mvmn.sonivm.lastfm.LastFMScrobblingService;
import x.mvmn.sonivm.playqueue.PlaybackQueueService;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.ui.EqualizerWindow;
import x.mvmn.sonivm.ui.SonivmMainWindow;
import x.mvmn.sonivm.ui.SonivmTrayIconPopupMenu;
import x.mvmn.sonivm.ui.SonivmUI;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.util.ver.SemanticVersion;
import x.mvmn.sonivm.util.ver.SemanticVersionUtil;

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
		EqualizerWindow eqWindow = new EqualizerWindow(appVersion + " equalizer", 10);
		eqWindow.setIconImage(sonivmIcon);

		return new SonivmUI(mainWindow, eqWindow, sonivmIcon, trayIconPopupMenu, preferencesService, sonivmController,
				lastFMScrobblingService, playbackQueueService, winAmpSkinsService, eqService, eqPresetService, playQueueTableModel);
	}

	@Bean
	@Scope("singleton")
	public SemanticVersion sonivmVersion() {
		return SemanticVersionUtil.parse(appVersion);
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
}
