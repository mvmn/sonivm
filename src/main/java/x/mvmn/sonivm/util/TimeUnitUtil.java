package x.mvmn.sonivm.util;

import java.util.concurrent.TimeUnit;

public class TimeUnitUtil {

	public static String prettyPrintFromSeconds(final long seconds) {
		final long hr = TimeUnit.SECONDS.toHours(seconds);
		final long min = TimeUnit.SECONDS.toMinutes(seconds - TimeUnit.HOURS.toMillis(hr));
		final long sec = TimeUnit.SECONDS.toSeconds(seconds - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
		StringBuilder result = new StringBuilder();
		if (hr > 0) {
			result.append(String.format("%02d", hr));
		}
		if (min > 0) {
			result.append(String.format("%02d", min));
		}
		result.append(String.format("%02d", sec));
		return result.toString();
	}
}
