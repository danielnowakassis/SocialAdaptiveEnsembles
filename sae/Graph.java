package moa.classifiers.sae;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;


/**
 * Undirected Graph. It uses edge lists to represent connections between two
 * given nodes. In this class, any method m that receives two nodes is going to
 * output the same result if called as m(v,u) or m(u,v), i.e., order is not
 * relevant (undirected edges).
 *
 * @author Heitor M. Gomes and Jean Paul Barddal
 *
 * @param <N> Node value
 * @param <E> Edge value
 */
public class Graph<N, E> {

    /**
     * Node has an unique ID, it shall not repeat within the same Graph. 
     * This ID is used as the key value for the graph Hash Table of Nodes, 
     * therefore it SHOULD NEVER CHANGE. That is the reason why there is not a 
     * mutator for it. The node value (N value) is a reference to an external 
     * object, therefore it might change accordingly to user's needs. Every 
     * node has a template value N and a Hash map to the Nodes it can reach, 
     * such that: 
     * - key = Node unique ID 
     * - value = E (edge value or reference)
     *
     * @author heitor
     */
    private class Node {

        // Unique ID. Must not repeat for the same Graph. 
        final public int ID;
        // All nodes directly connected to this
        public HashMap<Node, E> neighbors = new HashMap<Node, E>();
        // Node value. 
        public N value;

        public Node(int ID, N value) {
            this.ID = ID;
            this.value = value;
        }

        /**
         * @param u Neighbor node
         * @return edge value for neighbor u or null if not adjacent to u
         */
        public E getEdgeForNeighbor(Node u) {
            return neighbors.get(u);
        }

        /**
         * @return All neighbors of this node in a List
         */
        public List<N> getNeighbors() {
            List<N> neighborsValues = new ArrayList<N>();
            for (Node node : neighbors.keySet()) {
                neighborsValues.add(node.value);
            }
            return neighborsValues;
        }

        // MUTATORS (change the internal state)
        public void addAdjacent(Node v, E e) {
            if (!neighbors.containsKey(v)) {
                neighbors.put(v, e);
            }
        }

        public void setEdgeForAdjacent(Node u, E e) {
            neighbors.put(u, e);
        }

        /**
         * Remove the connection between the current node and u. If the graph
         * is undirected this method MUST be called for node u as well. 
         * @param Node u
         */
        public void removeAdjacent(Node u) {
            neighbors.remove(u);
        }

        // Administrivia methods... 
        @Override
        public int hashCode() {
            return ID;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            } else {
                // Safe to cast, since class is checked by the if statement
                @SuppressWarnings("unchecked")
                Node node = (Node) obj;
                if (this.ID == node.ID) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder("Node ");
            str.append(ID);
            str.append(": (");
            str.append(value);
            str.append(") Edges: ");
            for (Node n : neighbors.keySet()) {
                str.append(n.ID);
                str.append("{");
                str.append(n.value);
                str.append("}");
                str.append("-[");
                str.append(neighbors.get(n));
                str.append("] ");
            }
            return str.toString();
        }

        public ArrayList<Node> getNeighboors() {
            return new ArrayList<Node>(this.neighbors.keySet());
        }
    }
    
    private HashMap<Integer, Node> nodes
            = new HashMap<Integer, Graph<N, E>.Node>();
    private long edgeCounter;

    // Only for SFNClassifier
    private String choosenCentralityMetric;
    private boolean metricUpdated;
    private HashMap<Integer, Float> metricValues;

    public final String CLOSENESS = "Closeness";
    
    
    //CONSTRUCTOR
    //Note that choosenCentralityMetric will be used only with SFNClassifier
    public Graph(String choosenCentralityMetric) {
        this.choosenCentralityMetric = choosenCentralityMetric;
        this.nodes = new HashMap<Integer, Graph<N, E>.Node>();
        this.edgeCounter = 0;
    }
    
    public Graph() {
    	this.choosenCentralityMetric = null;
    }

    // ACCESSORS (do not change internal state)
    public long getNodesQuantity() {
        return nodes.size();
    }

    public long getEdgesQuantity() {
        return edgeCounter;
    }

    /**
     * If there is no edge between v and u, returns null
     *
     * @param vID id of node v
     * @param uID id of node u
     * @return The Edge value (E) for the given nodes v and u
     */
    public E getEdge(int vID, int uID) {
        Node v = nodes.get(vID), u = nodes.get(uID);
        if (v != null) {
            return v.getEdgeForNeighbor(u);
        }
        return null;
    }
    
