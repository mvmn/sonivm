package x.mvmn.sonivm.ui.retro;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.ini4j.Wini;

public class WinAmpSkinUtil {

	public static BufferedImage createTransparencyMask(int width, int height, String numPoints, String pointList) {
		BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		if (numPoints != null && pointList != null) {
			Graphics2D graphics = mask.createGraphics();
			graphics.setColor(new Color(0, 0, 0, 0));
			graphics.drawRect(0, 0, width, height);
			graphics.dispose();

			int[] numbersOfPoints = Stream.of(numPoints.split("\\s*,\\s*")).mapToInt(Integer::parseInt).toArray();
			String[] pointNumStrs = pointList.split("\\s*,\\s*|\\s+");
			List<Point> points = new ArrayList<>();
			boolean x = true;
			int v = 0;
			for (int i = 0; i < pointNumStrs.length; i++) {
				if (x) {
					v = Integer.parseInt(pointNumStrs[i]);
				} else {
					points.add(new Point(v, Integer.parseInt(pointNumStrs[i])));
				}
				x = !x;
			}
			for (int numberOfPoints : numbersOfPoints) {
				if (points.size() >= numberOfPoints) {
					generateMaskRegion(mask, points.subList(0, numberOfPoints));
					if (numberOfPoints <= points.size()) {
						points = points.subList(numberOfPoints, points.size());
					} else {
						break;
					}
				} else {
					break;
				}
			}
		} else {
			Graphics2D graphics = mask.createGraphics();
			graphics.setColor(new Color(255, 255, 255, 255));
			graphics.drawRect(0, 0, width, height);
			graphics.dispose();
		}
		return mask;
	}

	private static void generateMaskRegion(BufferedImage image, List<Point> points) {
		if (points != null && !points.isEmpty()) {
			Graphics2D graphics = image.createGraphics();
			graphics.setColor(new Color(255, 255, 255, 255));
			int[] xes = points.stream().mapToInt(p -> p.x).toArray();
			int[] ys = points.stream().mapToInt(p -> p.y).toArray();
			graphics.fillPolygon(xes, ys, points.size());
			graphics.dispose();
		}
	}

	public static Color getColor(Wini skinIni, String section, String property, Color fallback) {
		Color result = fallback;
		if (skinIni != null) {
			String bgColor = skinIni.get(section, property);
			if (bgColor != null && bgColor.trim().matches("^#[A-F0-9]{6}$")) {
				String color = bgColor.trim().substring(1);
				int red = Integer.parseInt(color.substring(0, 2), 16);
				int green = Integer.parseInt(color.substring(2, 4), 16);
				int blue = Integer.parseInt(color.substring(4, 6), 16);
				result = new Color(red, green, blue);
			}
		}
		return result;
	}
}
