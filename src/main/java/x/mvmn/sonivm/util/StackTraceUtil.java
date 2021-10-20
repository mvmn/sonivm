package x.mvmn.sonivm.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StackTraceUtil {

	public static String toString(Throwable t) {
		StringWriter strw = new StringWriter();
		t.printStackTrace(new PrintWriter(strw));
		return strw.toString();
	}
}
