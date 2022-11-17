package moa.classifiers.sae;

import java.util.HashMap;

import com.yahoo.labs.samoa.instances.Instance;


/**
 * Includes a 'set' of Connection objects. 
 * @author heitor */
public class Connections {

	/** Cantor's Pairing function is used to encode 2 Expert's IDs into 1 connection ID.
	 * Initial capacity = 440 is sufficient to store all possible connections 
	 * for 30 different experts. Although, this does not constraints the 
	 * HashMap to grow larger (See Java.Util.HashMap. HashMap<K,V>(int)). */
	protected HashMap<Long, Connection> connections = 
		new HashMap<Long, Connection>(440);
	/** Aggregation. Created and maintained outside the scope of this class */
	protected final HashMap<Integer, Expert> experts;
	
	public Connections(HashMap<Integer, Expert> experts) {
		this.experts = experts;
	}

	/**
	 * Reset all connections. */
	public void reset()	{
		for(Connection c : connections.values())
			c.reset();
	}
	
	/** Cantor Pairing Function. Receives 2 integers (converted to long for 
	 * calculations) which are encoded into an unique and reversible integer 
	 * The data type for the encoded integer is long to prevent overflow. 
	 * Original function: (long) (((x + y) * (x + y + 1)) / 2.0 + y)
	 * Modified function: (long) (((x + y)/2.0)*((x + y + 1)/2.0) * 2 + y)
	 * These simple changes on the modified version prevent overflow during 
	 * the multiplication on the original version ((x+y) * (x + y + 1))*/
	public static long MakeCantorPair(long x, long y) {
		return (long) (((x + y)/2.0)*((x + y + 1)/2.0) * 2 + y);
	}
	
	/** Reverse the encoded value z into x and y */
	public static int[] ReverseCantorPair(long z) {
		int[] pair = new int[2];
		long t = (long) Math.floor((Math.sqrt(1.0 + 8 * z) - 1.0)/2.0);
		long x = (long) (t * (t + 3) / 2.0 - z);
		long y = (long) (z - t * (t + 1) / 2.0);
		pair[0] = (int) x;
		pair[1] = (int) y;
		return pair;
	}
	
	/** Create all possible connections to an expert. 
	 * @param expert */
	public void addAllConnections(Expert expert) {
		int newID = expert.getID();
		for(Expert old : experts.values()) {
			int oldID = old.getID();
			if(newID != oldID)
				connections.put(Connections.MakeCantorPair(oldID, newID), 
						new Connection(old, expert));
		}
	}

	/** Remove all connections that an expert has. 
	 * @param rid expert id */
	public void removeAllConnections(int rid) {
		for(Integer nid : experts.keySet())	{
			if(nid < rid)
				connections.remove(Connections.MakeCantorPair(nid, rid));
			else
				connections.remove(Connections.MakeCantorPair(rid, nid));
		}
	}

	/** Activate/deactivate connections based on its Cs. 
	 * @param network
	 * @param periodLength
	 * @param csMin */
	public void update(Graph<Integer, Long> network, int periodLength,
			double scMin) {
		for(Connection c : connections.values()) {
			if(c.getSc(periodLength) < scMin) {
				//if(network.getEdge(c.getFirst().getID(), c.getSecond().getID()) != null) {
				network.removeEdge(c.getFirst().getID(), c.getSecond().getID());
				c.setActive(false);
				//}
			}
			else {
				network.setEdge(c.getFirst().getID(), c.getSecond().getID(), c.getID());
				c.setActive(true);
			}
		}
	}

	/** If experts had same prediction on the last instance, update sameAction counter.
	 * @param instance */
	public void updateSimilarities(Instance instance) {
		for(Connection c : connections.values())
			c.updateActionCounter();
	}
	
	/* Accessors */
	public int expertsSize() {
		return experts.size();
	}
	public HashMap<Integer, Expert> getExperts() {
		return experts;
	}
	public HashMap<Long, Connection> getConnections() {
		return connections;
	}
	
	public String toString() {
		StringBuilder strb = new StringBuilder();
		strb.append("Connections size: " + connections.size() + "\n");
		for(Connection c : connections.values()) {
			strb.append(c.toString() + "\n");
		}
		return strb.toString();
	}
	public String toString(int idexpert) {
		StringBuilder strb = new StringBuilder();
		strb.append("Connections for expert id: " + idexpert + "\n");
		for(Connection c : connections.values()) {
			if(c.getFirst().getID() == idexpert || c.getSecond().getID() == idexpert)
				strb.append(c.toString() + "\n");
		}
		return strb.toString();
	}
}
