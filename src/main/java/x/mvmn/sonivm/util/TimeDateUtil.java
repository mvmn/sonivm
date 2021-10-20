package x.mvmn.sonivm.util;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeDateUtil {
	private static final Pattern PTRN_DATE_STARTS_WITH_YEAR = Pattern.compile("^\\d{4}\\-.*");

	public static String prettyPrintFromSeconds(final int seconds) {
		final long hr = TimeUnit.SECONDS.toHours(seconds);
		final long min = TimeUnit.SECONDS.toMinutes(seconds - TimeUnit.HOURS.toSeconds(hr));
		final long sec = TimeUnit.SECONDS.toSeconds(seconds - TimeUnit.HOURS.toSeconds(hr) - TimeUnit.MINUTES.toSeconds(min));
		StringBuilder result = new StringBuilder();
		if (hr > 0) {
			result.append(String.format("%02d", hr)).append(":");
		}
		result.append(String.format("%02d", min)).append(":");
		result.append(String.format("%02d", sec));
		return result.toString();
	}

	public static String yearFromDateTagValue(String date) {
		if (date != null && PTRN_DATE_STARTS_WITH_YEAR.matcher(date.trim()).matches()) {
			date = date.trim().substring(0, 4);
		}
		return date;
	}
}
