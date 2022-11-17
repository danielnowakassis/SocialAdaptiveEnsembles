package  moa.classifiers.sae.combination;

import moa.classifiers.sae.Graph;

import java.util.Collection;
import java.util.Set;

public interface ICombination {
	Collection<Set<Integer>> combine(Graph<Integer, Long> network);
}
