package jape;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdgeSubdivision;

public class Builder {

	private final GeometryFactory geomFact = new GeometryFactory();

	private Polygon convertPath2DToPolygon(Path2D.Double path) {
		List<Coordinate> coordinates = new ArrayList<>();
		PathIterator iter = path.getPathIterator(null);
		while (!iter.isDone()) {
			double[] coords = new double[6];
			iter.currentSegment(coords);
			double x = coords[0];
			double y = coords[1];
			coordinates.add(new Coordinate(x, y));
			iter.next();
		}
		return geomFact.createPolygon(coordinates.toArray(new Coordinate[coordinates.size()]));
	}

	private QuadEdgeSubdivision convertPointsToQuadEdgeSubdivision(Set<Point2D.Double> points) {
		// convert input into jts objects
		Coordinate[] pointCoordinates = new Coordinate[points.size()];
		int i = 0;
		for (Point2D.Double point : points) {
			pointCoordinates[i] = new Coordinate(point.x, point.y);
			i++;
		}
		DelaunayTriangulationBuilder trigBuilder = new DelaunayTriangulationBuilder();
		trigBuilder.setSites(geomFact.createMultiPoint(pointCoordinates));
		// perform the analysis
		QuadEdgeSubdivision qes = trigBuilder.getSubdivision();
		return qes;
	}

	// TODO accept non-rectangular bounds
	public Set<Point2D.Double> createRandomPoints(int pointCount, long seed, Path2D.Double bounds) {
		double xMin = 0.0;
		double xMax = 0.0;
		double yMin = 0.0;
		double yMax = 0.0;
		Random rng = new Random(seed);
		Set<Point2D.Double> points = new HashSet<>();
		// convert input into jts objects
		Polygon container = convertPath2DToPolygon(bounds);
		// calculate min/max from bounds
		Coordinate[] contCoords = container.getCoordinates();
		System.out.println("foo " + contCoords.length);
		for (int i = 0; i < contCoords.length; i++) {
			if (i == 0 || contCoords[i].x < xMin)
				xMin = contCoords[i].x;
			if (i == 0 || contCoords[i].x > xMax)
				xMax = contCoords[i].x;
			if (i == 0 || contCoords[i].y < yMin)
				yMin = contCoords[i].y;
			if (i == 0 || contCoords[i].y > yMax)
				yMax = contCoords[i].y;
		}
		// don't incremenet i automatically
		// only done if point inside bounds
		for (int i = 0; i < pointCount;) {
			double x = (rng.nextDouble() * (xMax - xMin)) + xMin;
			double y = (rng.nextDouble() * (yMax - yMin)) + yMin;
			if (container.contains(geomFact.createPoint(new Coordinate(x, y)))) {
				System.out.println("Adding " + x + "," + y);
				points.add(new Point2D.Double(x, y));
				i++;
			} else {
				System.out.println("Failing " + x + "," + y);
			}
		}
		return points;
	}

	public Set<Point2D.Double> relax(Set<Point2D.Double> points, Path2D.Double bounds) {
		Set<Point2D.Double> pointsOut = new HashSet<>();
		// convert input into jts objects
		QuadEdgeSubdivision qes = convertPointsToQuadEdgeSubdivision(points);
		Polygon container = convertPath2DToPolygon(bounds);

		// for each voronoi polygon, use the centroid as a new point
		GeometryCollection voronoi = (GeometryCollection) qes.getVoronoiDiagram(geomFact);
		for (int i = 0; i < voronoi.getNumGeometries(); i++) {
			Polygon p = (Polygon) voronoi.getGeometryN(i);
			// only use the part of the polygon within bounds
			p = (Polygon) p.intersection(container);
			Coordinate coordinate = p.getCentroid().getCoordinate();
			pointsOut.add(new Point2D.Double(coordinate.x, coordinate.y));
		}
		return pointsOut;
	}

	public IslandMap buildData(Set<Point2D.Double> points, Path2D.Double bounds) {
		// convert input into jts objects

		QuadEdgeSubdivision qes = convertPointsToQuadEdgeSubdivision(points);
		Polygon container = convertPath2DToPolygon(bounds);
		return new IslandMap(qes, container);
	}

	public void run() {
		// setup border
		Path2D.Double bounds = new Path2D.Double(new Rectangle2D.Double(0.0, 0.0, 640.0, 480.0));
		// create some initial points
		Set<Point2D.Double> points = createRandomPoints(400, 42, bounds);
		// relax them
		points = relax(points, bounds);
		points = relax(points, bounds);
		points = relax(points, bounds);
		// assign land/water to corners
		// assign land/water to polygons
		IslandMap worldMap = buildData(points, bounds);

		display(worldMap, bounds);
	}

	public void display(IslandMap worldMap, Path2D.Double bounds) {

		// convert input into jts objects
		Polygon container = convertPath2DToPolygon(bounds);
		// calculate min/max from bounds
		Coordinate[] contCoords = container.getCoordinates();
		double xMin = Double.MAX_VALUE;
		double xMax = Double.MIN_VALUE;
		double yMin = Double.MAX_VALUE;
		double yMax = Double.MIN_VALUE;
		for (int i = 0; i < contCoords.length; i++) {
			if (contCoords[i].x < xMin)
				xMin = contCoords[i].x;
			if (contCoords[i].x > xMax)
				xMax = contCoords[i].x;
			if (contCoords[i].y < yMin)
				yMin = contCoords[i].y;
			if (contCoords[i].y > yMax)
				yMax = contCoords[i].y;
		}

		int xSize = (int) (xMax - xMin);
		int ySize = (int) (yMax - yMin);

		final BufferedImage img = new BufferedImage(xSize, ySize, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = (Graphics2D) img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.setColor(Color.white);
		g.fillRect(0,0, xSize,ySize);

		//poly edges
		for (Path2D.Double region : worldMap.getRegions()) {
			//if poly water
			if (worldMap.getIsSiteUnderwater(worldMap.getSiteOfRegion(region))) {
				g.setColor(Color.blue);
				g.fill(region);
			} else {
				//poly ground
				g.setColor(Color.green);
				g.fill(region);
			}
			g.setColor(Color.black);
			g.setStroke(new BasicStroke(1));
			g.draw(region);
		}
		//tri edges
		for (Point2D.Double orig: worldMap.getSites()) {
			for (Point2D.Double dest : worldMap.getLinkedSites(orig)) {
				g.setColor(Color.white);
				g.setStroke(new BasicStroke(1));
				int x1 = (int) orig.x;
				int y1 = (int) orig.y;
				int x2 = (int) dest.x;
				int y2 = (int) dest.y;
				g.drawLine(x1,y1, x2,y2);
			}
		}

		//points
		/*
		g.setColor(Color.black);
		g.setStroke(new BasicStroke(1));
		for (Point2D.Double site: worldMap.getSites()) {
			int x = (int) site.x;
			int y = (int) site.y;
			g.fillOval(x - 1, y - 1, 3, 3);
		}
		*/
		//outline
		g.setPaint(Color.black);
		g.setStroke(new BasicStroke(3));
		g.draw(bounds);

		final JFrame frame = new JFrame() {
			@Override
			public void paint(Graphics g) {
				g.drawImage(img, 25, 35, null);
			}
		};

		frame.setTitle("java fortune");
		frame.setVisible(true);
		frame.setSize(img.getWidth() + 50, img.getHeight() + 70);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

	public static void main(String[] args) {
		new Builder().run();
	}
}
