package moa.classifiers.sae;



/**
 * Represents the connection between two Experts. 
 * Its ID is defined by its underlying experts IDs using
 * the CantorPairing function. 
 * @author heitor */
public class Connection {
	/* Connection unique identifier set by the Cantor Pairing Function using 
	 * expert's IDs (x, y), where x.createdOn < y.createdOn and x != y */
	protected long ID;
	/* First expert */
	protected Expert first;
	/* Second expert */
	protected Expert second;
	/* When first and second have the same decision this is updated */
	protected int sameActions;
	/* Whether this connection is active or not */
	protected boolean active;
	
	public Connection(Expert first, Expert second) {
		assert first.getID() < second.getID();
		this.ID = Connections.MakeCantorPair(first.getID(), second.getID());
		this.first = first;
		this.second = second;
		reset();
	}
	/** Only reset the sameActions counter. 
	 * DOES NOT reset active flag, since it would change network structure. */
	public void reset() {
		sameActions = 0;
	}
	/** Use lastPredictedClass from both experts to check if they predicted the same class */
	public void updateActionCounter() { 
		if(first.getLastPredictedClass() == second.getLastPredictedClass())
			++sameActions;
	}
	
	/**
	 * One connection is said to be redundant iif: 
	 * It is active and its sc is greater than or equal to scMax. 
	 * The connection can be removed even if it is not active. 
	 * @param csMax 
	 * @param periodLength */
	public boolean isRedundant(double scMax, int periodLength) {
		if(getSc(periodLength) >= scMax/* && active*/)
			return true;
		return false;
	}
	
	/* Accessors */
	public long getID()	{
		return ID;
	}
	public boolean isActive() {
		return active;
	}
	/** Similarity coefficient (Sc) given the period length */
	public double getSc(int periodLength) {
		return sameActions / (double) periodLength;
	}
	public Expert getFirst() {
		return first;
	}
	public Expert getSecond() {
		return second;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder(100);
		str.append(ID);
		str.append(", [firstId=");
		str.append(first.getID());
		str.append(", secondId=");
		str.append(second.getID());
		str.append("], sameActions=");
		str.append(sameActions);
		str.append(", ");
		str.append(active ? "active" : "inactive");
	/*	str.append(", checksum: ");
		int members[] = AbstractConnections.ReverseCantorPair(ID);
		str.append(members[0]);
		str.append("->");
		str.append(first.hashCode());
		str.append(", ");
		str.append(members[1]);
		str.append("->");
		str.append(second.hashCode()); */
		return str.toString();
	}
	/* Mutators */
	public void setActive(boolean value)	{
		active = value;
	}

}
