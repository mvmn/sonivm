package x.mvmn.sonivm.config;

import java.awt.TrayIcon;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import x.mvmn.sonivm.Sonivm;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.ui.EqualizerWindow;
import x.mvmn.sonivm.ui.SonivmController;
import x.mvmn.sonivm.ui.SonivmMainWindow;
import x.mvmn.sonivm.ui.SonivmTrayIconPopupMenu;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;

@Configuration
public class SonivmConfig {

	private static final Logger LOGGER = Logger.getLogger(SonivmConfig.class.getCanonicalName());

	@Value("Sonivm v${app.version:0.0.1}")
	private String appVersion = "Sonivm";

	@Bean
	@Scope("singleton")
	public SonivmMainWindow sonivmMainWindow(@Autowired SonivmController sonivmController,
			@Autowired PlaybackQueueTableModel playbackQueueTableModel, @Autowired PreferencesService appPreferencesService) {

		initLookAndFeel(appPreferencesService);

		return new SonivmMainWindow(appVersion, sonivmController, playbackQueueTableModel);
	}

	@Bean
	@Scope("singleton")
	public EqualizerWindow equalizerWindow() {
		return new EqualizerWindow(appVersion + " equalizer", 10);
	}

	@Bean
	@Scope("singleton")
	public TrayIcon sonivmTrayIcon(@Autowired SonivmTrayIconPopupMenu sonivmTrayIconPopupMenu) {
		TrayIcon trayIcon = new TrayIcon(Sonivm.sonivmIcon);
		trayIcon.setImageAutoSize(true);
		trayIcon.setPopupMenu(sonivmTrayIconPopupMenu.getUIComponent());
		return trayIcon;
	}

	private void initLookAndFeel(PreferencesService appPreferencesService) {
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
}
