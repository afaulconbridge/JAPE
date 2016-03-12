package jape.graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Graph<T> {
	
	private Map<T,Set<T>> map = new HashMap<>();

	public void addDirectedEdge(T source, T end) {
		if (!map.containsKey(source)){
			map.put(source, new HashSet<>());
		}
		map.get(source).add(end);
	}

	public void addUndirectedEdge(T source, T end) {
		addDirectedEdge(source, end);
		addDirectedEdge(end, source);
	}
	
	public Set<T> getConnected(T source) {
		if (!map.containsKey(source)) throw new IllegalArgumentException("source not in graph");
		return Collections.unmodifiableSet(map.get(source));
	}
	
	public Map<T,Set<T>> get() {
		return map;
	}
}
