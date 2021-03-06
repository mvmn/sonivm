package x.mvmn.sonivm.ui.retro.rasterui;

import java.awt.image.BufferedImage;

import lombok.Getter;
import lombok.Setter;

public class RasterUIBooleanIndicator extends RasterUIComponent {

	protected final BufferedImage imageInactive;
	@Getter
	@Setter
	protected volatile boolean active = false;

	public RasterUIBooleanIndicator(RasterGraphicsWindow parent, BufferedImage imageActive, BufferedImage imageInactive) {
		super(parent, imageActive);
		this.imageInactive = imageInactive;
	}

	public RasterUIBooleanIndicator(RasterGraphicsWindow parent, BufferedImage imageActive, BufferedImage imageInactive, int x, int y) {
		super(parent, imageActive, x, y);
		this.imageInactive = imageInactive;
	}

	@Override
	public BufferedImage getImage() {
		return active ? image : imageInactive;
	}
}
