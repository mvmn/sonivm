package x.mvmn.sonivm.ui.retro.rasterui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.plaf.LayerUI;

import lombok.Setter;
import x.mvmn.sonivm.ui.util.swing.ImageUtil;
import x.mvmn.sonivm.ui.util.swing.RectLocationAndSize;
import x.mvmn.sonivm.ui.util.swing.RectanglePointRange;

public class RasterGraphicsWindow extends JFrame implements RectLocationAndSize {
	private static final long serialVersionUID = 6971200625034588294L;

	protected final BufferedImage backgroundImage;
	protected final BufferedImage transparencyMask;
	protected final int initialWidth;
	protected final int initialHeight;
	protected final RectanglePointRange moveZone;
	protected final RectanglePointRange resizeZone;
	protected final RectanglePointRange closeZone;

	protected final JPanel backgroundPanel;
	protected final LayerUI<JPanel> layerUI;
	protected final JLayer<JPanel> componentsLayer;

	protected volatile boolean isBeingClosed = true;
	protected volatile boolean isBeingResized = false;
	protected volatile boolean isBeingMoved = false;
	protected volatile int dragFromX = 0;
	protected volatile int dragFromY = 0;
	protected volatile Point initialLocationBeforeMove = null;
	protected volatile int resizedFromWidth = 0;

	protected CopyOnWriteArrayList<RasterUIComponent> components = new CopyOnWriteArrayList<>();
	protected CopyOnWriteArrayList<BiConsumer<Point, Point>> moveListeners = new CopyOnWriteArrayList<>();
	protected CopyOnWriteArrayList<Consumer<Point>> postMoveListeners = new CopyOnWriteArrayList<>();
	@Setter
	protected volatile BiFunction<Point, Point, Point> moveAdjuster;
	protected CopyOnWriteArrayList<Runnable> resizeListeners = new CopyOnWriteArrayList<>();
	protected CopyOnWriteArrayList<Runnable> closeListeners = new CopyOnWriteArrayList<>();

	public RasterGraphicsWindow(int width,
			int height,
			BufferedImage backgroundImage,
			BufferedImage transparencyMask,
			RectanglePointRange moveZone,
			RectanglePointRange resizeZone,
			RectanglePointRange closeZone) {
		this.setUndecorated(true);
		super.setBackground(new Color(0, 0, 0, 0));
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.backgroundImage = backgroundImage;
		this.transparencyMask = transparencyMask;
		this.initialWidth = width;
		this.initialHeight = height;
		this.moveZone = moveZone;
		this.resizeZone = resizeZone;
		this.closeZone = closeZone;

		super.setSize(width, height);
		this.setMinimumSize(new Dimension(width, height));
		this.getContentPane().setLayout(new BorderLayout());

		this.backgroundPanel = new JPanel() {
			private static final long serialVersionUID = -7619624446072478858L;

			@Override
			protected void paintComponent(Graphics g) {
				paintBackgroundPanel(g);
			}
		};
		this.layerUI = new LayerUI<JPanel>() {
			private static final long serialVersionUID = 1L;

			@Override
			public void paint(Graphics g, JComponent c) {
				paintLayerUI(g, c);
			}
		};

		componentsLayer = new JLayer<JPanel>(backgroundPanel, layerUI);

		this.getContentPane().add(componentsLayer, BorderLayout.CENTER);
		this.setResizable(false);
		this.initMoveResize();
	}

	protected void paintBackgroundPanel(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		if (RasterGraphicsWindow.this.getWidth() == initialWidth) {
			g2.drawImage(backgroundImage, 0, 0, null);
		} else {
			g2.drawImage(backgroundImage.getScaledInstance(RasterGraphicsWindow.this.getWidth(), -1, Image.SCALE_SMOOTH), 0, 0, null);
		}
		applyTransparencyMask(g2);
	}

