package x.mvmn.sonivm.util;

public class StringUtil {

	public static String blankForNull(String val) {
		return val != null ? val : "";
	}

	public static String nullForBlank(String val) {
		return val != null && val.isBlank() ? null : val;
	}

	public static String nullForEmpty(String val) {
		return val != null && val.isEmpty() ? null : val;
	}
}
