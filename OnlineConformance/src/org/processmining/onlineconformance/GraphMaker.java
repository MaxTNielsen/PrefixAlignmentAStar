package org.processmining.onlineconformance;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class GraphMaker {

	public static void main(String[] args) throws IOException {

		File folder = new File("D:\\temp\\onlineconformance\\LM1MM1T10\\tree 48 n3");

		TIntIntMap depths = new TIntIntHashMap(10, 0.7f, -1, -1);
		TIntIntMap queues = new TIntIntHashMap(10, 0.7f, -1, -1);
		int d = 0;
		int q = 0;
		for (File f : folder.listFiles()) {
			String name = f.getName();

			if (name.startsWith("statistics") && name.contains("fin")) {

				int start = name.indexOf('d');
				int depth = Integer.parseInt(name.substring(start + 1, start + 3).trim());
				start = name.indexOf('q');
				int queue = Integer.parseInt(name.substring(start + 1, name.indexOf('.') - 4).trim());
				System.out.println(depth + "," + queue);
				if (depths.putIfAbsent(depth, d) < 0) {
					d++;
				}
				if (queues.putIfAbsent(queue, q) < 0) {
					q++;
				}
			}
		}
		int[] qs = new int[queues.size()];
		int[] ds = new int[depths.size()];

		double[][] times = new double[depths.size()][queues.size()];
		int[][] cnt = new int[depths.size()][queues.size()];
		Set<String> optimal = new HashSet<String>();
		String[] headers = null;
		for (File f : folder.listFiles()) {
			String name = f.getName();
			if (name.startsWith("statistics") && name.contains("fin")) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
				String line = reader.readLine();
				if (headers == null) {
					headers = line.split(";");
				}
				long sumDCost = 0;
				line = reader.readLine();
				do {
					String[] cols = line.split(";");
					d = depths.get(Integer.parseInt(cols[0]));
					ds[d] = Integer.parseInt(cols[0]);
					q = queues.get(Integer.parseInt(cols[2]));
					qs[q] = Integer.parseInt(cols[2]);
					times[d][q] += Double.parseDouble(cols[11]);
					sumDCost += Integer.parseInt(cols[23]);
					cnt[d][q]++;
					line = reader.readLine();
				} while (line != null);
				reader.close();
				if (sumDCost == 0) {
					// optimality reached.
					optimal.add(name);
				}
			}

		}

		System.out.println(optimal.toString());

		System.out.println("depth\tqueue\ttime per trace");
		for (d = 0; d < times.length; d++) {
			for (q = 0; q < times[d].length; q++) {
				if (cnt[d][q] > 0) {
					System.out.print(ds[d] + "\t");
					System.out.print(qs[q] + "\t");
					System.out.println(times[d][q] / cnt[d][q]);
				}
			}

		}
	}
}
