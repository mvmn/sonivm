package x.mvmn.sonivm.ui.retro;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.util.TriConsumer;
import org.ini4j.Wini;
import org.springframework.stereotype.Service;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import x.mvmn.sonivm.ui.retro.exception.WSZLoadingException;
import x.mvmn.sonivm.ui.retro.rasterui.ExtRasterUISlider;
import x.mvmn.sonivm.ui.retro.rasterui.RasterFrameWindow;
import x.mvmn.sonivm.ui.retro.rasterui.RasterGraphicsWindow;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIBooleanIndicator;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIButton;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIMultiIndicator;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUISlider;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUITextComponent;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIToggleButton;
import x.mvmn.sonivm.ui.util.swing.ImageUtil;
import x.mvmn.sonivm.ui.util.swing.RectLocationAndSize;
import x.mvmn.sonivm.ui.util.swing.RectanglePointRange;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.ui.util.swing.SwingUtil.Direction;
import x.mvmn.sonivm.util.Tuple3;
import x.mvmn.sonivm.util.Tuple4;
import x.mvmn.sonivm.util.UnsafeFunction;

@Service
public class RetroUIFactory {

	private static final int SNAP_DISTANCE_PIXELS = 15;

	public Tuple3<RetroUIMainWindow, RetroUIEqualizerWindow, RetroUIPlaylistWindow> construct(File winAmpSkinZipFile)
			throws WSZLoadingException {
		Tuple3.Tuple3Builder<RetroUIMainWindow, RetroUIEqualizerWindow, RetroUIPlaylistWindow> resultBuilder = Tuple3
				.<RetroUIMainWindow, RetroUIEqualizerWindow, RetroUIPlaylistWindow> builder();

		ImageIO.setUseCache(false);
		ZipFile skinZip = winAmpSkinZipFile != null ? new ZipFile(winAmpSkinZipFile) : null;

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
		BufferedImage handleReleased = ImageUtil.subImageOrBlank(posBar, 248, 0, 29, posBar.getHeight());
		BufferedImage handlePressed = ImageUtil.subImageOrBlank(posBar, 278, 0, 29, posBar.getHeight());

		BufferedImage buttonsBmp = loadImage(skinZip, "CBUTTONS.BMP");

		BufferedImage mainWindowTransparencyMask = null;
		if (mainWindowNumPoints != null && mainWindowPointList != null) {
			mainWindowTransparencyMask = WinAmpSkinUtil.createTransparencyMask(275, 116, mainWindowNumPoints, mainWindowPointList);
		}
		BufferedImage eqWindowTransparencyMask = null;
		if (eqWindowNumPoints != null && eqWindowPointList != null) {
			eqWindowTransparencyMask = WinAmpSkinUtil.createTransparencyMask(275, 116, eqWindowNumPoints, eqWindowPointList);
		}

		RasterGraphicsWindow mainWin = new RasterGraphicsWindow(275, 116, argbBackgroundImage, mainWindowTransparencyMask,
				new RectanglePointRange(0, 0, 275, 16), new RectanglePointRange(255, 96, 275, 116),
				new RectanglePointRange(264, 3, 264 + 9, 3 + 9));

		BufferedImage titleBarBmp = ImageUtil.convert(loadImage(skinZip, "titlebar.bmp"), BufferedImage.TYPE_INT_ARGB);
		BufferedImage mainWinTitleBarActive = ImageUtil.subImageOrBlank(titleBarBmp, 27, 0, 275, 14);
		ImageUtil.drawOnto(mainWinTitleBarActive, ImageUtil.subImageOrBlank(titleBarBmp, 0, 0, 9, 9), 6, 3);
		ImageUtil.drawOnto(mainWinTitleBarActive, ImageUtil.subImageOrBlank(titleBarBmp, 9, 0, 9, 9), 244, 3);
		ImageUtil.drawOnto(mainWinTitleBarActive, ImageUtil.subImageOrBlank(titleBarBmp, 0, 18, 9, 9), 254, 3);
		ImageUtil.drawOnto(mainWinTitleBarActive, ImageUtil.subImageOrBlank(titleBarBmp, 18, 0, 9, 9), 264, 3);

		ImageUtil.drawOnto(argbBackgroundImage, ImageUtil.subImageOrBlank(titleBarBmp, 304, 0, 8, 43), 10, 22);
		RasterUIBooleanIndicator titleBar = mainWin.addComponent(window -> new RasterUIBooleanIndicator(window, mainWinTitleBarActive,
				ImageUtil.subImageOrBlank(titleBarBmp, 27, 15, 275, 14), 0, 0));

		RasterUISlider seekSlider = mainWin.addComponent(window -> new RasterUISlider(window,
				ImageUtil.subImageOrBlank(posBar, 0, 0, 248, posBar.getHeight()), handleReleased, handlePressed, 16, 72, 219, 0, false));
		// seekSlider.addListener(() -> System.out.println("Seek: " + seekSlider.getSliderPosition()));

		// 136x36, 5buttons 23x18, 1 button 23x16
		RasterUIButton[] controlButtons = new RasterUIButton[5];
		for (int i = 0; i < 5; i++) {
			int x = i * 23;
			RasterUIButton btn = mainWin.addComponent(window -> new RasterUIButton(window,
					ImageUtil.subImageOrBlank(buttonsBmp, x, 0, 22, 18), ImageUtil.subImageOrBlank(buttonsBmp, x, 18, 22, 18), 16 + x, 88));
			controlButtons[i] = btn;
			// final int btnNum = i + 1;
			// btn.addListener(() -> System.out.println("Pressed " + btnNum));
		}
		RasterUIButton btnEject = mainWin.addComponent(window -> new RasterUIButton(window,
				ImageUtil.subImageOrBlank(buttonsBmp, 114, 0, 22, 16), ImageUtil.subImageOrBlank(buttonsBmp, 114, 16, 22, 16), 136, 89));
		// btnEject.addListener(() -> System.out.println("Pressed eject"));
		BufferedImage volumeSliderBmp = loadImage(skinZip, "VOLUME.BMP");
		BufferedImage[] volumeSliderBackgrounds = new BufferedImage[28];
		for (int i = 0; i < volumeSliderBackgrounds.length; i++) {
			volumeSliderBackgrounds[i] = new BufferedImage(68, 13, BufferedImage.TYPE_INT_ARGB);
			ImageUtil.drawOnto(volumeSliderBackgrounds[i], ImageUtil.subImageOrBlank(volumeSliderBmp, 0, i * 15, 68, 13), 0, 0);
		}

		BufferedImage volumeReleased;
		BufferedImage volumePressed;
		if (volumeSliderBmp.getHeight() - 422 > 0) {
			volumeReleased = ImageUtil.subImageOrBlank(volumeSliderBmp, 15, 422, 14, volumeSliderBmp.getHeight() - 422);
			volumePressed = ImageUtil.subImageOrBlank(volumeSliderBmp, 0, 422, 14, volumeSliderBmp.getHeight() - 422);
		} else {
			volumeReleased = new BufferedImage(14, 11, BufferedImage.TYPE_INT_ARGB);
			volumePressed = new BufferedImage(14, 11, BufferedImage.TYPE_INT_ARGB);
		}

		ExtRasterUISlider volumeSlider = mainWin.addComponent(window -> new ExtRasterUISlider(window, volumeSliderBackgrounds,
				volumeReleased, volumePressed, 107, 57, 68 - 14, 68 - 14, false, val -> (int) Math.round(val * 28.0d / 54.0d), 0, 1));
		// volumeSlider.addListener(() -> System.out.println("Volume: " + volumeSlider.getSliderPosition()));

		BufferedImage balanceSliderBmp = loadImage(skinZip, "BALANCE.BMP");
		BufferedImage[] balanceSliderBackgrounds = new BufferedImage[28];
		BufferedImage balanceReleased = volumeReleased;
		BufferedImage balancePressed = volumePressed;
		if (balanceSliderBmp == null) {
			for (int i = 0; i < balanceSliderBackgrounds.length; i++) {
				balanceSliderBackgrounds[i] = ImageUtil.subImageOrBlank(volumeSliderBackgrounds[i], 15, 0, 38, 13);
			}
		} else {
			for (int i = 0; i < balanceSliderBackgrounds.length; i++) {
				balanceSliderBackgrounds[i] = new BufferedImage(38, 13, BufferedImage.TYPE_INT_ARGB);
				ImageUtil.drawOnto(balanceSliderBackgrounds[i], ImageUtil.subImageOrBlank(balanceSliderBmp, 9, i * 15, 38, 13), 0, 0);
			}
			if (balanceSliderBmp.getHeight() - 422 > 0) {
				balanceReleased = ImageUtil.subImageOrBlank(balanceSliderBmp, 15, 422, 14, balanceSliderBmp.getHeight() - 422);
				balancePressed = ImageUtil.subImageOrBlank(balanceSliderBmp, 0, 422, 14, balanceSliderBmp.getHeight() - 422);
			}
		}

		final BufferedImage balanceReleasedFinal = balanceReleased;
		final BufferedImage balancePressedFinal = balancePressed;

		ExtRasterUISlider balanceSlider = mainWin
				.addComponent(window -> new ExtRasterUISlider(window, balanceSliderBackgrounds, balanceReleasedFinal, balancePressedFinal,
						177, 57, 38 - 14, (38 - 14) / 2, false, val -> (int) Math.round(Math.abs((38 - 14) / 2 - val) * 28 / 12.0d), 0, 1));
		// balanceSlider.addListener(() -> System.out.println("Balance: " + balanceSlider.getSliderPosition()));

		BufferedImage shufRepBmp = loadImage(skinZip, "SHUFREP.BMP");

		BufferedImage eqToggleOn;
		BufferedImage eqToggleOnPressed;
		BufferedImage eqToggleOff;
		BufferedImage eqToggleOffPressed;
		BufferedImage playlistToggleOn;
		BufferedImage playlistToggleOnPressed;
		BufferedImage playlistToggleOff;
		BufferedImage playlistToggleOffPressed;

		eqToggleOff = ImageUtil.subImageOrBlank(shufRepBmp, 0, 61, 23, 12);
		eqToggleOffPressed = ImageUtil.subImageOrBlank(shufRepBmp, 46, 61, 23, 12);
		playlistToggleOff = ImageUtil.subImageOrBlank(shufRepBmp, 23, 61, 23, 12);
		playlistToggleOffPressed = ImageUtil.subImageOrBlank(shufRepBmp, 69, 61, 23, 12);
		eqToggleOn = ImageUtil.subImageOrBlank(shufRepBmp, 0, 73, 23, 12);
		eqToggleOnPressed = ImageUtil.subImageOrBlank(shufRepBmp, 46, 73, 23, 12);
		playlistToggleOn = ImageUtil.subImageOrBlank(shufRepBmp, 23, 73, 23, 12);
		playlistToggleOnPressed = ImageUtil.subImageOrBlank(shufRepBmp, 69, 73, 23, 12);

		RasterUIToggleButton btnEqToggle = mainWin.addComponent(
				window -> new RasterUIToggleButton(window, eqToggleOn, eqToggleOnPressed, eqToggleOff, eqToggleOffPressed, 219, 58));

		RasterUIToggleButton btnPlaylistToggle = mainWin.addComponent(window -> new RasterUIToggleButton(window, playlistToggleOn,
				playlistToggleOnPressed, playlistToggleOff, playlistToggleOffPressed, 242, 58));

		RasterUIToggleButton btnShuffleToggle = mainWin.addComponent(window -> new RasterUIToggleButton(window,
				ImageUtil.subImageOrBlank(shufRepBmp, 0, 30, 28, 15), ImageUtil.subImageOrBlank(shufRepBmp, 0, 45, 28, 15),
				ImageUtil.subImageOrBlank(shufRepBmp, 0, 0, 28, 15), ImageUtil.subImageOrBlank(shufRepBmp, 0, 15, 28, 15), 211, 89));

		RasterUIToggleButton btnRepeatToggle = mainWin.addComponent(window -> new RasterUIToggleButton(window,
				ImageUtil.subImageOrBlank(shufRepBmp, 28, 30, 47, 15), ImageUtil.subImageOrBlank(shufRepBmp, 28, 45, 47, 15),
				ImageUtil.subImageOrBlank(shufRepBmp, 28, 0, 47, 15), ImageUtil.subImageOrBlank(shufRepBmp, 28, 15, 47, 15), 164, 89));

		BufferedImage monosterBmp = Optional.ofNullable(loadImage(skinZip, "monoster.bmp"))
				.orElseGet(() -> new BufferedImage(56, 24, BufferedImage.TYPE_INT_ARGB));
		RasterUIBooleanIndicator monoIndicator = mainWin.addComponent(window -> new RasterUIBooleanIndicator(window,
				ImageUtil.subImageOrBlank(monosterBmp, 29, 0, monosterBmp.getWidth() - 29, 12),
				ImageUtil.subImageOrBlank(monosterBmp, 29, 12, monosterBmp.getWidth() - 29, 12), 212, 41));
		RasterUIBooleanIndicator stereoIndicator = mainWin.addComponent(window -> new RasterUIBooleanIndicator(window,
				ImageUtil.subImageOrBlank(monosterBmp, 0, 0, 29, 12), ImageUtil.subImageOrBlank(monosterBmp, 0, 12, 29, 12), 239, 41));

		BufferedImage numbers[] = new BufferedImage[12]; // 0-9, blank, minus
		BufferedImage numExtBmp = loadImage(skinZip, "nums_ex.bmp");
		if (numExtBmp == null) {
			BufferedImage numbersBmp = ImageUtil.convert(loadImage(skinZip, "numbers.bmp"), BufferedImage.TYPE_INT_ARGB);
			if (numbersBmp == null) {
				numbersBmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			}
			for (int i = 0; i < 11; i++) {
				numbers[i] = ImageUtil.subImageOrBlank(numbersBmp, 9 * i, 0, 9, numbersBmp.getHeight());
			}
			numbers[11] = ImageUtil.subImageOrBlank(numbersBmp, 9 * 10, 0, 9, numbersBmp.getHeight(), true);
			ImageUtil.drawOnto(numbers[11], ImageUtil.subImageOrBlank(numbersBmp, 9 * 2 + 1, 6, 7, 1), 1, 6);
		} else {
			numExtBmp = ImageUtil.convert(numExtBmp, BufferedImage.TYPE_INT_ARGB);
			for (int i = 0; i < 12; i++) {
				numbers[i] = ImageUtil.subImageOrBlank(numExtBmp, 9 * i, 0, 9, numExtBmp.getHeight());
			}
		}
		RasterUIMultiIndicator playTimeNumber0 = mainWin.addComponent(window -> new RasterUIMultiIndicator(mainWin, numbers, 36, 26));
		RasterUIMultiIndicator playTimeNumber1 = mainWin.addComponent(window -> new RasterUIMultiIndicator(mainWin, numbers, 48, 26));
		RasterUIMultiIndicator playTimeNumber2 = mainWin.addComponent(window -> new RasterUIMultiIndicator(mainWin, numbers, 60, 26));
		RasterUIMultiIndicator playTimeNumber3 = mainWin.addComponent(window -> new RasterUIMultiIndicator(mainWin, numbers, 78, 26));
		RasterUIMultiIndicator playTimeNumber4 = mainWin.addComponent(window -> new RasterUIMultiIndicator(mainWin, numbers, 90, 26));
		playTimeNumber0.setState(10);

		BufferedImage playpausBmp = ImageUtil.convert(loadImage(skinZip, "playpaus.bmp"), BufferedImage.TYPE_INT_ARGB);
		BufferedImage playStates[] = new BufferedImage[4];
		for (int i = 0; i < playStates.length; i++) {
			playStates[i] = ImageUtil.subImageOrBlank(playpausBmp, 9 * i, 0, 9, 9);
		}

		RasterUIMultiIndicator playStateIndicator = mainWin.addComponent(window -> new RasterUIMultiIndicator(mainWin, playStates, 24, 28));
		playStateIndicator.setState(2);

		BufferedImage textBmp = loadImage(skinZip, "text.bmp");
		BufferedImage textCharSpace = ImageUtil.subImageOrBlank(textBmp, 142, 0, 6, 6);
		Color backgroundColor = ImageUtil.averageColor(textCharSpace, 0, 0, 6, 6);
		BufferedImage textCharA = ImageUtil.subImageOrBlank(textBmp, 0, 0, 5, 6);
		Color darkest = ImageUtil.findMinOrMaxColor(textCharA, 0, 0, 5, 6, false);
		Color brightest = ImageUtil.findMinOrMaxColor(textCharA, 0, 0, 5, 6, true);

		Color textColor;
		int bgColorBrightness = backgroundColor.getRed() + backgroundColor.getGreen() + backgroundColor.getBlue();
		int darkestColorBrightness = darkest.getRed() + darkest.getGreen() + darkest.getBlue();
		int brightestColorBrightness = brightest.getRed() + brightest.getGreen() + brightest.getBlue();
		int darkDiff = Math.abs(bgColorBrightness - darkestColorBrightness);
		int brightDiff = Math.abs(bgColorBrightness - brightestColorBrightness);

		textColor = darkDiff > brightDiff ? darkest : brightest;

		RasterUITextComponent nowPlayingText = mainWin
				.addComponent(window -> new RasterUITextComponent(mainWin, backgroundColor, textColor, 154, 10, 111, 25, 16));
		nowPlayingText.setText("SONIVM");
		nowPlayingText.setOffset(10);

		resultBuilder.a(RetroUIMainWindow.builder()
				.window(mainWin)
				.titleBar(titleBar)
				.balanceSlider(balanceSlider)
				.volumelider(volumeSlider)
				.seekSlider(seekSlider)
				.btnEject(btnEject)
				.btnPrevious(controlButtons[0])
				.btnPlay(controlButtons[1])
				.btnPause(controlButtons[2])
				.btnStop(controlButtons[3])
				.btnNext(controlButtons[4])
				.monoIndicator(monoIndicator)
				.stereoIndicator(stereoIndicator)
				.btnEqToggle(btnEqToggle)
				.btnPlaylistToggle(btnPlaylistToggle)
				.btnShuffleToggle(btnShuffleToggle)
				.btnRepeatToggle(btnRepeatToggle)
				.playStateIndicator(playStateIndicator)
				.playTimeNumbers(new RasterUIMultiIndicator[] { playTimeNumber0, playTimeNumber1, playTimeNumber2, playTimeNumber3,
						playTimeNumber4 })
				.nowPlayingText(nowPlayingText)
				.build());
		//////////////////// //////////////////// //////////////////// //////////////////// ////////////////////

		BufferedImage eqmainBmp = loadImage(skinZip, "EQMAIN.BMP");
		BufferedImage eqArgbBackgroundImage = new BufferedImage(275, 116, BufferedImage.TYPE_INT_ARGB);
		ImageUtil.drawOnto(eqArgbBackgroundImage, ImageUtil.subImageOrBlank(eqmainBmp, 0, 0, 275, 116), 0, 0);

		RasterGraphicsWindow eqWin = new RasterGraphicsWindow(275, 116, eqArgbBackgroundImage, eqWindowTransparencyMask,
				new RectanglePointRange(0, 0, 275, 16), null, new RectanglePointRange(264, 3, 264 + 9, 3 + 9));

		BufferedImage eqTitleBarBmp = ImageUtil.convert(loadImage(skinZip, "eqmain.bmp"), BufferedImage.TYPE_INT_ARGB);
		BufferedImage eqTitleBarActive = ImageUtil.subImageOrBlank(eqTitleBarBmp, 0, 134, 275, 14);
		ImageUtil.drawOnto(eqTitleBarActive, ImageUtil.subImageOrBlank(eqTitleBarBmp, 0, 116, 9, 9), 264, 3);
		RasterUIBooleanIndicator eqTitleBar = eqWin.addComponent(window -> new RasterUIBooleanIndicator(window, eqTitleBarActive,
				ImageUtil.subImageOrBlank(eqTitleBarBmp, 0, 149, 275, 14), 0, 0));

		ImageUtil.drawOnto(mainWinTitleBarActive, ImageUtil.subImageOrBlank(titleBarBmp, 0, 0, 9, 9), 6, 3);

		BufferedImage[] eqSliderBackgrounds = new BufferedImage[28];
		for (int i = 0; i < 28; i++) {
			eqSliderBackgrounds[27 - i] = new BufferedImage(14, 63, BufferedImage.TYPE_INT_ARGB);
			ImageUtil.drawOnto(eqSliderBackgrounds[27 - i],
					ImageUtil.subImageOrBlank(eqmainBmp, 13 + (i % 14) * 15, (i < 14 ? 0 : 65) + 164, 14, 63), 0, 0);
		}

		ExtRasterUISlider[] eqSliders = new ExtRasterUISlider[10];
		for (int i = 0; i < 10; i++) {
			final int index = i;
			eqSliders[i] = eqWin.addComponent(window -> new ExtRasterUISlider(window, eqSliderBackgrounds,
					ImageUtil.subImageOrBlank(eqmainBmp, 0, 164, 11, 11), ImageUtil.subImageOrBlank(eqmainBmp, 0, 164 + 12, 11, 11),
					78 + index * 18, 38, 51, 25, true, val -> (int) Math.round(val * 28.0d / 51.0d), 1, 0));
			// eqSliders[i].addListener(() -> System.out
			// .println("EQ " + index + ": " + eqSliders[index].getSliderPosition() + " " + eqSliders[index].getHeight()));
		}
		ExtRasterUISlider eqGainSlider = eqWin.addComponent(window -> new ExtRasterUISlider(window, eqSliderBackgrounds,
				ImageUtil.subImageOrBlank(eqmainBmp, 0, 164, 11, 11), ImageUtil.subImageOrBlank(eqmainBmp, 0, 164 + 12, 11, 11), 21, 38, 51,
				25, true, val -> (int) Math.round(val * 28.0d / 51.0d), 1, 0));
		// eqGainSlider.addListener(() -> System.out.println("Gain: " + eqGainSlider.getSliderPosition()));

		RasterUIButton btnPresets = eqWin.addComponent(window -> new RasterUIButton(window,
				ImageUtil.subImageOrBlank(eqmainBmp, 224, 164, 44, 12), ImageUtil.subImageOrBlank(eqmainBmp, 224, 176, 44, 12), 217, 18));

		RasterUIToggleButton btnEqOn = eqWin.addComponent(window -> new RasterUIToggleButton(window,
				ImageUtil.subImageOrBlank(eqmainBmp, 69, 119, 25, 12), ImageUtil.subImageOrBlank(eqmainBmp, 187, 119, 25, 12),
				ImageUtil.subImageOrBlank(eqmainBmp, 10, 119, 25, 12), ImageUtil.subImageOrBlank(eqmainBmp, 128, 119, 25, 12), 14, 18));
		RasterUIToggleButton btnEqAuto = eqWin.addComponent(window -> new RasterUIToggleButton(window,
				ImageUtil.subImageOrBlank(eqmainBmp, 94, 119, 33, 12), ImageUtil.subImageOrBlank(eqmainBmp, 212, 119, 33, 12),
				ImageUtil.subImageOrBlank(eqmainBmp, 35, 119, 33, 12), ImageUtil.subImageOrBlank(eqmainBmp, 153, 119, 33, 12), 39, 18));

		resultBuilder.b(RetroUIEqualizerWindow.builder()
				.window(eqWin)
				.titleBar(eqTitleBar)
				.gainSlider(eqGainSlider)
				.eqSliders(eqSliders)
				.btnPresets(btnPresets)
				.btnEqOnOff(btnEqOn)
				.btnEqAuto(btnEqAuto)
				.build());
		//////////////////// //////////////////// //////////////////// //////////////////// ////////////////////

		BufferedImage pleditBmp = ImageUtil.convert(loadImage(skinZip, "PLEDIT.BMP"), BufferedImage.TYPE_INT_ARGB);
		BufferedImage plFrameTopLeftActive = ImageUtil.subImageOrBlank(pleditBmp, 0, 0, 25, 20);
		BufferedImage plFrameTitleActive = ImageUtil.subImageOrBlank(pleditBmp, 26, 0, 100, 20);
		BufferedImage plFrameTopExtenderActive = ImageUtil.subImageOrBlank(pleditBmp, 127, 0, 25, 20);
		BufferedImage plFrameTopRightActive = ImageUtil.subImageOrBlank(pleditBmp, 153, 0, 25, 20);

		BufferedImage plFrameTopLeftInactive = ImageUtil.subImageOrBlank(pleditBmp, 0, 21, 25, 20);
		BufferedImage plFrameTitleInactive = ImageUtil.subImageOrBlank(pleditBmp, 26, 21, 100, 20);
		BufferedImage plFrameTopExtenderInactive = ImageUtil.subImageOrBlank(pleditBmp, 127, 21, 25, 20);
		BufferedImage plFrameTopRightInactive = ImageUtil.subImageOrBlank(pleditBmp, 153, 21, 25, 20);

		BufferedImage plFrameBottomExtender = ImageUtil.subImageOrBlank(pleditBmp, 179, 0, 25, 38);
		BufferedImage plFrameBottomExtenderBig = ImageUtil.subImageOrBlank(pleditBmp, 205, 0, 75, 38);

		BufferedImage plFrameLeft = ImageUtil.subImageOrBlank(pleditBmp, 0, 42, 12, 29);
		BufferedImage plFrameRight = ImageUtil.subImageOrBlank(pleditBmp, 31, 42, 20, 29);

		BufferedImage plSliderButtonActive = ImageUtil.subImageOrBlank(pleditBmp, 52, 53, 8, 18);
		BufferedImage plSliderButtonInactive = ImageUtil.subImageOrBlank(pleditBmp, 61, 53, 8, 18);
		BufferedImage plFrameBottomLeft = ImageUtil.subImageOrBlank(pleditBmp, 0, 72, 125, 38);
		BufferedImage plFrameBottomRight = ImageUtil.subImageOrBlank(pleditBmp, 126, 72, 150, 38);

		// int btnY = 111;
		// int btnW = 22;
		// int btnH = 18;

		// int divW = 3;
		// int divH = 54;
		// int divLongH = 72;

		// Function<Tuple3<Integer, Integer, Integer>, Tuple2<BufferedImage[], BufferedImage[]>> cutOutButtons = numNCoords -> {
		// BufferedImage[] buttonsActive = new BufferedImage[numNCoords.getA()];
		// BufferedImage[] buttonsInActive = new BufferedImage[numNCoords.getA()];
		//
		// for (int i = 0; i < buttonsActive.length; i++) {
		// buttonsActive[i] = ImageUtil.subImageOrBlank(pleditBmp, numNCoords.getB(), numNCoords.getC() + (1 + btnH) * i, btnW, btnH);
		// buttonsInActive[i] = ImageUtil.subImageOrBlank(pleditBmp, numNCoords.getB() + 1 + btnW, numNCoords.getC() + (1 + btnH) * i,
		// btnW, btnH);
		// }
		//
		// return Tuple2.<BufferedImage[], BufferedImage[]> builder().a(buttonsActive).b(buttonsInActive).build();
		// };

		// TODO: Add support for playlist window buttons and menus
		// Tuple2<BufferedImage[], BufferedImage[]> addButtons = cutOutButtons.apply(Tuple3.of(3, 0, btnY));
		// Tuple2<BufferedImage[], BufferedImage[]> removeButtons = cutOutButtons.apply(Tuple3.of(4, 54, btnY));
		// Tuple2<BufferedImage[], BufferedImage[]> selectionButtons = cutOutButtons.apply(Tuple3.of(3, 104, btnY));
		// Tuple2<BufferedImage[], BufferedImage[]> miscButtons = cutOutButtons.apply(Tuple3.of(3, 154, btnY));
		// Tuple2<BufferedImage[], BufferedImage[]> listButtons = cutOutButtons.apply(Tuple3.of(3, 204, btnY));
		//
		// BufferedImage addBtnDivider = ImageUtil.subImageOrBlank(pleditBmp, 48, 111, divW, divH);
		// BufferedImage removeBtnDivider = ImageUtil.subImageOrBlank(pleditBmp, 100, 111, divW, divLongH);
		// BufferedImage selectionBtnDivider = ImageUtil.subImageOrBlank(pleditBmp, 150, 111, divW, divH);
		// BufferedImage miscBtnDivider = ImageUtil.subImageOrBlank(pleditBmp, 200, 111, divW, divH);
		// BufferedImage listBtnDivider = ImageUtil.subImageOrBlank(pleditBmp, 250, 111, divW, divH);

		Wini plEditIni = loadIniFile(skinZip, "PLEDIT.TXT");
		Color playlistTextColor = WinAmpSkinUtil.getColor(plEditIni, "Text", "Normal", textColor);
		Color playlistBackgroundColor = WinAmpSkinUtil.getColor(plEditIni, "Text", "NormalBG", backgroundColor);
		Color currentTrackTextColor = WinAmpSkinUtil.getColor(plEditIni, "Text", "Current", textColor.brighter());
		Color selectionBackgroundColor = WinAmpSkinUtil.getColor(plEditIni, "Text", "SelectedBG", backgroundColor.brighter());

		String[][] dummyTableData = IntStream.range(0, 100)
				.mapToObj(i -> new String[] { "Test" + i, "asdasd" })
				.collect(Collectors.toList())
				.toArray(new String[][] {});
		DefaultTableModel playlistTableModel = new DefaultTableModel(dummyTableData, new String[] { "Track", "Length" }) {
			private static final long serialVersionUID = -8801060966096981340L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		JTable playlistTable = new JTable(playlistTableModel);
		playlistTable.setOpaque(true);
		playlistTable.setBackground(playlistBackgroundColor);
		playlistTable.setForeground(playlistTextColor);
		playlistTable.setGridColor(playlistTextColor);
		playlistTable.setSelectionBackground(selectionBackgroundColor);
		playlistTable.setSelectionForeground(textColor);
		playlistTable.setBorder(BorderFactory.createEmptyBorder());
		playlistTable.getTableHeader().setBackground(playlistTextColor);
		playlistTable.getTableHeader().setForeground(playlistBackgroundColor);
		playlistTable.setIntercellSpacing(new Dimension(0, 0));

		RasterFrameWindow plEditWin = new RasterFrameWindow(275, 116, playlistTable, backgroundColor, plFrameTopLeftActive,
				plFrameTitleActive, plFrameTopExtenderActive, plFrameTopRightActive, plFrameTopLeftInactive, plFrameTitleInactive,
				plFrameTopExtenderInactive, plFrameTopRightInactive, plFrameLeft, plFrameRight, plFrameBottomLeft, plFrameBottomExtender,
				plFrameBottomExtenderBig, plFrameBottomRight, new RectanglePointRange(0, 0, 275, 16),
				new RectanglePointRange(255, 96, 275, 116), new RectanglePointRange(264, 3, 264 + 9, 3 + 9), plSliderButtonActive,
				plSliderButtonInactive);

		resultBuilder.c(RetroUIPlaylistWindow.builder()
				.window(plEditWin)
				.playlistTable(playlistTable)
				.playlistTableModel(playlistTableModel)
				.playlistColors(RetroUIPlaylistWindow.PlaylistColors.builder()
						.backgroundColor(backgroundColor)
						.selectionBackgroundColor(selectionBackgroundColor)
						.textColor(textColor)
						.currentTrackTextColor(currentTrackTextColor)
						.build())
				.build());
		//////////////////// //////////////////// //////////////////// //////////////////// ////////////////////

		BiPredicate<RectLocationAndSize, RectLocationAndSize> areAdjacent = (w1, w2) -> w1.getLeft() == w2.getRight()
				|| w1.getRight() == w2.getLeft() || w1.getBottom() == w2.getTop() || w1.getTop() == w2.getBottom();

		BiFunction<RectLocationAndSize, RectLocationAndSize, Direction> getAdjacency = (w1, w2) -> {
			if (w1.getLeft() == w2.getRight()) {
				return Direction.LEFT;
			} else if (w1.getRight() == w2.getLeft()) {
				return Direction.RIGHT;
			} else if (w1.getBottom() == w2.getTop()) {
				return Direction.BOTTOM;
			} else if (w1.getTop() == w2.getBottom()) {
				return Direction.TOP;
			}
			return null;
		};

		BiFunction<RectLocationAndSize, RectLocationAndSize, Integer> distanceRight = (w1, w2) -> Math.abs(w1.getRight() - w2.getLeft());
		BiFunction<RectLocationAndSize, RectLocationAndSize, Integer> distanceBottom = (w1, w2) -> Math.abs(w1.getBottom() - w2.getTop());

		BiPredicate<RectLocationAndSize, RectLocationAndSize> overlapsByX = (w1,
				w2) -> !(w1.getLeft() > w2.getRight() || w1.getRight() < w2.getLeft());
		BiPredicate<RectLocationAndSize, RectLocationAndSize> overlapsByY = (w1,
				w2) -> !(w1.getTop() > w2.getBottom() || w1.getBottom() < w2.getTop());

		Function<Integer, BiFunction<RectLocationAndSize, RectLocationAndSize, Direction>> isNearFunctionFactory = distance -> (w1, w2) -> {
			if (distanceRight.apply(w1, w2) <= distance && overlapsByY.test(w1, w2)) {
				return Direction.RIGHT;
			} else if (distanceRight.apply(w2, w1) <= distance && overlapsByY.test(w1, w2)) {
				return Direction.LEFT;
			} else if (distanceBottom.apply(w1, w2) <= distance && overlapsByX.test(w1, w2)) {
				return Direction.TOP;
			} else if (distanceBottom.apply(w2, w1) <= distance && overlapsByX.test(w1, w2)) {
				return Direction.BOTTOM;
			} else {
				return null;
			}
		};

		BiFunction<RectLocationAndSize, RectLocationAndSize, Direction> isNear = isNearFunctionFactory.apply(SNAP_DISTANCE_PIXELS);

		BiFunction<RectLocationAndSize, RasterGraphicsWindow, Point> snap = (w1, w2) -> {
			Direction nearDir = isNear.apply(w1, w2);
			if (nearDir != null && w2.isVisible()) {
				int x = 0;
				int y = 0;
				switch (nearDir) {
					case RIGHT:
						x = w2.getLeft() - w1.getWidth();
						y = w1.getTop();
					break;
					case LEFT:
						x = w2.getRight();
						y = w1.getTop();
					break;
					case TOP:
						x = w1.getLeft();
						y = w2.getTop() - w1.getHeight();
					break;
					case BOTTOM:
						x = w1.getLeft();
						y = w2.getBottom();
					break;
				}

				switch (nearDir) {
					case RIGHT:
					case LEFT:
						if (Math.abs(y - w2.getTop()) < SNAP_DISTANCE_PIXELS) {
							y = w2.getTop();
						}
						if (Math.abs(y - w2.getBottom()) < SNAP_DISTANCE_PIXELS) {
							y = w2.getBottom();
						}
						if (Math.abs(y + w1.getHeight() - w2.getTop()) < SNAP_DISTANCE_PIXELS) {
							y = w2.getTop() - w1.getHeight();
						}
						if (Math.abs(y + w1.getHeight() - w2.getBottom()) < SNAP_DISTANCE_PIXELS) {
							y = w2.getBottom() - w1.getHeight();
						}
					break;
					case TOP:
					case BOTTOM:
						if (Math.abs(x - w2.getRight()) < SNAP_DISTANCE_PIXELS) {
							x = w2.getRight();
						}
						if (Math.abs(x - w2.getLeft()) < SNAP_DISTANCE_PIXELS) {
							x = w2.getLeft();
						}
						if (Math.abs(x + w1.getWidth() - w2.getRight()) < SNAP_DISTANCE_PIXELS) {
							x = w2.getRight() - w1.getWidth();
						}
						if (Math.abs(x + w1.getWidth() - w2.getLeft()) < SNAP_DISTANCE_PIXELS) {
							x = w2.getLeft() - w1.getWidth();
						}
					break;
				}

				return new Point(x, y);
			}
			return new Point(w1.getLeft(), w1.getTop());
		};

		AtomicBoolean eqSnapped = new AtomicBoolean();
		AtomicBoolean plSnapped = new AtomicBoolean();

		Direction[] windowAdjacency = new Direction[3];

		Runnable updateSnapping = () -> {
			windowAdjacency[0] = null;
			windowAdjacency[1] = null;
			windowAdjacency[2] = null;

			boolean eq = false;
			boolean pl = false;
			if (mainWin.isVisible() && eqWin.isVisible() && areAdjacent.test(mainWin, eqWin)) {
				windowAdjacency[0] = getAdjacency.apply(mainWin, eqWin);
				eq = true;
				if (plEditWin.isVisible() && areAdjacent.test(eqWin, plEditWin)) {
					windowAdjacency[2] = getAdjacency.apply(eqWin, plEditWin);
					pl = true;
				}
			}
			if (mainWin.isVisible() && eqWin.isVisible() && areAdjacent.test(mainWin, plEditWin)) {
				windowAdjacency[1] = getAdjacency.apply(mainWin, plEditWin);
				pl = true;
				if (!eq && plEditWin.isVisible() && areAdjacent.test(eqWin, plEditWin)) {
					windowAdjacency[2] = getAdjacency.apply(eqWin, plEditWin);
					eq = true;
				}
			}
			eqSnapped.set(eq);
			plSnapped.set(pl);
		};

		mainWin.addPostMoveListener(p -> updateSnapping.run());
		eqWin.addPostMoveListener(p -> updateSnapping.run());
		plEditWin.addPostMoveListener(p -> updateSnapping.run());

		mainWin.addMoveListener((p1, p2) -> {
			if (eqSnapped.get()) {
				eqWin.setLocation(eqWin.getLocation().x + (p2.x - p1.x), eqWin.getLocation().y + (p2.y - p1.y));
			}
			if (plSnapped.get()) {
				plEditWin.setLocation(plEditWin.getLocation().x + (p2.x - p1.x), plEditWin.getLocation().y + (p2.y - p1.y));
			}
		});

		TriConsumer<RasterGraphicsWindow, RasterGraphicsWindow, Direction> restoreAdjacency = (w1, w2, direction) -> {
			switch (direction) {
				case TOP:
					w2.setLocation(w2.getLocation().x, w1.getLocation().y - w2.getHeight());
				break;
				case BOTTOM:
					w2.setLocation(w2.getLocation().x, w1.getLocation().y + w1.getHeight());
				break;
				case LEFT:
					w2.setLocation(w1.getLocation().x - w2.getWidth(), w2.getLocation().y);
				break;
				case RIGHT:
					w2.setLocation(w1.getLocation().x + w1.getWidth(), w2.getLocation().y);
				break;
				default:
			}
		};

		Consumer<RasterGraphicsWindow> ensureVisibility = win -> {
			Tuple4<Boolean, String, Point, Dimension> windowState = SwingUtil.getWindowState(win);
			Rectangle screenBounds = SwingUtil.getScreenBounds(windowState.getB()).orElse(SwingUtil.getDefaultScreenBounds());
			Point topLeft = new Point(screenBounds.x + windowState.getC().x, screenBounds.y + windowState.getC().y);
			Point topRightPlus16Px = new Point(screenBounds.x + windowState.getC().x + windowState.getD().width,
					screenBounds.y + windowState.getC().y + 16);
			if (!(screenBounds.contains(topLeft) || screenBounds.contains(topRightPlus16Px))) {
				SwingUtil.moveToScreenCenter(win);
			}
		};

		mainWin.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				eqWin.setSize(mainWin.getSize());
				plEditWin.setScaleFactor(mainWin.getScaleFactor());

				if (eqSnapped.get()) {
					if (windowAdjacency[0] != null) {
						restoreAdjacency.accept(mainWin, eqWin, windowAdjacency[0]);
					} else if (plSnapped.get() && windowAdjacency[2] != null) {
						restoreAdjacency.accept(eqWin, plEditWin, windowAdjacency[2]);
					}
				}
				if (plSnapped.get()) {
					if (windowAdjacency[1] != null) {
						restoreAdjacency.accept(mainWin, plEditWin, windowAdjacency[1]);
					} else if (eqSnapped.get() && windowAdjacency[2] != null) {
						restoreAdjacency.accept(eqWin, plEditWin, windowAdjacency[2]);
					}
				}
			}

			@Override
			public void componentShown(ComponentEvent e) {
				ensureVisibility.accept(mainWin);
				updateSnapping.run();
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				updateSnapping.run();
			}
		});

		eqWin.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				eqWin.repaint();
				ensureVisibility.accept(eqWin);
				updateSnapping.run();
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				updateSnapping.run();
			}
		});

		plEditWin.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				ensureVisibility.accept(plEditWin);
				updateSnapping.run();
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				updateSnapping.run();
			}
		});

		mainWin.setMoveAdjuster((p1, p2) -> {
			RectanglePointRange winFutureLocation = RectanglePointRange.of(p2, mainWin.getSize());
			Point p = p2;
			if (!eqSnapped.get()) {
				p = snap.apply(winFutureLocation, eqWin);
			}
			if (!plSnapped.get()) {
				p = snap.apply(RectanglePointRange.of(p, mainWin.getSize()), plEditWin);
			}
			return p;
		});

		eqWin.setMoveAdjuster((p1, p2) -> {
			RectanglePointRange winFutureLocation = RectanglePointRange.of(p2, eqWin.getSize());
			Point p = snap.apply(winFutureLocation, mainWin);
			p = snap.apply(RectanglePointRange.of(p, eqWin.getSize()), plEditWin);
			return p;
		});

		plEditWin.setMoveAdjuster((p1, p2) -> {
			RectanglePointRange winFutureLocation = RectanglePointRange.of(p2, plEditWin.getSize());
			Point p = snap.apply(winFutureLocation, mainWin);
			p = snap.apply(RectanglePointRange.of(p, plEditWin.getSize()), eqWin);
			return p;
		});

		mainWin.addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				if (plEditWin.isVisible()) {
					plEditWin.toFront();
					plEditWin.repaint();
				}
				if (eqWin.isVisible()) {
					eqWin.toFront();
					eqWin.repaint();
				}
				mainWin.toFront();
				titleBar.setActive(true);
				titleBar.repaint();
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				titleBar.setActive(false);
				titleBar.repaint();
			}
		});

		eqWin.addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				if (plEditWin.isVisible()) {
					plEditWin.toFront();
					plEditWin.repaint();
				}
				if (mainWin.isVisible()) {
					mainWin.toFront();
					mainWin.repaint();
				}
				eqWin.toFront();
				eqTitleBar.setActive(true);
				eqTitleBar.repaint();
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				eqTitleBar.setActive(false);
				eqTitleBar.repaint();
			}
		});

		plEditWin.addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				if (eqWin.isVisible()) {
					eqWin.toFront();
					eqWin.repaint();
				}
				if (mainWin.isVisible()) {
					mainWin.toFront();
					mainWin.repaint();
				}
				plEditWin.toFront();
				plEditWin.setActive(true);
				plEditWin.repaint();
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				plEditWin.setActive(false);
				plEditWin.repaint();
			}
		});

		mainWin.setAutoRequestFocus(false);
		eqWin.setAutoRequestFocus(false);
		plEditWin.setAutoRequestFocus(false);

		btnEqToggle.setButtonOn(true);
		btnEqToggle.addListener(() -> eqWin.setVisible(btnEqToggle.isButtonOn()));
		btnPlaylistToggle.setButtonOn(true);
		btnPlaylistToggle.addListener(() -> plEditWin.setVisible(btnPlaylistToggle.isButtonOn()));

		eqWin.addCloseListener(() -> btnEqToggle.setButtonOn(false));
		plEditWin.addCloseListener(() -> btnPlaylistToggle.setButtonOn(false));

		return resultBuilder.build();
	}

	protected <T> T load(ZipFile skinZip, String fileName, UnsafeFunction<InputStream, T, ?> converter) throws WSZLoadingException {
		try {
			if (skinZip == null) {
				try (InputStream is = RetroUIFactory.class.getResourceAsStream("/retroskin/sonivm/" + fileName.toUpperCase())) {
					if (is == null) {
						return null;
					}
					return converter.apply(is);
				} catch (Exception e) {
					throw new WSZLoadingException("Failed to load embedded Sonivm skin", e);
				}
			}
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
		return load(skinZip, fileName, inputStream -> {
			byte[] img = IOUtils.toByteArray(inputStream);
			BufferedImage result = null;
			boolean retry = false;
			int maxRetries = 10;
			do {
				retry = false;
				try {
					result = ImageIO.read(new ByteArrayInputStream(img));
				} catch (EOFException eof) {
					if (maxRetries-- < 1) {
						throw eof;
					}
					// Dumbest crutch ever for the weird EOF error in JRE BMP reader
					// Weirdest thing is that it works!
					img = Arrays.copyOf(img, img.length + 1024);
					retry = true;
				}
			} while (retry);
			return result;
		});
	}

	protected Wini loadIniFile(ZipFile skinZip, String fileName) throws WSZLoadingException {
		return load(skinZip, fileName, Wini::new);
	}

	private static volatile Tuple3<RetroUIMainWindow, RetroUIEqualizerWindow, RetroUIPlaylistWindow> retroUIWindows;

	public static void main(String args[]) throws Exception {

		ComponentAdapter scrollThreadAdaptor = new ComponentAdapter() {
			private volatile Thread titleScrollThread;
			AtomicInteger offset = new AtomicInteger();

			@Override
			public void componentShown(ComponentEvent e) {
				titleScrollThread = new Thread(() -> {
					while (true) {
						Tuple3<RetroUIMainWindow, RetroUIEqualizerWindow, RetroUIPlaylistWindow> windows = RetroUIFactory.retroUIWindows;
						if (windows != null) {
							RasterUITextComponent nowPlaying = windows.getA().nowPlayingText;
							int newOffset = offset.getAndAdd(5);
							if (nowPlaying.getTextFullWidth() + 16 <= newOffset) {
								offset.set(0);
								newOffset = 0;
							}
							nowPlaying.setOffset(newOffset);
						}
						try {
							Thread.sleep(100);
							Thread.yield();
						} catch (InterruptedException ex) {
							Thread.interrupted();
							return;
						}
					}
				});
				titleScrollThread.setDaemon(true);
				titleScrollThread.start();
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				titleScrollThread.interrupt();
			}
		};

		Consumer<File> onSkinSelect = skinZipFile -> {
			try {
				System.out.println("Loading skin: " + (skinZipFile != null ? skinZipFile.getName() : "Embedded skin"));
				Tuple3<RetroUIMainWindow, RetroUIEqualizerWindow, RetroUIPlaylistWindow> retroUIWindows = new RetroUIFactory()
						.construct(skinZipFile);
				if (RetroUIFactory.retroUIWindows != null) {
					retroUIWindows.getA().getWindow().setLocation(RetroUIFactory.retroUIWindows.getA().getWindow().getLocation());
					retroUIWindows.getA().getWindow().setSize(RetroUIFactory.retroUIWindows.getA().getWindow().getSize());

					retroUIWindows.getB().getWindow().setLocation(RetroUIFactory.retroUIWindows.getB().getWindow().getLocation());
					retroUIWindows.getB().getWindow().setSize(RetroUIFactory.retroUIWindows.getB().getWindow().getSize());
					boolean eqVisible = RetroUIFactory.retroUIWindows.getB().getWindow().isVisible();
					retroUIWindows.getB().getWindow().setVisible(eqVisible);

					retroUIWindows.getA().btnEqToggle.setButtonOn(eqVisible);
					retroUIWindows.getA().getWindow().addComponentListener(scrollThreadAdaptor);

					retroUIWindows.getA().getWindow().setVisible(true);

					RasterFrameWindow plWin = RetroUIFactory.retroUIWindows.getC().getWindow();
					retroUIWindows.getC().getWindow().setLocation(plWin.getLocation());
					retroUIWindows.getC().getWindow().setScaleFactor(plWin.getScaleFactor());
					retroUIWindows.getC().getWindow().setSizeExtensions(plWin.getWidthExtension(), plWin.getHeightExtension());
					retroUIWindows.getC().getWindow().setVisible(plWin.isVisible());

					retroUIWindows.getA().btnPlaylistToggle.setButtonOn(plWin.isVisible());

					RetroUIFactory.retroUIWindows.getA().getWindow().setVisible(false);
					RetroUIFactory.retroUIWindows.getB().getWindow().setVisible(false);
					plWin.setVisible(false);
				} else {
					retroUIWindows.getA().getWindow().addComponentListener(scrollThreadAdaptor);
					retroUIWindows.getA().getWindow().setLocation(100, 100);
					retroUIWindows.getA().getWindow().setVisible(true);

					retroUIWindows.getB().getWindow().setLocation(100, 216);
					retroUIWindows.getB().getWindow().setVisible(true);

					retroUIWindows.getC().getWindow().setLocation(100, 332);
					retroUIWindows.getC().getWindow().setVisible(true);
				}
				retroUIWindows.getA().setPlaybackNumbers(12, 34, true);
				retroUIWindows.getA().setPlybackIndicatorState(false, false);
				retroUIWindows.getA().btnPlay.addListener(() -> retroUIWindows.getA().setPlybackIndicatorState(true, false));
				retroUIWindows.getA().btnStop.addListener(() -> retroUIWindows.getA().setPlybackIndicatorState(false, false));
				retroUIWindows.getA().btnPause.addListener(() -> retroUIWindows.getA().setPlybackIndicatorState(true, true));

				RetroUIFactory.retroUIWindows = retroUIWindows;
			} catch (Exception e) {
				e.printStackTrace();
			}
		};

		JFrame skinSelector = new JFrame();

		File skinsFolder = new File("/Users/mvmn/Downloads/winamp_skins");
		Set<String> skins = Stream
				.concat(Stream.of(" < Sonivm > "),
						Stream.of(skinsFolder.listFiles())
								.filter(f -> !f.isDirectory() && f.getName().toLowerCase().endsWith(".wsz"))
								.map(File::getName))
				.collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(str -> str.toLowerCase()))));
		JList<String> skinList = new JList<>(skins.toArray(new String[skins.size()]));

		skinList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		skinList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				String skinFileName = skinList.getModel().getElementAt(skinList.getSelectedIndex());
				onSkinSelect.accept(skinFileName.equals(" < Sonivm > ") ? null : new File(skinsFolder, skinFileName));
			}
		});

		skinSelector.getContentPane().setLayout(new BorderLayout());
		skinSelector.getContentPane().add(new JScrollPane(skinList), BorderLayout.CENTER);

		skinSelector.pack();
		skinSelector.setVisible(true);

		onSkinSelect.accept(null);

	}
}
