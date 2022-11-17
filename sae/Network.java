package moa.classifiers.sae;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import com.yahoo.labs.samoa.instances.Instance;


import moa.classifiers.Classifier;
import moa.core.MiscUtils;

/**
 * @author heitor */
public class Network {

	/* HashMap containing all the active experts and the candidate*/
	public HashMap<Integer, Expert> experts = new HashMap<Integer, Expert>(30);
	/* Encompasses all possible connections */
	public Connections connections;
	/* Graph representation of the Network. 
	 * Vertices = Experts' IDs
	 * Edges = Connections' IDs (only active connections) */
	public Graph<Integer, Long> network = new Graph<Integer, Long>();
	/* Subnetworks are responsible for combining votes */
	public Subnetworks subnetworks;
	/* Expert to be added to the network. */
	protected Expert candidate = null;
	/* Expert id sequence generator */
	protected int nextExpertID;
	
	/* Number of correctly classified instances in the current period () */
	protected int correctlyClassified;
	
	/* Base learner set for this network. */
	protected Classifier baseLearner;
	
	/* Number of instances seen (reset along with network) */
	protected int numberOfInstancesSeen;
	/* Prominence metrics. Used to analyze network topology. */
	protected double density, avgDegree;
	/* Removed experts counter. Used to assess by which criteria experts were 
	 * removed during the period (reset along with network). */
	protected long removedByPerformanceCounter;
	protected long removedByRedundancyCounter;
	
	/* Pajek file project file output */
	protected OutputPajek outputPajek = null;
	/* Measurements file (includes prominence metrics) output */
	protected OutputNetworkMeasurements outputNetworkMeasurements = null;

	public Network(Classifier baseLearner, double periodLength,
			boolean doNotCreateNetworkFile, boolean doNotCreateMeasurementsFile, 
			String pajekFileName, String measurementsFileName, String combinationClassName, 
			String votingClassName, Random random) {
		
		Date now = Calendar.getInstance().getTime();
		if(! doNotCreateNetworkFile)
			outputPajek = new OutputPajek(pajekFileName, now);
		if(! doNotCreateMeasurementsFile)
			outputNetworkMeasurements = new OutputNetworkMeasurements(measurementsFileName, now);
		
		this.baseLearner = baseLearner;
		/* Create the connections object. parameter 'experts' is a reference to this.experts. */
		connections = new Connections(experts);
		
		/* Create the first expert and the first candidate. */
		Expert first = new Expert(this.nextExpertID++, baseLearner, 0, false);
		candidate = new Expert(this.nextExpertID++, baseLearner, (long) periodLength, true);
		/* The first expert goes into the Graph, therefore it will not only be trained
		 * during the first period, but will also be used for predictions. */
		experts.put(first.getID(), first);
		network.addNode(first.getID(), first.getID());
		/* The first candidate only goes into the experts HashTable, thus it is trained
		 * during the first period, but will not be used for predictions. */
		experts.put(candidate.getID(), candidate);
		connections.addAllConnections(candidate);
		
		subnetworks = new Subnetworks(experts, network, combinationClassName, votingClassName, random);
	}
	
	/** Reset classification statistics from last period and add a new candidate */
	public void reset(int periodLength, long ticks) {
		correctlyClassified = 0;
		numberOfInstancesSeen = 0;
		candidate = new Expert(this.nextExpertID++, baseLearner, ticks, true);
		experts.put(candidate.getID(), candidate);
		connections.addAllConnections(candidate);
		
		for(Expert e : experts.values())
			e.reset(periodLength);
		connections.reset();
	}
	
	/** Update network structure (create/remove experts, extract measurements, 
	 * activate/deactivate connections, generate subnetworks). 
	 * @param periodLength 
	 * @param csMin */
	public void update(int maxExperts, int period, long ticks, int periodLength, double scMin, 
			ArrayList<Instance> Ier, double minE, double scMax)
	{
		/* Add last network to pajek project file. It will be null if it should not
		 * create a pajek output. */
		if(outputPajek != null)
			//outputPajek.addNetwork(network, experts, periodLength, ticks);
			outputPajek.addNetwork(connections, periodLength, ticks);
		
		if(outputNetworkMeasurements != null) {
			extractMeasurements();
			outputNetworkMeasurements.addMeasurements(period, ticks, density, 
					avgDegree, network.getNodesQuantity(), network.getEdgesQuantity(), 
					subnetworks.getSubnetworksSize(), subnetworks.getNetTieCounter(), 
					getCorrectlyClassifiedRatio(), removedByPerformanceCounter, 
					removedByRedundancyCounter, candidate.getCorrectlyClassifiedRatio(periodLength));
			removedByPerformanceCounter = 0;
		}
		removeExperts(minE, scMax, periodLength);
		addExpert(maxExperts, ticks, periodLength, Ier);
		connections.update(network, periodLength, scMin);
		subnetworks.update();
	}

