package x.mvmn.sonivm.ui.util;

import com.sshtools.twoslices.Toast;
import com.sshtools.twoslices.ToastType;

import lombok.experimental.UtilityClass;
import x.mvmn.sonivm.Sonivm;

@UtilityClass
public class NotificationsUtil {

	public static void notify(String text) {
		try {
			Toast.builder().type(ToastType.INFO).title("Sonivm").icon(Sonivm.class.getResource("/sonivm_logo.png")).content(text).toast();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
