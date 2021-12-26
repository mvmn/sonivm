package x.mvmn.sonivm.ui.retro.rasterui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ScrollBarUI;

import lombok.Getter;
import x.mvmn.sonivm.ui.util.swing.ImageUtil;
import x.mvmn.sonivm.ui.util.swing.RectanglePointRange;

public class RasterFrameWindow extends RasterGraphicsWindow {

	private static final long serialVersionUID = 8746593596309812903L;

	@Getter
	protected volatile boolean active;

	protected volatile BufferedImage backgroundImage;
	@Getter
	protected volatile int widthExtension;
	@Getter
	protected volatile int heightExtension;
	protected volatile double scaleFactor = 1.0d;

	protected volatile int extensionWidthPixels = 0;
	protected volatile int extensionHeightPixels = 0;

	protected volatile int resizeInitialWidthExt;
	protected volatile int resizeInitialHeightExt;

	// protected final int initialWidth;
	// protected final int initialHeight;
	protected final int widthExtenderWidth;
	protected final int heightExtenderHeight;

	protected final int widthBeforeExtension;
	protected final int heightBeforeExtension;
	protected final int widthAfterExtension;
	protected final int heightAfterExtension;

	protected final BufferedImage topLeftActive;
	protected final BufferedImage titleActive;
	protected final BufferedImage topExtenderActive;
	protected final BufferedImage topRightActive;
	protected final BufferedImage topLeft;
	protected final BufferedImage title;
	protected final BufferedImage topExtender;
	protected final BufferedImage topRight;
	protected final BufferedImage left;
	protected final BufferedImage right;
	protected final BufferedImage bottomLeft;
	protected final BufferedImage bottomExtender;
	protected final BufferedImage bottomSpecialExtender;
	protected final BufferedImage bottomRight;
	protected final Color backgroundColor;
	protected final JComponent wrappedComponent;
	protected final JScrollPane wrappedComponentScrollPane;
	protected volatile RasterUISlider scrollSlider;
	protected final BufferedImage sliderButtonPressed;
	protected final BufferedImage sliderButtonReleased;
	protected final BufferedImage sliderBackground;

