package  moa.classifiers.sae.combination;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import moa.classifiers.sae.Graph;

/**
 * If freeCombination is used then the only reason to update connections is to identify redundant experts. 
 * This class simply considers every expert as an individual subnetwork. */
public class FreeCombination implements ICombination {

	@Override
	public Collection<Set<Integer>> combine(Graph<Integer, Long> network) {
		Set<Integer> allVertices = new HashSet<Integer>(network.getNodesIDs());
		Collection<Set<Integer>> wholeGraph = new ArrayList<Set<Integer>>();
		wholeGraph.add(allVertices);
		return wholeGraph;
	}
}
