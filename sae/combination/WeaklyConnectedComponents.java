package  moa.classifiers.sae.combination;

import moa.classifiers.sae.Graph;

import java.util.Collection;
import java.util.Set;


public class WeaklyConnectedComponents implements ICombination {

	@Override
	public Collection<Set<Integer>> combine(Graph<Integer, Long> network) {
		return network.WeaklyConnectedComponents();
	}
	
}
