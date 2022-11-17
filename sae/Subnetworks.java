package moa.classifiers.sae;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;


import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.sae.combination.ICombination;
import moa.classifiers.sae.vote.*;

public class Subnetworks {
	/* All subnetworks associated with 'network' attribute */
	protected Collection<Set<Integer>> subnetworks;
	/* Algorithm used to combine classifiers: MaximalCliques, WeaklyConnectedComponents, ... */
	protected ICombination combinationAlgorithm;
	/* Algorithm used for voting. Tie break is part of the algorithm. */
	protected AbstractVote votingAlgorithm;
	
	/* Experts hash table (Aggregation) */
	protected final HashMap<Integer, Expert> experts;
	/* Jung graph (Aggregation) */
	protected final Graph<Integer, Long> network;
	
	/** 
	 * Set combination/voting class using reflection and parameters combinationClass/votingClass. 
	 * Call update() to initialize other attributes. 
	 * @param experts reference to experts hash map
	 * @param network reference
	 * @param combinationClass string that indicates which combination class to be instantiated */
	@SuppressWarnings("unchecked")
	public Subnetworks(HashMap<Integer, Expert> experts, Graph<Integer, Long> network, 
			String combinationClassName, String votingClassName, Random random) {
		
	    try {
	    	@SuppressWarnings("rawtypes")
			Class classCombination = Class.forName(combinationClassName);
	    	@SuppressWarnings("rawtypes")
			Class classVoting = Class.forName(votingClassName);
	    	combinationAlgorithm = (ICombination) classCombination.newInstance();
	    	votingAlgorithm = (AbstractVote) classVoting.getDeclaredConstructor(HashMap.class, Random.class).newInstance(experts, random);
	    	
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		this.experts = experts;
		this.network = network;
		update();
	}
	
	/** Update subnetworks structure according to network current structure. */
	public void update() {
		subnetworks = null;
		subnetworks = combinationAlgorithm.combine(network);
		votingAlgorithm.update();
	}

	/**
	 * Combine votes within each subnetwork, afterwards combine subnetworks votes on the network vote. 
	 * @see hmg.sae.vote for implementation details on how prediction occurs. 
	 * @param instance
	 * @return array where the position with the highest value indicates the predicted class. */
	public double[] combineVotes(Instance instance, int periodLength, long ticks) {
		return votingAlgorithm.predictVote(instance, periodLength, ticks, subnetworks);
	}
	
	/* Accessors */
	public long getNetTieCounter() {
		return votingAlgorithm.getNetTieCounter();
	}
	public int getSubnetworksSize() {
		return subnetworks.size();
	}
}
