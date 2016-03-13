package jape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdge;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdgeSubdivision;

import jape.graph.Graph;

public class BasicMap {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	/**
	 * Contained space is completely divided into regsion. Each region is a
	 * convex polygon. Each region contains one site.
	 */
	private final Set<Polygon> regions = new HashSet<>();
	/**
	 * Points on the map. Each site is in one region.
	 */
	private final Set<Coordinate> sites = new HashSet<>();
	/**
	 * Mapping between one site to the one region that contains it.
	 */
	private final Map<Coordinate, Polygon> siteToRegion = new HashMap<>();
	/**
	 * Mapping between one region and the one site it contains.
	 */
	private final Map<Polygon, Coordinate> regionToSite = new HashMap<>();
	/**
	 * Mapping between one site and the neighbouring sites. Regions of neighbouring sites
	 * will share one edge between two vertexs.
	 */
	private final Map<Coordinate, Set<Coordinate>> siteToSites = new HashMap<>();
	
	private final Map<ImmutableList<Coordinate>, Coordinate> siteToSiteMidpoints = new HashMap<>();

	private final Polygon boundary;
	
	public BasicMap(QuadEdgeSubdivision qes, Polygon boundary) {
		this.boundary = boundary;
		GeometryFactory geomFact = new GeometryFactory();
		GeometryCollection voronoi = (GeometryCollection) qes.getVoronoiDiagram(geomFact);
		for (int i = 0; i < voronoi.getNumGeometries(); i++) {
			Polygon p = (Polygon) voronoi.getGeometryN(i);
			Coordinate coord = (Coordinate) p.getUserData();
			Coordinate site = new Coordinate(coord.x, coord.y);
			sites.add(site);
			// only use the part of the polygon within bounds
			Polygon region = (Polygon) p.intersection(boundary);
			regions.add(region);
			siteToRegion.put(site, region);
			regionToSite.put(region, site);
		}
		// go through the triangles and link neighbours
		List<QuadEdge> quadEdges = qes.getPrimaryEdges(false);
		for (QuadEdge quadEdge : quadEdges) {
			Coordinate orig = new Coordinate(quadEdge.orig().getX(), quadEdge.orig().getY());
			Coordinate dest = new Coordinate(quadEdge.dest().getX(), quadEdge.dest().getY());
			
			// check both orig and dest are valid sites
			if (!sites.contains(orig))
				throw new IllegalArgumentException("Trying to link from invalid site " + orig);
			if (!sites.contains(dest))
				throw new IllegalArgumentException("Trying to link to invalid site " + dest);

			if (!boundary.contains(geomFact.createPoint(orig)))
				continue;
			if (!boundary.contains(geomFact.createPoint(dest)))
				continue;
			
			Polygon origPath = getRegionOfSite(orig);
			Polygon destPath = getRegionOfSite(dest);

			// check the polys meet
			// since we are trimming polys to bounds, sometimes polys may only have
			// joined edges out of bounds
			Set<Coordinate> origCorners = new HashSet<>();
			origCorners.addAll(Arrays.asList(origPath.getCoordinates()));
			origCorners.retainAll(Arrays.asList(destPath.getCoordinates()));
			if (origCorners.size() >= 2) {
				// if they share at least two corners, must share an edge
				// therefore link them
				if (!siteToSites.containsKey(orig)) {
					siteToSites.put(orig, new HashSet<>());
				}
				siteToSites.get(orig).add(dest);

				//handle the inverse too
				if (!siteToSites.containsKey(dest)) {
					siteToSites.put(dest, new HashSet<>());
				}
				siteToSites.get(dest).add(orig);
			}
			
			//store the midpoint of the edge
			//first check the edge comes to within the bounds
			Coordinate[] edgeCoords = new Coordinate[2];
			edgeCoords[0] = quadEdge.rot().orig().getCoordinate();
			edgeCoords[1] = quadEdge.rot().dest().getCoordinate();
			LineString polyEdge = geomFact.createLineString(edgeCoords);
			if (boundary.intersects(polyEdge)) {
				polyEdge = (LineString) boundary.intersection(polyEdge);
	
				double midPointX = (polyEdge.getCoordinateN(0).x+polyEdge.getCoordinateN(1).x)/2.0;
				double midPointY = (polyEdge.getCoordinateN(0).y+polyEdge.getCoordinateN(1).y)/2.0;
				Coordinate midPoint = new Coordinate(midPointX, midPointY);
				siteToSiteMidpoints.put(ImmutableList.of(orig, dest), midPoint);
				siteToSiteMidpoints.put(ImmutableList.of(dest, orig), midPoint);
			}
			
		}
		log.info("siteToSites length = "+siteToSites.keySet().size());
	}

	public Polygon getBoundary() {
		return boundary;
	}

	public Set<Polygon> getRegions() {
		return Collections.unmodifiableSet(regions);
	}

	public Set<Coordinate> getSites() {
		return Collections.unmodifiableSet(sites);
	}

	public Polygon getRegionOfSite(Coordinate site) {
		return siteToRegion.get(site);
	}

	public Coordinate getSiteOfRegion(Polygon region) {
		return regionToSite.get(region);
	}

	public Set<Coordinate> getLinkedSites(Coordinate orig) {
		if (orig == null)
			throw new IllegalArgumentException("orig cannot be null");
		if (!sites.contains(orig))
			throw new IllegalArgumentException("orig must be a site");
		if (!siteToSites.containsKey(orig))
			return Collections.unmodifiableSet(Collections.emptySet());
		return Collections.unmodifiableSet(siteToSites.get(orig));
	}
	
	public Coordinate getSiteToSiteMidpoint(Coordinate orig, Coordinate dest) {
		return siteToSiteMidpoints.get(ImmutableList.of(orig, dest));
	}
}
