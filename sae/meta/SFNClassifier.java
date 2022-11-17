package moa.classifiers.sae.meta;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.sae.Graph;
import moa.core.Measurement;
import moa.options.ClassOption;

import com.github.javacliparser.*;
import com.yahoo.labs.samoa.instances.Instance;
import weka.core.Utils;

/**
 *
 * @author Jean Paul Barddal
 */
public class SFNClassifier extends AbstractClassifier implements MultiClassClassifier {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//The Node used in SFNClassifier
    public class SFNCVertex {

        //the base classifier
        private Classifier baseClassifier;

        //control variables to test with expected threshold
        private int instances = 0;
        private int hits = 0;

        //lastClassification value
        @SuppressWarnings("unused")
		private double lastClassification;

        //total instances seen
        @SuppressWarnings("unused")
		private int instancesSeen;

        //constructor
        public SFNCVertex(Classifier baseClassifier) {
            this.baseClassifier = baseClassifier.copy();
            this.baseClassifier.resetLearning();
            this.instances = this.instancesSeen = this.hits = 0;
            this.lastClassification = 0.0d;
        }

        //interfaces
        public double[] getVotesForInstance(Instance instance) {
            double ret[] = this.baseClassifier.getVotesForInstance(instance);
            lastClassification = Utils.maxIndex(ret);
            if (Utils.maxIndex(ret) == (int) instance.classValue()) {
                this.hits++;
            }
            this.instances++;
            this.instancesSeen++;
            return ret;
        }

        public void trainOnInstance(Instance instance) {
            //train
            this.baseClassifier.trainOnInstance(instance);
        }

        //MUTATORS
        public double getHitRate() {
            if (this.instances == 0) {
                return 0.0;
            }
            return this.hits / (double) this.instances;
        }

        public void cleanStats() {
            this.hits = 0;
            this.instances = 0;
        }

    }

    //CONSTRUCTOR
    public SFNClassifier() {
    }

    //the option variables
    public FloatOption expectedHitRateOption
            = new FloatOption("expectedHitRate", 'e',
                    "Determine the expected threshold of the net.",
                    0.95f, 0.0f, 1.0f);
    public ClassOption baseLeanerOption
            = new ClassOption("baseLeaner", '1', "Classifier to train.",
                    Classifier.class, "trees.HoeffdingTree");
    private final String metrics[]
            = {"Betweenness", "Closeness",
                "Degree", "Pagerank", "Eigenvector"};
    public MultiChoiceOption adoptedMetricOption
            = new MultiChoiceOption("adoptedMetric",
                    'm',
                    "Determine which metric will be used for voting on instances.",
                    metrics, metrics, 4);
    public IntOption updatePeriodOption
            = new IntOption("updatePeriod", 'u',
                    "Define how many periods the network remains without any udpate.",
                    1000, 0, 50000);

    public IntOption kMaxOption
            = new IntOption("kMax", 'k',
                    "Determines the maximum amount of nodes in the network.",
                    10, 3, 1000);

    //the attributes
    private transient Graph<SFNCVertex, Integer> network
            = new Graph<SFNCVertex, Integer>(adoptedMetricOption.getValueAsCLIString());
    private int lastID = 0;
    private int instancesSeen = 0;
    private ArrayList<Instance> misclassifiedInstances
            = new ArrayList<Instance>();
    private int instancesInThisPeriod = 0;
    private int hits = 0;

    //MUTATORS
    private void updateNetwork() {
        // we determine which vertex should be removed
        int toRemove = this.chooseVertexForRemoval();

        // we remove this vertex, already working with the rewiring process
        removeVertex(toRemove);

        // we instantiate a new classifier if we are not 
        // reaching the expected threshold
        SFNCVertex newVertex = null;
        if (instancesSeen >= this.updatePeriodOption.getValue()
                && (hits / instancesInThisPeriod) < this.expectedHitRateOption.getValue()) {
            newVertex = instantiateNewVertex(misclassifiedInstances);

        }

        //we add this new classifier to the network
        addVertex(newVertex);

        //clear the misclassified instances array
        this.misclassifiedInstances.clear();

        // clear the stats of the nodes
        for (Integer iterator : network.getNodesIDs()) {
            ((SFNCVertex) network.getNode(iterator)).cleanStats();
        }

        // clear global stats
        this.hits = 0;
        this.instancesInThisPeriod = 0;
                
    }

    private void addVertex(SFNCVertex newVertex) {
        //insere o novo classificador na rede
        if (newVertex != null) {
            // we determine which node will establish 
            // a connection to this new node
            int neighbor = chooseNeighbor();

            //we add the newVertex to the network
            //and attach it to the choosen neighbor
            network.addNode(lastID++, newVertex);
            network.setEdge(neighbor, lastID - 1, 1);

        }
    }

    private int chooseNeighbor() {
        int choosen = -1;
        ArrayList<Integer> nodes = new ArrayList<Integer>(network.getNodesIDs());
        double sumHitRates = 0.0d;
        for (Integer iterator : nodes) {
            sumHitRates += ((SFNCVertex) network.getNode(iterator)).getHitRate();
        }

        //shuffle the nodes
        Collections.shuffle(nodes);

        if (nodes.size() == 1) {
            return nodes.get(0);
        } else {
            double random = Math.random() * 100;
            for (Integer iterator : nodes) {
                random -= (((SFNCVertex) network.getNode(iterator)).getHitRate() / sumHitRates) * 100;
                if (random <= 0.0d) {
                    choosen = iterator;
                    break;
                }
            }
        }
        return choosen;
    }

