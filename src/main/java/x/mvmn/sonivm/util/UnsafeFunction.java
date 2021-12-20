package x.mvmn.sonivm.util;

public interface UnsafeFunction<I, O, E extends Exception> {
	O apply(I t) throws E;
}