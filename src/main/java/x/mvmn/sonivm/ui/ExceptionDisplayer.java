package x.mvmn.sonivm.ui;

import java.util.function.Consumer;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import lombok.Builder;
import lombok.Data;

public class ExceptionDisplayer implements Consumer<ExceptionDisplayer.ErrorData> {

	public static ExceptionDisplayer INSTANCE = new ExceptionDisplayer();

	@Data
	@Builder
	public static class ErrorData {
		final String message;
		final Throwable error;
	}

	@Override
	public void accept(ErrorData errorData) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(() -> show(errorData));
		} else {
			show(errorData);
		}
	}

	protected void show(ErrorData errorData) {
		JOptionPane.showMessageDialog(null, errorData.getError().getClass().getCanonicalName() + " " + errorData.getError().getMessage(),
				errorData.getMessage(), JOptionPane.ERROR_MESSAGE);
	}
}
