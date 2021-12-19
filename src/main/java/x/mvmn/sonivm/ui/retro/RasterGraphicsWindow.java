package x.mvmn.sonivm.ui.retro;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
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
import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.plaf.LayerUI;

import x.mvmn.sonivm.ui.util.swing.ImageUtil;
import x.mvmn.sonivm.ui.util.swing.RectanglePointRange;

public class RasterGraphicsWindow extends JFrame {
	private static final long serialVersionUID = 6971200625034588294L;

	protected final BufferedImage backgroundImage;
	protected final int initialWidth;
	protected final int initialHeight;
	protected final RectanglePointRange dragZone;
	protected final RectanglePointRange resizeZone;

	protected final JLayer<JPanel> componentsLayer;

	private volatile boolean isBeingResized = false;
	private volatile boolean isBeingMoved = false;
	private volatile int dragFromX = 0;
	private volatile int dragFromY = 0;
	private volatile Point initialLocationBeforeMove = null;
	private volatile int resizedFromWidth = 0;

	private CopyOnWriteArrayList<RasterUIComponent> components = new CopyOnWriteArrayList<>();

	public RasterGraphicsWindow(int width, int height, BufferedImage backgroundImage, RectanglePointRange dragZone,
			RectanglePointRange resizeZone) throws Exception {
		this.setUndecorated(true);
		this.backgroundImage = backgroundImage;
		this.initialWidth = width;
		this.initialHeight = height;
		this.dragZone = dragZone;
		this.resizeZone = resizeZone;

		this.setSize(width, height);
		this.setMinimumSize(new Dimension(width, height));
		this.getContentPane().setLayout(new BorderLayout());

		JPanel imgPanel = new JPanel() {
			private static final long serialVersionUID = -7619624446072478858L;

			@Override
			protected void paintComponent(Graphics g) {
				if (RasterGraphicsWindow.this.getWidth() == initialWidth) {
					g.drawImage(backgroundImage, 0, 0, null);
				} else {
					g.drawImage(backgroundImage.getScaledInstance(RasterGraphicsWindow.this.getWidth(), -1, Image.SCALE_SMOOTH), 0, 0,
							null);
				}
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

					g2.dispose();
				}
			}
		};

		componentsLayer = new JLayer<JPanel>(imgPanel, layerUI);

		this.getContentPane().add(componentsLayer, BorderLayout.CENTER);
		this.setResizable(false);
		this.initMoveResize();
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

	protected void initMoveResize() {
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				RasterGraphicsWindow.this.isBeingResized = false;
				RasterGraphicsWindow.this.isBeingMoved = false;
			}

			@Override
			public void mousePressed(MouseEvent e) {
				int x = originalScaleCoord(e.getX());
				int y = originalScaleCoord(e.getY());
				if (resizeZone != null && resizeZone.inRange(x, y)) {
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
				} else if (RasterGraphicsWindow.this.isBeingMoved) {
					int deltaX = e.getXOnScreen() - RasterGraphicsWindow.this.dragFromX;
					int deltaY = e.getYOnScreen() - RasterGraphicsWindow.this.dragFromY;
					RasterGraphicsWindow.this.setLocation(RasterGraphicsWindow.this.initialLocationBeforeMove.x + deltaX,
							RasterGraphicsWindow.this.initialLocationBeforeMove.y + deltaY);
				}
			}
		});
	}

	public static void main(String args[]) throws Exception {
		String skin = "MetrixMetalDream";
		// String skin = "Bento_Classified";
		BufferedImage mainWindowBackgroundBmp = ImageIO.read(new File("/Users/mvmn/Downloads/winamp_skins/" + skin + "/MAIN.BMP"));

		BufferedImage argbBackgroundImage = new BufferedImage(275, 116, BufferedImage.TYPE_INT_ARGB);
		ImageUtil.drawOnto(argbBackgroundImage, mainWindowBackgroundBmp, 0, 0);

		BufferedImage posBar = ImageIO.read(new File("/Users/mvmn/Downloads/winamp_skins/" + skin + "/POSBAR.BMP"));
		BufferedImage handleReleased = posBar.getSubimage(248, 0, 29, 10);
		BufferedImage handlePressed = posBar.getSubimage(278, 0, 29, 10);

		BufferedImage buttonsBmp = ImageIO.read(new File("/Users/mvmn/Downloads/winamp_skins/" + skin + "/CBUTTONS.BMP"));

		RasterGraphicsWindow w = new RasterGraphicsWindow(275, 116, argbBackgroundImage, new RectanglePointRange(0, 0, 275, 16),
				new RectanglePointRange(260, 100, 275, 116));
		RasterUISlider slider = w.addComponent(
				window -> new RasterUISlider(window, posBar.getSubimage(0, 0, 248, 10), handleReleased, handlePressed, 16, 72, 219, false));
		slider.addListener(() -> System.out.println(slider.getSliderPosition()));

		// 136x36, 23x18, 23x16
		for (int i = 0; i < 5; i++) {
			int x = i * 23;
			RasterUIButton btn = w.addComponent(window -> new RasterUIButton(window, buttonsBmp.getSubimage(x, 0, 22, 18),
					buttonsBmp.getSubimage(x, 18, 22, 18), 16 + x, 88));
			final int btnNum = i + 1;
			btn.addListener(() -> System.out.println("Pressed " + btnNum));
		}

		w.setLocation(100, 100);
		w.setVisible(true);
	}
}
