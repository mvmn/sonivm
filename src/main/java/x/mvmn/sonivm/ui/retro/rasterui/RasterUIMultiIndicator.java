package x.mvmn.sonivm.ui.retro.rasterui;

import java.awt.image.BufferedImage;

import lombok.Getter;

public class RasterUIMultiIndicator extends RasterUIComponent {

	protected final BufferedImage[] stateImages;
	@Getter
	protected volatile int state = 0;

	public RasterUIMultiIndicator(RasterGraphicsWindow parent, BufferedImage[] stateImages) {
		super(parent, stateImages[0]);
		this.stateImages = stateImages;
	}

	public RasterUIMultiIndicator(RasterGraphicsWindow parent, BufferedImage[] stateImages, int x, int y) {
		super(parent, stateImages[0], x, y);
		this.stateImages = stateImages;
	}

	@Override
	public BufferedImage getImage() {
		return stateImages[state];
	}

	public void setState(int state) {
		this.state = Math.min(Math.max(0, state), stateImages.length - 1);
	}
}
