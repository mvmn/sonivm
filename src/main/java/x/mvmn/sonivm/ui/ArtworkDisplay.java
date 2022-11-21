package x.mvmn.sonivm.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.ToolTipManager;

import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.StandardArtwork;

import x.mvmn.sonivm.ui.util.swing.SwingUtil;

public class ArtworkDisplay extends JComponent {

	private volatile Image img;
	private volatile JToolTip toolTip;

	@Override
	protected void paintComponent(Graphics g) {
		if (img != null) {
			g.drawImage(img, 0, 0, null);
		} else {
			g.setColor(this.getBackground());
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			g.setColor(this.getForeground());
			g.drawRect(0, 0, this.getWidth(), this.getHeight());
		}
	}

	public void set(Artwork artwork) {
		this.img = null;
		this.toolTip = null;

		if (artwork != null && artwork instanceof StandardArtwork) {
			try {
				BufferedImage originalImage = (BufferedImage) artwork.getImage();
				if (originalImage != null) {
					Image tooltipImage = originalImage;
					Rectangle screenBounds = SwingUtil.getDefaultScreenBounds();
					if (originalImage.getWidth() > screenBounds.width || originalImage.getHeight() > screenBounds.height) {

						double widthRatio = (screenBounds.width - 100) / originalImage.getWidth();
						double heightRatio = (screenBounds.height - 100) / originalImage.getHeight();
						double ratio = Math.min(widthRatio, heightRatio);

						tooltipImage = originalImage.getScaledInstance((int) (originalImage.getWidth() * ratio),
								(int) (originalImage.getHeight() * ratio), Image.SCALE_FAST);
					}
					Image finalTooltipImage = tooltipImage;
					Dimension imgDimension = new Dimension(tooltipImage.getWidth(null), tooltipImage.getHeight(null));
					this.img = originalImage.getScaledInstance(this.getWidth(), this.getHeight(), Image.SCALE_SMOOTH);
					this.toolTip = new JToolTip() {
						@Override
						public Dimension getPreferredSize() {
							return imgDimension;
						}

						@Override
						public Dimension getMinimumSize() {
							return imgDimension;
						}

						@Override
						protected void paintComponent(Graphics g) {
							g.drawImage(finalTooltipImage, 0, 0, null);
						}
					};
					this.toolTip.setComponent(this);
					this.toolTip.setSize(imgDimension);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (this.toolTip != null) {
			ToolTipManager.sharedInstance().registerComponent(this);
		} else {
			ToolTipManager.sharedInstance().unregisterComponent(this);
		}
		this.setToolTipText("");
	}

	@Override
	public void setPreferredSize(Dimension preferredSize) {
		super.setPreferredSize(new Dimension(preferredSize.height, preferredSize.height));
	}

	@Override
	public void setSize(int width, int height) {
		super.setSize(height, height);
	}

	@Override
	public JToolTip createToolTip() {
		return toolTip;
	}
}
