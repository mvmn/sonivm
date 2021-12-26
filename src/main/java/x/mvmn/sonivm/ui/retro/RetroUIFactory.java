package x.mvmn.sonivm.ui.retro;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.ini4j.Wini;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import x.mvmn.sonivm.ui.retro.exception.WSZLoadingException;
import x.mvmn.sonivm.ui.retro.rasterui.RasterFrameWindow;
import x.mvmn.sonivm.ui.retro.rasterui.RasterGraphicsWindow;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIButton;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIIndicator;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUISlider;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIToggleButton;
import x.mvmn.sonivm.ui.util.swing.ImageUtil;
import x.mvmn.sonivm.ui.util.swing.RectanglePointRange;
import x.mvmn.sonivm.util.Tuple2;
import x.mvmn.sonivm.util.Tuple3;
import x.mvmn.sonivm.util.UnsafeFunction;

public class RetroUIFactory {

	private static final int SNAP_INTERVAL_PIXELS = 20;

	public Tuple3<RetroUIMainWindow, RetroUIEqualizerWindow, Void> construct(File winAmpSkinZipFile) throws WSZLoadingException {
		Tuple3.Tuple3Builder<RetroUIMainWindow, RetroUIEqualizerWindow, Void> resultBuilder = Tuple3
				.<RetroUIMainWindow, RetroUIEqualizerWindow, Void> builder();

		ImageIO.setUseCache(false);
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
		BufferedImage handleReleased = ImageUtil.subImageOrBlank(posBar, 248, 0, 29, posBar.getHeight());
		BufferedImage handlePressed = ImageUtil.subImageOrBlank(posBar, 278, 0, 29, posBar.getHeight());

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
				new RectanglePointRange(0, 0, 275, 16), new RectanglePointRange(260, 100, 275, 116),
				new RectanglePointRange(264, 3, 264 + 9, 3 + 9));

		BufferedImage titleBarBmp = ImageUtil.convert(loadImage(skinZip, "titlebar.bmp"), BufferedImage.TYPE_INT_ARGB);
		BufferedImage mainWinTitleBarActive = ImageUtil.subImageOrBlank(titleBarBmp, 27, 0, 275, 14);
		ImageUtil.drawOnto(mainWinTitleBarActive, ImageUtil.subImageOrBlank(titleBarBmp, 0, 0, 9, 9), 6, 3);
		ImageUtil.drawOnto(mainWinTitleBarActive, ImageUtil.subImageOrBlank(titleBarBmp, 9, 0, 9, 9), 244, 3);
		ImageUtil.drawOnto(mainWinTitleBarActive, ImageUtil.subImageOrBlank(titleBarBmp, 0, 18, 9, 9), 254, 3);
		ImageUtil.drawOnto(mainWinTitleBarActive, ImageUtil.subImageOrBlank(titleBarBmp, 18, 0, 9, 9), 264, 3);

		ImageUtil.drawOnto(argbBackgroundImage, ImageUtil.subImageOrBlank(titleBarBmp, 304, 0, 8, 43), 10, 22);
		RasterUIIndicator titleBar = mainWin.addComponent(window -> new RasterUIIndicator(window, mainWinTitleBarActive,
				ImageUtil.subImageOrBlank(titleBarBmp, 27, 15, 275, 14), 0, 0));

		RasterUISlider seekSlider = mainWin.addComponent(window -> new RasterUISlider(window,
				ImageUtil.subImageOrBlank(posBar, 0, 0, 248, posBar.getHeight()), handleReleased, handlePressed, 16, 72, 219, 0, false));
		seekSlider.addListener(() -> System.out.println("Seek: " + seekSlider.getSliderPosition()));

		// 136x36, 5buttons 23x18, 1 button 23x16
		RasterUIButton[] controlButtons = new RasterUIButton[5];
		for (int i = 0; i < 5; i++) {
			int x = i * 23;
			RasterUIButton btn = mainWin.addComponent(window -> new RasterUIButton(window,
					ImageUtil.subImageOrBlank(buttonsBmp, x, 0, 22, 18), ImageUtil.subImageOrBlank(buttonsBmp, x, 18, 22, 18), 16 + x, 88));
			final int btnNum = i + 1;
			controlButtons[i] = btn;
			btn.addListener(() -> System.out.println("Pressed " + btnNum));
		}
		RasterUIButton btnEject = mainWin.addComponent(window -> new RasterUIButton(window,
				ImageUtil.subImageOrBlank(buttonsBmp, 114, 0, 22, 16), ImageUtil.subImageOrBlank(buttonsBmp, 114, 16, 22, 16), 136, 89));
		btnEject.addListener(() -> System.out.println("Pressed eject"));

