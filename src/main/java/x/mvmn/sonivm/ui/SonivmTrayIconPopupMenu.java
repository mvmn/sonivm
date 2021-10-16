package x.mvmn.sonivm.ui;

import java.awt.MenuItem;
import java.awt.PopupMenu;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import x.mvmn.sonivm.ui.model.PlaybackQueueEntry;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;

@Component
public class SonivmTrayIconPopupMenu {

	private final PopupMenu popupMenu = new PopupMenu();
	private final MenuItem miNowPlaying = new MenuItem("Stopped");
	private final MenuItem miEqualizer = new MenuItem("Equalizer");

	private final MenuItem miPreviousTrack = new MenuItem("<< Previous track");
	private final MenuItem miPlayPause = new MenuItem("-> Play");
	private final MenuItem miStop = new MenuItem("[x] Stop");
	private final MenuItem miNextTrack = new MenuItem(">> Next track");

	@Autowired
	private SonivmMainWindow mainWindow;

	@Autowired
	private EqualizerWindow equalizerWindow;

	@Autowired
	private SonivmController controller;

	@PostConstruct
	public void init() {
		miNowPlaying.addActionListener(actEvent -> {
			mainWindow.setVisible(true);
			mainWindow.toFront();
			mainWindow.requestFocus();
		});

		miEqualizer.addActionListener(actEvent -> {
			equalizerWindow.setVisible(true);
			equalizerWindow.toFront();
			equalizerWindow.requestFocus();
		});
		miPreviousTrack.addActionListener(actEvent -> controller.onPreviousTrack());
		miPlayPause.addActionListener(actEvent -> controller.onPlayPause());
		miStop.addActionListener(actEvent -> controller.onStop());
		miNextTrack.addActionListener(actEvent -> controller.onNextTrack());

		popupMenu.add(miNowPlaying);
		popupMenu.add(miEqualizer);
		popupMenu.addSeparator();
		popupMenu.add(miPreviousTrack);
		popupMenu.add(miPlayPause);
		popupMenu.add(miStop);
		popupMenu.add(miNextTrack);
		popupMenu.addSeparator();
		popupMenu.add(SwingUtil.menuItem("Quit", event -> controller.onQuit()));
	}

	public PopupMenu getUIComponent() {
		return popupMenu;
	}

	public void updateNowPlaying(PlaybackQueueEntry trackInfo) {
		miNowPlaying.setLabel((trackInfo != null ? trackInfo.toDisplayStr() : "Stopped"));
	}

	public void setPlayPauseButtonState(boolean playing) {
		miPlayPause.setLabel(playing ? "|| Pause" : "-> Play");
	}
}
