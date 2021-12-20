package x.mvmn.sonivm.ui.retro;

import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.ini4j.Wini;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import x.mvmn.sonivm.ui.retro.exception.WSZLoadingException;
import x.mvmn.sonivm.ui.retro.rasterui.RasterGraphicsWindow;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIButton;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIHeading;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUISlider;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIToggleButton;
import x.mvmn.sonivm.ui.util.swing.ImageUtil;
import x.mvmn.sonivm.ui.util.swing.RectanglePointRange;
import x.mvmn.sonivm.util.Tuple3;
import x.mvmn.sonivm.util.UnsafeFunction;

public class RetroUIFactory {

	public Tuple3<RetroUIMainWindow, RetroUIEqualizerWindow, Void> construct(File winAmpSkinZipFile) throws WSZLoadingException {
		Tuple3.Tuple3Builder<RetroUIMainWindow, RetroUIEqualizerWindow, Void> resultBuilder = Tuple3
				.<RetroUIMainWindow, RetroUIEqualizerWindow, Void> builder();

		ZipFile skinZip = new ZipFile(winAmpSkinZipFile);

		String mainWindowNumPoints = null;
		String mainWindowPointList = null;
		String eqWindowNumPoints = null;
		String eqWindowPointList = null;
		Wini regionIni = loadIniFile(skinZip, "region.txt");
		if (regionIni != null) {
			mainWindowNumPoints = regionIni.get("Normal", "NumPoints", String.class);
			mainWindowPointList = regionIni.get("Normal", "PointList", String.class);

			eqWindowNumPoints = regionIni.get("Equalizer", "NumPoints", String.class);
			eqWindowPointList = regionIni.get("Equalizer", "PointList", String.class);
		}

		BufferedImage mainWindowBackgroundBmp = loadImage(skinZip, "MAIN.BMP");
		BufferedImage argbBackgroundImage = new BufferedImage(275, 116, BufferedImage.TYPE_INT_ARGB);
		ImageUtil.drawOnto(argbBackgroundImage, mainWindowBackgroundBmp, 0, 0);

		BufferedImage posBar = loadImage(skinZip, "POSBAR.BMP");
		BufferedImage handleReleased = posBar.getSubimage(248, 0, 29, posBar.getHeight());
		BufferedImage handlePressed = posBar.getSubimage(278, 0, 29, posBar.getHeight());

		BufferedImage buttonsBmp = loadImage(skinZip, "CBUTTONS.BMP");

		BufferedImage mainWindowTransparencyMask = null;
		if (mainWindowNumPoints != null && mainWindowPointList != null) {
			mainWindowTransparencyMask = SkinUtil.createTransparencyMask(275, 116, mainWindowNumPoints, mainWindowPointList);
		}
		BufferedImage eqWindowTransparencyMask = null;
		if (eqWindowNumPoints != null && eqWindowPointList != null) {
			eqWindowTransparencyMask = SkinUtil.createTransparencyMask(275, 116, eqWindowNumPoints, eqWindowPointList);
		}

		RasterGraphicsWindow mainWin = new RasterGraphicsWindow(275, 116, argbBackgroundImage, mainWindowTransparencyMask,
				new RectanglePointRange(0, 0, 275, 16), new RectanglePointRange(260, 100, 275, 116));

		BufferedImage titleBarBmp = ImageUtil.convert(loadImage(skinZip, "titlebar.bmp"), BufferedImage.TYPE_INT_ARGB);
		RasterUIHeading titleBar = mainWin.addComponent(window -> new RasterUIHeading(window, titleBarBmp.getSubimage(27, 0, 275, 14),
				titleBarBmp.getSubimage(27, 15, 275, 14), 0, 0));

		RasterUISlider seekSlider = mainWin.addComponent(window -> new RasterUISlider(window,
				posBar.getSubimage(0, 0, 248, posBar.getHeight()), handleReleased, handlePressed, 16, 72, 219, 0, false));
		seekSlider.addListener(() -> System.out.println("Seek: " + seekSlider.getSliderPosition()));

		// 136x36, 5buttons 23x18, 1 button 23x16
		RasterUIButton[] controlButtons = new RasterUIButton[5];
		for (int i = 0; i < 5; i++) {
			int x = i * 23;
			RasterUIButton btn = mainWin.addComponent(window -> new RasterUIButton(window, buttonsBmp.getSubimage(x, 0, 22, 18),
					buttonsBmp.getSubimage(x, 18, 22, 18), 16 + x, 88));
			final int btnNum = i + 1;
			controlButtons[i] = btn;
			btn.addListener(() -> System.out.println("Pressed " + btnNum));
		}
		RasterUIButton btnEject = mainWin.addComponent(window -> new RasterUIButton(window, buttonsBmp.getSubimage(114, 0, 22, 16),
				buttonsBmp.getSubimage(114, 16, 22, 16), 136, 89));
		btnEject.addListener(() -> System.out.println("Pressed eject"));

		BufferedImage balanceSliderBmp = loadImage(skinZip, "BALANCE.BMP");
		if (balanceSliderBmp == null) {
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

		ExtRasterUISlider balanceSlider = mainWin
				.addComponent(window -> new ExtRasterUISlider(window, balanceSliderBackgrounds, balanceReleased, balancePressed, 177, 57,
						38 - 14, (38 - 14) / 2, false, val -> (int) Math.round(Math.abs((38 - 14) / 2 - val) * 28 / 12.0d), 0, 1));
		balanceSlider.addListener(() -> System.out.println("Balance: " + balanceSlider.getSliderPosition()));

		BufferedImage volumeSliderBmp = loadImage(skinZip, "VOLUME.BMP");
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

		ExtRasterUISlider volumeSlider = mainWin.addComponent(window -> new ExtRasterUISlider(window, volumeSliderBackgrounds,
				volumeReleased, volumePressed, 107, 57, 68 - 14, 68 - 14, false, val -> (int) Math.round(val * 28.0d / 54.0d), 0, 1));
		volumeSlider.addListener(() -> System.out.println("Volume: " + volumeSlider.getSliderPosition()));

		mainWin.setBackground(new Color(0, 0, 0, 0));
		resultBuilder.a(RetroUIMainWindow.builder()
				.window(mainWin)
				.balanceSlider(balanceSlider)
				.volumelider(volumeSlider)
				.seekSlider(seekSlider)
				.btnEject(btnEject)
				.btnPrevious(controlButtons[0])
				.btnPlay(controlButtons[1])
				.btnPause(controlButtons[2])
				.btnStop(controlButtons[3])
				.btnNext(controlButtons[4])
				.build());

		BufferedImage shufRepBmp = loadImage(skinZip, "SHUFREP.BMP");
		RasterUIToggleButton btnEqToggle = mainWin.addComponent(
				window -> new RasterUIToggleButton(window, shufRepBmp.getSubimage(0, 73, 23, 12), shufRepBmp.getSubimage(46, 73, 23, 12),
						shufRepBmp.getSubimage(0, 61, 23, 12), shufRepBmp.getSubimage(46, 61, 23, 12), 219, 58));

		RasterUIToggleButton btnPlaylistToggle = mainWin.addComponent(
				window -> new RasterUIToggleButton(window, shufRepBmp.getSubimage(23, 73, 23, 12), shufRepBmp.getSubimage(69, 73, 23, 12),
						shufRepBmp.getSubimage(23, 61, 23, 12), shufRepBmp.getSubimage(69, 61, 23, 12), 242, 58));

		BufferedImage eqWindowBackgroundBmp = loadImage(skinZip, "EQMAIN.BMP");
		BufferedImage eqArgbBackgroundImage = new BufferedImage(275, 116, BufferedImage.TYPE_INT_ARGB);
		ImageUtil.drawOnto(eqArgbBackgroundImage, eqWindowBackgroundBmp.getSubimage(0, 0, 275, 116), 0, 0);

		RasterGraphicsWindow eqWin = new RasterGraphicsWindow(275, 116, eqArgbBackgroundImage, eqWindowTransparencyMask,
				new RectanglePointRange(0, 0, 275, 16), null);
		eqWin.setBackground(new Color(0, 0, 0, 0));

		BufferedImage eqTitleBarBmp = ImageUtil.convert(loadImage(skinZip, "eqmain.bmp"), BufferedImage.TYPE_INT_ARGB);
		RasterUIHeading eqTitleBar = eqWin.addComponent(window -> new RasterUIHeading(window, eqTitleBarBmp.getSubimage(0, 134, 275, 14),
				eqTitleBarBmp.getSubimage(0, 149, 275, 14), 0, 0));

		resultBuilder.b(RetroUIEqualizerWindow.builder().window(eqWin).build());

		Supplier<Boolean> isEQWinInSnapPosition = () -> mainWin.getLocation().x == eqWin.getLocation().x
				&& mainWin.getLocation().y + mainWin.getHeight() == eqWin.getLocation().y;

		Supplier<Boolean> isEQWinNearSnapPosition = () -> Math.abs(mainWin.getLocation().x - eqWin.getLocation().x) < 10
				&& Math.abs(mainWin.getLocation().y + mainWin.getHeight() - eqWin.getLocation().y) < 10;

		AtomicBoolean eqWindowSnapped = new AtomicBoolean(isEQWinInSnapPosition.get());
		Runnable moveEQWindowBelowMain = () -> eqWin.setLocation(mainWin.getLocation().x, mainWin.getLocation().y + mainWin.getHeight());

		mainWin.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent e) {
				if (eqWindowSnapped.get()) {
					moveEQWindowBelowMain.run();
				} else {
					eqWindowSnapped.set(isEQWinInSnapPosition.get());
				}
			}

			@Override
			public void componentResized(ComponentEvent e) {
				if (eqWindowSnapped.get()) {
					eqWin.setSize(mainWin.getSize());
					moveEQWindowBelowMain.run();
				} else {
					eqWin.setSize(mainWin.getSize());
				}
			}

			@Override
			public void componentShown(ComponentEvent e) {
				eqWindowSnapped.set(isEQWinInSnapPosition.get());
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				eqWindowSnapped.set(false);
			}
		});

		eqWin.addMoveListener(e -> {
			if (isEQWinNearSnapPosition.get()) {
				moveEQWindowBelowMain.run();
			}
			eqWindowSnapped.set(isEQWinInSnapPosition.get());
		});

		eqWin.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				eqWindowSnapped.set(isEQWinInSnapPosition.get());
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				eqWindowSnapped.set(false);
			}
		});

		mainWin.addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				titleBar.setFocused(true);
				titleBar.repaint();
				if (eqWin.isVisible()) {
					eqWin.toFront();
					eqWin.repaint();
				}
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				titleBar.setFocused(false);
				titleBar.repaint();
			}
		});

		eqWin.addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				eqTitleBar.setFocused(true);
				eqTitleBar.repaint();
				if (mainWin.isVisible()) {
					mainWin.toFront();
					mainWin.repaint();
				}
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				eqTitleBar.setFocused(false);
				eqTitleBar.repaint();
			}
		});

		mainWin.setAutoRequestFocus(false);
		eqWin.setAutoRequestFocus(false);

		btnEqToggle.setButtonOn(true);
		btnEqToggle.addListener(() -> eqWin.setVisible(btnEqToggle.isButtonOn()));
		btnPlaylistToggle.setButtonOn(true);

		return resultBuilder.build();
	}

	protected <T> T load(ZipFile skinZip, String fileName, UnsafeFunction<InputStream, T, ?> converter) throws WSZLoadingException {
		try {
			FileHeader fileHeader = skinZip.getFileHeaders()
					.stream()
					.filter(fh -> fh.getFileName().equalsIgnoreCase(fileName)
							|| fh.getFileName().toLowerCase().endsWith("/" + fileName.toLowerCase()))
					.findAny()
					.orElse(null);
			if (fileHeader == null) {
				return null;
			}
			try (ZipInputStream inputStream = skinZip.getInputStream(fileHeader)) {
				return converter.apply(inputStream);
			} catch (Exception e) {
				throw new WSZLoadingException(
						"Failed to load file " + fileName + " from WinAmp skin " + skinZip.getFile().getAbsolutePath(), e);
			}
		} catch (ZipException ze) {
			throw new WSZLoadingException("Failed to get list of files from WinAmp skin " + skinZip.getFile().getAbsolutePath(), ze);
		}
	}

	protected BufferedImage loadImage(ZipFile skinZip, String fileName) throws WSZLoadingException {
		return load(skinZip, fileName, ImageIO::read);
	}

	protected Wini loadIniFile(ZipFile skinZip, String fileName) throws WSZLoadingException {
		return load(skinZip, fileName, Wini::new);
	}

	public static void main(String args[]) throws Exception {
		// String skin = "base-2.91";
		// String skin = "MetrixMetalDream";
		// String skin = "Bento_Classified";
		// String skin = "Necromech";
		// String skin = "Lime";

		Stream.of(new File("/Users/mvmn/Downloads/winamp_skins").listFiles())
				.filter(f -> !f.isDirectory() && f.getName().toLowerCase().endsWith(".wsz"))
				.filter(f -> f.getName().contains("cyborgani") || f.getName().contains("base"))
				.forEach(skinZipFile -> {
					try {
						System.out.println("Loading skin: " + skinZipFile.getName());
						Tuple3<RetroUIMainWindow, RetroUIEqualizerWindow, Void> retroUIWindows = new RetroUIFactory()
								.construct(skinZipFile);
						retroUIWindows.getA().getWindow().setLocation(100, 100);
						retroUIWindows.getA().getWindow().setVisible(true);

						retroUIWindows.getB().getWindow().setLocation(100, 216);
						retroUIWindows.getB().getWindow().setVisible(true);

					} catch (Exception e) {
						e.printStackTrace();
					}
				});
	}
}
