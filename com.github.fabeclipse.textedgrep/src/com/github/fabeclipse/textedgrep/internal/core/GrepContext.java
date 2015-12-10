package com.github.fabeclipse.textedgrep.internal.core;

import java.util.ArrayList;
import java.util.Formatter;

import com.github.fabeclipse.textedgrep.IGrepContext;
import com.github.fabeclipse.textedgrep.IGrepTarget;

/**
 * @since 3.0
 */
public class GrepContext implements IGrepContext {
	protected static class GrowingIntArray {
		private int chunkSize = 1; // to prevent /0 exceptions in getAt()
		private ArrayList<int[]> data = new ArrayList<int[]>(1000);
		public GrowingIntArray addChunk(int[] chunk) {
			if (data.size() == 0)
				chunkSize = chunk.length;
			else if (chunk.length != chunkSize)
				throw new IllegalArgumentException("wrong chunksize (" + chunk.length + "), should be " + chunkSize);
			data.add(chunk);
			return this;
		}
		public int getAt(int position) {
			return data.get(position / chunkSize)[position % chunkSize];
		}
		public int size() {
			return data.size() * chunkSize;
		}
	}
	private final IGrepTarget target;
	private final StringBuilder grep;
	private final Formatter formatter;
	private int numberOfMatches;
	private int numGrepLines;

	private final GrowingIntArray lineMap = new GrowingIntArray();
	private final GrowingIntArray matchBegin = new GrowingIntArray();
	private final GrowingIntArray matchEnd = new GrowingIntArray();
	private final GrowingIntArray matchMap = new GrowingIntArray();
	private final GrowingIntArray colorMap = new GrowingIntArray();

	private boolean linesDone;
	private boolean matchDone;

	public GrepContext(IGrepTarget target) {
		this.target = target;
		grep = new StringBuilder();
		formatter = new Formatter(grep);
	}

	/* (non-Javadoc)
	 * @see com.github.fabeclipse.textedgrep.IGrepContext#getOriginalLine(int)
	 */
	@Override
	public int getOriginalLine(int grepLine) {
		int originalLine = 0;
		// if the grep context is empty, it is
		// OK to ask for line 0 even if it 
		// is beyond the array size
		if (numGrepLines > 0)
			originalLine = lineMap.getAt(grepLine);
		return originalLine;
	}
	
	/* (non-Javadoc)
	 * @see com.github.fabeclipse.textedgrep.IGrepContext#getMatchBeginForGrepLine(int, int)
	 */
	@Override
	public int getMatchBeginForGrepLine(int grepLine, int index) {
		return matchBegin.getAt(matchMap.getAt(grepLine) + index);
	}
	
	/* (non-Javadoc)
	 * @see com.github.fabeclipse.textedgrep.IGrepContext#getMatchEndForGrepLine(int, int)
	 */
	@Override
	public int getMatchEndForGrepLine(int grepLine, int index) {
		return matchEnd.getAt(matchMap.getAt(grepLine) + index);
	}
	
	/* (non-Javadoc)
	 * @see com.github.fabeclipse.textedgrep.IGrepContext#getColorForGrepLine(int)
	 */
	@Override
	public int getColorForGrepLine(int grepLine) {
		return colorMap.getAt(grepLine);
	}

	/* (non-Javadoc)
	 * @see com.github.fabeclipse.textedgrep.IGrepContext#getNumberOfMatches()
	 */
	@Override
	public int getNumberOfMatches() {
		return numberOfMatches;
	}

	/* (non-Javadoc)
	 * @see com.github.fabeclipse.textedgrep.IGrepContext#getNumberOfMatchesForGrepLine(int)
	 */
	@Override
	public int getNumberOfMatchesForGrepLine(int grepLine) {
		int n;
		// a b c d e  <- matchBegin/End array
		// ^ ^   ^ ^
		// 0 1   2 3  <- matchMap index into above array
		// line 0 has 1 match
		// line 1 has 2 matches
		// line 2 has 1 match
		// number of matches for 1 is 3 - 1 = 2
		if (grepLine + 1 < numGrepLines)
			n = matchMap.getAt(grepLine + 1) - matchMap.getAt(grepLine);
		else
			n = numberOfMatches - matchMap.getAt(grepLine);
		return n;
	}
	
	/* (non-Javadoc)
	 * @see com.github.fabeclipse.textedgrep.IGrepContext#getMaxOriginalLine()
	 */
	@Override
	public int getMaxOriginalLine() {
		// if the grep context is empty
		// return 0 as max line
		if (numGrepLines > 0)
			return lineMap.getAt(numGrepLines - 1);
		else
			return 0;
	}
	/* (non-Javadoc)
	 * @see com.github.fabeclipse.textedgrep.IGrepContext#getText()
	 */
	@Override
	public String getText() {
		return grep.toString();
	}
	
	/* (non-Javadoc)
	 * @see com.github.fabeclipse.textedgrep.IGrepContext#getTarget()
	 */
	@Override
	public IGrepTarget getTarget() {
		return target;
	}

	public void addLineChunks(int[] lineMapChunk, int[] matchMapChunk, int[] colorMapChunk) {
		addLineChunks(lineMapChunk, matchMapChunk, colorMapChunk, lineMapChunk.length);
	}

	public void addMatchChunks(int[] matchBeginChunk, int[] matchEndChunk) {
		addMatchChunks(matchBeginChunk, matchEndChunk, matchBeginChunk.length);
	}

	public void addMatchChunks(int[] matchBeginChunk, int[] matchEndChunk, int len) {
		if (len != matchBeginChunk.length)
			matchDone = true;
		else if (matchDone)
			throw new IllegalArgumentException("Incomplete match chunk already submitted.");
		numberOfMatches = matchBegin.size() + len;
		matchBegin.addChunk(matchBeginChunk);
		matchEnd.addChunk(matchEndChunk);
	}

	public void addLineChunks(int[] lineMapChunk, int[] matchMapChunk, int[] colorMapChunk, int len) {
		if (len != lineMapChunk.length)
			linesDone = true;
		else if (linesDone)
			throw new IllegalArgumentException("Incomplete line chunk already submitted.");
		numGrepLines = lineMap.size() + len;
		lineMap.addChunk(lineMapChunk);
		matchMap.addChunk(matchMapChunk);
		colorMap.addChunk(colorMapChunk);
	}

	public void addLine(String line) {
		formatter.format("%s\n", line);
	}

	public void seal() {
		// remove trailing newline, as the StyledText Document
		// returns one line more if the last line has a line terminator
		if (grep.length() > 0) {
			char lastChar = grep.charAt(grep.length() - 1);
			if (lastChar == '\n') {
				grep.deleteCharAt(grep.length() - 1);
			}
		}
		formatter.close();
		linesDone = true;
		matchDone = true;
	}
}