	protected void paintLayerUI(Graphics g, JComponent c) {
		c.paint(g);

		if (!components.isEmpty()) {
			Graphics2D g2 = (Graphics2D) g.create();

			for (RasterUIComponent uiComponent : components) {
				BufferedImage componentImage = uiComponent.getImage();
				int x = uiComponent.getX();
				int y = uiComponent.getY();
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
				if (RasterGraphicsWindow.this.getWidth() == initialWidth) {
					g2.drawImage(componentImage, x, y, null);
				} else {
					double scale = getScaleFactor();
					int newX = (int) Math.round(x * scale);
					int newY = (int) Math.round(y * scale);
					if (componentImage.getWidth() > componentImage.getHeight()) {
						int newWidth = (int) Math.round(componentImage.getWidth() * scale);
						g2.drawImage(componentImage.getScaledInstance(newWidth, -1, Image.SCALE_SMOOTH), newX, newY, null);
					} else {
						int newHeight = (int) Math.round(componentImage.getHeight() * scale);
						g2.drawImage(componentImage.getScaledInstance(-1, newHeight, Image.SCALE_SMOOTH), newX, newY, null);
					}
				}
			}

			applyTransparencyMask(g2);
			g2.dispose();
		}
	}

	protected void applyTransparencyMask(Graphics2D g2) {
		if (transparencyMask != null) {
			if (RasterGraphicsWindow.this.getWidth() == initialWidth) {
				ImageUtil.applyTransparency(g2, transparencyMask);
			} else {
				ImageUtil.applyTransparency(g2,
						transparencyMask.getScaledInstance(RasterGraphicsWindow.this.getWidth(), -1, Image.SCALE_SMOOTH));
			}
		}
	}

	public RasterGraphicsWindow(int width,
			int height,
			BufferedImage backgroundImage,
			RectanglePointRange dragZone,
			RectanglePointRange resizeZone,
			RectanglePointRange closeZone) {
		this(width, height, backgroundImage, null, dragZone, resizeZone, closeZone);
	}

	public void repaintChildComponent(RasterUIComponent component) {
		double scale = getScaleFactor();
		componentsLayer.repaint(newScaleCoord(scale, component.getX()), newScaleCoord(scale, component.getY()),
				newScaleCoord(scale, component.getWidth()), newScaleCoord(scale, component.getHeight()));
	}

	public <T extends RasterUIComponent> T addComponent(Function<RasterGraphicsWindow, T> componentCreator) {
		T component = componentCreator.apply(this);
		components.add(component);
		MouseListener mouseListener = component.getMouseListener();
		MouseMotionListener mouseMotionListener = component.getMouseMotionListener();
		if (mouseListener != null) {
			this.addMouseListener(mouseListener);
		}
		if (mouseMotionListener != null) {
			this.addMouseMotionListener(mouseMotionListener);
		}
		return component;
	}

	public void removeComponent(RasterUIComponent component) {
		components.remove(component);
		MouseListener mouseListener = component.getMouseListener();
		MouseMotionListener mouseMotionListener = component.getMouseMotionListener();
		if (mouseListener != null) {
			this.removeMouseListener(mouseListener);
		}
		if (mouseMotionListener != null) {
			this.removeMouseMotionListener(mouseMotionListener);
		}
	}

	public double getScaleFactor() {
		return getWidth() / (double) initialWidth;
	}

	public int newScaleCoord(double scale, int coord) {
		return (int) Math.round(coord * scale);
	}

	public int originalScaleCoord(int coord) {
		double scale = getScaleFactor();
		return (int) Math.round(coord / scale);
	}

	protected void handleClose() {
		if (this.isVisible()) {
			this.setVisible(false);
			closeListeners.forEach(Runnable::run);
		}
	}

