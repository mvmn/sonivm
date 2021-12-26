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
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.plaf.LayerUI;

import x.mvmn.sonivm.ui.util.swing.ImageUtil;
import x.mvmn.sonivm.ui.util.swing.RectanglePointRange;
import x.mvmn.sonivm.util.Tuple2;

public class RasterGraphicsWindow extends JFrame {
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
	protected CopyOnWriteArrayList<Consumer<Tuple2<Point, Point>>> moveListeners = new CopyOnWriteArrayList<>();
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
		this.backgroundImage = backgroundImage;
		this.transparencyMask = transparencyMask;
		this.initialWidth = width;
		this.initialHeight = height;
		this.moveZone = moveZone;
		this.resizeZone = resizeZone;
		this.closeZone = closeZone;

		this.setSize(width, height);
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
					int newWidth = (int) Math.round(componentImage.getWidth() * scale);
					int newX = (int) Math.round(x * scale);
					int newY = (int) Math.round(y * scale);
					g2.drawImage(componentImage.getScaledInstance(newWidth, -1, Image.SCALE_SMOOTH), newX, newY, null);
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

	public double getScaleFactor() {
		return getWidth() / (double) initialWidth;
	}

	protected int newScaleCoord(double scale, int coord) {
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
		System.out.println("Init move/resize");
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
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
		this.setLocation(to);
		// If movement was limited by macOS title bar then the actual location of the window
		// might differ from the one set via setLocation
		Point finalTo = this.getLocation();
		this.moveListeners.stream().forEach(listener -> listener.accept(Tuple2.<Point, Point> builder().a(from).b(finalTo).build()));
	}

	public void addMoveListener(Consumer<Tuple2<Point, Point>> moveListener) {
		this.moveListeners.add(moveListener);
	}

	public void addResizeListener(Runnable resizeListener) {
		this.resizeListeners.add(resizeListener);
	}

	public void addCloseListener(Runnable closeListener) {
		this.closeListeners.add(closeListener);
	}
}
