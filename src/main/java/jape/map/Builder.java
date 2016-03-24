package jape.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdgeSubdivision;

public class Builder {

	private final GeometryFactory geomFact = new GeometryFactory();

	private QuadEdgeSubdivision convertPointsToQuadEdgeSubdivision(Set<Coordinate> points) {
		// convert input into jts objects
		Coordinate[] pointCoordinates = new Coordinate[points.size()];
		int i = 0;
		for (Coordinate point : points) {
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
	public Set<Coordinate> createRandomPoints(int pointCount, long seed, Polygon container) {
		double xMin = 0.0;
		double xMax = 0.0;
		double yMin = 0.0;
		double yMax = 0.0;
		Random rng = new Random(seed);
		Set<Coordinate> points = new HashSet<>();
		// calculate min/max from bounds
		Coordinate[] contCoords = container.getCoordinates();
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
		// don't increment i automatically
		// only done if point inside bounds
		// TODO in theory with a weird shape this could take too long
		for (int i = 0; i < pointCount;) {
			double x = (rng.nextDouble() * (xMax - xMin)) + xMin;
			double y = (rng.nextDouble() * (yMax - yMin)) + yMin;
			if (container.contains(geomFact.createPoint(new Coordinate(x, y)))) {
				// System.out.println("Adding " + x + "," + y);
				points.add(new Coordinate(x, y));
				i++;
			} else {
				// System.out.println("Failing " + x + "," + y);
			}
		}
		return points;
	}

	public Set<Coordinate> relax(Set<Coordinate> points, Polygon container) {
		Set<Coordinate> pointsOut = new HashSet<>();
		// convert input into jts objects
		QuadEdgeSubdivision qes = convertPointsToQuadEdgeSubdivision(points);

		// for each voronoi polygon, use the centroid as a new point
		GeometryCollection voronoi = (GeometryCollection) qes.getVoronoiDiagram(geomFact);
		for (int i = 0; i < voronoi.getNumGeometries(); i++) {
			Polygon p = (Polygon) voronoi.getGeometryN(i);
			// only use the part of the polygon within bounds
			p = (Polygon) p.intersection(container);
			Coordinate coordinate = p.getCentroid().getCoordinate();
			pointsOut.add(new Coordinate(coordinate.x, coordinate.y));
		}
		return pointsOut;
	}

	public IslandMap buildData(Set<Coordinate> points, Polygon container) {
		// convert input into jts objects

		QuadEdgeSubdivision qes = convertPointsToQuadEdgeSubdivision(points);
		return IslandMap.build(qes, container);
	}

	public void run() {
		// setup border
		Polygon bounds = geomFact.createPolygon(new Coordinate[] { new Coordinate(0.0, 0.0), new Coordinate(0.0, 480.0),
				new Coordinate(640.0, 480.0), new Coordinate(640.0, 0.0), new Coordinate(0.0, 0.0) });
		// create some initial points
		Set<Coordinate> points = createRandomPoints(400, 42, bounds);
		// relax them
		points = relax(points, bounds);
		points = relax(points, bounds);
		points = relax(points, bounds);
		// assign land/water to corners
		// assign land/water to polygons
		IslandMap worldMap = buildData(points, bounds);

		display(worldMap, bounds);
	}

	public void display(IslandMap worldMap, Polygon container) {
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

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
		BufferedImage img = gc.createCompatibleImage(xSize, ySize);

		Graphics2D g = (Graphics2D) img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g.setColor(Color.white);
		g.fillRect(0, 0, xSize, ySize);

		// poly edges
		for (Polygon region : worldMap.getRegions()) {
			g.setColor(getHeightColor(worldMap.getHeightOfSite(worldMap.getSiteOfRegion(region))));
			g.fill(AWTGeomUtils.polygonToPath2D(region));

			g.setColor(Color.black);
			g.setStroke(new BasicStroke(1));
			g.draw(AWTGeomUtils.polygonToPath2D(region));
		}
		// tri edges
		for (Coordinate orig : worldMap.getSites()) {
			for (Coordinate dest : worldMap.getLinkedSites(orig)) {
				Coordinate mid = worldMap.getSiteToSiteMidpoint(orig, dest);

				g.setColor(Color.white);
				g.setStroke(new BasicStroke(1));
				int x1 = (int) orig.x;
				int y1 = (int) orig.y;
				int xm = (int) mid.x;
				int ym = (int) mid.y;
				int x2 = (int) dest.x;
				int y2 = (int) dest.y;
				g.drawLine(x1, y1, xm, ym);
				g.drawLine(xm, ym, x2, y2);
			}
		}

		// points
		/*
		 * g.setColor(Color.black); g.setStroke(new BasicStroke(1)); for
		 * (Coordinate site: worldMap.getSites()) { int x = (int) site.x; int y
		 * = (int) site.y; g.fillOval(x - 1, y - 1, 3, 3); }
		 */
		// outline
		g.setPaint(Color.black);
		g.setStroke(new BasicStroke(3));
		g.draw(AWTGeomUtils.polygonToPath2D(container));

		JFrame frame = new JFrame();
		frame.getContentPane().add(new JScrollPane(new JLabel(new ImageIcon(img))));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private Color getHeightColor(double height) {
		if (height < 0.0) {
			// underwater
			float r = 0.0f;
			float g = 0.0f;
			float b = (float) (1.0 +height);
			return new Color(r, g, b);
		} else {
			// above ground
			// interpolate from 0,255,0 to 245,222,173
			int r = (int) ((245 * height) + (0 * (1.0 - height)));
			int g = (int) ((222 * height) + (255 * (1.0 - height)));
			int b = (int) ((173 * height) + (0 * (1.0 - height)));
			return new Color(r, g, b);
		}
	}

	public static void main(String[] args) {
		new Builder().run();
	}
}
