package x.mvmn.sonivm.ui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.util.Tuple4;

@RequiredArgsConstructor
public class SonivmUI {

	private static final Logger LOGGER = Logger.getLogger(SonivmUI.class.getSimpleName());

	@Getter
	protected final SonivmMainWindow mainWindow;
	@Getter
	protected final EqualizerWindow eqWindow;
	protected final PreferencesService preferencesService;
	protected final SonivmController sonivmController;

	public void showMainWindow() {
		SwingUtil.showAndBringToFront(mainWindow);
	}

	public void showEqWindow() {
		SwingUtil.showAndBringToFront(eqWindow);
	}

	public void toggleEqWindow() {
		if (!eqWindow.isVisible()) {
			SwingUtil.showAndBringToFront(eqWindow);
		} else {
			eqWindow.setVisible(false);
		}
	}

	@PostConstruct
	public void init() {
		LOGGER.info("Restoring window positions/sizes/visibility");
		try {
			Tuple4<Boolean, String, Point, Dimension> mainWindowState = preferencesService.getMainWindowState();
			SwingUtil.runOnEDT(() -> {
				applyWindowState(mainWindow, mainWindowState, true);
				restorePlayQueueColumnsState();

			}, true);
			Tuple4<Boolean, String, Point, Dimension> eqWindowState = preferencesService.getEQWindowState();
			SwingUtil.runOnEDT(() -> applyWindowState(eqWindow, eqWindowState, false), true);
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Failed to restore window states", t);
		}
	}

	public static void applyWindowState(Window window, Tuple4<Boolean, String, Point, Dimension> windowState, boolean visibleByDefault) {
		if (windowState != null) {
			SwingUtil.restoreWindowState(window, windowState);
		} else {
			window.pack();
			SwingUtil.moveToScreenCenter(window);
			window.setVisible(visibleByDefault);
		}
	}

	private void restorePlayQueueColumnsState() {
		try {
			int[] playQueueColumnPositions = preferencesService.getPlayQueueColumnPositions();
			if (playQueueColumnPositions != null && playQueueColumnPositions.length > 0) {
				SwingUtil.runOnEDT(() -> mainWindow.setPlayQueueTableColumnPositions(playQueueColumnPositions), true);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to read+apply column positions for playback queue table", e);
		}

		try {
			int[] playQueueColumnWidths = preferencesService.getPlayQueueColumnWidths();
			if (playQueueColumnWidths != null && playQueueColumnWidths.length > 0) {
				SwingUtil.runOnEDT(() -> mainWindow.setPlayQueueTableColumnWidths(playQueueColumnWidths), true);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to read+apply column width for playback queue table", e);
		}
	}
}
