package x.mvmn.sonivm.ui.retro.rasterui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JLabel;

import x.mvmn.sonivm.ui.util.swing.SwingUtil;

public class RasterUITextComponent extends RasterUIComponent {

	protected final Color backgroundColor;
	protected final Color textColor;
	protected final int rollTextOffset;
	protected volatile String text = "";
	protected volatile int xOffset;
	protected volatile Font font;
	protected volatile Rectangle textBounds;

	public RasterUITextComponent(RasterGraphicsWindow parent,
			Color backgroundColor,
			Color textColor,
			int width,
			int height,
			int x,
			int y,
			int rollTextOffset) {
		super(parent, new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), x, y);
		this.backgroundColor = backgroundColor;
		this.textColor = textColor;
		this.rollTextOffset = rollTextOffset;
	}

	protected void render() {
		if (font == null) {
			font = SwingUtil.resizeToPx(new JLabel().getFont().deriveFont(Font.BOLD), this.image.getHeight() + 2, this.image.getGraphics());
		}

		Graphics2D g = this.image.createGraphics();
		g.setColor(backgroundColor);
		g.fillRect(0, 0, this.image.getWidth(), this.image.getHeight());
		g.setColor(textColor);
		g.setPaint(textColor);
		g.setFont(font);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		textBounds = g.getFontMetrics(font).getStringBounds(text, g).getBounds();
		g.drawString(text, -xOffset, textBounds.height - 2);
		if (rollTextOffset >= 0) {
			while (textBounds.width - xOffset < this.image.getWidth()) {
				xOffset -= textBounds.width + rollTextOffset;
				g.drawString(text, -xOffset, textBounds.height - 2);
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
}
