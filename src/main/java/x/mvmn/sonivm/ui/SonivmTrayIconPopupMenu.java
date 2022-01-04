package x.mvmn.sonivm.ui;

import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.util.List;

import org.springframework.stereotype.Component;

import x.mvmn.sonivm.playqueue.PlaybackQueueEntry;
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
	private final MenuItem miQuit = new MenuItem("Quit");

	private final MenuItem miRetroUIMainWindow = new MenuItem("Main Window");
	private final MenuItem miRetroUIEQWindow = new MenuItem("Equalizer");
	private final MenuItem miRetroUIPlaylistWindow = new MenuItem("Playlist");
	private final Menu miRetroUISkins = new Menu("Skins...");
	private final MenuItem miImportSkins = new MenuItem("Import skins...");
	private final MenuItem miRefreshSkinsList = new MenuItem("Reload skins list");

	private volatile SonivmUIController sonivmUI;

	public SonivmTrayIconPopupMenu() {
		popupMenu.add(miNowPlaying);
		popupMenu.add(miEqualizer);
		popupMenu.addSeparator();
		popupMenu.add(miPreviousTrack);
		popupMenu.add(miPlayPause);
		popupMenu.add(miStop);
		popupMenu.add(miNextTrack);
		popupMenu.addSeparator();
		Menu retroUIMenu = new Menu("Retro UI...");
		retroUIMenu.add(miRetroUIMainWindow);
		retroUIMenu.add(miRetroUIEQWindow);
		retroUIMenu.add(miRetroUIPlaylistWindow);
		retroUIMenu.add(miRetroUISkins);
		retroUIMenu.add(miImportSkins);
		retroUIMenu.add(miRefreshSkinsList);
		popupMenu.add(retroUIMenu);
		popupMenu.addSeparator();
		popupMenu.add(miQuit);
	}

	public void registerHandler(SonivmUIController sonivmUI) {
		this.sonivmUI = sonivmUI;

		miNowPlaying.addActionListener(actEvent -> sonivmUI.onShowMainWindow());
		miEqualizer.addActionListener(actEvent -> sonivmUI.onShowEQWindow());

		miPreviousTrack.addActionListener(actEvent -> sonivmUI.onPreviousTrack());
		miPlayPause.addActionListener(actEvent -> sonivmUI.onPlayPause());
		miStop.addActionListener(actEvent -> sonivmUI.onStop());
		miNextTrack.addActionListener(actEvent -> sonivmUI.onNextTrack());

		miRetroUIMainWindow.addActionListener(actEvent -> sonivmUI.onShowRetroUIMainWindow());
		miRetroUIEQWindow.addActionListener(actEvent -> sonivmUI.onShowRetroUIEQWindow());
		miRetroUIPlaylistWindow.addActionListener(actEvent -> sonivmUI.onShowRetroUIPlaylistWindow());

		miImportSkins.addActionListener(actEvent -> sonivmUI.onImportSkins());
		miRefreshSkinsList.addActionListener(actEvent -> sonivmUI.onRefreshSkinsList());

		miRetroUISkins.addActionListener(actEvent -> sonivmUI.showSkinBrowser());

		miQuit.addActionListener(actEvent -> sonivmUI.onQuit());
	}

	public void setSkinsList(List<String> skins) {
		miRetroUISkins.removeAll();
		skins.stream().forEach(skinFileName -> miRetroUISkins.add(SwingUtil.menuItem(skinFileName, e -> onSkinSelect(skinFileName))));
	}

	public void onSkinSelect(String skinFileName) {
		sonivmUI.onRetroUiSkinChange(skinFileName);
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
