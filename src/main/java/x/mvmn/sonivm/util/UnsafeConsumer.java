package x.mvmn.sonivm.util;

public interface UnsafeConsumer<T> {

	void accept(T t) throws Exception;

}