		BufferedImage balanceSliderBmp = loadImage(skinZip, "BALANCE.BMP");
		if (balanceSliderBmp == null) {
			balanceSliderBmp = new BufferedImage(47, 418, BufferedImage.TYPE_INT_ARGB);
		}
		BufferedImage[] balanceSliderBackgrounds = new BufferedImage[28];
		for (int i = 0; i < balanceSliderBackgrounds.length; i++) {
			balanceSliderBackgrounds[i] = new BufferedImage(38, 13, BufferedImage.TYPE_INT_ARGB);
			ImageUtil.drawOnto(balanceSliderBackgrounds[i], ImageUtil.subImageOrBlank(balanceSliderBmp, 9, i * 15, 38, 13), 0, 0);
		}
		BufferedImage balanceReleased;
		BufferedImage balancePressed;
		if (balanceSliderBmp.getHeight() - 422 > 0) {
			balanceReleased = ImageUtil.subImageOrBlank(balanceSliderBmp, 15, 422, 14, balanceSliderBmp.getHeight() - 422);
			balancePressed = ImageUtil.subImageOrBlank(balanceSliderBmp, 0, 422, 14, balanceSliderBmp.getHeight() - 422);
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
		volumeSlider.addListener(() -> System.out.println("Volume: " + volumeSlider.getSliderPosition()));

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
		RasterUIIndicator monoIndicator = mainWin.addComponent(
				window -> new RasterUIIndicator(window, ImageUtil.subImageOrBlank(monosterBmp, 29, 0, monosterBmp.getWidth() - 29, 12),
						ImageUtil.subImageOrBlank(monosterBmp, 29, 12, monosterBmp.getWidth() - 29, 12), 212, 41));
		RasterUIIndicator stereoIndicator = mainWin.addComponent(window -> new RasterUIIndicator(window,
				ImageUtil.subImageOrBlank(monosterBmp, 0, 0, 29, 12), ImageUtil.subImageOrBlank(monosterBmp, 0, 12, 29, 12), 239, 41));

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
		RasterUIIndicator eqTitleBar = eqWin.addComponent(
				window -> new RasterUIIndicator(window, eqTitleBarActive, ImageUtil.subImageOrBlank(eqTitleBarBmp, 0, 149, 275, 14), 0, 0));

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
			eqSliders[i].addListener(() -> System.out.println("EQ " + index + ": " + eqSliders[index].getSliderPosition()));
		}
		ExtRasterUISlider eqGainSlider = eqWin.addComponent(window -> new ExtRasterUISlider(window, eqSliderBackgrounds,
				ImageUtil.subImageOrBlank(eqmainBmp, 0, 164, 11, 11), ImageUtil.subImageOrBlank(eqmainBmp, 0, 164 + 12, 11, 11), 21, 38, 51,
				25, true, val -> (int) Math.round(val * 28.0d / 51.0d), 1, 0));
		eqGainSlider.addListener(() -> System.out.println("Gain: " + eqGainSlider.getSliderPosition()));

		RasterUIButton btnPresets = eqWin.addComponent(window -> new RasterUIButton(window,
				ImageUtil.subImageOrBlank(eqmainBmp, 224, 164, 44, 12), ImageUtil.subImageOrBlank(eqmainBmp, 224, 176, 44, 12), 217, 18));

		RasterUIToggleButton btnEqOn = eqWin.addComponent(window -> new RasterUIToggleButton(window,
				ImageUtil.subImageOrBlank(eqmainBmp, 69, 119, 25, 12), ImageUtil.subImageOrBlank(eqmainBmp, 187, 119, 25, 12),
				ImageUtil.subImageOrBlank(eqmainBmp, 10, 119, 25, 12), ImageUtil.subImageOrBlank(eqmainBmp, 128, 119, 25, 12), 15, 18));
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

		int btnY = 111;
		int btnW = 22;
		int btnH = 18;

		int divW = 3;
		int divH = 54;
		int divLongH = 72;

