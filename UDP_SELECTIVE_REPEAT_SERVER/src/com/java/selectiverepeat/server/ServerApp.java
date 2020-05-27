package com.java.selectiverepeat.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.java.selectiverepeat.server.entity.ServerPacket;
import com.java.selectiverepeat.server.helper.ServerHelper;

public class ServerApp {
	public static Set<Integer> PacketHistory = new HashSet<>();
	public static Map<Integer, ServerPacket> currentWindowData = new HashMap<>();
	public static Integer TOTALPACKETLOSSCOUNT = 0;
	public static String fileName;
	public static Integer windowSize = 0;
	public static Integer expectedWindowSize = 0;
	public static PrintWriter writer;

	public static void receiveData(int portNumber, double probabilityPacketLoss)
			throws IOException, InterruptedException {

		DatagramSocket ds = new DatagramSocket(portNumber);
		System.out.println("Server listening.. .... .... .....");
		byte[] b1 = new byte[1024];
		String tempFileName = System.getProperty("java.class.path") + "\\..\\resources\\serverfiles\\" + fileName.trim()
				+ ".txt";
		File baseFile = new File(tempFileName);
		baseFile.createNewFile();
		writer = new PrintWriter(tempFileName, "UTF-8");

		while (true) {
			DatagramPacket dp = new DatagramPacket(b1, b1.length);
			ds.receive(dp);
			ServerPacket currentPacket = ServerHelper.decipherPacket(b1); // deserializing
			int sequenceNumber = currentPacket.getSequenceNumber(); // seq-no
			// check if the correct packet is received or else dont send an acknowledgment
			// Does pak-hist alread contains pak-seq then send duplicate pck
			if (PacketHistory.contains(sequenceNumber)) {
				System.out.println("Received Duplicate packet " + sequenceNumber);
				byte[] acknowledgmentNumberBytes = ServerHelper.getAcknowledgmentNumber(sequenceNumber);
				DatagramPacket dp2 = new DatagramPacket(acknowledgmentNumberBytes, acknowledgmentNumberBytes.length,
						dp.getAddress(), dp.getPort());
				ds.send(dp2);
			} else {
				// else chech if the rec-pck-seq is with in expected window size
				// eg : 50 <40 invalid
				// eg o <1 ||5 < 9 : valid
				if (sequenceNumber < expectedWindowSize) {
					byte[] acknowledgmentNumberBytes = ServerHelper.getAcknowledgmentNumber(sequenceNumber);
					DatagramPacket dp2 = new DatagramPacket(acknowledgmentNumberBytes, acknowledgmentNumberBytes.length,
							dp.getAddress(), dp.getPort());
					// TimeUnit.MILLISECONDS.sleep(50);
					ds.send(dp2);
					System.out.println("Packet No : " + sequenceNumber + "\t");
					// packet stored with seq as key and data as value
					currentWindowData.put(sequenceNumber, currentPacket);
					// keeping track of pck-rec
					PacketHistory.add(sequenceNumber);
					/*
					 * System.out.println("currentWindowdata" + currentWindowData.size()
					 * +"window Size: "+ windowSize);
					 */
					// eg : 5-buffered >= 5- windowsize then write into file and clear the buffer
					if (currentWindowData.size() >= windowSize) {
						/* System.out.println("Inside if loop"); */
						writeToFile();
					}
				} else {
					// else drop packet without sending ack
					System.out.println("Packet loss, sequence number:" + sequenceNumber);
					expectedWindowSize = expectedWindowSize + windowSize;
					TOTALPACKETLOSSCOUNT = TOTALPACKETLOSSCOUNT + 1;
					System.out.println("TotalPacketLoss: " + TOTALPACKETLOSSCOUNT);
				}

			}
			/*
			 * System.out.println(sequenceNumber+"received sequencenumber");
			 * System.out.println(expectedWindowSize + "expected Window Size.");
			 */

		}
	}

	public static void writeToFile() throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
		for (Integer iterator : currentWindowData.keySet()) {

			writer.write(currentWindowData.get(iterator).getData().trim());
			out.flush();
		}
		out.close();
		currentWindowData.clear();

	}

	public static void main(String[] args) throws IOException, InterruptedException {
		int portNumber;
		double probability;

		if (args.length != 4) {
			System.out.println("The total number of arguments required are 4");
			System.out.println("The arguments are portNumber,fileName ,probability for packet loss, windowsize");
		} else {
			portNumber = Integer.parseInt(args[0]);
			fileName = args[1];
			probability = Double.parseDouble(args[2]);
			windowSize = Integer.parseInt(args[3]);
			expectedWindowSize = Integer.parseInt(args[3]);
			receiveData(portNumber, probability);
			writer.close();
		}
	}
}
