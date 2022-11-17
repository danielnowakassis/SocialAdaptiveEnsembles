package  moa.classifiers.sae.vote;

import moa.classifiers.sae.Expert;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import com.yahoo.labs.samoa.instances.Instance;
import weka.core.Utils;

public abstract class AbstractVote {
	
	/* Experts hash table (Aggregation) */
	protected final HashMap<Integer, Expert> experts;
	/* Random tie breaking initialization */
	protected Random random;
	
	
	/* Counter for subnetwork ties */
	protected long subTieCounter = 0;
	/* Counter for network ties */
	protected long netTieCounter = 0;
	
	AbstractVote(HashMap<Integer, Expert> experts, Random random) {
		this.experts = experts;
		this.random = random;
	}
	
	public abstract double[] predictVote(Instance instance, int periodLength, long ticks, Collection<Set<Integer>> subnetworks);
	
	/**
	 * Default implementation of tie break is Random tie break. 
	 * If there are k classes equally distributed over N instances
	 * then P(i is Correctly Classified) = 1/k for Random Tie Break. 
	 * @param k number of classes.
	 * @return index representing the selected class. 
	 */
	protected int tieBreak(int k) {
		return random.nextInt(k);
	}
	
	public void update() {
		netTieCounter = 0;
		subTieCounter = 0;
	}
	
	/**
	 * Calculate index of the position with maximum value. 
	 * Indicates ties with -1
	 * @param votes
	 * @return index of slot with maximum value (-1 if there is a tie) */
	protected int maxIndex(double[] votes) {
		int maxValueIndex = Utils.maxIndex(votes);
		double secMaxValue = Utils.kthSmallestValue(votes, votes.length-1);

		if(votes[maxValueIndex] == secMaxValue)
			return -1;
		return maxValueIndex;
	}
	
	/* Accessors */
	public long getNetTieCounter() {
		return netTieCounter;
	}

	
}