		Function<Tuple3<Integer, Integer, Integer>, Tuple2<BufferedImage[], BufferedImage[]>> cutOutButtons = numNCoords -> {
			BufferedImage[] buttonsActive = new BufferedImage[numNCoords.getA()];
			BufferedImage[] buttonsInActive = new BufferedImage[numNCoords.getA()];

			for (int i = 0; i < buttonsActive.length; i++) {
				buttonsActive[i] = ImageUtil.subImageOrBlank(pleditBmp, numNCoords.getB(), numNCoords.getC() + (1 + btnH) * i, btnW, btnH);
				buttonsInActive[i] = ImageUtil.subImageOrBlank(pleditBmp, numNCoords.getB() + 1 + btnW, numNCoords.getC() + (1 + btnH) * i,
						btnW, btnH);
			}

			return Tuple2.<BufferedImage[], BufferedImage[]> builder().a(buttonsActive).b(buttonsInActive).build();
		};

		Tuple2<BufferedImage[], BufferedImage[]> addButtons = cutOutButtons.apply(Tuple3.of(3, 0, btnY));
		Tuple2<BufferedImage[], BufferedImage[]> removeButtons = cutOutButtons.apply(Tuple3.of(4, 54, btnY));
		Tuple2<BufferedImage[], BufferedImage[]> selectionButtons = cutOutButtons.apply(Tuple3.of(3, 104, btnY));
		Tuple2<BufferedImage[], BufferedImage[]> miscButtons = cutOutButtons.apply(Tuple3.of(3, 154, btnY));
		Tuple2<BufferedImage[], BufferedImage[]> listButtons = cutOutButtons.apply(Tuple3.of(3, 204, btnY));

		BufferedImage addBtnDivider = ImageUtil.subImageOrBlank(pleditBmp, 48, 111, divW, divH);
		BufferedImage removeBtnDivider = ImageUtil.subImageOrBlank(pleditBmp, 100, 111, divW, divLongH);
		BufferedImage selectionBtnDivider = ImageUtil.subImageOrBlank(pleditBmp, 150, 111, divW, divH);
		BufferedImage miscBtnDivider = ImageUtil.subImageOrBlank(pleditBmp, 200, 111, divW, divH);
		BufferedImage listBtnDivider = ImageUtil.subImageOrBlank(pleditBmp, 250, 111, divW, divH);

		Color backgroundColor = Color.BLACK;
		Wini plEditIni = loadIniFile(skinZip, "PLEDIT.TXT");
		if (plEditIni != null) {
			String bgColor = plEditIni.get("Text", "NormalBG");
			if (bgColor != null && bgColor.trim().startsWith("#")) {
				String color = bgColor.trim().substring(1);
				if (color.matches("^[A-F0-9]{6}$")) {
					int red = Integer.parseInt(color.substring(0, 2), 16);
					int green = Integer.parseInt(color.substring(2, 4), 16);
					int blue = Integer.parseInt(color.substring(4, 6), 16);
					backgroundColor = new Color(red, green, blue);
				}
			}
		}

		RasterFrameWindow plEditWin = new RasterFrameWindow(275, 116, backgroundColor, plFrameTopLeftActive, plFrameTitleActive,
				plFrameTopExtenderActive, plFrameTopRightActive, plFrameTopLeftInactive, plFrameTitleInactive, plFrameTopExtenderInactive,
				plFrameTopRightInactive, plFrameLeft, plFrameRight, plFrameBottomLeft, plFrameBottomExtender, plFrameBottomExtenderBig,
				plFrameBottomRight, new RectanglePointRange(0, 0, 275, 16), new RectanglePointRange(260, 100, 275, 116),
				new RectanglePointRange(264, 3, 264 + 9, 3 + 9));
		plEditWin.setVisible(true);

		//////////////////// //////////////////// //////////////////// //////////////////// ////////////////////

		Supplier<Boolean> isEQWinInSnapPosition = () -> mainWin.getLocation().x == eqWin.getLocation().x
				&& mainWin.getLocation().y + mainWin.getHeight() == eqWin.getLocation().y;

