package x.mvmn.sonivm.util;

import java.util.concurrent.TimeUnit;

public class TimeUnitUtil {

	public static String prettyPrintFromSeconds(final long seconds) {
		final long hr = TimeUnit.SECONDS.toHours(seconds);
		final long min = TimeUnit.SECONDS.toMinutes(seconds - TimeUnit.HOURS.toSeconds(hr));
		final long sec = TimeUnit.SECONDS.toSeconds(seconds - TimeUnit.HOURS.toSeconds(hr) - TimeUnit.MINUTES.toSeconds(min));
		StringBuilder result = new StringBuilder();
		if (hr > 0) {
			result.append(String.format("%02d", hr)).append(":");
		}
		if (min > 0) {
			result.append(String.format("%02d", min)).append(":");
		}
		result.append(String.format("%02d", sec));
		return result.toString();
	}
}
