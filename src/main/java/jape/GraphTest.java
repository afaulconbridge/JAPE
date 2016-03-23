package jape;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import org.jgraph.JGraph;
import org.jgraph.graph.GraphConstants;
import org.jgrapht.ListenableGraph;
import org.jgrapht.VertexFactory;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.generate.RandomGraphGenerator;
import org.jgrapht.graph.ClassBasedVertexFactory;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.jgrapht.graph.ListenableUndirectedGraph;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import jape.map.Builder;
import jape.map.IslandMap;

public class GraphTest {

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
		// assign land/water to corners
		// assign land/water to polygons
		IslandMap worldMap = builder.buildData(points, bounds);

		builder.display(worldMap, bounds);
		
		// create a JGraphT graph
		ListenableGraph<Coordinate, DefaultEdge> graph = new ListenableUndirectedGraph<>(DefaultEdge.class);
		
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
					}
				}
			}
		}
				
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
		BufferedImage img = gc.createCompatibleImage(800, 800);
		
		Graphics2D g = (Graphics2D) img.createGraphics();
		for (DefaultEdge edge : graph.edgeSet()) {
			Coordinate source = graph.getEdgeSource(edge);
			Coordinate target = graph.getEdgeTarget(edge);
			int x1 = (int) source.x;
			int y1 = (int) source.y;
			int x2 = (int) target.x;
			int y2 = (int) target.y;
			g.drawLine(x1, y1, x2, y2);
		}

		// Show in Frame
		JFrame frame = new JFrame();		
		frame.getContentPane().add(new JScrollPane(new JLabel(new ImageIcon(img))));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}
