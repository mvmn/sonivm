package x.mvmn.sonivm.ui;

import java.util.function.Consumer;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ExceptionDisplayer implements Consumer<Throwable> {

	public static ExceptionDisplayer INSTANCE = new ExceptionDisplayer();

	@Override
	public void accept(Throwable t) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> show(t));
		} else {
			show(t);
		}
	}

	protected void show(Throwable t) {
		JOptionPane.showMessageDialog(null, t.getClass().getCanonicalName() + " " + t.getMessage(), "Error occurred",
				JOptionPane.ERROR_MESSAGE);
	}
}
