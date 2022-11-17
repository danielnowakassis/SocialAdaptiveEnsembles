package moa.classifiers.sae;


import java.util.ArrayList;

import com.yahoo.labs.samoa.instances.Instance;

import weka.core.Utils;
import moa.classifiers.Classifier;

/**
 * Represents a base learner instance along with other data for SAE algorithm. 
 * Including: when it was created, an unique ID, previous accuracy, etc. 
 * It does not have a periodLength associated with it, therefore future versions of
 * the algorithm that may change the period length during execution will integrate
 * with this class.  
 * @author heitor */
public class Expert implements Comparable<Expert> {
	/* This expert's unique ID */
	protected int ID;
	/* Base learner used to train and predict */
	public Classifier learner;
	/* Number of correctly classified instances (current period) */
	protected int correctlyClassified;
	/* Correctly classified ratio (last period - used for WeightedMajorityVoting) */
	protected double correctlyClassifiedRatioLastPeriod;
	/* Last predicted class by this classifier. */
	protected int lastPredictedClass;
	/* When this expert was created (time t) */
	protected long createdOn;
	
	/* Boolean flag to check whether this expert is currently considered a candidate or not. */
	public boolean candidate;

	@Override
	public int compareTo(Expert other) {
		int o1 = this.getCorrectlyClassified(), o2 = other.getCorrectlyClassified();
		return (o1 > o2 ? -1 : (o1 == o2 ? 0 : 1));
	}
	
	/**
	 * Initialize attributes and set expert ID. 
	 * May only be called by constructors. 
	 * @param ID must be be unique
	 * @param learner
	 * @param createdOn
	 */
	protected void init(int ID, Classifier learner, long createdOn, boolean candidate) {
		this.ID = ID;
		this.candidate = candidate;
		// Copy classifier object
		this.learner = learner.copy(); 
		this.createdOn = createdOn;
		this.lastPredictedClass = -1;
	}
	
	/**
	 * Create a new expert. Only the first expert and candidates must be
	 * created using this constructor because correctlyClassifiedRatioLastPeriod is
	 * set to 1.0. 
	 * @param ID must be be unique
	 * @param learner base learner
	 * @param createdon when it was created
	 * @param candidate whether it is a candidate or not */
	public Expert(int ID, Classifier learner, long createdOn, boolean candidate) {
		init(ID, learner, createdOn, candidate);
		correctlyClassifiedRatioLastPeriod = 1.0;
	}
	
	/**
	 * Creates an expert and train it on every instance from Ier.
	 * @param ID must be be unique
	 * @param learner base learner
	 * @param createdOn when it was created
	 * @param Ier set of instances to train the expert */
	public Expert(int ID, Classifier learner, long createdOn, ArrayList<Instance> Ier, boolean candidate) {
		init(ID, learner, createdOn, candidate);
		for(Instance i : Ier)
			this.learner.trainOnInstance(i);
	}

	/**
	 * Reset 'memory' about correctlyClassified instances. 
	 * @param periodLength
	 */
	public void reset(int periodLength) {
		this.correctlyClassifiedRatioLastPeriod = getCorrectlyClassifiedRatio(periodLength);
		this.correctlyClassified = 0;
	}

	/**
	 * Predict the class value, if correctly classified increment counter.
	 * It depends on test(..) being called first. To avoid this 'dependency'
	 * it would be necessary to request another prediction from the learner, 
	 * but since each instance is predicted by every expert once 
	 * (SAE2.trainOnInstanceImpl(...)), it would be redundant do to so twice. 
	 * 
	 * @see moa.classifiers.meta.SAE2
	 * @param instance
	 * @return last predicted class value */
	public int checkAccuracy(Instance instance) {
		if(lastPredictedClass == instance.classValue()) 
			++correctlyClassified;
		return lastPredictedClass;
	}
	
	/**
	 * Train learner on instance. 
	 * @param instance */
	public void train(Instance instance) {
		learner.trainOnInstance(instance);
	}
	
	/**
	 * Train learner on multiple instances. 
	 * @param instances */
	public void train(ArrayList<Instance> instances) {
		for(Instance i : instances)
			learner.trainOnInstance(i);
	}
	
	/**
	 * Predict class value using expert's learner and updates last prediction 
	 * @param instance
	 * @return predicted class index */
	public int test(Instance instance) {
		lastPredictedClass = Utils.maxIndex(learner.getVotesForInstance(instance));
		return lastPredictedClass;
	}
	
	/* Accessors */
	public boolean isCandidate() {
		return candidate;
	}
	public int getID() {
		return ID;
	}
	public int getCorrectlyClassified() {
		return correctlyClassified;
	}
	public double getCorrectlyClassifiedRatio(int periodLength) {
		return periodLength > 0 ? correctlyClassified/(double)periodLength : 0.0;	
	}
	public double getCorrectlyClassifiedRatioLastPeriod() {
		return correctlyClassifiedRatioLastPeriod;
	}
	public long getCreatedOn() {
		return createdOn;
	}
	public int getLastPredictedClass() {
		return lastPredictedClass;
	}
	public String nodeLabel(int periodLength, long ticks) {
		StringBuilder str = new StringBuilder(100);
		str.append(ID);
		str.append("-");
		str.append(String.format("%.2f", getCorrectlyClassifiedRatio(periodLength)));
		str.append("-");
		str.append(ticks - createdOn);
		return str.toString();
	}	
}
