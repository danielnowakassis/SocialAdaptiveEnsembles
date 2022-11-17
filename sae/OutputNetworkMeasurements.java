package moa.classifiers.sae;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class OutputNetworkMeasurements {
	private FileWriter measurementsFile;
	private BufferedWriter writeBuffer;
	
	/* Create and prepare the measurements file (.csv) for writting */
	public OutputNetworkMeasurements(String fileName, Date now)
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
		fileName += " " + dateFormat.format(now) + ".csv";
		try
		{
			measurementsFile = new FileWriter(fileName);
			writeBuffer = new BufferedWriter(measurementsFile);
			
			StringBuilder header = new StringBuilder(200);
			header.append("period;instances seen;density;avg degree;vertex count;edges count;#n(n-1)/2;#subnetworks;#ties;network accuracy;#rm by performance;#rm by redundancy;candidate accuracy\n");
			writeBuffer.write(header.toString());
			writeBuffer.flush();
		}
		catch(Exception e)
		{
			System.out.println("Not possible to create " + fileName);
		}
	}
	
	public void addMeasurements(int period, long instancesSeen, double density, double avgDegree, long numVertex, 
			long numEdges, int numSubnetworks, long ties, double txR, long rmByPerformance, long rmByRedundancy, 
			double candidateAccuracy)
	{
		StringBuilder measurements = new StringBuilder(200);
		measurements.append(period);
		measurements.append(";");
		measurements.append(instancesSeen);
		measurements.append(";");
		measurements.append(density);
		measurements.append(";");
		measurements.append(avgDegree);	
		measurements.append(";");
		measurements.append(numVertex);
		measurements.append(";");
		measurements.append(numEdges);
		measurements.append(";");
		measurements.append( ((numVertex*(numVertex-1))/2.0) );
		measurements.append(";");
		measurements.append(numSubnetworks);
		measurements.append(";");
		measurements.append(ties);
		measurements.append(";");
		measurements.append(txR);
		measurements.append(";");
		measurements.append(rmByPerformance);
		measurements.append(";");
		measurements.append(rmByRedundancy);
		measurements.append(";");
		measurements.append(candidateAccuracy);
		measurements.append("\n");
		try {
			writeBuffer.write(measurements.toString());
			writeBuffer.flush();
		} catch (IOException e) {
			System.out.println("Not possible to write to MEASUREMENTS file" + 
					e.getMessage() + "\n");
			e.printStackTrace();
		}
	}
	
	public void close()
	{
		try
		{
			writeBuffer.close();
			measurementsFile.close();
		}
		catch (IOException e) 
		{
			System.out.println("Not possible to close MEASUREMENTS file" + 
				e.getMessage() + "\n");
			e.printStackTrace();
		}
	}
}
