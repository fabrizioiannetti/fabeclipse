package plots.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataImporter {

	private List<int[]> data = new ArrayList<>();

	public int getCount() {
		return data.size();
	}

	public int[] getData(int index) {
		return data.get(index);
	}

	@SuppressWarnings("unchecked")
	public void importFromText(String text, String pattern) {
		ArrayList<int[]> chunks[] = null;
		int[][] chunk;
		int count = 0;
		int total = 0;
		int groups = -1;

		data.clear();
		// check how many groups we'll find
		Matcher matcher = Pattern.compile(pattern).matcher(text);
		Scanner s = new Scanner(text);
		// TODO: investigate the lambda version
//		s.forEachRemaining(l -> {});
		while (s.hasNextLine() && groups < 0) {
			String line = s.nextLine();
			if (matcher.reset(line).find()) {
				// first match. determine group count
				groups = matcher.groupCount();
			}
		}
		s.close();
		if (groups < 0) {
			// no data found
			return;
		}

		System.out.println("groups:" + groups);
		int gb = 0;
		if (groups > 0) {
			chunks = new ArrayList[groups];
			gb = 1; // first group is 1, set base group to 1
		} else {
			chunks = new ArrayList[1];
			// just one group, leave base to 0
		}
		for (int i = 0; i < chunks.length; i++) {
			chunks[i] = new ArrayList<>();
		}
		chunk = new int[chunks.length][10000];
		// parse input text
		s = new Scanner(text);
		while (s.hasNextLine()) {
			String line = s.nextLine();
			if (matcher.reset(line).find()) {
				if (matcher.groupCount() != groups) {
					// TODO: log unknown traces?
					continue;
				}
				// TODO: safe to use index 0?
				final boolean reallocate = count >= chunk[0].length;
				for (int group = 0; group <= groups - gb; group++) {
					String strval = matcher.group(group + gb);
					int val = Integer.parseInt(strval);
//				System.out.println("found val:" + val);
					if (reallocate) {
						chunks[group].add(chunk[group]);
						chunk[group] = new int[chunk[group].length];
						count = 0;
					}
					chunk[group][count] = val;
				}
				// TODO: safe to use index 0?
				if (reallocate)
					total += chunk[0].length;
				count++;
			}
		}
		s.close();
		// add last chunk
		if (count > 0) {
			total += count;
			for (int i = 0; i < chunks.length; i++) {
				chunks[i].add(chunk[i]);
			}
		}

		// merge arrays
		int[][] data = new int[chunks.length][total];
		int copied = 0;
		int i = 0;
		System.out.printf("Data collected: total len=%d (per set)\n", total);
		while (total > copied) {
			// TODO: is 0 as index below OK?
			int toCopy = Math.min(chunk[0].length, total - copied);
			System.out.printf("arraycopy: offset=%d, size=%d\n", copied, toCopy);
			for (int g = 0; g < chunks.length; g++) {
				chunk[g] = chunks[g].get(i);
				System.arraycopy(chunk[g], 0, data[g], copied, toCopy);
			}
			i++;
			copied += toCopy;
		}
		System.out.println("arraycopy done");
		// add to list of data arrays
		for (int g = 0; g < data.length; g++)
			this.data.add(data[g]);
	}

}