	protected void initMoveResize() {
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				boolean wasMoved = isBeingMoved;
				RasterGraphicsWindow.this.isBeingResized = false;
				RasterGraphicsWindow.this.isBeingMoved = false;
				if (isBeingClosed) {
					int x = originalScaleCoord(e.getX());
					int y = originalScaleCoord(e.getY());
					if (inCloseZone(x, y)) {
						RasterGraphicsWindow.this.handleClose();
					}
				}
				RasterGraphicsWindow.this.isBeingClosed = false;
				if (wasMoved) {
					postMoveListeners.forEach(it -> it.accept(e.getLocationOnScreen()));
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				int x = originalScaleCoord(e.getX());
				int y = originalScaleCoord(e.getY());
				if (inCloseZone(x, y)) {
					RasterGraphicsWindow.this.isBeingClosed = true;
				} else if (inResizeZone(x, y)) {
					onResizeStart(e);
				} else if (inMoveZone(x, y)) {
					onMoveStart(e);
				}
			}
		});
		this.addMouseMotionListener(new MouseMotionAdapter() {

			@Override
			public void mouseDragged(MouseEvent e) {
				if (RasterGraphicsWindow.this.isBeingResized) {
					onResize(e);
				} else if (RasterGraphicsWindow.this.isBeingMoved) {
					onMove(e);
				}
			}
		});
	}

	protected void onResizeStart(MouseEvent e) {
		this.isBeingResized = true;
		this.dragFromX = e.getXOnScreen();
		this.dragFromY = e.getYOnScreen();
		this.resizedFromWidth = RasterGraphicsWindow.this.getWidth();
	}

	protected void onMoveStart(MouseEvent e) {
		this.isBeingMoved = true;
		this.dragFromX = e.getXOnScreen();
		this.dragFromY = e.getYOnScreen();
		this.initialLocationBeforeMove = RasterGraphicsWindow.this.getLocation();
	}

	protected boolean inCloseZone(int x, int y) {
		return closeZone != null && closeZone.inRange(x, y);
	}

	protected boolean inResizeZone(int x, int y) {
		return resizeZone != null && resizeZone.inRange(x, y);
	}

	protected boolean inMoveZone(int x, int y) {
		return moveZone != null && moveZone.inRange(x, y);
	}

	protected void onResize(MouseEvent e) {
		int currentWidth = this.resizedFromWidth;
		int additionalPixels = e.getXOnScreen() - this.dragFromX;

		int newWidth = currentWidth + additionalPixels;
		if (newWidth < initialWidth) {
			newWidth = initialWidth;
		}
		int newHeight = (int) Math.round(initialHeight * newWidth / initialWidth);
		this.setSize(new Dimension(newWidth, newHeight));
		this.resizeListeners.stream().forEach(Runnable::run);
	}

	protected void onMove(MouseEvent e) {
		int deltaX = e.getXOnScreen() - this.dragFromX;
		int deltaY = e.getYOnScreen() - this.dragFromY;
		Point from = this.getLocation();
		Point to = new Point(this.initialLocationBeforeMove.x + deltaX, this.initialLocationBeforeMove.y + deltaY);
		if (moveAdjuster != null) {
			to = moveAdjuster.apply(from, to);
		}

		this.setLocation(to);
		// If movement was limited by macOS title bar then the actual location of the window
		// might differ from the one set via setLocation
		Point finalTo = this.getLocation();
		this.moveListeners.stream().forEach(listener -> listener.accept(from, finalTo));
	}

	public void addMoveListener(BiConsumer<Point, Point> moveListener) {
		this.moveListeners.add(moveListener);
	}

	public void addPostMoveListener(Consumer<Point> postMoveListener) {
		this.postMoveListeners.add(postMoveListener);
	}

	public void addResizeListener(Runnable resizeListener) {
		this.resizeListeners.add(resizeListener);
	}

	public void addCloseListener(Runnable closeListener) {
		this.closeListeners.add(closeListener);
	}

	@Override
	public int getLeft() {
		return this.getLocation().x;
	}

	@Override
	public int getTop() {
		return this.getLocation().y;
	}

	@Override
	public int getRight() {
		return getLeft() + getWidth();
	}

	@Override
	public int getBottom() {
		return getTop() + getHeight();
	}
}
