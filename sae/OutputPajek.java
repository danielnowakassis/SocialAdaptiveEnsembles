package moa.classifiers.sae;



import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/* Responsible for writing JUNG Graphs to PAJEK project file.  
 * It should be created and updated with the addition of new networks.
 * TODO: Output individual files (.net) that can be read by other softwares */
public class OutputPajek 
{
	private FileWriter networkFile;
	private BufferedWriter networkBuffer;
	private int networkCounter = 0;
	
	/* Create and prepare the PAJEK project (.paj) file for writting */
	public OutputPajek(String fileName, Date now)
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
		fileName  += " " + dateFormat.format(now) + ".paj";
		try
		{
			networkFile = new FileWriter(fileName);
			networkBuffer = new BufferedWriter(networkFile);
		}
		catch(Exception e)
		{
			System.out.println("Not possible to create " + fileName);
		}
	}
	
	/** Append network to the network file. Special case
	 * where it appends only one expert as the whole network. */
	public void addNetwork(Expert expert, int periodLength, long ticks)
	{
		++networkCounter;
		StringBuilder net = new StringBuilder(100);
		net.append("*Network period");
		net.append(networkCounter);
		net.append("\n*Vertices 1\n \"");
		net.append(expert.nodeLabel(periodLength, ticks));
		net.append("\"\n*Edges\n");
		
		try 
		{
			networkBuffer.write(net.toString());
		} 
		catch (IOException e) 
		{
			System.out.println("Not possible to add network " + 
					e.getMessage() + "\n");
			e.printStackTrace();
		}
	}
	
	/* Append the network to the output file. Use UndirectedGraph instead of connections. 
	 * Cannot output edges weight. 
	 */
	public void addNetwork(Graph<Integer, Long> graph, HashMap<Integer, Expert> experts, 
			int periodLength, long ticks, Connections connections)
	{
		++networkCounter;
		StringBuilder header = new StringBuilder(100);
		header.append("*Network period");
		header.append(networkCounter);
		header.append("\n");
		
		StringBuilder vertices = new StringBuilder(400);
		StringBuilder edges = new StringBuilder(1000);
		
		vertices.append("*Vertices ");
		vertices.append(graph.getNodesQuantity());
		vertices.append("\n");
		edges.append("*Edges\n");
		HashMap<Integer, Integer> processedVertices = 
				new HashMap<Integer,Integer>((int) graph.getNodesQuantity());
		
		int verticesCounter = 1;
		
		for(Integer i : graph.getNodesIDs()) 
		{
			processedVertices.put(i, verticesCounter);
			vertices.append(verticesCounter++);
			vertices.append(" \"");
			vertices.append(experts.get(i).nodeLabel(periodLength, ticks));
			vertices.append("\"\n");
		}
		 
		for(Long l : connections.getConnections().keySet())
		{
			int[] pair = Connections.ReverseCantorPair(l);
			edges.append(processedVertices.get(pair[0]));
			edges.append(' ');
			edges.append(processedVertices.get(pair[1]));
			edges.append('\n');
		}
		
		try
		{
			networkBuffer.write(header.toString());
			networkBuffer.write(vertices.toString());
			networkBuffer.write(edges.toString());
			networkBuffer.write("\n\n");
			networkBuffer.flush();
		}
		catch(IOException e)
		{
			System.out.println("Not possible to add network " + 
					e.getMessage() + "\n");
			e.printStackTrace();
		}
	}
	
	/** Append network to the network file */
	public void addNetwork(Connections connections, int periodLength, long ticks)
	{		
		++networkCounter;
		StringBuilder header = new StringBuilder(100);
		header.append("*Network period");
		header.append(networkCounter);
		header.append("\n");
		
		StringBuilder vertices = new StringBuilder(400);
		int verticesCounter = 1;
		StringBuilder edges = new StringBuilder(1000);
		//int edgesCounter = 1;
		HashMap<Integer, Integer> processedVertices = 
			new HashMap<Integer,Integer>(connections.expertsSize());
		
		vertices.append("*Vertices ");
		vertices.append(connections.expertsSize());
		vertices.append("\n");
		
		edges.append("*Edges\n");
		
		if(connections.expertsSize() == 1)
		{
			for(Expert e : connections.getExperts().values())
			{
				vertices.append(verticesCounter++);
				vertices.append(" \"");
				vertices.append(e.nodeLabel(periodLength, ticks));
				vertices.append("\"\n");
			}
		}
		
		for(Connection c : connections.getConnections().values())
		{
			Expert first = c.getFirst(), second = c.getSecond();
			if(! processedVertices.containsKey(first.getID()) )
			{
				processedVertices.put(first.getID(), verticesCounter);
				vertices.append(verticesCounter++);
				vertices.append(" \"");
				if(first.candidate)
					vertices.append("*");
				vertices.append(first.nodeLabel(periodLength, ticks));
				vertices.append("\"\n");
			}
			if(! processedVertices.containsKey(second.getID()) )
			{
				processedVertices.put(second.getID(), verticesCounter);
				vertices.append(verticesCounter++);
				vertices.append(" \"");
				if(second.candidate)
					vertices.append("*");
				vertices.append(second.nodeLabel(periodLength, ticks));
				vertices.append("\"\n");
			}
			
			if(c.isActive())
			{
				edges.append(processedVertices.get(first.getID()));
				edges.append(" ");
				edges.append(processedVertices.get(second.getID()));
				edges.append(" ");
				//edges.append(edgesCounter++);
				edges.append(c.getSc(periodLength));
				edges.append("\n");
			}
		}
		try
		{
			networkBuffer.write(header.toString());
			networkBuffer.write(vertices.toString());
			networkBuffer.write(edges.toString());
			networkBuffer.write("\n\n");
			networkBuffer.flush();
		}
		catch(IOException e)
		{
			System.out.println("Not possible to add network " + 
					e.getMessage() + "\n");
			e.printStackTrace();
		}
	}
	
	public void close()
	{
		try
		{
			networkBuffer.close();
			networkFile.close();
		}
		catch (IOException e) 
		{
			System.out.println("Not possible to close NETWORK file" + 
				e.getMessage() + "\n");
			e.printStackTrace();
		}
	}
}