	public RasterFrameWindow(int baseWidth,
			int baseHeight,
			JComponent wrappedComponent,
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
			RectanglePointRange closeZone,
			BufferedImage sliderButtonPressed,
			BufferedImage sliderButtonReleased) {
		super(baseWidth, baseHeight, new BufferedImage(baseWidth, baseHeight, BufferedImage.TYPE_INT_ARGB), dragZone, resizeZone,
				closeZone);
		this.backgroundImage = super.backgroundImage;
		// this.initialWidth = initialWidth;
		// this.initialHeight = initialHeight;

		this.backgroundColor = backgroundColor;
		this.wrappedComponent = wrappedComponent;
		this.wrappedComponentScrollPane = new JScrollPane(wrappedComponent);

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

		this.sliderButtonPressed = sliderButtonPressed;
		this.sliderButtonReleased = sliderButtonReleased;
		this.sliderBackground = ImageUtil.subImageOrBlank(right, 4, 0, 8 + 1, 29); // 8 is slider button width + 1 is slider button x offset

		this.wrappedComponentScrollPane.getVerticalScrollBar().setUI(new ScrollBarUI() {});
		this.wrappedComponentScrollPane.getViewport().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				onScrollInScrollPane();
			}
		});
		this.wrappedComponentScrollPane.getViewport().setBackground(backgroundColor);
		this.wrappedComponentScrollPane.setBorder(BorderFactory.createEmptyBorder());

		reconstructScrollSlider();

		this.getContentPane().setLayout(null);
		this.getContentPane().add(wrappedComponentScrollPane);

		updateComponentLocations();
		updateExtPixelSizes();
		renderBackground();
	}

	protected void reconstructScrollSlider() {
		if (this.scrollSlider != null) {
			this.removeComponent(this.scrollSlider);
		}
		int range = heightExtenderHeight * 2 + extensionHeightPixels - sliderButtonPressed.getHeight();
		this.scrollSlider = new RasterUISlider(this, sliderBackground, sliderButtonPressed, sliderButtonReleased,
				initialWidth - right.getWidth() + 4 + extensionWidthPixels, topRight.getHeight(), range, 0, true, 1, 0);
		this.scrollSlider.addListener(() -> onScrollViaSlider(this.scrollSlider.getSliderPositionRatio()));
		this.addComponent(window -> this.scrollSlider);
	}

	protected void onScrollInScrollPane() {
		Rectangle visibleRect = this.wrappedComponent.getVisibleRect();
		int totalHeight = this.wrappedComponent.getHeight();
		if (totalHeight > 0 && totalHeight > visibleRect.height) {
			this.scrollSlider.setSliderPositionRatio(visibleRect.y / (double) (totalHeight - visibleRect.height), false);
		} else {
			this.scrollSlider.setSliderPositionRatio(0.0d, false);
		}
	}

	protected void onScrollViaSlider(double sliderPositionRatio) {
		Rectangle visibleRect = this.wrappedComponent.getVisibleRect();
		int y;
		if (this.wrappedComponent.getHeight() > visibleRect.height) {
			y = (int) Math.round((this.wrappedComponent.getHeight() - visibleRect.height) * sliderPositionRatio);
		} else {
			y = 0;
		}
		this.wrappedComponent.scrollRectToVisible(new Rectangle(visibleRect.x, y, visibleRect.width, visibleRect.height));
	}

	private void updateComponentLocations() {
		int width = initialWidth + widthExtenderWidth * widthExtension;
		int height = initialHeight + heightExtenderHeight * heightExtension;
		int widthScaled = newScaleCoord(scaleFactor, width);
		int heightScaled = newScaleCoord(scaleFactor, height);

		this.componentsLayer.setBounds(0, 0, widthScaled, heightScaled);
		int leftPx = newScaleCoord(getScaleFactor(), left.getWidth());
		int topPx = newScaleCoord(getScaleFactor(), topLeft.getHeight());
		int rightPx = newScaleCoord(getScaleFactor(), initialWidth + extensionWidthPixels - right.getWidth());
		int bottomPx = newScaleCoord(getScaleFactor(), initialHeight + extensionHeightPixels - bottomLeft.getHeight());
		this.wrappedComponentScrollPane.setBounds(leftPx, topPx, rightPx - leftPx, bottomPx - topPx);
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

		renderTitleBar();

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

	protected void renderTitleBar() {
		BufferedImage topLeft;
		BufferedImage widthExtender;
		BufferedImage title;
		BufferedImage topRight;
		if (!active) {
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
	}

	@Override
	protected void paintBackgroundPanel(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		int width = newScaleCoord(getScaleFactor(), this.extensionWidthPixels + this.initialWidth);
		if (backgroundImage.getWidth() == width) {
			g2.drawImage(backgroundImage, 0, 0, null);
		} else {
			g2.drawImage(backgroundImage.getScaledInstance(width, -1, BufferedImage.SCALE_SMOOTH), 0, 0, null);
		}
		applyTransparencyMask(g2);
		this.wrappedComponentScrollPane.repaint();
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
		this.updateComponentSize();
	}

	@Override
	protected void onResize(MouseEvent e) {
		int additionalXPixels = this.originalScaleCoord(e.getXOnScreen() - this.dragFromX);
		int additionalYPixels = this.originalScaleCoord(e.getYOnScreen() - this.dragFromY);

		int widthExtDelta = (int) Math.round(additionalXPixels / (double) widthExtenderWidth);
		int heightExtDelta = (int) Math.round(additionalYPixels / (double) heightExtenderHeight);

		if (widthExtDelta != 0 || heightExtDelta != 0) {
			int newWidthExt = Math.max(0, this.resizeInitialWidthExt + widthExtDelta);
			int newHeightExt = Math.max(0, this.resizeInitialHeightExt + heightExtDelta);
			if (newWidthExt != this.widthExtension || newHeightExt != this.heightExtension) {
				this.widthExtension = newWidthExt;
				this.heightExtension = newHeightExt;
				onSizeExtensionsChange();
			}
		}
		onScrollInScrollPane();
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
		reconstructScrollSlider();
		this.resizeListeners.stream().forEach(Runnable::run);

	}

	protected void updateComponentSize() {
		double scaleFactor = getScaleFactor();
		int width = initialWidth + widthExtenderWidth * widthExtension;
		int height = initialHeight + heightExtenderHeight * heightExtension;
		int widthScaled = newScaleCoord(scaleFactor, width);
		int heightScaled = newScaleCoord(scaleFactor, height);
		this.backgroundImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		renderBackground();
		this.setSize(widthScaled, heightScaled);
		updateComponentLocations();
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

	public void setActive(boolean active) {
		this.active = active;
		renderTitleBar();
	}
}
