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

	public void importFromText(String text, String pattern) {
		ArrayList<int[]> chunks = new ArrayList<>(1000);
		int[] chunk = new int[10000];
		int count = 0;
		int total = 0;
		int groups = -1;
		
		// check how many groups we'll find
		// TODO: limit this to a fixed horizon?
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

		// parse input text
		s = new Scanner(text);
		while (s.hasNextLine()) {
			String line = s.nextLine();
			if (matcher.reset(line).find()) {
				int group;
				if (matcher.groupCount() > 0)
					group = 1;
				else
					group = 0;
				String strval = matcher.group(group);
				int val = Integer.parseInt(strval);
				System.out.println("found val:" + val);
				if (count >= chunk.length) {
					chunks.add(chunk);
					chunk = new int[chunk.length];
					total += chunk.length;
					count = 0;
				}
				chunk[count++] = val;
			}
		}
		s.close();
		// add last chunk
		if (count > 0) {
			total += count;
			chunks.add(chunk);
		}

		// merge arrays
		int[] data = new int[total];
		int copied = 0;
		int i = 0;
		while (total > copied) {
			chunk = chunks.get(i);
			int toCopy = Math.min(chunk.length, total - copied);
			System.out.printf("arraycopy: offset=%d, size=%d\n", copied, toCopy);
			System.arraycopy(chunk, 0, data, copied, toCopy);
			i++;
			copied += toCopy;
		}

		// add to list of data arrays
		this.data.add(data);
	}

}
