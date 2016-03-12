package jape;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class AWTGeomUtils {

	
	public static List<Point2D.Double> Path2DToPoint2DList(final Path2D.Double path) {
		List<Point2D.Double> toReturn = new ArrayList<>();
		PathIterator origPathIterator = path.getPathIterator(null);
		while (!origPathIterator.isDone()) {
			float[] coords = new float[6];
			int segType = origPathIterator.currentSegment(coords);
			if (segType != PathIterator.SEG_CLOSE) {
				toReturn.add(new Point2D.Double(coords[0], coords[1]));
			}
			origPathIterator.next();
		}
		return toReturn;
	}
}
