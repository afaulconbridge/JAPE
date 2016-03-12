package jape;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

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
	
	public static Path2D.Double polygonToPath2D(Polygon polygon) {
		Path2D.Double path = new Path2D.Double();
		LinearRing ring = (LinearRing) polygon.getExteriorRing();
		Coordinate[] coords = ring.getCoordinates();
		for (int i = 0; i < coords.length; i++) {
			if (i == 0) {
				path.moveTo(coords[i].x, coords[i].y);
			} else {
				path.lineTo(coords[i].x, coords[i].y);
			}
		}
		path.closePath();
		return path;
	}
}
