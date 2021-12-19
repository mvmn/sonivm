package x.mvmn.sonivm.ui.retro;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

public class RasterUISlider extends RasterUIComponent implements MouseListener, MouseMotionListener {

	protected final BufferedImage sliderBackground;
	protected final BufferedImage sliderButtonReleased;
	protected final BufferedImage sliderButtonPressed;
	protected final int range;
	protected final boolean vertical;

	protected volatile int sliderPosition;
	protected volatile boolean pressed;
	protected volatile int dragStartX;
	protected volatile int dragStartY;
	protected volatile int dragStartSliderPos;

	public RasterUISlider(RasterGraphicsWindow parent, BufferedImage sliderBackground, BufferedImage sliderButtonReleased,
			BufferedImage sliderButtonPressed, int x, int y, int range, boolean vertical) {
		super(parent, new BufferedImage(sliderBackground.getWidth(), sliderBackground.getHeight(), BufferedImage.TYPE_INT_ARGB), x, y);
		this.sliderBackground = sliderBackground;
		this.sliderButtonReleased = sliderButtonReleased;
		this.sliderButtonPressed = sliderButtonPressed;
		this.range = range;
		this.vertical = vertical;
	}

	@Override
	public BufferedImage getImage() {
		BufferedImage argbBackgroundImage = super.image;
		Graphics2D g2d = argbBackgroundImage.createGraphics();
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		g2d.drawImage(sliderBackground, 0, 0, null);
		int x = 0;
		int y = 0;
		if (vertical) {
			y += sliderPosition;
		} else {
			x += sliderPosition;
		}
		g2d.drawImage(pressed ? sliderButtonPressed : sliderButtonReleased, x, y, null);

		return argbBackgroundImage;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (this.pressed) {
			int offset;
			if (vertical) {
				offset = e.getY() - this.dragStartY;
			} else {
				offset = e.getX() - this.dragStartX;
			}
			int newSliderPos = dragStartSliderPos + parent.originalScaleCoord(offset);
			if (newSliderPos < 0) {
				newSliderPos = 0;
			} else if (newSliderPos > range) {
				newSliderPos = range;
			}
			sliderPosition = newSliderPos;
			parent.componentRepaint(this);
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		boolean wasPressed = this.pressed;
		int x = parent.originalScaleCoord(e.getX());
		int y = parent.originalScaleCoord(e.getY());
		if (x >= this.getX() && x <= this.getX() + this.getWidth() && y >= this.getY() && y <= this.getY() + this.getHeight()) {
			this.pressed = true;
			this.dragStartX = e.getX();
			this.dragStartY = e.getY();
			this.dragStartSliderPos = sliderPosition;
		}
		if (wasPressed != this.pressed) {
			parent.componentRepaint(this);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		boolean wasPressed = this.pressed;
		this.pressed = false;
		if (wasPressed != this.pressed) {
			parent.componentRepaint(this);
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public MouseListener getMouseListener() {
		return this;
	}

	@Override
	public MouseMotionListener getMouseMotionListener() {
		return this;
	}
}
