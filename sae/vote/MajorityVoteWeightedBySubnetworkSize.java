package  moa.classifiers.sae.vote;

import moa.classifiers.sae.Expert;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import com.yahoo.labs.samoa.instances.Instance;
/**
 * Perform a majority vote within subnetworks and weight these based on 
 * subnetwork size. Note: This is the original SAE voting scheme. 
 * 
 * For each s subnetwork in network
 *    For each e expert in s
 *       subVotes[y_e]++
 *    y_s = max(subVotes)
 *    votes[y_s] += subVotes[y_s]/s.size
 * @author heitor
 */
public class MajorityVoteWeightedBySubnetworkSize extends AbstractVote {

	public MajorityVoteWeightedBySubnetworkSize(HashMap<Integer, Expert> experts, Random random) {
		super(experts, random);
	}

	@Override
	public double[] predictVote(Instance instance, int periodLength, long ticks, 
			Collection<Set<Integer>> subnetworks) {
		int numClasses = instance.numClasses();
		double[] netVotes = new double[numClasses];
		
		for(Set<Integer> subnetwork : subnetworks) {
			double[] subVotes = new double[numClasses];
			for(Integer vertex : subnetwork) {
				Expert e = experts.get(vertex);
				int expertPrediction = e.test(instance);
				subVotes[expertPrediction]++;
			}
			int maxValueIndex = maxIndex(subVotes);
			/* If there was a tie within the subnetwork, then this subnetwork vote IS NOT counted
			 * in the network prediction. */
			if(maxValueIndex == -1) 
				++subTieCounter;
			else 
				/* Normalize votes by subnetwork size. */
				netVotes[maxValueIndex] += subVotes[maxValueIndex]/subnetwork.size();
		}
		int maxValueIndex = maxIndex(netVotes);
		
		/* maxValueIndex = -1 indicates that a tie occurred */
		if(maxValueIndex == -1) {
			++netTieCounter;
			/* break ties */
			netVotes[tieBreak(instance.numClasses())] = 10000;
		}
		return netVotes;
	}
}
