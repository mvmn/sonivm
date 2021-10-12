package x.mvmn.sonivm.ui.util.swing;

import javax.swing.ImageIcon;

public abstract class ImageUtil {

	public static ImageIcon resizeImageIcon(ImageIcon imgIcon, int width, int height, int hints) {
		return new ImageIcon(imgIcon.getImage().getScaledInstance(width, height, hints));
	}

	public static ImageIcon fromClasspathResource(String resource) {
		return new ImageIcon(ImageUtil.class.getResource(resource));
	}
}