    /**
     * If there is no node with the given ID, returns null
     *
     * @param ID id of requested node
     * @return Node object corresponding to the given ID
     */
    public N getNode(int ID) {
        Node v = nodes.get(ID);
        if (v != null) {
            return v.value;
        }
        return null;
    }

    public List<Integer> getNodesIDs() {
        return new ArrayList<Integer>(this.nodes.keySet());
    }
    
    /**
     * Return null if graph does not contain node v and an empty list if it
     * exists but has no neighbors.
     *
     * @param ID id of node
     * @return all neighbors of node
     */
    public List<N> getNeighbors(int ID) {
        Node v = nodes.get(ID);
        if (v != null) {
            return v.getNeighbors();
        }
        return null;
    }

    /**
     * Return null if graph does not contain node v and an empty list if it
     * exists but has no neighbors.
     *
     * @param ID id of node v
     * @return all neighbors' ids of v
     */
    public List<Integer> getNeighborsIDs(int ID) {
        Node v = nodes.get(ID);
        if (v != null) {
            ArrayList<Integer> rs = new ArrayList<Integer>();
            for (Node n : v.getNeighboors()) {
                rs.add(n.ID);
            }
            return rs;
        }
        return null;
    }

    /**
     * Total number of existing edges divided by the maximum number of edges
     *
     * @return edge density, i.e., #edges / (#nodes*(#nodes-1)/2)
     */
    public double getEdgeDensity() {
    	long numPossibleEdges = getNodesQuantity() * (getNodesQuantity()-1);
        return numPossibleEdges == 0 ? 0 : 
        	getEdgesQuantity() / (getNodesQuantity() * (getNodesQuantity() - 1) / 2.0);
    }

    /**
     * Uses breadth first-search to find the geodesic distance between two nodes
     * identified by theirs unique IDs (vID and uID). Return: -1 = uID is
     * unreachable through vID or null = node with vID or node with uID does not
     * exists int <> -1 = geodesic length between vID and uID
     *
     * @param vID id of node v
     * @param uID id of node u
     * @return The geodesic length between nodes vID and uID
     */
    public Integer geodesicDistance(int vID, int uID) {

        class GeoNode {
            int nodeID;
            int lengthFromBegin;

            GeoNode(int nodeID, int lengthFromBegin) {
                this.nodeID = nodeID;
                this.lengthFromBegin = lengthFromBegin;
            }
        }

        Node v = nodes.get(vID), u = nodes.get(uID);
        if (v == null || u == null) {
            return null;
        }

        Set<Integer> explored = new HashSet<Integer>();
        Queue<GeoNode> frontier = new ArrayDeque<GeoNode>();

        frontier.add(new GeoNode(v.ID, 0));

        while (!frontier.isEmpty()) {
            GeoNode current = frontier.remove();
            explored.add(current.nodeID);

            if (current.nodeID == uID) {
                return new Integer(current.lengthFromBegin);
            }

            for (Integer n : getNeighborsIDs(current.nodeID)) {
                if (!explored.contains(n)) {
                    boolean isInFrontier = false;
                    for (GeoNode f : frontier) {
                        if (f.nodeID == n) {
                            isInFrontier = true;
                            break;
                        }
                    }
                    if (!isInFrontier) {
                        frontier.add(new GeoNode(n, current.lengthFromBegin + 1));
                    }
                }
            }
        }

        return new Integer(-1);
    }

    // MUTATORS (change the internal state)
    /**
     * If any (or both) nodes does not exist, nothing is added. If there is
     * already an edge between these nodes it replaces the current edge value by
     * the new the existing edge. Otherwise edge is created (i.e. activated).
     *
     * @param vID id of node v
     * @param uID id of node u
     * @param e edge value
     */
    public void setEdge(int vID, int uID, E e) {
        Node v = nodes.get(vID), u = nodes.get(uID);
        if (v != null && u != null) {
            if (v.getEdgeForNeighbor(u) != null) {
                v.setEdgeForAdjacent(u, e);
                u.setEdgeForAdjacent(v, e);
            } else {
                v.addAdjacent(u, e);
                u.addAdjacent(v, e);
                ++edgeCounter;
            }
            //updates the flag for the centrality metrics
            metricUpdated = false;
        }
    }

