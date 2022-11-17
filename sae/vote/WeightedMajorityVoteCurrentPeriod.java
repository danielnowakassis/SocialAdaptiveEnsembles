package  moa.classifiers.sae.vote;

import moa.classifiers.sae.Expert;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import com.yahoo.labs.samoa.instances.Instance;

/**
 * Use expert accuracy as weight to vote within subnetwork. Use subnetwork 
 * average accuracy (sum of all experts accuracy divide by subnetwork size) 
 * to vote within network. This voting strategy does not take into account
 * the distribution of votes within the subnetwork, i.e. it does not weight 
 * if the decision was unanimous or split. The accuracy is obtained from 
 * CURRENT PERIOD. 
 * 
 * For each s subnetwork in network
 *    For each e expert in s
 *        subVotes[y_e] += e.accuracy
 *    y_s = max(subVotes)
 *    votes[y_s] += s.accuracy
 * return max(votes)
 * @author heitor
 */
public class WeightedMajorityVoteCurrentPeriod extends AbstractVote {

	public WeightedMajorityVoteCurrentPeriod(HashMap<Integer, Expert> experts, Random random) {
		super(experts, random);
	}

	@Override
	public double[] predictVote(Instance instance, int periodLength, long ticks, 
			Collection<Set<Integer>> subnetworks) {
		int numClasses = instance.numClasses();
		double[] netVotes = new double[numClasses];
		
		for(Set<Integer> subnetwork : subnetworks) {
			double subSumAccuracy = 0.0;
			double[] subVotes = new double[numClasses];
			for(Integer vertex : subnetwork) {
				Expert e = experts.get(vertex);
				int expertPrediction = e.test(instance);
				
				/* Asks for accuracy given the amount of instances seen so far (ticks % periodLength). */
				double expertAccuracy = e.getCorrectlyClassifiedRatio((int) ((ticks % periodLength) == 0 ? periodLength : (ticks % periodLength)));
				
				subVotes[expertPrediction] += expertAccuracy;
				subSumAccuracy += expertAccuracy;
			}
			int maxValueIndex = maxIndex(subVotes);
			
			/* If there was a tie within the subnetwork, then this subnetwork vote IS NOT counted
			 * in the network prediction. */
			if(maxValueIndex == -1)
				++subTieCounter;
			else
				netVotes[maxValueIndex] += subSumAccuracy/subnetwork.size();
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
