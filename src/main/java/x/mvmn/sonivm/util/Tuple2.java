package x.mvmn.sonivm.util;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@Builder
public class Tuple2<A, B> {
	private final A a;
	private final B b;
}
