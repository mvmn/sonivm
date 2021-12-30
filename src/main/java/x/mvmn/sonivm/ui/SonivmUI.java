package x.mvmn.sonivm.ui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;

@RequiredArgsConstructor
public class SonivmUI {

	// private static final Logger LOGGER = Logger.getLogger(SonivmUI.class.getSimpleName());

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
}
