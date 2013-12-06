import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Driver {

	static AtomicInteger msgCount = new AtomicInteger();
	Long startTime, endTime;
	double duration;
	static String addr = "54.200.55.63";
	static int port = 59144;
	static int numOfNodes = 13, totalReq = 65;
	static int numOfReqDone = 0;
	int[] numOfReq;
	int[][] quorums, intersections;
	HashSet<Integer> interscts;

	public Driver() {
		interscts = new HashSet<Integer>();
		intersections = new int[numOfNodes][];
		if (numOfNodes < 8) {
			quorums = new int[2][];
		} else {
			quorums = new int[3][];
		}
		numOfReq = new int[numOfNodes];
		for (int i = 0; i < numOfNodes; i++) {
			numOfReq[i] = 0;
		}
		for (int i = totalReq; i > 0; i--) {
			int p = (int) (Math.random() * numOfNodes);
			numOfReq[p] += 1;
		}
	}

	public void group() {
		int numOfEachGroup = (int) (Math.ceil(numOfNodes / 3.0) + 1);
		for (int i = 0; i < quorums.length; i++) {
			quorums[i] = new int[numOfEachGroup];
		}
		int numOfBorrow;
		if (numOfNodes < 8) {
			int left = 0, right = 0, i;
			if (numOfNodes % 2 == 0) {
				left = right = numOfNodes / 2;
			} else {
				left = numOfNodes / 2;
				right = numOfNodes - left++;
			}

			for (i = 0; i < left; i++) {
				quorums[0][i] = i;
			}
			int k = --i;
			interscts.add(k);
			for (int j = 0; j < right; j++, i++) {
				quorums[1][j] = i;
			}
			int[] inter = new int[1];
			inter[0] = k;
			for (int j = 0; j < numOfNodes; j++) {
				intersections[j] = inter;
			}
		} else {
			int i;
			// Quorum 1
			for (i = 0; i < numOfEachGroup; i++) {
				quorums[0][i] = i;
			}
			int i1 = i--;
			// Quorum 2
			interscts.add(i);
			for (int j = 0; j < numOfEachGroup; j++, i++) {
				quorums[1][j] = i;
			}
			int i2 = i;
			// Quorum3
			numOfBorrow = numOfEachGroup - (numOfNodes - i);
			int left = 0, right = 0;
			if (numOfBorrow % 2 == 0) {
				left = right = numOfBorrow / 2;
			} else {
				left = numOfBorrow / 2;
				right = numOfBorrow - left;
			}
			int counter = 0;
			for (int j = 0; j < left; j++) {
				quorums[2][counter++] = j;
				interscts.add(j);
			}
			int k = i - right;
			for (int j = 0; j < right; j++) {
				quorums[2][counter++] = k + j;
				interscts.add(k + j);
			}
			for (int j = 0; j < numOfNodes - i; j++) {
				quorums[2][counter++] = i + j;
			}

			for (int j = 0; j < numOfNodes; j++) {
				if (interscts.contains(j)) {
					intersections[j] = toIntArray(interscts);
				} else {
					ArrayList<Integer> tmp = new ArrayList<>();
					int m;
					if (j < i1) {
						m = 0;
					} else if (j < i2) {
						m = 1;
					} else {
						m = 2;
					}
					for (int l = 0; l < quorums[m].length; l++) {
						if (interscts.contains(quorums[m][l])) {
							tmp.add(quorums[m][l]);
						}
					}
					intersections[j] = toIntArray(tmp);
				}
			}
		}
	}

	public int[] toIntArray(Collection<Integer> c) {
		int[] inter = new int[c.size()];
		int i = 0;
		for (Integer in : c) {
			inter[i++] = in;
		}
		return inter;
	}

	public static void printToWeb(String s) {
		byte[] b = s.getBytes();

		try {
			DatagramSocket udp = new DatagramSocket();
			DatagramPacket pkt = new DatagramPacket(b, b.length, InetAddress.getByName(addr), port);
			udp.send(pkt);
			udp.close();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void runMaekawaImprovedAlgorithm() {
		ArrayList<ArrayList<Node>> lists = new ArrayList<ArrayList<Node>>();
		Thread[] threads = new Thread[numOfNodes];

		Node[] ps = new Node[numOfNodes];
		for (int i = 0; i < numOfNodes; i++) {
			if (interscts.contains(i)) {
				ps[i] = new IntersectionNode();
			} else {
				ps[i] = new Node();
			}
			lists.add(new ArrayList<Node>());
		}
		for (int i = 0; i < lists.size(); i++) {
			for (int j = 0; j < intersections[i].length; j++) {
				lists.get(i).add(ps[intersections[i][j]]);
			}
		}
		for (int i = 0;i<numOfReq.length;i++){
			numOfReq[i] = 5;
		}
		for (int i = 0; i < ps.length; i++) {
			ps[i].init(i, lists.get(i), numOfReq[i]);
		}
		startTime = System.currentTimeMillis();
		for (int i = 0; i < ps.length; i++) {
			threads[i] = new Thread(ps[i]);
			threads[i].start();
		}
		for (Thread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
			}
		}
		endTime = System.currentTimeMillis();
		duration = (endTime - startTime) / 1000.0;
	}

	public static void main(String args[]) {
		Driver d = new Driver();
		d.group();
		d.runMaekawaImprovedAlgorithm();
		System.out.println("\nAll the requests have been done.");
		System.out.println("Total Nodes: " + Driver.numOfNodes);
		System.out.println("Total Requests: " + Driver.totalReq);
		System.out.println("Total Messages: " + Driver.msgCount);
		System.out.println("Total Time: " + String.valueOf(d.duration) + " s.");
		
	}
}
