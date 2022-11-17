package  moa.classifiers.sae.combination;

import moa.classifiers.sae.Graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MaximalCliques implements ICombination {
	
	private BronKerboschCliqueFinder<Integer, Long> cliques = null;
	
	@Override
	public Collection<Set<Integer>> combine(Graph<Integer, Long> network) {
		cliques = new BronKerboschCliqueFinder<Integer, Long>(network);
		return cliques.getAllMaximalCliques();
	}
	
	/**
	 * Adopted from JGraphT for use with Jung.
	 * The BronKerbosch algorithm is an algorithm for finding maximal cliques in an undirected graph
	 * This algorithmn is taken from Coenraad Bron- Joep Kerbosch in 1973.
	 * This works on undirected graph
	 *  See {@linktourl  See http://en.wikipedia.org/wiki/Bron%E2%80%93Kerbosch_algorithm}
	 *  
	 * ** EDITED: Adapted it to work with as.Graph instead of Jung Graph. It uses nodes unique identifiers
	 * instead of nodes raw values to generate cliques. ** 
	 *  
	 * @author Reuben Doetsch
	 *
	 * @param <V> vertex class of graph
	 * @param <E> edge class of graph
	 */
	private class BronKerboschCliqueFinder<V, E>
	{
	    private final Graph<V, E> graph;
	    private Collection<Set<Integer>> cliques;

	    /**
	     * Creates a new clique finder. Make sure this is a simple graph.
	     *
	     * @param graph the graph in which cliques are to be found; graph must be
	     * simple
	     */
	    public BronKerboschCliqueFinder(Graph<V, E> graph) {
	        this.graph = graph;
	    }
	    
	    /**
	     * Finds all maximal cliques of the graph. A clique is maximal if it is
	     * impossible to enlarge it by adding another vertex from the graph. Note
	     * that a maximal clique is not necessarily the biggest clique in the graph.
	     *
	     * @return Collection of cliques (each of which is represented as a Set of
	     * vertices)
	     */
	    public Collection<Set<Integer>> getAllMaximalCliques() {
	        // TODO:  assert that graph is simple
	        cliques = new ArrayList<Set<Integer>>();
	        List<Integer> potential_clique = new ArrayList<Integer>();
	        List<Integer> candidates = new ArrayList<Integer>();
	        List<Integer> already_found = new ArrayList<Integer>();
	        candidates.addAll(graph.getNodesIDs());
	        findCliques(potential_clique, candidates, already_found);
	        return cliques;
	    }

	    /**
	     * Finds the biggest maximal cliques of the graph.
	     *
	     * @return Collection of cliques (each of which is represented as a Set of
	     * vertices)
	     */
	    @SuppressWarnings("unused")
		public Collection<Set<Integer>> getBiggestMaximalCliques() {
	        // first, find all cliques
	        getAllMaximalCliques();

	        int maximum = 0;
	        Collection<Set<Integer>> biggest_cliques = new ArrayList<Set<Integer>>();
	        for (Set<Integer> clique : cliques) {
	            if (maximum < clique.size()) {
	                maximum = clique.size();
	            }
	        }
	        for (Set<Integer> clique : cliques) {
	            if (maximum == clique.size()) {
	                biggest_cliques.add(clique);
	            }
	        }
	        return biggest_cliques;
	    }

	    private void findCliques(List<Integer> potential_clique, List<Integer> candidates,
	        List<Integer> already_found) {
	        List<Integer> candidates_array = new ArrayList<Integer>(candidates);
	        if (!end(candidates, already_found)) {
	            // for each candidate_node in candidates do
	            for (Integer candidate : candidates_array) {
	                List<Integer> new_candidates = new ArrayList<Integer>();
	                List<Integer> new_already_found = new ArrayList<Integer>();

	                // move candidate node to potential_clique
	                potential_clique.add(candidate);
	                candidates.remove(candidate);

	                // create new_candidates by removing nodes in candidates not
	                // connected to candidate node
	                for (Integer new_candidate : candidates) {
	                    if (graph.getEdge(candidate, new_candidate) != null) {
	                        new_candidates.add(new_candidate);
	                    } // of if
	                } // of for

	                // create new_already_found by removing nodes in already_found
	                // not connected to candidate node
	                for (Integer new_found : already_found) {
	                    if (graph.getEdge(candidate, new_found) != null) {
	                        new_already_found.add(new_found);
	                    } // of if
	                } // of for

	                // if new_candidates and new_already_found are empty
	                if (new_candidates.isEmpty() && new_already_found.isEmpty()) {
	                    // potential_clique is maximal_clique
	                    cliques.add(new HashSet<Integer>(potential_clique));
	                } // of if
	                else {
	                    // recursive call
	                    findCliques(
	                        potential_clique,
	                        new_candidates,
	                        new_already_found);
	                } // of else

	                // move candidate_node from potential_clique to already_found;
	                already_found.add(candidate);
	                potential_clique.remove(candidate);
	            } // of for
	        } // of if
	    }

	    private boolean end(List<Integer> candidates, List<Integer> already_found) {
	        // if a node in already_found is connected to all nodes in candidates
	        boolean end = false;
	        int edgecounter;
	        for (Integer found : already_found) {
	            edgecounter = 0;
	            for (Integer candidate : candidates) {
	                if (graph.getEdge(found, candidate) != null) {
	                    edgecounter++;
	                } // of if
	            } // of for
	            if (edgecounter == candidates.size()) {
	                end = true;
	            }
	        } // of for
	        return end;
	    }
	}

}
