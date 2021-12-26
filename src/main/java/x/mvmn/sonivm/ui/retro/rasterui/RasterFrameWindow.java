package x.mvmn.sonivm.ui.retro.rasterui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import x.mvmn.sonivm.ui.util.swing.ImageUtil;
import x.mvmn.sonivm.ui.util.swing.RectanglePointRange;

public class RasterFrameWindow extends RasterGraphicsWindow {

	private static final long serialVersionUID = 8746593596309812903L;

	protected volatile BufferedImage backgroundImage;
	private volatile int widthExtension;
	private volatile int heightExtension;
	private volatile double scaleFactor = 1.0d;

	private volatile int extensionWidthPixels = 0;
	private volatile int extensionHeightPixels = 0;

	private volatile int resizeInitialWidthExt;
	private volatile int resizeInitialHeightExt;

	// private final int initialWidth;
	// private final int initialHeight;
	private final int widthExtenderWidth;
	private final int heightExtenderHeight;

	private final int widthBeforeExtension;
	private final int heightBeforeExtension;
	private final int widthAfterExtension;
	private final int heightAfterExtension;

	private final BufferedImage topLeftActive;
	private final BufferedImage titleActive;
	private final BufferedImage topExtenderActive;
	private final BufferedImage topRightActive;
	private final BufferedImage topLeft;
	private final BufferedImage title;
	private final BufferedImage topExtender;
	private final BufferedImage topRight;
	private final BufferedImage left;
	private final BufferedImage right;
	private final BufferedImage bottomLeft;
	private final BufferedImage bottomExtender;
	private final BufferedImage bottomSpecialExtender;
	private final BufferedImage bottomRight;
	private final Color backgroundColor;

	public RasterFrameWindow(int baseWidth,
			int baseHeight,
			Color backgroundColor,
			BufferedImage topLeftActive,
			BufferedImage titleActive,
			BufferedImage topExtenderActive,
			BufferedImage topRightActive,
			BufferedImage topLeft,
			BufferedImage title,
			BufferedImage topExtender,
			BufferedImage topRight,
			BufferedImage left,
			BufferedImage right,
			BufferedImage bottomLeft,
			BufferedImage bottomExtender,
			BufferedImage bottomSpecialExtender,
			BufferedImage bottomRight,
			RectanglePointRange dragZone,
			RectanglePointRange resizeZone,
			RectanglePointRange closeZone) {
		super(baseWidth, baseHeight, new BufferedImage(baseWidth, baseHeight, BufferedImage.TYPE_INT_ARGB), dragZone, resizeZone,
				closeZone);
		this.backgroundImage = super.backgroundImage;
		// this.initialWidth = initialWidth;
		// this.initialHeight = initialHeight;

		this.backgroundColor = backgroundColor;

		this.widthExtenderWidth = bottomExtender.getWidth(); // 25
		this.heightExtenderHeight = left.getHeight(); // 29
		this.widthBeforeExtension = left.getWidth();
		this.heightBeforeExtension = topLeft.getHeight();
		this.widthAfterExtension = right.getWidth();
		this.heightAfterExtension = bottomLeft.getHeight();

		this.topLeftActive = topLeftActive;
		this.titleActive = titleActive;
		this.topExtenderActive = topExtenderActive;
		this.topRightActive = topRightActive;
		this.topLeft = topLeft;
		this.title = title;
		this.topExtender = topExtender;
		this.topRight = topRight;
		this.left = left;
		this.right = right;
		this.bottomLeft = bottomLeft;
		this.bottomExtender = bottomExtender;
		this.bottomSpecialExtender = bottomSpecialExtender;
		this.bottomRight = bottomRight;

		// Background image: PLEDIT.TXT color background or black + frame
		// Re-draw background + frame and re-create scroll slider on extension change
		// Absolute positioning for tracklist component
		// JLayer for buttons

		updateExtPixelSizes();
		renderBackground();
	}

