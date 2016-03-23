package jape;

public interface Heuristic<V> {
	public double getCostEstimate(V source, V target);
}
