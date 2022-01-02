package x.mvmn.sonivm.ui.util.swing;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
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
		return drawOnto(background, img, x, y, AlphaComposite.SRC, 1.0f);
	}

	public static BufferedImage drawOnto(BufferedImage background, BufferedImage img, int x, int y, int alphaCompositeType, float alpha) {
		Graphics2D g2d = background.createGraphics();
		g2d.setComposite(AlphaComposite.getInstance(alphaCompositeType, alpha));
		g2d.drawImage(img, x, y, null);
		g2d.dispose();
		return background;
	}

	public static void applyTransparencyMask(BufferedImage image, BufferedImage mask) {
		if (image.getWidth() != mask.getWidth() || image.getHeight() != mask.getHeight()) {
			throw new IllegalArgumentException("Image and mask must have same size");
		}
		int width = image.getWidth();
		int height = image.getHeight();
		int[] imagePixels = image.getRGB(0, 0, width, height, null, 0, width);
		int[] maskPixels = mask.getRGB(0, 0, width, height, null, 0, width);

		for (int i = 0; i < imagePixels.length; i++) {
			int color = imagePixels[i] & 0x00ffffff; // Mask preexisting alpha
			int alpha = maskPixels[i] << 24; // Shift blue to alpha
			imagePixels[i] = color | alpha;
		}

		image.setRGB(0, 0, width, height, imagePixels, 0, width);
	}

	public static void applyTransparency(Graphics2D graphics2d, Image mask) {
		AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.DST_IN, 1.0F);
		graphics2d.setComposite(ac);
		graphics2d.drawImage(mask, 0, 0, null);
	}

	public static BufferedImage convert(BufferedImage image, int type) {
		if (image == null) {
			return null;
		}

		BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), type);
		drawOnto(result, image, 0, 0);
		return result;
	}

	public static BufferedImage subImageOrBlank(BufferedImage sourceImage, int x, int y, int width, int height) {
		return subImageOrBlank(sourceImage, x, y, width, height, false);
	}

	public static BufferedImage subImageOrBlank(BufferedImage sourceImage, int x, int y, int width, int height, boolean newImage) {
		if (sourceImage.getWidth() >= x + width && sourceImage.getHeight() >= y + height) {
			BufferedImage subImg = sourceImage.getSubimage(x, y, width, height);
			if (newImage) {
				BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				ImageUtil.drawOnto(result, subImg, 0, 0);
				return result;
			} else {
				return subImg;
			}
		} else {
			return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		}
	}

	public static Color averageColor(BufferedImage bi, int x, int y, int width, int height) {
		long sumRed = 0;
		long sumGreen = 0;
		long sumBlue = 0;
		for (int xc = x; xc < x + width; xc++) {
			for (int yc = y; yc < y + height; yc++) {
				int color = bi.getRGB(xc, yc);
				int red = (color >> 16) & 0xFF;
				int green = (color >> 8) & 0xFF;
				int blue = color & 0xFF;
				sumRed += red;
				sumGreen += green;
				sumBlue += blue;
			}
		}
		long count = width * height;
		return new Color((int) (sumRed / count), (int) (sumGreen / count), (int) (sumBlue / count));
	}

	public static Color findMinOrMaxColor(BufferedImage bi, int x, int y, int width, int height, boolean brightest) {
		int result = 0;
		int resultBrightness = brightest ? 0 : 256 + 256 + 256;
		for (int xc = x; xc < x + width; xc++) {
			for (int yc = y; yc < y + height; yc++) {
				int color = bi.getRGB(xc, yc);

				int colorBrightness = SwingUtil.getColorBrightness(color);
				boolean brighter = colorBrightness > resultBrightness;
				if (!(brightest ^ brighter)) {
					result = color;
					resultBrightness = colorBrightness;
				}
			}
		}
		return new Color(result);
	}
}
