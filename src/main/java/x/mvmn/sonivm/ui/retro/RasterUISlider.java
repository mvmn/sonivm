package x.mvmn.sonivm.ui.retro;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.CopyOnWriteArrayList;

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

	protected final CopyOnWriteArrayList<Runnable> listerens = new CopyOnWriteArrayList<>();

	public RasterUISlider(RasterGraphicsWindow parent, BufferedImage sliderBackground, BufferedImage sliderButtonReleased,
			BufferedImage sliderButtonPressed, int x, int y, int range, boolean vertical) {
		super(parent, new BufferedImage(sliderBackground.getWidth(), sliderBackground.getHeight(), BufferedImage.TYPE_INT_ARGB), x, y);
		this.sliderBackground = sliderBackground;
		this.sliderButtonReleased = sliderButtonReleased;
		this.sliderButtonPressed = sliderButtonPressed;
		this.range = range;
		this.vertical = vertical;
		repaint();
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
			int newSliderPos = this.dragStartSliderPos + this.parent.originalScaleCoord(offset);
			updateSliderPosition(newSliderPos);
		}
	}

	protected void updateSliderPosition(int newSliderPos) {
		int oldSliderPos = this.sliderPosition;
		if (newSliderPos < 0) {
			newSliderPos = 0;
		} else if (newSliderPos > range) {
			newSliderPos = range;
		}
		this.sliderPosition = newSliderPos;
		if (oldSliderPos != this.sliderPosition) {
			repaint();
			listerens.forEach(Runnable::run);
		}
	}

	@Override
	public void repaint() {
		BufferedImage argbBackgroundImage = super.image;
		Graphics2D g2d = argbBackgroundImage.createGraphics();
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		g2d.drawImage(sliderBackground, 0, 0, null);
		int x = 0;
		int y = 0;
		if (vertical) {
			y += this.sliderPosition;
		} else {
			x += this.sliderPosition;
		}
		g2d.drawImage(pressed ? sliderButtonPressed : sliderButtonReleased, x, y, null);
		super.repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		boolean wasPressed = this.pressed;
		if (inComponentRange(e.getX(), e.getY())) {
			if (inButtonRange(e.getX(), e.getY())) {
				this.pressed = true;
				this.dragStartX = e.getX();
				this.dragStartY = e.getY();
				this.dragStartSliderPos = this.sliderPosition;
			} else {
				updateSliderPosition(parent.originalScaleCoord(vertical ? e.getY() : e.getX()) - (vertical ? this.getY() : this.getX())
						- (vertical ? sliderButtonReleased.getHeight() : sliderButtonReleased.getWidth()) / 2);
			}
		}
		if (wasPressed != this.pressed) {
			repaint();
		}
	}

	private boolean inButtonRange(int parentX, int parentY) {
		int x = parent.originalScaleCoord(parentX) - this.getX();
		int y = parent.originalScaleCoord(parentY) - this.getY();

		int btnx = vertical ? 0 : sliderPosition;
		int btny = vertical ? sliderPosition : 0;

		return x >= btnx && x <= btnx + sliderButtonReleased.getWidth() && y >= btny && y <= btny + sliderButtonReleased.getHeight();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		boolean wasPressed = this.pressed;
		this.pressed = false;
		if (wasPressed != this.pressed) {
			repaint();
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

	public void addListener(Runnable onClick) {
		listerens.add(onClick);
	}

	public int getSliderPosition() {
		return sliderPosition;
	}

	public void setSliderPosition(int sliderPosition) {
		updateSliderPosition(sliderPosition);
	}
}
