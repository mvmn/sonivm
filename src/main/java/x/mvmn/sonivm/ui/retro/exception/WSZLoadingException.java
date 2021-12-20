package x.mvmn.sonivm.ui.retro.exception;

public class WSZLoadingException extends Exception {
	private static final long serialVersionUID = -916976770535859968L;

	public WSZLoadingException() {
		super();
	}

	public WSZLoadingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public WSZLoadingException(String message, Throwable cause) {
		super(message, cause);
	}

	public WSZLoadingException(String message) {
		super(message);
	}

	public WSZLoadingException(Throwable cause) {
		super(cause);
	}
}
