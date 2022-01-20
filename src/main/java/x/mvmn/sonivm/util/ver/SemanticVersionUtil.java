package x.mvmn.sonivm.util.ver;

import java.util.stream.Stream;

public abstract class SemanticVersionUtil {

	public static SemanticVersion parse(String ver) {
		return new SemanticVersion(
				Stream.of(ver.split("[^0-9]+")).map(String::trim).filter(s -> !s.isEmpty()).mapToInt(Integer::parseInt).toArray());
	}
}
