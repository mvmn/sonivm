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
	protected final RectanglePointRange dragZone;
	protected final RectanglePointRange resizeZone;
	protected final RectanglePointRange closeZone;

	protected final JLayer<JPanel> componentsLayer;

	private volatile boolean isBeingClosed = true;
	private volatile boolean isBeingResized = false;
	private volatile boolean isBeingMoved = false;
	private volatile int dragFromX = 0;
	private volatile int dragFromY = 0;
	private volatile Point initialLocationBeforeMove = null;
	private volatile int resizedFromWidth = 0;

	protected CopyOnWriteArrayList<RasterUIComponent> components = new CopyOnWriteArrayList<>();
	protected CopyOnWriteArrayList<Consumer<Tuple2<Point, Point>>> moveListeners = new CopyOnWriteArrayList<>();
	protected CopyOnWriteArrayList<Runnable> resizeListeners = new CopyOnWriteArrayList<>();
	protected CopyOnWriteArrayList<Runnable> closeListeners = new CopyOnWriteArrayList<>();

	public RasterGraphicsWindow(int width, int height, BufferedImage backgroundImage, BufferedImage transparencyMask,
			RectanglePointRange dragZone, RectanglePointRange resizeZone, RectanglePointRange closeZone) {
		this.setUndecorated(true);
		super.setBackground(new Color(0, 0, 0, 0));
		this.backgroundImage = backgroundImage;
		this.transparencyMask = transparencyMask;
		this.initialWidth = width;
		this.initialHeight = height;
		this.dragZone = dragZone;
		this.resizeZone = resizeZone;
		this.closeZone = closeZone;

		this.setSize(width, height);
		this.setMinimumSize(new Dimension(width, height));
		this.getContentPane().setLayout(new BorderLayout());

		JPanel imgPanel = new JPanel() {
			private static final long serialVersionUID = -7619624446072478858L;

			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
				if (RasterGraphicsWindow.this.getWidth() == initialWidth) {
					g2.drawImage(backgroundImage, 0, 0, null);
				} else {
					g2.drawImage(backgroundImage.getScaledInstance(RasterGraphicsWindow.this.getWidth(), -1, Image.SCALE_SMOOTH), 0, 0,
							null);
				}
				applyTransparencyMask(g2);
			}
		};
		LayerUI<JPanel> layerUI = new LayerUI<JPanel>() {
			private static final long serialVersionUID = 1L;

			@Override
			public void paint(Graphics g, JComponent c) {
				super.paint(g, c);

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
							double scale = RasterGraphicsWindow.this.getWidth() / (double) initialWidth;
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
		};

		componentsLayer = new JLayer<JPanel>(imgPanel, layerUI);

		this.getContentPane().add(componentsLayer, BorderLayout.CENTER);
		this.setResizable(false);
		this.initMoveResize();
	}

	protected void applyTransparencyMask(Graphics2D g2) {
		if (RasterGraphicsWindow.this.getWidth() == initialWidth) {
			if (transparencyMask != null) {
				ImageUtil.applyTransparency(g2, transparencyMask);
			}
		} else {
			if (transparencyMask != null) {
				ImageUtil.applyTransparency(g2,
						transparencyMask.getScaledInstance(RasterGraphicsWindow.this.getWidth(), -1, Image.SCALE_SMOOTH));
			}
		}
	}

	public RasterGraphicsWindow(int width, int height, BufferedImage backgroundImage, RectanglePointRange dragZone,
			RectanglePointRange resizeZone, RectanglePointRange closeZone) {
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

	protected double getScaleFactor() {
		return RasterGraphicsWindow.this.getWidth() / (double) RasterGraphicsWindow.this.initialWidth;
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
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				RasterGraphicsWindow.this.isBeingResized = false;
				RasterGraphicsWindow.this.isBeingMoved = false;
				if (isBeingClosed) {
					int x = originalScaleCoord(e.getX());
					int y = originalScaleCoord(e.getY());
					if (closeZone != null && closeZone.inRange(x, y)) {
						RasterGraphicsWindow.this.handleClose();
					}
				}
				RasterGraphicsWindow.this.isBeingClosed = false;
			}

			@Override
			public void mousePressed(MouseEvent e) {
				int x = originalScaleCoord(e.getX());
				int y = originalScaleCoord(e.getY());
				if (closeZone != null && closeZone.inRange(x, y)) {
					RasterGraphicsWindow.this.isBeingClosed = true;
				} else if (resizeZone != null && resizeZone.inRange(x, y)) {
					RasterGraphicsWindow.this.isBeingResized = true;
					RasterGraphicsWindow.this.dragFromX = e.getXOnScreen();
					RasterGraphicsWindow.this.resizedFromWidth = RasterGraphicsWindow.this.getWidth();
				} else if (dragZone != null && dragZone.inRange(x, y)) {
					RasterGraphicsWindow.this.isBeingMoved = true;
					RasterGraphicsWindow.this.dragFromX = e.getXOnScreen();
					RasterGraphicsWindow.this.dragFromY = e.getYOnScreen();
					RasterGraphicsWindow.this.initialLocationBeforeMove = RasterGraphicsWindow.this.getLocation();
				}
			}
		});
		this.addMouseMotionListener(new MouseMotionAdapter() {

			@Override
			public void mouseDragged(MouseEvent e) {
				if (RasterGraphicsWindow.this.isBeingResized) {
					int currentWidth = RasterGraphicsWindow.this.resizedFromWidth;
					int additionalPixels = e.getXOnScreen() - RasterGraphicsWindow.this.dragFromX;

					int newWidth = currentWidth + additionalPixels;
					if (newWidth < initialWidth) {
						newWidth = initialWidth;
					}
					int newHeight = (int) Math.round(initialHeight * newWidth / initialWidth);
					RasterGraphicsWindow.this.setSize(new Dimension(newWidth, newHeight));
					RasterGraphicsWindow.this.resizeListeners.stream().forEach(Runnable::run);
				} else if (RasterGraphicsWindow.this.isBeingMoved) {
					int deltaX = e.getXOnScreen() - RasterGraphicsWindow.this.dragFromX;
					int deltaY = e.getYOnScreen() - RasterGraphicsWindow.this.dragFromY;
					Point from = RasterGraphicsWindow.this.getLocation();
					Point to = new Point(RasterGraphicsWindow.this.initialLocationBeforeMove.x + deltaX,
							RasterGraphicsWindow.this.initialLocationBeforeMove.y + deltaY);
					RasterGraphicsWindow.this.setLocation(to);
					Point finalTo = RasterGraphicsWindow.this.getLocation(); // If movement was limited by title bar the actual location might differ from
																				// desired one
					RasterGraphicsWindow.this.moveListeners.stream()
							.forEach(listener -> listener.accept(Tuple2.<Point, Point> builder().a(from).b(finalTo).build()));
				}
			}
		});
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