    /**
     * Add a node as long as it id does not exists already (vID is unique). If
     * it already exists, then simply do not add it.
     *
     * @param vID id of node v to be added
     * @param v value of node
     */
    public void addNode(int vID, N v) {
        if (!nodes.containsKey(vID)) {
            nodes.put(vID, new Node(vID, v));
        }

        //updates the flag for the centrality metrics
        metricUpdated = false;
    }

    /**
     * Remove node v from Graph. If v is not in the Graph, nothing happens.
     * This method removes all edges that v has before removing it. 
     * @param vID id of node v to be removed
     */
    public void removeNode(int vID) {
        Node node = nodes.get(vID);
        if(node != null) {
        	Iterator<Entry<Node, E>> it = node.neighbors.entrySet().iterator();
        	while(it.hasNext()) {
        		Entry<Node, E> current = (Entry<Node, E>) it.next();
        		Node neighbor = current.getKey();
        		
        		neighbor.removeAdjacent(node);
        		it.remove();
        		--edgeCounter;
        	}
        	nodes.remove(vID);
            //updates the flag for the centrality metrics
            metricUpdated = false;
        }
    }

    /**
     * Remove edge between node v and u. If v or u does not exists or there is no edge between then, 
     * nothing happens. 
     * @param vID
     * @param uID
     */
    public void removeEdge(int vID, int uID) {
    	Node v = nodes.get(vID), u = nodes.get(uID);
    	if (v != null && u != null && getEdge(vID, uID) != null) {
    		v.removeAdjacent(u);
    		u.removeAdjacent(v);
    		--edgeCounter;
    	}
        //updates the flag for the centrality metrics
        metricUpdated = false;
    }

    /* Measures of centrality */

    /**
     * Get the centrality metric for a given node vID
     * @param vID
     * @return the value of the metric
     */
    public float getCentralityMetric(int vID) {
        if (this.choosenCentralityMetric != null) {
            calculateCentralityMetric();
            return metricValues.get(vID);
        }
        return -1.0f;
    }

    public HashMap<Integer, Float>  getCentralityMetric() {
    	if (this.choosenCentralityMetric != null) 
            calculateCentralityMetric();
    	return metricValues;
    }
    
    /**
     * Method that triggers the update of the chosen metric of centrality. 
     * It just updates the metric if there were changes in the graph since last update. 
     * Also, it uses the attribute "chosenCentralityMetric" to decide which metric must
     *  be computed. 
     */
    private void calculateCentralityMetric() {
        if (!metricUpdated) {
            if (this.getNodesQuantity() < 3 || this.choosenCentralityMetric.equals("Degree")) {
                this.metricValues = calculateDegree();
            } else if (this.choosenCentralityMetric.equals("Betweenness")) {
                this.metricValues = calculateBetweenness();
            } else if (this.choosenCentralityMetric.equals("Closeness")) {
                this.metricValues = calculateCloseness();
            } else if (this.choosenCentralityMetric.equals("Eigenvector")) {
                this.metricValues = calculateKatzCentrality(0.0f);
            } else if (this.choosenCentralityMetric.equals("Pagerank")) {
                this.metricValues = calculateKatzCentrality(0.5f);
            }
        }
        metricUpdated = true;
    }