		Supplier<Boolean> isEQWinNearSnapPosition = () -> Math.abs(mainWin.getLocation().x - eqWin.getLocation().x) < SNAP_INTERVAL_PIXELS
				&& Math.abs(mainWin.getLocation().y + mainWin.getHeight() - eqWin.getLocation().y) < SNAP_INTERVAL_PIXELS;

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
				eqWin.setSize(mainWin.getSize());
				plEditWin.setScaleFactor(mainWin.getScaleFactor());
				if (eqWindowSnapped.get()) {
					moveEQWindowBelowMain.run();
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
				titleBar.setActive(true);
				titleBar.repaint();
				if (eqWin.isVisible()) {
					eqWin.toFront();
					eqWin.repaint();
				}
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
				eqTitleBar.setActive(true);
				eqTitleBar.repaint();
				if (mainWin.isVisible()) {
					mainWin.toFront();
					mainWin.repaint();
				}
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				eqTitleBar.setActive(false);
				eqTitleBar.repaint();
			}
		});

		mainWin.setAutoRequestFocus(false);
		eqWin.setAutoRequestFocus(false);
		plEditWin.setAutoRequestFocus(false);

		btnEqToggle.setButtonOn(true);
		btnEqToggle.addListener(() -> eqWin.setVisible(btnEqToggle.isButtonOn()));
		btnPlaylistToggle.setButtonOn(true);

		eqWin.addCloseListener(() -> btnEqToggle.setButtonOn(false));

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

	private static volatile Tuple3<RetroUIMainWindow, RetroUIEqualizerWindow, Void> retroUIWindows;

	public static void main(String args[]) throws Exception {
		Consumer<File> onSkinSelect = skinZipFile -> {
			try {
				System.out.println("Loading skin: " + skinZipFile.getName());
				Tuple3<RetroUIMainWindow, RetroUIEqualizerWindow, Void> retroUIWindows = new RetroUIFactory().construct(skinZipFile);
				if (RetroUIFactory.retroUIWindows != null) {
					retroUIWindows.getA().getWindow().setLocation(RetroUIFactory.retroUIWindows.getA().getWindow().getLocation());
					retroUIWindows.getA().getWindow().setSize(RetroUIFactory.retroUIWindows.getA().getWindow().getSize());
					retroUIWindows.getA().getWindow().setVisible(true);

					retroUIWindows.getB().getWindow().setLocation(RetroUIFactory.retroUIWindows.getB().getWindow().getLocation());
					retroUIWindows.getB().getWindow().setSize(RetroUIFactory.retroUIWindows.getB().getWindow().getSize());
					boolean eqVisible = RetroUIFactory.retroUIWindows.getB().getWindow().isVisible();
					retroUIWindows.getB().getWindow().setVisible(eqVisible);

					retroUIWindows.getA().btnEqToggle.setButtonOn(eqVisible);

					RetroUIFactory.retroUIWindows.getA().getWindow().setVisible(false);
					RetroUIFactory.retroUIWindows.getA().getWindow().dispose();
					RetroUIFactory.retroUIWindows.getB().getWindow().setVisible(false);
					RetroUIFactory.retroUIWindows.getB().getWindow().dispose();
				} else {
					retroUIWindows.getA().getWindow().setLocation(100, 100);
					retroUIWindows.getA().getWindow().setVisible(true);

					retroUIWindows.getB().getWindow().setLocation(100, 216);
					retroUIWindows.getB().getWindow().setVisible(true);
				}
				RetroUIFactory.retroUIWindows = retroUIWindows;

			} catch (Exception e) {
				e.printStackTrace();
			}
		};

		JFrame skinSelector = new JFrame();

		File skinsFolder = new File("/Users/mvmn/Downloads/winamp_skins");
		Set<String> skins = Stream.of(skinsFolder.listFiles())
				.filter(f -> !f.isDirectory() && f.getName().toLowerCase().endsWith(".wsz"))
				.map(File::getName)
				.collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(str -> str.toLowerCase()))));
		JList<String> skinList = new JList<>(skins.toArray(new String[skins.size()]));

		skinList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		skinList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				String skinFileName = skinList.getModel().getElementAt(skinList.getSelectedIndex());
				onSkinSelect.accept(new File(skinsFolder, skinFileName));
			}
		});

		skinSelector.getContentPane().setLayout(new BorderLayout());
		skinSelector.getContentPane().add(new JScrollPane(skinList), BorderLayout.CENTER);

		skinSelector.pack();
		skinSelector.setVisible(true);
	}
}
