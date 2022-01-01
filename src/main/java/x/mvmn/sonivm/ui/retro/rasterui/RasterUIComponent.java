package x.mvmn.sonivm.ui.retro.rasterui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.CopyOnWriteArrayList;

public class RasterUIComponent implements MouseListener, MouseMotionListener {

	protected final BufferedImage image;
	protected final RasterGraphicsWindow parent;
	protected volatile int x = 0;
	protected volatile int y = 0;
	protected volatile boolean pressed;
	protected volatile boolean mouseInRange;

	protected final CopyOnWriteArrayList<Runnable> listerens = new CopyOnWriteArrayList<>();

	public RasterUIComponent(RasterGraphicsWindow parent, BufferedImage image) {
		this.parent = parent;
		this.image = image;
	}

	public RasterUIComponent(RasterGraphicsWindow parent, BufferedImage image, int x, int y) {
		this(parent, image);
		this.x = x;
		this.y = y;
	}

	public BufferedImage getImage() {
		return image;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getWidth() {
		return image.getWidth();
	}

	public int getHeight() {
		return image.getHeight();
	}

	protected boolean inComponentRange(int parentX, int parentY) {
		int x = parent.originalScaleCoord(parentX);
		int y = parent.originalScaleCoord(parentY);
		return x >= this.getX() && x <= this.getX() + this.getWidth() && y >= this.getY() && y <= this.getY() + this.getHeight();
	}

	public void repaint() {
		parent.repaintChildComponent(this);
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		boolean wasPressed = this.pressed;
		if (inComponentRange(e.getX(), e.getY())) {
			this.pressed = true;
			this.mouseInRange = true;
		}
		if (this.pressed != wasPressed) {
			repaint();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		boolean wasPressed = this.pressed;
		this.pressed = false;
		if (this.pressed != wasPressed) {
			if (this.mouseInRange) {
				onPressBeforeRepaint();
				repaint();
				onPressAfterRepaint();
			}
		}
	}

	protected void onPressBeforeRepaint() {}

	protected void onPressAfterRepaint() {
		listerens.forEach(Runnable::run);
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	public MouseListener getMouseListener() {
		return this;
	}

	public MouseMotionListener getMouseMotionListener() {
		return this;
	}

	public void addListener(Runnable onClick) {
		listerens.add(onClick);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (this.pressed) {
			boolean wasInRange = this.mouseInRange;
			this.mouseInRange = inComponentRange(e.getX(), e.getY());
			if (wasInRange != this.mouseInRange) {
				repaint();
			}
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	public boolean isAutoScaled() {
		return false;
	}

	public void setScale(double scale) {}
}
