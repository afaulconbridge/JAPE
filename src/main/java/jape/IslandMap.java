package jape;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdgeSubdivision;

public class IslandMap extends BasicMap {

	private Map<Coordinate, Boolean> pointWater = new HashMap<>();
	
	private double waterLevel = 0.0; //range -1.0 to 1.0
	
	public IslandMap(QuadEdgeSubdivision qes, Polygon container) {
		super(qes, container);
		
		OpenSimplexNoise osn = new OpenSimplexNoise();

		LinearRing perimeter = (LinearRing) container.getExteriorRing();
		for (Polygon region : getRegions()) {
			//see which corners are below water
			int underwaterCount = 0;
			for (Coordinate point : region.getCoordinates()) {
				Double height = osn.eval(point.x, point.y);
				double distanceFromPerimeter = perimeter.distance(container.getFactory().createPoint(point));
				if (distanceFromPerimeter < 100.0) {
					height = Math.min(height, (distanceFromPerimeter/50.0)-1.0);
				}
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
			double underwaterFraction = (double) underwaterCount / (double) region.getCoordinates().length;
			Coordinate site = getSiteOfRegion(region);
			if (underwaterFraction > 0.5) {
				pointWater.put(site, Boolean.TRUE);
			} else {
				pointWater.put(site, Boolean.FALSE);				
			}
		}
	}

	public boolean getIsSiteUnderwater(Coordinate site) {
		return pointWater.get(site);
	}
}
