package x.mvmn.sonivm.ui.retro.rasterui;

import java.awt.image.BufferedImage;

import lombok.Getter;
import lombok.Setter;

public class RasterUIHeading extends RasterUIComponent {

	protected final BufferedImage imageUnfocused;
	@Getter
	@Setter
	protected volatile boolean focused = false;

	public RasterUIHeading(RasterGraphicsWindow parent, BufferedImage imageFocused, BufferedImage imageUnfocused) {
		super(parent, imageFocused);
		this.imageUnfocused = imageUnfocused;
	}

	public RasterUIHeading(RasterGraphicsWindow parent, BufferedImage imageFocused, BufferedImage imageUnfocused, int x, int y) {
		super(parent, imageFocused, x, y);
		this.imageUnfocused = imageUnfocused;
	}

	@Override
	public BufferedImage getImage() {
		return focused ? image : imageUnfocused;
	}
}
