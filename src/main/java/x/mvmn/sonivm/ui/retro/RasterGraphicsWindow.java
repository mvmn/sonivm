package x.mvmn.sonivm.ui.retro;

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
import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.plaf.LayerUI;

import org.ini4j.Wini;

import x.mvmn.sonivm.ui.util.swing.ImageUtil;
import x.mvmn.sonivm.ui.util.swing.RectanglePointRange;

public class RasterGraphicsWindow extends JFrame {
	private static final long serialVersionUID = 6971200625034588294L;

	protected final BufferedImage backgroundImage;
	protected final BufferedImage transparencyMask;
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
	private CopyOnWriteArrayList<Runnable> moveListeners = new CopyOnWriteArrayList<>();
	private CopyOnWriteArrayList<Runnable> resizeListeners = new CopyOnWriteArrayList<>();

	public RasterGraphicsWindow(int width, int height, BufferedImage backgroundImage, BufferedImage transparencyMask,
			RectanglePointRange dragZone, RectanglePointRange resizeZone) throws Exception {
		this.setUndecorated(true);
		this.backgroundImage = backgroundImage;
		this.transparencyMask = transparencyMask;
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
			RectanglePointRange resizeZone) throws Exception {
		this(width, height, backgroundImage, null, dragZone, resizeZone);
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
					RasterGraphicsWindow.this.resizeListeners.stream().forEach(Runnable::run);
				} else if (RasterGraphicsWindow.this.isBeingMoved) {
					int deltaX = e.getXOnScreen() - RasterGraphicsWindow.this.dragFromX;
					int deltaY = e.getYOnScreen() - RasterGraphicsWindow.this.dragFromY;
					RasterGraphicsWindow.this.setLocation(RasterGraphicsWindow.this.initialLocationBeforeMove.x + deltaX,
							RasterGraphicsWindow.this.initialLocationBeforeMove.y + deltaY);
					RasterGraphicsWindow.this.moveListeners.stream().forEach(Runnable::run);
				}
			}
		});
	}

	public void addMoveListener(Runnable moveListener) {
		this.moveListeners.add(moveListener);
	}

	public void addResizeListener(Runnable resizeListener) {
		this.resizeListeners.add(resizeListener);
	}

	public static void main(String args[]) throws Exception {
		// String skin = "base-2.91";
		// String skin = "MetrixMetalDream";
		// String skin = "Bento_Classified";
		// String skin = "Necromech";
		// String skin = "Lime";

		BiFunction<File, String, File> getSkinFile = (skinFolder, skinFileName) -> Stream.of(skinFolder.listFiles())
				.filter(file -> file.getName().equalsIgnoreCase(skinFileName))
				.findAny()
				.orElse(null);

		Stream.of(new File("/Users/mvmn/Downloads/winamp_skins").listFiles())
				.filter(File::isDirectory)
				.filter(f -> f.getName().equalsIgnoreCase("Alien_Metalloid_Build_2-0_"))
				.forEach(skinFolder -> {
					try {
						System.out.println("Loading skin: " + skinFolder.getName());

						String mainWindowNumPoints = null;
						String mainWindowPointList = null;
						String eqWindowNumPoints = null;
						String eqWindowPointList = null;
						File regionFile = new File(skinFolder, "region.txt");
						if (regionFile.exists()) {
							Wini regionIni = new Wini(regionFile);
							mainWindowNumPoints = regionIni.get("Normal", "NumPoints", String.class);
							mainWindowPointList = regionIni.get("Normal", "PointList", String.class);

							eqWindowNumPoints = regionIni.get("Equalizer", "NumPoints", String.class);
							eqWindowPointList = regionIni.get("Equalizer", "PointList", String.class);
						}

						BufferedImage mainWindowBackgroundBmp = ImageIO.read(getSkinFile.apply(skinFolder, "MAIN.BMP"));
						BufferedImage argbBackgroundImage = new BufferedImage(275, 116, BufferedImage.TYPE_INT_ARGB);
						ImageUtil.drawOnto(argbBackgroundImage, mainWindowBackgroundBmp, 0, 0);

						BufferedImage posBar = ImageIO.read(getSkinFile.apply(skinFolder, "POSBAR.BMP"));
						BufferedImage handleReleased = posBar.getSubimage(248, 0, 29, posBar.getHeight());
						BufferedImage handlePressed = posBar.getSubimage(278, 0, 29, posBar.getHeight());

						BufferedImage buttonsBmp = ImageIO.read(getSkinFile.apply(skinFolder, "CBUTTONS.BMP"));

						BufferedImage mainWindowTransparencyMask = null;
						if (mainWindowNumPoints != null && mainWindowPointList != null) {
							mainWindowTransparencyMask = SkinUtil.createTransparencyMask(275, 116, mainWindowNumPoints,
									mainWindowPointList);
						}
						BufferedImage eqWindowTransparencyMask = null;
						if (eqWindowNumPoints != null && eqWindowPointList != null) {
							eqWindowTransparencyMask = SkinUtil.createTransparencyMask(275, 116, eqWindowNumPoints, eqWindowPointList);
						}

						RasterGraphicsWindow mainWin = new RasterGraphicsWindow(275, 116, argbBackgroundImage, mainWindowTransparencyMask,
								new RectanglePointRange(0, 0, 275, 16), new RectanglePointRange(260, 100, 275, 116));
						RasterUISlider slider = mainWin.addComponent(window -> new RasterUISlider(window,
								posBar.getSubimage(0, 0, 248, posBar.getHeight()), handleReleased, handlePressed, 16, 72, 219, 0, false));
						slider.addListener(() -> System.out.println("Seek: " + slider.getSliderPosition()));

						// 136x36, 5buttons 23x18, 1 button 23x16
						for (int i = 0; i < 5; i++) {
							int x = i * 23;
							RasterUIButton btn = mainWin.addComponent(window -> new RasterUIButton(window,
									buttonsBmp.getSubimage(x, 0, 22, 18), buttonsBmp.getSubimage(x, 18, 22, 18), 16 + x, 88));
							final int btnNum = i + 1;
							btn.addListener(() -> System.out.println("Pressed " + btnNum));
						}
						RasterUIButton btn = mainWin.addComponent(window -> new RasterUIButton(window,
								buttonsBmp.getSubimage(114, 0, 22, 16), buttonsBmp.getSubimage(114, 16, 22, 16), 136, 89));
						btn.addListener(() -> System.out.println("Pressed eject"));

						File balanceFile = getSkinFile.apply(skinFolder, "BALANCE.BMP");
						BufferedImage balanceSliderBmp;
						if (balanceFile != null && balanceFile.exists()) {
							balanceSliderBmp = ImageIO.read(balanceFile);
						} else {
							balanceSliderBmp = new BufferedImage(47, 418, BufferedImage.TYPE_INT_ARGB);
						}
						BufferedImage[] balanceSliderBackgrounds = new BufferedImage[28];
						for (int i = 0; i < balanceSliderBackgrounds.length; i++) {
							balanceSliderBackgrounds[i] = new BufferedImage(38, 13, BufferedImage.TYPE_INT_ARGB);
							ImageUtil.drawOnto(balanceSliderBackgrounds[i], balanceSliderBmp.getSubimage(9, i * 15, 38, 13), 0, 0);
						}
						BufferedImage balanceReleased;
						BufferedImage balancePressed;
						if (balanceSliderBmp.getHeight() - 422 > 0) {
							balanceReleased = balanceSliderBmp.getSubimage(15, 422, 14, balanceSliderBmp.getHeight() - 422);
							balancePressed = balanceSliderBmp.getSubimage(0, 422, 14, balanceSliderBmp.getHeight() - 422);
						} else {
							balanceReleased = new BufferedImage(14, 11, BufferedImage.TYPE_INT_ARGB);
							balancePressed = new BufferedImage(14, 11, BufferedImage.TYPE_INT_ARGB);
						}

						ExtRasterUISlider balanceSlider = mainWin.addComponent(window -> new ExtRasterUISlider(window,
								balanceSliderBackgrounds, balanceReleased, balancePressed, 177, 57, 38 - 14, (38 - 14) / 2, false,
								val -> (int) Math.round(Math.abs((38 - 14) / 2 - val) * 28 / 12.0d), 0, 1));
						balanceSlider.addListener(() -> System.out.println("Balance: " + balanceSlider.getSliderPosition()));

						BufferedImage volumeSliderBmp = ImageIO.read(getSkinFile.apply(skinFolder, "VOLUME.BMP"));
						BufferedImage[] volumeSliderBackgrounds = new BufferedImage[28];
						for (int i = 0; i < balanceSliderBackgrounds.length; i++) {
							volumeSliderBackgrounds[i] = new BufferedImage(68, 13, BufferedImage.TYPE_INT_ARGB);
							ImageUtil.drawOnto(volumeSliderBackgrounds[i], volumeSliderBmp.getSubimage(0, i * 15, 68, 13), 0, 0);
						}

						BufferedImage volumeReleased;
						BufferedImage volumePressed;
						if (volumeSliderBmp.getHeight() - 422 > 0) {
							volumeReleased = volumeSliderBmp.getSubimage(15, 422, 14, volumeSliderBmp.getHeight() - 422);
							volumePressed = volumeSliderBmp.getSubimage(0, 422, 14, volumeSliderBmp.getHeight() - 422);
						} else {
							volumeReleased = new BufferedImage(14, 11, BufferedImage.TYPE_INT_ARGB);
							volumePressed = new BufferedImage(14, 11, BufferedImage.TYPE_INT_ARGB);
						}

						ExtRasterUISlider volumeSlider = mainWin.addComponent(
								window -> new ExtRasterUISlider(window, volumeSliderBackgrounds, volumeReleased, volumePressed, 107, 57,
										68 - 14, 68 - 14, false, val -> (int) Math.round(val * 28.0d / 54.0d), 0, 1));
						volumeSlider.addListener(() -> System.out.println("Volume: " + volumeSlider.getSliderPosition()));

						mainWin.setLocation(100, 100);
						mainWin.setBackground(new Color(0, 0, 0, 0));
						mainWin.setVisible(true);

						BufferedImage eqWindowBackgroundBmp = ImageIO.read(getSkinFile.apply(skinFolder, "EQMAIN.BMP"));
						BufferedImage eqArgbBackgroundImage = new BufferedImage(275, 116, BufferedImage.TYPE_INT_ARGB);
						ImageUtil.drawOnto(eqArgbBackgroundImage, eqWindowBackgroundBmp.getSubimage(0, 0, 275, 116), 0, 0);

						RasterGraphicsWindow eqWin = new RasterGraphicsWindow(275, 116, eqArgbBackgroundImage, eqWindowTransparencyMask,
								new RectanglePointRange(0, 0, 275, 16), null);
						eqWin.setLocation(100, 216);
						eqWin.setBackground(new Color(0, 0, 0, 0));
						eqWin.setVisible(true);

						mainWin.addResizeListener(() -> eqWin.setSize(mainWin.getSize()));
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
	}
}