	protected void renderBackground() {
		// Title bar:
		// - topLeft
		// - 0.5 extender * widthExtension (left half 12 px or complete image - depending on what fits)
		// 2 * extender
		// title
		// 0.5 extender * widthExtension (left half 13 px or complete image - depending on what fits)
		// 2 * extender
		// topRight

		Graphics2D g = this.backgroundImage.createGraphics();
		g.setColor(backgroundColor);
		g.fillRect(0, 0, this.backgroundImage.getWidth(), this.backgroundImage.getHeight());
		g.dispose();

		BufferedImage topLeft;
		BufferedImage widthExtender;
		BufferedImage title;
		BufferedImage topRight;
		if (isActive()) {
			topLeft = this.topLeft;
			widthExtender = this.topExtender;
			title = this.title;
			topRight = this.topRight;
		} else {
			topLeft = this.topLeftActive;
			widthExtender = this.topExtenderActive;
			title = this.titleActive;
			topRight = this.topRightActive;
		}

		ImageUtil.drawOnto(this.backgroundImage, topLeft, 0, 0);
		int offset = topLeft.getWidth();
		int numOfExtenderHalves = 5 + widthExtension;
		offset = drawTitleExtensions(numOfExtenderHalves, offset, widthExtender, false);
		ImageUtil.drawOnto(this.backgroundImage, title, offset, 0);
		offset += title.getWidth();
		offset = drawTitleExtensions(numOfExtenderHalves, offset, widthExtender, true);
		ImageUtil.drawOnto(this.backgroundImage, topRight, offset, 0);

		int actualWindowWidth = initialWidth + 25 * widthExtension;
		for (int i = 0; i < heightExtension + 2; i++) {
			int yOffset = heightExtenderHeight * i + topLeft.getHeight();
			ImageUtil.drawOnto(this.backgroundImage, left, 0, yOffset);
			ImageUtil.drawOnto(this.backgroundImage, right, actualWindowWidth - right.getWidth(), yOffset);
		}

		int bottomLineYOffset = heightExtenderHeight * (heightExtension + 2) + topLeft.getHeight();
		ImageUtil.drawOnto(this.backgroundImage, this.bottomLeft, 0, bottomLineYOffset);
		ImageUtil.drawOnto(this.backgroundImage, this.bottomRight, actualWindowWidth - this.bottomRight.getWidth(), bottomLineYOffset);

		if (widthExtension > 0) {
			drawBottomWidthExtension(actualWindowWidth, bottomLineYOffset);
		}
	}

	@Override
	protected void paintBackgroundPanel(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		g2.drawImage(backgroundImage, 0, 0, null);
		applyTransparencyMask(g2);
	}

	private void drawBottomWidthExtension(int actualWindowWidth, int bottomLineYOffset) {
		int numOfExtendersToDraw = widthExtension;
		if (numOfExtendersToDraw >= 3) {
			ImageUtil.drawOnto(this.backgroundImage, this.bottomSpecialExtender,
					actualWindowWidth - this.bottomRight.getWidth() - this.bottomSpecialExtender.getWidth(), bottomLineYOffset);
			numOfExtendersToDraw -= 3;
		}
		for (int i = 0; i < numOfExtendersToDraw; i++) {
			ImageUtil.drawOnto(this.backgroundImage, this.bottomExtender, this.bottomLeft.getWidth() + widthExtenderWidth * i,
					bottomLineYOffset);
		}
	}

	private int drawTitleExtensions(int numberOfTitleExtensionHalves, int offset, BufferedImage widthExtender, boolean rightOfTitle) {
		int currentNumOfExtHalvesToDraw = numberOfTitleExtensionHalves;
		if (currentNumOfExtHalvesToDraw % 2 == 1) {
			ImageUtil.drawOnto(this.backgroundImage, widthExtender, offset, 0);
			offset += rightOfTitle ? 13 : 12;
			currentNumOfExtHalvesToDraw--;
		}
		for (int i = 0; i < currentNumOfExtHalvesToDraw / 2; i++) {
			ImageUtil.drawOnto(this.backgroundImage, widthExtender, offset, 0);
			offset += 25; // widthExtender.getWidth() is 25 in WinAmp skins
		}
		return offset;
	}

