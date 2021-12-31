package x.mvmn.sonivm.ui;

import java.awt.MenuItem;
import java.awt.PopupMenu;

import org.springframework.stereotype.Component;

import x.mvmn.sonivm.playqueue.PlaybackQueueEntry;

@Component
public class SonivmTrayIconPopupMenu {

	private final PopupMenu popupMenu = new PopupMenu();
	private final MenuItem miNowPlaying = new MenuItem("Stopped");
	private final MenuItem miEqualizer = new MenuItem("Equalizer");

	private final MenuItem miPreviousTrack = new MenuItem("<< Previous track");
	private final MenuItem miPlayPause = new MenuItem("-> Play");
	private final MenuItem miStop = new MenuItem("[x] Stop");
	private final MenuItem miNextTrack = new MenuItem(">> Next track");
	private final MenuItem miQuit = new MenuItem("Quit");

	public SonivmTrayIconPopupMenu() {
		popupMenu.add(miNowPlaying);
		popupMenu.add(miEqualizer);
		popupMenu.addSeparator();
		popupMenu.add(miPreviousTrack);
		popupMenu.add(miPlayPause);
		popupMenu.add(miStop);
		popupMenu.add(miNextTrack);
		popupMenu.addSeparator();
		popupMenu.add(miQuit);
	}

	public void registerHandler(SonivmUIController sonivmUI) {
		miNowPlaying.addActionListener(actEvent -> sonivmUI.onShowMainWindow());
		miEqualizer.addActionListener(actEvent -> sonivmUI.onShowEQWindow());

		miPreviousTrack.addActionListener(actEvent -> sonivmUI.onPreviousTrack());
		miPlayPause.addActionListener(actEvent -> sonivmUI.onPlayPause());
		miStop.addActionListener(actEvent -> sonivmUI.onStop());
		miNextTrack.addActionListener(actEvent -> sonivmUI.onNextTrack());
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
