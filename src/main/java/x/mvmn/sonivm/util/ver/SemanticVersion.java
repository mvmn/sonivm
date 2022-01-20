package x.mvmn.sonivm.util.ver;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SemanticVersion implements Comparable<SemanticVersion> {
	@Getter
	protected final int[] versionElements;

	@Override
	public int compareTo(SemanticVersion o) {
		int result = 0;
		for (int i = 0; i < Math.max(this.versionElements.length, o.getVersionElements().length); i++) {
			result = Integer.compare(this.versionElements.length > i ? this.versionElements[i] : 0,
					o.getVersionElements().length > i ? o.getVersionElements()[i] : 0);
			if (result != 0) {
				break;
			}
		}
		return result;
	}
}