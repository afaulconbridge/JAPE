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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdge;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdgeSubdivision;

import jape.graph.Graph;

public class BasicMap {

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
		List<QuadEdge> edges = qes.getPrimaryEdges(false);
		for (QuadEdge edge : edges) {
			Coordinate orig = new Coordinate(edge.orig().getX(), edge.orig().getY());
			Coordinate dest = new Coordinate(edge.dest().getX(), edge.dest().getY());
			// check both orig and dest are valid sites
			if (!sites.contains(orig))
				throw new IllegalArgumentException("Trying to link from invalid site " + orig);
			if (!sites.contains(dest))
				throw new IllegalArgumentException("Trying to link to invalid site " + dest);

			// check the polys meet
			// since we are trimming polys to bounds, sometimes polys may only have
			// joined edges out of bounds
			Polygon origPath = getRegionOfSite(orig);
			Polygon destPath = getRegionOfSite(dest);

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
			}
		}
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
}
