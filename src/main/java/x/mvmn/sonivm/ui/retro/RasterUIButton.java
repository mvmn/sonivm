package x.mvmn.sonivm.ui.retro;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.CopyOnWriteArrayList;

import x.mvmn.sonivm.ui.util.swing.ImageUtil;

public class RasterUIButton extends RasterUIComponent implements MouseListener {

	protected final BufferedImage imageReleased;
	protected final BufferedImage imagePressed;
	protected volatile boolean pressed;

	protected final CopyOnWriteArrayList<Runnable> listerens = new CopyOnWriteArrayList<>();

	public RasterUIButton(RasterGraphicsWindow parent, BufferedImage imageReleased, BufferedImage imagePressed, int x, int y) {
		super(parent, new BufferedImage(imageReleased.getWidth(), imageReleased.getHeight(), BufferedImage.TYPE_INT_ARGB), x, y);
		this.imageReleased = imageReleased;
		this.imagePressed = imagePressed;

		repaint();
	}

	@Override
	public void repaint() {
		ImageUtil.drawOnto(super.image, pressed ? imagePressed : imageReleased, 0, 0);
		super.repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		boolean wasPressed = this.pressed;
		if (inComponentRange(e.getX(), e.getY())) {
			this.pressed = true;
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
			repaint();
			listerens.forEach(Runnable::run);
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

	public void addListener(Runnable onClick) {
		listerens.add(onClick);
	}
}
