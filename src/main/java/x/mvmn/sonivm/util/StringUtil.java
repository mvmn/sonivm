package x.mvmn.sonivm.util;

import java.text.Normalizer;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StringUtil {

	public static String blankForNull(String val) {
		return val != null ? val : "";
	}

	public static String nullForBlank(String val) {
		return val != null && val.trim().isEmpty() ? null : val;
	}

	public static String nullForEmpty(String val) {
		return val != null && val.isEmpty() ? null : val;
	}

	public static String stripAccents(String val) {
		return val == null ? null : Normalizer.normalize(val, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
	}
}