	/**  Check if it is necessary to add a new Expert to the network. 
	 * 1. MaxExperts: if total number of active (used to predict "test" instances) experts (in graph) 
	 * is equal to maxExperts parameter, then replace expert with the lowest accuracy during last period. Otherwise
	 * just add candidate to the graph. This approach may add candidates that actually decrease the network accuracy. 
	*/
	protected void addExpert(int maxExperts, long ticks, 
			int periodLength, ArrayList<Instance> Ier) {
		/* Checks if there is a candidate to be added. The candidate could have been removed 
		 * from "experts" in removeExperts(...), thus it is necessary to check if it still exists before continuing. */
		if(experts.get(candidate.getID()) != null) {
			assert network.getNodesQuantity() <= maxExperts;
			/* Check if network size (vertex count) has reach its maximum value (maxExperts) */
			if(network.getNodesQuantity() == maxExperts) {
				List<Expert> expertsCopy = new ArrayList<Expert>(experts.values());
			    expertsCopy.remove(candidate);
			    /* Sort experts in descending order according to their average accuracy obtained during last period */
				Collections.sort(expertsCopy);
				Expert toBeRemoved = expertsCopy.get(expertsCopy.size() - 1);
				
				/* Remove worst performer */
				experts.remove(toBeRemoved.getID());
				network.removeNode(toBeRemoved.getID());
				connections.removeAllConnections(toBeRemoved.getID());
			}
			/* Reinforce training on incorrectly classified instances */
			candidate.train(Ier);
			candidate.candidate = false;
			/* Add candidate to graph */
			network.addNode(candidate.getID(), candidate.getID());
		}
	}
	
	/**
	 *  Check if among experts there are any with insufficient accuracy (acc < MinE) or that became redundant, 
	 *  and if so, remove them. The candidate can be "removed", even though it were never officially added to 
	 *  the network. 
	 *  Always keep at least one expert, even if its accuracy is below the threshold, otherwise there would be
	 *  none to predict during the next period. */
	protected void removeExperts(double minE, double scMax, int periodLength)
	{
		/* Low performance expert removal */
		List<Expert> expertsCopy = new ArrayList<Expert>(experts.values());
		/* Sort descending (highest accuracy to lowest accuracy) */
		Collections.sort(expertsCopy);
		/* The best performer cannot be removed, therefore the expert in the first
		 * position of the sorted array is not removed. */
		expertsCopy.remove(0);
		
		for(Expert e : expertsCopy) {
			if(e.getCorrectlyClassifiedRatio(periodLength) < minE) {
				experts.remove(e.getID());
				network.removeNode(e.getID());
				connections.removeAllConnections(e.getID());
				++removedByPerformanceCounter;
			}
		}
		
		/* Redundant expert removal */
		Iterator<Connection> itC = connections.getConnections().values().iterator();
		Set<Integer> toBeRemoved = new TreeSet<Integer>();
		
		while(itC.hasNext()) {
			Connection c = itC.next();
			if(c.isRedundant(scMax, periodLength)) {
				Expert rmE = c.getFirst();
				toBeRemoved.add(rmE.getID());
			}
		}
		/*	System.out.print("\nExperts BEFORE(" + network.getVertexCount() + "): ");
		for(Integer i : network.getVertices()) 
			System.out.print(i + " ");
		System.out.print("\nToBeRemoved(" + toBeRemoved.size() + "): ");
		for(Integer i : toBeRemoved)
			System.out.print(i + " "); */
		
		removedByRedundancyCounter = toBeRemoved.size();
		for(Integer i : toBeRemoved) {
			experts.remove(i);
			network.removeNode(i);
			connections.removeAllConnections(i);
		}
	}

	/** 
	 * Update connections similarities. Loop through all existing connections and
	 * check if both experts predicted the same class. 
	 * @param instance */
	public void updateConnections(Instance instance) {
		connections.updateSimilarities(instance);
	}
	
	/**
	 * Update correctly classified counters
	 * @param instance */
	public void checkExpertsAccuracy(Instance instance, int periodLength) {
		for(Expert e : experts.values())
			e.checkAccuracy(instance);
	}
	
	/**
	 * Train experts and update their similarities (connections). 
	 * 1 expert network: use all instances for training (no random sampling). 
	 * n experts network: use online bagging (lambda = 1) for sampling instances. 
	 * @param instance
	 * @param tick
	 * @return predicted class index */
	public void train(Instance instance, Random random) {
		++numberOfInstancesSeen;
		if(experts.size() == 1)
			experts.values().iterator().next().train(instance);
		else {
			for(Expert e : experts.values()) {
				int k = MiscUtils.poisson(1.0, random);
				if (k > 0) {
					Instance weightedInst = (Instance) instance.copy();
					weightedInst.setWeight(instance.weight() * k);
					e.train(weightedInst);
				}
			}
		}
	}
	
	/**
	 * Predict class using the network (combining subnetworks decisions). The candidate
	 * is not allowed to vote, but it attempt to predict the instance class anyway, 
	 * so it is possible to evaluate its similarity compared to the active experts.  
	 * @param instance
	 * @return array with votes, where position with max value is the predicted */
	public double[] test(Instance instance, int periodLength, long ticks)	{
		candidate.test(instance);
		return subnetworks.combineVotes(instance, periodLength, ticks);
	}
	
	/* Mutators */
	public void addCorrectlyClassified() {
		++correctlyClassified;
	}
	
	/* Accessors */
	public int getCorrectlyClassified() {
		return correctlyClassified;
	}
	public double getCorrectlyClassifiedRatio() {
		return correctlyClassified / (double) numberOfInstancesSeen;
	}

	/**
	 * Extract prominence measurements from the network */
	public void extractMeasurements() {
		extractDensity();
		extractAvgDegree();
		// TODO: add other measurements (clustering coefficient, betweenness, ...)
	}
	
	private void extractDensity() {
		density = network.getEdgeDensity();
	}
	
	private void extractAvgDegree()	{
		long g = network.getNodesQuantity();
		long L = network.getEdgesQuantity();
		avgDegree = g == 0 ? 0 : 2*L / (double) g;
	}
}
