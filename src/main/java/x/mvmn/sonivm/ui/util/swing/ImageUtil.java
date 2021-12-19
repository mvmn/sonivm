package x.mvmn.sonivm.ui.util.swing;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

public abstract class ImageUtil {

	public static ImageIcon resizeImageIcon(ImageIcon imgIcon, int width, int height, int hints) {
		return new ImageIcon(imgIcon.getImage().getScaledInstance(width, height, hints));
	}

	public static ImageIcon fromClasspathResource(String resource) {
		return new ImageIcon(ImageUtil.class.getResource(resource));
	}

	public static BufferedImage drawOnto(BufferedImage background, BufferedImage img, int x, int y) {
		Graphics2D g2d = background.createGraphics();
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		g2d.drawImage(img, x, y, null);
		g2d.dispose();
		return background;
	}
}
