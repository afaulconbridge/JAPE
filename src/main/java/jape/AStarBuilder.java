package jape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.jgrapht.graph.SimpleDirectedWeightedGraph;

public class AStarBuilder<V, E> {

	private final SimpleDirectedWeightedGraph<V, E> graph;
	private final Heuristic<V> heuristic;

	public AStarBuilder(SimpleDirectedWeightedGraph<V, E> graph, Heuristic<V> heuristic) {
		this.graph = graph;
		this.heuristic = heuristic;
	}

	public List<V> findpath(V start, V goal) {
		final Map<V, Double> costEstimate = new HashMap<>();
		final Map<V, Double> costSoFar = new HashMap<>();
		final Map<V, V> cameFrom = new HashMap<>();
		final Comparator<V> priorityComparator = new Comparator<V>() {
			@Override
			public int compare(V o1, V o2) {
				double cost1 = costEstimate.get(o1);
				double cost2 = costEstimate.get(o2);
				if (cost1 < cost2) {
					return -1;
				} else if (cost1 > cost2) {
					return 1;
				} else {
					return 0;
				}
			}
		};

		Queue<V> open = new PriorityQueue<>(11, priorityComparator);
		open.add(start);
		costEstimate.put(start, 0.0);
		costSoFar.put(start, 0.0);

		Set<V> closed = new HashSet<>();

		while (!goal.equals(open.peek())) {
			V current = open.poll();
			closed.add(current);

			for (E edge : graph.edgesOf(current)) {
				V edgeSource = graph.getEdgeSource(edge);
				V edgeTarget = graph.getEdgeTarget(edge);
				if (current.equals(edgeSource)) {
					V neighbour = edgeTarget;
					double cost = costSoFar.get(current) + graph.getEdgeWeight(edge);

					if (open.contains(neighbour) && cost < costSoFar.get(neighbour)) {
						// remove from open because new path is better
						open.remove(neighbour);
					}

					if (closed.contains(neighbour) && cost < costSoFar.get(neighbour)) {
						closed.remove(neighbour);
					}

					if (!closed.contains(neighbour) && !open.contains(neighbour)) {
						costSoFar.put(neighbour, cost);
						costEstimate.put(neighbour, cost + heuristic.getCostEstimate(neighbour, goal));
						open.add(neighbour);
						cameFrom.put(neighbour, current);
					}

				}
			}

			if (open.size() == 0) {
				// no path possible
				return null;
			}
		}

		List<V> path = new ArrayList<>();
		V current = goal;
		while (!start.equals(current)) {
			path.add(current);
			current = cameFrom.get(current);
		}
		path.add(start);
		return path;
	}

}
