package x.mvmn.sonivm.util;

import java.util.function.Consumer;
import java.util.function.Function;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CallUtil {

	public static interface UnsafeFunction<I, O> {
		O apply(I t) throws Exception;
	}

	public static void doUnsafe(UnsafeOperation task) {
		try {
			task.run();
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	public static Runnable unsafe(UnsafeOperation task) {
		return () -> doUnsafe(task);
	}

	public static <T> Consumer<T> unsafe(UnsafeConsumer<T> task) {
		return v -> doUnsafe(() -> task.accept(v));
	}

	public static <I, O> Function<I, O> safe(UnsafeFunction<I, O> unsafe) {
		return (I input) -> {
			try {
				return unsafe.apply(input);
			} catch (Throwable t) {
				throw new RuntimeException(t);
			}
		};
	}
}
