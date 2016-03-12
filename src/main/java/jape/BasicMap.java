package jape;

import java.awt.geom.Path2D;
import java.awt.geom.Path2D.Double;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdge;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdgeSubdivision;

import jape.graph.Graph;

public class BasicMap {

	/**
	 * Contained space is completely divided into regsion. Each region is a
	 * convex polygon. Each region contains one site.
	 */
	private final Set<Path2D.Double> regions = new HashSet<>();
	/**
	 * Points on the map. Each site is in one region.
	 */
	private final Set<Point2D.Double> sites = new HashSet<>();
	/**
	 * Mapping between one site to the one region that contains it.
	 */
	private final Map<Point2D.Double, Path2D.Double> siteToRegion = new HashMap<>();
	/**
	 * Mapping between one region and the one site it contains.
	 */
	private final Map<Path2D.Double, Point2D.Double> regionToSite = new HashMap<>();
	/**
	 * Mapping between one site and the neighbouring sites. Regions of neighbouring sites
	 * will share one edge between two vertexs.
	 */
	private final Map<Point2D.Double, Set<Point2D.Double>> siteToSites = new HashMap<>();

	public final double xMin;
	public final double xMax;
	public final double yMin;
	public final double yMax;
	
	public BasicMap(QuadEdgeSubdivision qes, Polygon container) {
		GeometryFactory geomFact = new GeometryFactory();
		GeometryCollection voronoi = (GeometryCollection) qes.getVoronoiDiagram(geomFact);
		for (int i = 0; i < voronoi.getNumGeometries(); i++) {
			Polygon p = (Polygon) voronoi.getGeometryN(i);
			Coordinate coord = (Coordinate) p.getUserData();
			Point2D.Double site = new Point2D.Double(coord.x, coord.y);
			sites.add(site);
			// only use the part of the polygon within bounds
			p = (Polygon) p.intersection(container);
			// get X,Y position of each vertex of the polygon
			Coordinate[] pcoord = p.getCoordinates();
			// convert the position coodinates into a path object
			Path2D.Double region = new Path2D.Double();
			for (int j = 0; j < pcoord.length; j++) {
				if (j == 0) {
					region.moveTo(pcoord[j].x, pcoord[j].y);
				} else {
					region.lineTo(pcoord[j].x, pcoord[j].y);
				}
			}
			region.closePath();
			regions.add(region);
			siteToRegion.put(site, region);
			regionToSite.put(region, site);
		}
		// go through the triangles and link neighbours
		List<QuadEdge> edges = qes.getPrimaryEdges(false);
		for (QuadEdge edge : edges) {
			Point2D.Double orig = new Point2D.Double(edge.orig().getX(), edge.orig().getY());
			Point2D.Double dest = new Point2D.Double(edge.dest().getX(), edge.dest().getY());
			// check both orig and dest are valid sites
			if (!sites.contains(orig))
				throw new IllegalArgumentException("Trying to link from invalid site " + orig);
			if (!sites.contains(dest))
				throw new IllegalArgumentException("Trying to link to invalid site " + dest);

			// check the polys meet
			// since we are trimming polys to bounds, sometimes polys may only have
			// joined edges out of bounds
			Path2D.Double origPath = getRegionOfSite(orig);
			Path2D.Double destPath = getRegionOfSite(dest);

			Set<Point2D.Double> origCorners = new HashSet<>();
			origCorners.addAll(AWTGeomUtils.Path2DToPoint2DList(origPath));
			origCorners.retainAll(AWTGeomUtils.Path2DToPoint2DList(destPath));
			if (origCorners.size() >= 2) {
				// if they share at least two corners, must share an edge
				// therefore link them
				if (!siteToSites.containsKey(orig)) {
					siteToSites.put(orig, new HashSet<>());
				}
				siteToSites.get(orig).add(dest);
			}
		}
	}

	public Set<Path2D.Double> getRegions() {
		return Collections.unmodifiableSet(regions);
	}

	public Set<Point2D.Double> getSites() {
		return Collections.unmodifiableSet(sites);
	}

	public Path2D.Double getRegionOfSite(Point2D.Double site) {
		return siteToRegion.get(site);
	}

	public Point2D.Double getSiteOfRegion(Path2D.Double region) {
		return regionToSite.get(region);
	}

	public Set<Point2D.Double> getLinkedSites(Point2D.Double orig) {
		if (orig == null)
			throw new IllegalArgumentException("orig cannot be null");
		if (!sites.contains(orig))
			throw new IllegalArgumentException("orig must be a site");
		if (!siteToSites.containsKey(orig))
			return Collections.unmodifiableSet(Collections.emptySet());
		return Collections.unmodifiableSet(siteToSites.get(orig));
	}
}