    //Betweenness
    private HashMap<Integer, Float> calculateBetweenness() {
        //declara��o de Cb
        HashMap<Integer, Float> cb = new HashMap<Integer, Float>();
        //inicializa��o de Cb
        for (Integer i : this.nodes.keySet()) {
            cb.put(i, 0.0f);
        }
        //declara��o de sigma
        HashMap<Integer, Float> sigma = new HashMap<Integer, Float>();

        //declara��o de d
        HashMap<Integer, Float> d = new HashMap<Integer, Float>();

        //declara��o de delta
        HashMap<Integer, Float> delta = new HashMap<Integer, Float>();

        for (Integer s : this.nodes.keySet()) {
            //declara��o de S
            Stack<Integer> stack = new Stack<Integer>();

            //declara��o de P
            HashMap<Integer, ArrayList<Integer>> p = new HashMap<Integer, ArrayList<Integer>>();
            for (Integer i : this.nodes.keySet()) {
                p.put(i, new ArrayList<Integer>());
            }

            //declara��o de Q
            LinkedList<Integer> queue = new LinkedList<Integer>();

            //inicializa��o de sigma e de d
            for (Integer i : nodes.keySet()) {
                sigma.put(i, 0.0f);
                d.put(i, -1.0f);
            }
            sigma.put(s, 1.0f);
            d.put(s, 0.0f);

            //push em Queue o s
            queue.push(s);

            while (queue.size() != 0) {
                int v = queue.remove();
                stack.push(v);

                for (Integer w : getNeighborsIDs(v)) {
                    if (d.get(w) < 0) {
                        queue.push(w);
                        d.put(w, d.get(v) + 1);
                    }

                    if (d.get(w) == d.get(v) + 1) {
                        sigma.put(w, sigma.get(w) + sigma.get(v));
                        p.get(w).add(v);
                    }
                }
            }
            //inicializa��o de delta
            for (Integer i : nodes.keySet()) {
                delta.put(i, 0.0f);
            }
            //while Stack is not empty
            while (!stack.empty()) {
                int w = stack.pop();
                for (Integer e : p.get(w)) {
                    delta.put(e, delta.get(e) + (sigma.get(e) / sigma.get(w)) * (1 + delta.get(w)));
                }
                if (w != s) {
                    cb.put(w, cb.get(w) + delta.get(w));
                }
            }
        }

        //normalization
        float sum = 0.0f;
        for (Integer i : cb.keySet()) {
            sum += cb.get(i);
        }
        for (Integer i : cb.keySet()) {
            cb.put(i, cb.get(i) / sum);
        }
        return cb;
    }

    //Degree
    private HashMap<Integer, Float> calculateDegree() {
        //hashmap de resultado
        HashMap<Integer, Float> rs = new HashMap<Integer, Float>();
        //vertices do grafo
        ArrayList<Integer> vertices = new ArrayList<Integer>(nodes.keySet());

        //armazena o grau dos n�s
        for (Integer i : vertices) {
            rs.put(i, (float) this.getNeighbors(i).size());
        }

        //normalize the result
        float sum = 0.0f;
        //calculates the sum of the values obtained
        for (Integer i : rs.keySet()) {
            sum += rs.get(i);
        }
        //updates the values
        if (sum != 0.0f) {
            for (Integer i : rs.keySet()) {
                rs.put(i, rs.get(i) / sum);
            }
        } else {
            for (Integer i : rs.keySet()) {
                rs.put(i, 1 / (float) this.getNodesQuantity());
            }
        }

        //retorna o resultado
        return rs;
    }

    /**
     * Note: This Closeness Centrality calculates Cc(v) values within
     * components, i.e. it works for multipartite graphs. Cc(v) = (N-1)/[sum(u
     * in graph) geodesicDistance(v,u]. There is no issue when v = u, since it
     * will add 0 to the sum. Also, if for any u geodesicDistance(v,u) == -1 (u
     * is unreachable from v and vice-versa) this contributes as 0 to Cc(v) as
     * well.
     *
     * @return For every node in the graph (key), its normalized Closeness
     * Centrality measure (value).
     */
    private HashMap<Integer, Float> calculateCloseness() {
        class CcNode {
            int nodeID;
            int lengthFromBegin;

            CcNode(int nodeID, int lengthFromBegin) {
                this.nodeID = nodeID;
                this.lengthFromBegin = lengthFromBegin;
            }
        }

        HashMap<Integer, Float> rs = new HashMap<Integer, Float>();
        for (Integer vID : getNodesIDs()) {
            int sum = 0;
            // bfs for each node
            Set<Integer> explored = new HashSet<Integer>();
            Queue<CcNode> frontier = new ArrayDeque<CcNode>();

            frontier.add(new CcNode(vID, 0));

            while (!frontier.isEmpty()) {
                CcNode current = frontier.remove();
                explored.add(current.nodeID);
                sum += current.lengthFromBegin;

                for (Integer n : getNeighborsIDs(current.nodeID)) {
                    if (!explored.contains(n)) {
                        boolean isInFrontier = false;
                        for (CcNode f : frontier) {
                            if (f.nodeID == n) {
                                isInFrontier = true;
                                break;
                            }
                        }
                        if (!isInFrontier) {
                            frontier.add(new CcNode(n, current.lengthFromBegin + 1));
                        }
                    }
                }
            }
            rs.put(vID, sum == 0 ? 0.0f : (getNodesQuantity() - 1) / (float) sum);
            //rs.put(vID, sum == 0 ? 0.0f : getNodesQuantity()/(float)sum);
        }

        float sum = 0.0f;
        for (Integer i : nodes.keySet()) {
            sum += rs.get(i);
        }
        for (Integer i : nodes.keySet()) {
            rs.put(i, rs.get(i) / sum);
        }

        return rs;
    }

