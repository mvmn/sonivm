package x.mvmn.sonivm.ui.retro;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

public class RasterUIComponent {

	protected final BufferedImage image;
	protected final RasterGraphicsWindow parent;
	protected volatile int x = 0;
	protected volatile int y = 0;

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

	public MouseListener getMouseListener() {
		return null;
	}

	public MouseMotionListener getMouseMotionListener() {
		return null;
	}

	protected boolean inComponentRange(int parentX, int parentY) {
		int x = parent.originalScaleCoord(parentX);
		int y = parent.originalScaleCoord(parentY);
		return x >= this.getX() && x <= this.getX() + this.getWidth() && y >= this.getY() && y <= this.getY() + this.getHeight();
	}

	public void repaint() {
		parent.repaintChildComponent(this);
	}
}