    private void removeVertex(int vID) {
        if (vID != -1) {
            ArrayList<Integer> vertices
                    = new ArrayList<Integer>(network.getNodesIDs());
            Collections.sort(vertices,
                    new Comparator<Object>() {
                        public int compare(final Object o1, final Object o2) {
                            final SFNCVertex p1 = network.getNode((Integer) o1);
                            final SFNCVertex p2 = network.getNode((Integer) o2);
                            return p1.getHitRate() < p2.getHitRate() ? -1 : (p1.getHitRate() > p2.getHitRate() ? +1 : 0);
                        }
                    });

            ArrayList<Integer> neighbors
                    = new ArrayList<Integer>(network.getNeighborsIDs(vID));

            network.removeNode(vID);

            //we sort the neighbors descendingly according to their hit rate
            Collections.sort(neighbors,
                    new Comparator<Object>() {
                        public int compare(final Object o1, final Object o2) {
                            final SFNCVertex p1 = (network.getNode((Integer) o1));
                            final SFNCVertex p2 = (network.getNode((Integer) o2));
                            return p1.getHitRate() < p2.getHitRate() ? +1 : (p1.getHitRate() > p2.getHitRate() ? -1 : 0);
                        }
                    });

            //pega o vertice com maior hit rate
            //ele será a origem de novas arestas para todos os outros vertices
            int choosenNeighbor = neighbors.get(0);

            //estabelece as novas arestas
            for (int ite = 1; ite < neighbors.size(); ite++) {
                network.setEdge(choosenNeighbor, neighbors.get(ite), 1);

            }
        }
    }

    private int chooseVertexForRemoval() {
        ArrayList<Integer> allNodes
                = new ArrayList<Integer>(network.getNodesIDs());
        //sorts the array in ascending order by comparing hit rates
        Collections.sort(allNodes,
                new Comparator<Object>() {
                    public int compare(final Object o1, final Object o2) {
                        final SFNCVertex p1 = (SFNCVertex) network.getNode((Integer) o1);
                        final SFNCVertex p2 = (SFNCVertex) network.getNode((Integer) o2);
                        return p1.getHitRate() < p2.getHitRate() ? -1 : (p1.getHitRate() > p2.getHitRate() ? +1 : 0);
                    }
                });
        // the first vertex will be the worst one
        if (network.getNodesQuantity() > kMaxOption.getValue()) {
            return allNodes.get(0);
        }
        return -1;
    }

    //the MOA overrided methods
    @Override
    public void resetLearningImpl() {
        network = new Graph<SFNCVertex, Integer>(adoptedMetricOption.getValueAsCLIString());
        this.lastID = 0;
    }

    @Override
    public void trainOnInstanceImpl(Instance instnc) {
        //trains each expert using the instance
        for (Integer iterator : network.getNodesIDs()) {
            ((SFNCVertex) network.getNode(iterator)).trainOnInstance(instnc);
        }
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        Measurement vet[] = new Measurement[1];
        vet[0] = new Measurement("# nodes", network.getNodesQuantity());
        return vet;
    }

    @Override
    public void getModelDescription(StringBuilder sb, int i) {
    }

    @Override
    public boolean isRandomizable() {
        return true;
    }

    @Override
    public double[] getVotesForInstance(Instance instnc) {
        //votes obtained and weighted by the centrality metrics
        double[] votes = new double[instnc.numClasses()];

        //in case the network has no nodes, we add the first
        if (network.getNodesQuantity() == 0) {
            ArrayList<Instance> arr = new ArrayList<Instance>();
            SFNCVertex newVertice = instantiateNewVertex(arr);
            network.addNode(lastID++, newVertice);
        }

        //for each node in the network, we request its vote
        for (Integer iterator : network.getNodesIDs()) {
            int vote = 
                    Utils.maxIndex(((SFNCVertex) network.getNode(iterator)).
                            getVotesForInstance(instnc));            
            votes[vote] += network.getCentralityMetric(iterator);
        }

        //we determine the global prediction
        double globalPrediction = Utils.maxIndex(votes);
        
        if (this.instancesSeen != 0 && (instancesSeen % updatePeriodOption.getValue()) == 0) {
            //atualiza a rede
            updateNetwork();
        }

        //in case the network misclassified the instance, we add it to an array
        if (((int) globalPrediction) != ((int) instnc.classValue())) {
            misclassifiedInstances.add(instnc);
        } else {
            hits++;
        }

        //increments both instances seen and instances of the current period
        instancesInThisPeriod++;
        instancesSeen++;

        //return the votes obtained already weighted               
        return votes;
    }

    @Override
    public String getPurposeString() {
        return "SFNClassifier: A scale-free network algorithm for the classification task.";
    }

    //AUXILIAR METHODS
    private SFNCVertex instantiateNewVertex(ArrayList<Instance> arr) {
        SFNCVertex newVertex = new SFNCVertex((Classifier) getPreparedClassOption(baseLeanerOption));
        for (Instance instance : arr) {
            newVertex.trainOnInstance(instance);
        }
        return newVertex;
    }
}
