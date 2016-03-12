package jape;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdgeSubdivision;

public class IslandMap extends BasicMap {

	private Map<Point2D.Double, Boolean> pointWater = new HashMap<>();
	
	private double waterLevel = 0.0; //range -1.0 to 1.0
	
	public IslandMap(QuadEdgeSubdivision qes, Polygon container) {
		super(qes, container);
		
		OpenSimplexNoise osn = new OpenSimplexNoise();
		
		for (Path2D.Double region : getRegions()) {
			//see which corners are below water
			List<Point2D.Double> points = AWTGeomUtils.Path2DToPoint2DList(region);
			int underwaterCount = 0;
			for (Point2D.Double point : points) {
				Double height = osn.eval(point.x, point.y);
				//System.out.println(point+" : "+height);
				if (waterLevel > height) {
					pointWater.put(point, Boolean.TRUE);
					underwaterCount ++;
				} else {
					pointWater.put(point, Boolean.FALSE);
				}
			}
			//if more than half corners below water, 
			//polygon is below water
			double underwaterFraction = (double) underwaterCount / (double) points.size();
			Point2D.Double site = getSiteOfRegion(region);
			if (underwaterFraction > 0.5) {
				pointWater.put(site, Boolean.TRUE);
			} else {
				pointWater.put(site, Boolean.FALSE);				
			}
		}
	}

	public boolean getIsSiteUnderwater(Point2D.Double site) {
		return pointWater.get(site);
	}
}