    //Pagerank
    private HashMap<Integer, Float> calculateKatzCentrality(float alpha) {
        HashMap<Integer, Float> rs = new HashMap<Integer, Float>();
        HashMap<Integer, Float> old = new HashMap<Integer, Float>();
        HashMap<Integer, Float> current = new HashMap<Integer, Float>();

        //distribute pagerank values equally
        //pr(p;0) = 1/n;
        for (Integer i : nodes.keySet()) {
            old.put(i, (float) 1 / nodes.keySet().size());
        }

        boolean converged = false;

        //at each step of the computation, we have
        //pr(p_i; t+1) = ((1-d)/n) + d * (sum_p_j belongs to M(pi)) (pr(p_j; t) / L(p_j))
        //M(p_i) is the set of nodes that neighbours i
        //d is the parameter
        //L(p_j) is the # of neighbors of j
        //do it until it converges
        
        
        //float d = 0.5f;

        while (!converged) {
            for (Integer i : nodes.keySet()) {
                float sum = 0.0f;
                ArrayList<Integer> neighbors = (ArrayList<Integer>) getNeighborsIDs(i);
                for (Integer j : neighbors) {
                    sum += old.get(j) / (float) this.getNeighborsIDs(j).size();
                }
                current.put(i, ((1 - (float) alpha) / (float) nodes.keySet().size()) + alpha * sum);

                //System.out.println("v:" + i + ": " + current.get(i));
            }
            //System.out.println("-------------------");

            //determines whether results converged
            converged = true;
            for (Integer c : current.keySet()) {
                if (Math.abs(current.get(c) - old.get(c)) > 0.00001f) {
                    converged = false;
                    break;
                }
            }

            //copy current into old
            for (Integer c : current.keySet()) {
                old.put(c, current.get(c));
            }

        }

        //copy current to rs
        for (Integer i : nodes.keySet()) {
            rs.put(i, current.get(i));
        }

        float sum = 0.0f;
        for (Integer i : nodes.keySet()) {
            sum += rs.get(i);
        }
        for (Integer i : nodes.keySet()) {
            rs.put(i, rs.get(i) / sum);
        }

        return rs;
    }

    /**
     * Generate all the weakly connected components (WCC) or simply, connected components of an
     * undirected graph. 
     * @return collection containing k sets, each of which represent each component in the graph
     */
    public Collection<Set<Integer>> WeaklyConnectedComponents() {
    	List<Set<Integer>> wcc = new ArrayList<Set<Integer>>();
    	// Set of nodes already assigned to a component
    	Set<Integer> assigned = new HashSet<Integer>();
    	// List of nodes that have not been tested yet. Some of them may have already been assigned
    	// to a component because they are connected to a node that has been explored already. 
    	List<Integer> unexplored = getNodesIDs();
		
    	while(!unexplored.isEmpty()) {
    		Set<Integer> component = new HashSet<Integer>();
    		Node start = nodes.get(unexplored.get(0));
    		
    		assert start != null;
    		
    		while(assigned.contains(start.ID) && !unexplored.isEmpty()) {
    			start = nodes.get(unexplored.get(0));
        		unexplored.remove(0);
    		}
    		if(unexplored.isEmpty() && (start == null || assigned.contains(start.ID)))
    			return wcc;
    		
           	// DFS Stack
    		Stack<Integer> frontier = new Stack<Integer>();
    		frontier.push(start.ID);
    		while(!frontier.isEmpty()) {
    			Integer idTop = frontier.pop();
    			Node node = nodes.get(idTop);
    			if(node == null)
    				System.out.println(nodes);
    			component.add(node.ID);
    			assigned.add(node.ID);
    			for(Node neighbor : node.neighbors.keySet()) 
    				if(!assigned.contains(neighbor.ID)) 
    					frontier.add(neighbor.ID);
    		}
    		wcc.add(component);
    	}
    	return wcc;
    }
    
    // DEBUG methods
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();

        for (Integer id : nodes.keySet()) {
            str.append(nodes.get(id).toString());
            str.append("\n");
        }
        return str.toString();
    }
}