	@Override
	public double getScaleFactor() {
		return scaleFactor;
	}

	public void setScaleFactor(double scaleFactor) {
		this.scaleFactor = scaleFactor;
		this.repaint();
	}

	@Override
	protected void onResize(MouseEvent e) {
		int additionalXPixels = e.getXOnScreen() - this.dragFromX;
		int additionalYPixels = e.getYOnScreen() - this.dragFromY;

		int widthExtDelta = (int) Math.round(additionalXPixels / (double) widthExtenderWidth);
		int heightExtDelta = (int) Math.round(additionalYPixels / (double) heightExtenderHeight);

		if (widthExtDelta != 0 || heightExtDelta != 0) {
			this.widthExtension = Math.max(0, this.resizeInitialWidthExt + widthExtDelta);
			this.heightExtension = Math.max(0, this.resizeInitialHeightExt + heightExtDelta);
			onSizeExtensionsChange();
		}
	}

	@Override
	protected void onResizeStart(MouseEvent e) {
		super.onResizeStart(e);
		this.resizeInitialWidthExt = this.widthExtension;
		this.resizeInitialHeightExt = this.heightExtension;
	}

	protected void updateExtPixelSizes() {
		this.extensionWidthPixels = widthExtenderWidth * widthExtension;
		this.extensionHeightPixels = heightExtenderHeight * heightExtension;

	}

	protected void onSizeExtensionsChange() {
		updateExtPixelSizes();
		updateComponentSize();
		this.resizeListeners.stream().forEach(Runnable::run);

	}

	protected void updateComponentSize() {
		int width = initialWidth + widthExtenderWidth * widthExtension;
		int height = initialHeight + heightExtenderHeight * heightExtension;
		this.backgroundImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		renderBackground();
		this.setSize(width, height);
	}

	public void setWidthSizeExtension(int widthExtension) {
		this.widthExtension = Math.max(0, widthExtension);
		onSizeExtensionsChange();
	}

	public void setHeighSizeExtension(int heightExtension) {
		this.heightExtension = Math.max(0, heightExtension);
		onSizeExtensionsChange();
	}

	public void setSizeExtensions(int widthExtension, int heightExtension) {
		this.widthExtension = Math.max(0, widthExtension);
		this.heightExtension = Math.max(0, heightExtension);
		onSizeExtensionsChange();
	}

	@Override
	protected boolean inCloseZone(int x, int y) {
		return inZoneMindingExtensions(x, y, closeZone);
	}

	@Override
	protected boolean inResizeZone(int x, int y) {
		return inZoneMindingExtensions(x, y, resizeZone);
	}

	@Override
	protected boolean inMoveZone(int x, int y) {
		return inZoneMindingExtensions(x, y, moveZone);
	}

	protected boolean inZoneMindingExtensions(int x, int y, RectanglePointRange zone) {
		if (zone == null) {
			return false;
		}
		int left = zone.getLeft();
		int top = zone.getTop();
		int right = zone.getRight();
		int bottom = zone.getBottom();
		if (left > widthBeforeExtension + widthAfterExtension) {
			left += extensionWidthPixels;
		}
		if (top > heightBeforeExtension + heightAfterExtension) {
			top += extensionHeightPixels;
		}

		if (right > widthBeforeExtension) {
			right += extensionWidthPixels;
		}
		if (bottom > heightBeforeExtension) {
			bottom += extensionHeightPixels;
		}
		return new RectanglePointRange(left, top, right, bottom).inRange(x, y);
	}
}
