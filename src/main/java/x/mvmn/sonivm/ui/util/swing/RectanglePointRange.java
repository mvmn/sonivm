package x.mvmn.sonivm.ui.util.swing;

import java.awt.Point;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
public class RectanglePointRange {

	protected final int left;
	protected final int top;
	protected final int right;
	protected final int bottom;

	public RectanglePointRange(Point topLeft, Point bottomRight) {
		this.left = topLeft.x;
		this.top = topLeft.y;
		this.right = bottomRight.x;
		this.bottom = bottomRight.y;
	}

	public boolean inRange(int x, int y) {
		return x >= left && x <= right && y >= top && y <= bottom;
	}

	public boolean inRange(Point point) {
		return inRange(point.x, point.y);
	}
}
