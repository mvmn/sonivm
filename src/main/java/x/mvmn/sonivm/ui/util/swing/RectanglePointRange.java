package x.mvmn.sonivm.ui.util.swing;

import java.awt.Dimension;
import java.awt.Point;

import lombok.Data;

@Data
public class RectanglePointRange implements RectLocationAndSize {

	protected final int left;
	protected final int top;
	protected final int right;
	protected final int bottom;
	protected final int width;
	protected final int height;

	public RectanglePointRange(RectLocationAndSize source) {
		this(source.getLeft(), source.getTop(), source.getRight(), source.getBottom());
	}

	public RectanglePointRange(int left, int top, int right, int bottom) {
		if (left > right) {
			throw new IllegalArgumentException("Left after right");
		}
		if (top > bottom) {
			throw new IllegalArgumentException("Top below bottom");
		}
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
		this.width = this.right - this.left;
		this.height = this.bottom - this.top;
	}

	public RectanglePointRange(Point topLeft, Point bottomRight) {
		this(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);
	}

	public boolean inRange(int x, int y) {
		return x >= left && x <= right && y >= top && y <= bottom;
	}

	public boolean inRange(Point point) {
		return inRange(point.x, point.y);
	}

	public static RectanglePointRange of(Point location, Dimension size) {
		return new RectanglePointRange(location.x, location.y, location.x + size.width, location.y + size.height);
	}
}
