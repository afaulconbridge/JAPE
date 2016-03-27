package jape;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.event.MouseInputAdapter;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import jape.map.AWTGeomUtils;
import jape.map.Builder;
import jape.map.IslandMap;

public class GraphTest {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private IslandMap worldMap;
	private SimpleDirectedWeightedGraph<Coordinate, DefaultWeightedEdge> graph;
	private AStarBuilder<Coordinate, DefaultWeightedEdge> aStarBuilder;

	private Integer mouseX = null;
	private Integer mouseY = null;
	private Coordinate start = null;

	private class MyMouseInputAdapter extends MouseInputAdapter {
		@Override
		public void mouseExited(MouseEvent e) {
			mouseX = null;
			mouseY = null;
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			mouseX = e.getX();
			mouseY = e.getY();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			mouseX = e.getX();
			mouseY = e.getY();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			mouseX = e.getX();
			mouseY = e.getY();
		}

		@Override
		public void mousePressed(MouseEvent e) {
			mouseX = e.getX();
			mouseY = e.getY();
			if (e.getButton() == MouseEvent.BUTTON1) {
				// left click selects
				start = getCoordinateClosest(mouseX, mouseY);
			} else if (e.getButton() == MouseEvent.BUTTON3) {
				// right click clears
				start = null;
			}
		}
	}

	private class EuclidianHeuristic implements Heuristic<Coordinate> {
		@Override
		public double getCostEstimate(Coordinate source, Coordinate target) {
			return Math.sqrt(Math.pow(source.x - target.x, 2) + Math.pow(source.y - target.y, 2));
		}
	}

	public static void main(String[] args) {
		new GraphTest().run();
	}

	public void run() {

		Builder builder = new Builder();
		GeometryFactory geomFact = new GeometryFactory();
		Polygon bounds = geomFact.createPolygon(new Coordinate[] { new Coordinate(0.0, 0.0), new Coordinate(0.0, 800.0),
				new Coordinate(800.0, 800.0), new Coordinate(800.0, 0.0), new Coordinate(0.0, 0.0) });
		// create some initial points
		Set<Coordinate> points = builder.createRandomPoints(400, 42, bounds);
		// relax them
		points = builder.relax(points, bounds);
		points = builder.relax(points, bounds);
		points = builder.relax(points, bounds);
		// build a map
		worldMap = builder.buildData(points, bounds);

		// create a JGraphT graph
		graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

		// build the graph from the map
		for (Polygon poly : worldMap.getRegions()) {
			Coordinate site = worldMap.getSiteOfRegion(poly);
			if (!worldMap.getIsSiteUnderwater(site)) {
				graph.addVertex(site);
			}
		}
		for (Polygon poly : worldMap.getRegions()) {
			Coordinate site = worldMap.getSiteOfRegion(poly);
			if (!worldMap.getIsSiteUnderwater(site)) {
				for (Coordinate linkedSite : worldMap.getLinkedSites(site)) {
					if (!worldMap.getIsSiteUnderwater(linkedSite)) {
						graph.addEdge(site, linkedSite);
						double weight = site.distance(linkedSite);
						// scale weight by 1+altitude change^2
						double heightSite = worldMap.getHeightOfSite(site);
						double heightLinkedSite = worldMap.getHeightOfSite(linkedSite);
						double weightScale = Math.abs(heightSite - heightLinkedSite);
						// log.info("weight scale = "+weightScale);
						weight *= 1.0 + weightScale;
						graph.setEdgeWeight(graph.getEdge(site, linkedSite), weight);
					}
				}
			}
		}
		// TODO use midpoints of shared edges

		aStarBuilder = new AStarBuilder<>(graph, new EuclidianHeuristic());

		// Show in Frame
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
		final BufferedImage img = gc.createCompatibleImage(800, 800);
		final JLabel imgLabel = new JLabel(new ImageIcon(img));
		final JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new JScrollPane(imgLabel));
		frame.pack();
		frame.setVisible(true);

		MyMouseInputAdapter myMouseInputAdapter = new MyMouseInputAdapter();
		imgLabel.addMouseListener(myMouseInputAdapter);
		imgLabel.addMouseMotionListener(myMouseInputAdapter);
		imgLabel.addMouseWheelListener(myMouseInputAdapter);

		new Timer(1000 / 30, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Graphics2D g = (Graphics2D) img.getGraphics();
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				redraw(g);
				g.dispose();
				imgLabel.repaint();
			}
		}).start();
	}

	private Coordinate getCoordinateClosest(int x, int y) {
		Coordinate target = new Coordinate(x, y);
		Coordinate closest = null;
		double dist = 0.0;
		for (Coordinate coord : graph.vertexSet()) {
			double coordDist = target.distance(coord);
			if (closest == null || coordDist < dist) {
				closest = coord;
				dist = coordDist;
			}
		}
		return closest;
	}

	private Color getHeightColor(double height) {
		if (height < 0.0) {
			// underwater
			float r = 0.0f;
			float g = 0.0f;
			float b = (float) (1.0 + height);
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

	private void redraw(Graphics2D g) {

		// poly edges
		for (Polygon region : worldMap.getRegions()) {
			g.setColor(getHeightColor(worldMap.getHeightOfSite(worldMap.getSiteOfRegion(region))));
			g.fill(AWTGeomUtils.polygonToPath2D(region));
		}

		// graph edges
		for (DefaultWeightedEdge edge : graph.edgeSet()) {
			Coordinate source = graph.getEdgeSource(edge);
			Coordinate target = graph.getEdgeTarget(edge);
			int x1 = (int) source.x;
			int y1 = (int) source.y;
			int x2 = (int) target.x;
			int y2 = (int) target.y;
			g.setColor(Color.black);
			g.setStroke(new BasicStroke(1));
			// g.drawLine(x1, y1, x2, y2);
		}

		// work out which site the mouse is closest to, and highlight it
		if (mouseX != null && mouseY != null) {
			Coordinate closest = getCoordinateClosest(mouseX, mouseY);
			if (start != null) {
				// calculate shortest path
				List<Coordinate> path = aStarBuilder.findpath(start, closest);
				if (path != null) {
					// draw it
					for (int i = 1; i < path.size(); i++) {

						int x1 = (int) path.get(i - 1).x;
						int y1 = (int) path.get(i - 1).y;
						int x2 = (int) path.get(i).x;
						int y2 = (int) path.get(i).y;
						g.setColor(Color.red);
						g.setStroke(new BasicStroke(3));
						g.drawLine(x1, y1, x2, y2);
					}
				}
			}

			g.setColor(Color.pink);
			g.fillOval((int) closest.x - 4, (int) closest.y - 4, 9, 9);
		}

		// highlight the start coord, if present
		// highlight this after closest so it shows when selected initially
		if (start != null) {
			g.setColor(Color.red);
			g.fillOval((int) start.x - 4, (int) start.y - 4, 9, 9);
		}

	}
}
