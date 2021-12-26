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

	public static <X, Y, Z> Tuple3<X, Y, Z> of(X x, Y y, Z z) {
		return Tuple3.<X, Y, Z> builder().a(x).b(y).c(z).build();
	}
}
