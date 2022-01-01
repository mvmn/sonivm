package x.mvmn.sonivm.ui.retro.rasterui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JLabel;

import lombok.Setter;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;

public class RasterUITextComponent extends RasterUIComponent {

	protected final Color backgroundColor;
	protected final Color textColor;
	@Setter
	protected volatile int rollTextOffset;
	protected volatile String text = "";
	protected volatile int xOffset;
	protected volatile Font font;
	protected volatile Rectangle textBounds;
	protected final int originalWidth;
	protected final int originalHeight;

	protected volatile BufferedImage image;

	public RasterUITextComponent(RasterGraphicsWindow parent,
			Color backgroundColor,
			Color textColor,
			int width,
			int height,
			int x,
			int y,
			int rollTextOffset) {
		super(parent, new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), x, y);
		this.image = super.image;
		this.originalWidth = width;
		this.originalHeight = height;
		this.backgroundColor = backgroundColor;
		this.textColor = textColor;
		this.rollTextOffset = rollTextOffset;
	}

	protected void render() {
		if (font == null) {
			updateFont();
		}

		Graphics2D g = this.image.createGraphics();
		g.setColor(backgroundColor);
		g.fillRect(0, 0, this.image.getWidth(), this.image.getHeight());
		g.setColor(textColor);
		g.setPaint(textColor);
		g.setFont(font);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		textBounds = g.getFontMetrics(font).getStringBounds(text, g).getBounds();
		int xOffset = this.xOffset;
		g.drawString(text, -xOffset, textBounds.height - textBounds.height / 4);
		if (rollTextOffset >= 0) {
			while (textBounds.width - xOffset < this.image.getWidth()) {
				xOffset -= textBounds.width + rollTextOffset;
				g.drawString(text, -xOffset, textBounds.height - textBounds.height / 4);
			}
		}
		g.dispose();
		super.repaint();
	}

	public void setText(String text) {
		this.text = text != null ? text : "";
		render();
	}

	public void setOffset(int offset) {
		this.xOffset = offset;
		render();
	}

	public int getOffset() {
		return this.xOffset;
	}

	public int getTextFullWidth() {
		return font != null && !text.isEmpty() ? textBounds.width : 0;
	}

	@Override
	public boolean isAutoScaled() {
		return true;
	}

	@Override
	public void setScale(double scale) {
		int newWidth = (int) Math.round(scale * originalWidth);
		int newHeight = (int) Math.round(scale * originalHeight);
		if (newWidth != this.image.getWidth()) {
			image = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
			updateFont();
			render();
		}
	}

	protected void updateFont() {
		font = SwingUtil.resizeToPx(new JLabel().getFont().deriveFont(Font.BOLD), this.image.getHeight() + 2, this.image.getGraphics());
	}

	@Override
	public BufferedImage getImage() {
		return image;
	}

	@Override
	public int getWidth() {
		return image.getWidth();
	}

	@Override
	public int getHeight() {
		return image.getHeight();
	}
}
