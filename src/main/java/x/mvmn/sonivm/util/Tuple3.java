package x.mvmn.sonivm.util;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@Builder
public class Tuple3<A, B, C> {
	private final A a;
	private final B b;
	private final C c;
}
