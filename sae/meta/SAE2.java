package moa.classifiers.sae.meta;

import java.util.ArrayList;

import com.github.javacliparser.*;

import com.yahoo.labs.samoa.instances.Instance;
import weka.core.Utils;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.sae.Network;
import moa.core.Measurement;
import moa.options.ClassOption;


/**
 * Class that contains all SAE2 algorithm parameters in MOA. 
 * 
 * @author heitor */
public class SAE2 extends AbstractClassifier implements MultiClassClassifier {

	private static final long serialVersionUID = 1L;
	/* SAE Parameters */
	public ClassOption baseLearnerOption = new ClassOption("BaseLearner", 'l', 
		"Base learner for new experts", Classifier.class, "trees.HoeffdingTree");
	public IntOption periodLengthOption = new IntOption("PeriodLengthOption", 'c', 
		"Number of instances before a network update takes place", 100000, 1, Integer.MAX_VALUE);
	public IntOption maxExpertsOption  = new IntOption("MaxExperts", 'o', 
			"Maximum number of experts at once", 10);
	public FloatOption minEOption = new FloatOption("MinE", 'e', 
		"EXPERT minimum correctly classified rate", 0.70f, 0.01f, 1.0f);
	public FloatOption scMinOption = new FloatOption("ScMin", 'n', 
		"Minimum Similarity Coefficient between EXPERTS (Activate Connection)", 0.90f, 0.01f, 1.0f);
	public FloatOption scMaxOption = new FloatOption("ScMax", 'x', 
		"Maximum Similarity Coefficient between EXPERTS (Redundant Expert) - 1.01 = allow redundants", 
		0.99f, 0.01f, 1.01f);
	public MultiChoiceOption combinationMethodOption = new MultiChoiceOption(
            "combinationMethod", 'v', "Which algorithm should be used to group classifiers.", 
            new String[]{"moa.classifiers.sae.combination.MaximalCliques", "moa.classifiers.sae.combination.WeaklyConnectedComponents", 
            		"moa.classifiers.sae.combination.FreeCombination"}, new String[]{"MaximalCliques",
                    "WeaklyConnectedComponents", "FreeCombination"}, 0);
	public MultiChoiceOption votingMethodOption = new MultiChoiceOption(
            "votingMethod", 'a', "Which algorithm should be used for prediction and tie break.", 
            new String[]{"moa.classifiers.sae.vote.MajorityVote", "moa.classifiers.sae.vote.MajorityVoteWeightedBySubnetworkSize", 
            		"moa.classifiers.sae.vote.WeightedMajorityVoteCurrentPeriod", "moa.classifiers.sae.vote.WeightedMajorityVoteLastPeriod"}, 
            new String[]{"MajorityVote", "MajorityVoteWeightedBySubnetworkSize", "WeightedMajorityVoteCurrentPeriod", 
            		"WeightedMajorityVoteLastPeriod"}, 2);
	
	public FlagOption doNotWriteNetworkOption = new FlagOption("DoNotWriteNetwork", 'w', 
	"Activate/Deactivate pajek network output to file. ");
	public FlagOption doNotWriteMeasurementsOption = new FlagOption("DoNotWriteMeasurements", 'q', 
	"Activate/Deactivate network measurements output to file. ");
	public StringOption pajekFileOption = new StringOption("pajekFile", 'p',
	"Network pajek project file name.", "sae-net");
	public StringOption measurementsFileOption = new StringOption("measurementsFile", 'z',
	"Network measurements file name.", "sae-measurements");

	
	/* The underlying network of experts. */
	protected Network network;
	/* Time representation. Increments after every TrainOninstanceImpl call */
	protected long ticks;
	/* Incorrectly classified instances by the network (last period). */
	protected ArrayList<Instance> Ier = new ArrayList<Instance>();
	/* Period length c. */
	protected int periodLength;
	/* How many periods during training */
	protected long periodCounter;
	/* Records period lengths measurements */
	protected long periodLengthMeasurement;
	/* Period identifier (sequential number) */
	protected int periodIdentifier;
	
	/**
	 * Instantiate a new Network and initialize the time counter (ticks). 
	 * @see moa.classifiers.AbstractClassifier#resetLearningImpl() */
	public void resetLearningImpl()	{
		ticks = 1;
		periodLength = periodLengthOption.getValue();
		periodIdentifier = 1;
		network = new Network((Classifier) getPreparedClassOption
				(baseLearnerOption), periodLength, doNotWriteNetworkOption.isSet(), 
				doNotWriteMeasurementsOption.isSet(), pajekFileOption.getValue(), 
				measurementsFileOption.getValue(), 
				combinationMethodOption.getChosenLabel(), votingMethodOption.getChosenLabel(), this.classifierRandom);
	}

	public void trainOnInstanceImpl(Instance instance) {
		/* Predict class value using current network structure */
		int predictedClass = Utils.maxIndex(network.test(instance, periodLength, ticks));
		/* If correct prediction, then increment network accuracy counter. */
		if(predictedClass == instance.classValue())
			network.addCorrectlyClassified();
		/* Else, save instance to be used to train a new expert */
		else
			Ier.add(instance);
		/* Update each expert accuracy counter w.r.t. to last instance. 
		 * Does not call "test" again on each expert, it uses the attribute lastPredictedClass 
		 * for the comparison. */
		network.checkExpertsAccuracy(instance, periodLength);
		network.updateConnections(instance);
		network.train(instance, this.classifierRandom);
		if(ticks % periodLength == 0) {
			network.update(maxExpertsOption.getValue(), periodIdentifier, ticks, periodLength, 
					scMinOption.getValue(), Ier, minEOption.getValue(),
					scMaxOption.getValue());
			Ier.clear();
			network.reset(periodLength, ticks);
			
			++periodIdentifier;
			periodLengthMeasurement += periodLength;
			++periodCounter;
		}
		++ticks;
	}
	
	public double[] getVotesForInstance(Instance instance) {
		return network.test(instance, periodLength, ticks);
	}
	@Override
	public boolean isRandomizable() {
		return true;
	}
	@Override
	public void getModelDescription(StringBuilder arg0, int arg1) {
		// TODO Auto-generated method stub
	}
	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return null;
	}
}
