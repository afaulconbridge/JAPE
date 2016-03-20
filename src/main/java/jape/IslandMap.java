package jape;

import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdgeSubdivision;

public class IslandMap extends BasicMap {

	private Map<Coordinate, Boolean> pointWater = new HashMap<>();
	
	private final double heightPropScalingPower = 2.0;
	private final double waterLevel = 0.0; //range -1.0 to 1.0
	
	public IslandMap(Polygon container) {
		super(container);
	}

	public boolean getIsSiteUnderwater(Coordinate site) {
		return pointWater.get(site);
	}
	
	protected void setup(QuadEdgeSubdivision qes) {
		super.setup(qes);
		
		OpenSimplexNoise osn = new OpenSimplexNoise();

		LinearRing perimeter = (LinearRing) boundary.getExteriorRing();
		for (Polygon region : getRegions()) {
			//see which corners are below water
			int underwaterCount = 0;
			for (Coordinate coord : region.getCoordinates()) {
				Double height = osn.eval(coord.x, coord.y);
				Point point = geomFact.createPoint(coord);
				double distanceFromPerimeter = perimeter.distance(point);
				double distanceFromCenter = perimeter.getCentroid().distance(point);
				double distanceTotal = distanceFromPerimeter+distanceFromCenter;
				double distanceProp = distanceFromCenter/distanceTotal;

				height = (height/2.0)+0.5;
				//drop the height range by the square of the proportion of the distance to centre
				//i.e. make middle high and edges low
				height = height-Math.pow(distanceProp, heightPropScalingPower);
				
				if (waterLevel > height) {
					pointWater.put(coord, Boolean.TRUE);
					underwaterCount ++;
				} else {
					pointWater.put(coord, Boolean.FALSE);
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
	
	public static IslandMap build(QuadEdgeSubdivision qes, Polygon boundary) {
		IslandMap map  = new IslandMap(boundary);
		map.setup(qes);
		return map;
	}
}
