package x.mvmn.sonivm.model;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
public class IntRange {
	private final int from;
	private final int to;
}
