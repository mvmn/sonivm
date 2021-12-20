package x.mvmn.sonivm.util;

public interface UnsafeOperation<E extends Exception> {
	public void run() throws E;
}