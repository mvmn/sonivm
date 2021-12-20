package x.mvmn.sonivm.util;

public interface UnsafeConsumer<T, E extends Exception> {

	void accept(T t) throws E;

}
