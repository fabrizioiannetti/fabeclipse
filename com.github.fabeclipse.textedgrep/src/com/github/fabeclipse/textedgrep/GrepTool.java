/**
 * Copyright 2015 Fabrizio Iannetti.
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 */
package com.github.fabeclipse.textedgrep;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.fabeclipse.textedgrep.internal.core.GrepContext;

/**
 * Class that encapsulate a grep operation using a given regular
 * expression.
 * 
 * Calling {@link #grep(IGrepTarget, boolean)} yelds a result with
 * the filtered text and information on the source range for each line.
 * 
 * @since 2.0
 */
public class GrepTool {

	private static final int INITIAL_LINE_BUFFER_COUNT = 10000;

	private String[] regexList;
	private final boolean caseSensitive;

	public GrepTool(String regex, boolean caseSensitive) {
		this(new String[] {regex }, caseSensitive);
	}

	public GrepTool(String[] regexList, boolean caseSensitive) {
		super();
		this.regexList = regexList;
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Prepare for a grep operation.
	 * 
	 * The function returns a context that can be used
	 * to perform the grep later.
	 * 
	 * @since 3.0
	 */
	public IGrepContext grepStart(IGrepTarget target) {
		GrepContext grepContext = new GrepContext(target);
		return grepContext;
	}

	/**
	 * @since 3.0
	 */
	public IGrepContext grep(IGrepTarget target, boolean multiple) {
		GrepContext grepContext = new GrepContext(target);
		grep(grepContext, new GrepMonitor(), multiple);
		return grepContext;
	}

	/**
	 * 
	 * @throws InterruptedException 
	 * @since 3.0
	 */
	public IGrepContext grep(IGrepContext gc, GrepMonitor monitor, boolean multiple) {
		// check the grep context is of the right concrete type
		if (!(gc instanceof GrepContext)) {
			throw new IllegalArgumentException("Illegal Grep context implementation");
		}
		GrepContext grepContext = (GrepContext) gc;
		IGrepTarget target = grepContext.getTarget();
		// start with 10 thousands lines (in the grep result)
		int[] lineMap  = new int[INITIAL_LINE_BUFFER_COUNT];
		int[] matchMap = new int[lineMap.length];
		int[] colorMap = new int[lineMap.length];
		int[] matchBegin = new int[INITIAL_LINE_BUFFER_COUNT];
		int[] matchEnd   = new int[matchBegin.length];

		final Matcher[] matchers = new Matcher[regexList.length];
		int matchCount = 0;
		for (String rx : regexList)
			matchers[matchCount++] = Pattern.compile(rx, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE).matcher("");
		int lineNum = 0;
		int grepLineNum = 0;
		int submittedGrepLineNum = 0;
		int matchNum = 0;
		int submittedMatchNum = 0;
		int progressPercent = 0;
		target.start();
		monitor.fireProgress(progressPercent);
		long readChars = 0;
		final long targetLen = target.getLength();
		while(target.hasNextLine() && !monitor.isCanceled()) {
			String line = target.nextLine();
			boolean found = false;
			// run each matcher on the line, the first one wins, i.e.
			// one line can't be matched by more than one matcher
			for (int m = 0 ; m < matchCount && !found; m++) {
				final Matcher matcher = matchers[m];
				matcher.reset(line);
				// look for matches on this line
				while (matcher.find()) {
					if (!found) {
						// update grep line array/count only at first match
						found = true;
						if ((grepLineNum - submittedGrepLineNum) >= lineMap.length) {
							// add full chunks to the grep context
							grepContext.addLineChunks(lineMap, matchMap, colorMap);
							monitor.fireChange(grepContext);
							// allocate new chunks
							lineMap  = new int[lineMap.length];
							matchMap = new int[lineMap.length];
							colorMap = new int[lineMap.length];
							submittedGrepLineNum += lineMap.length;
						}
						matchMap[grepLineNum - submittedGrepLineNum] = matchNum;
						colorMap[grepLineNum - submittedGrepLineNum] = m;
						lineMap[grepLineNum - submittedGrepLineNum] = lineNum;
						grepLineNum++;
						// add line to the grep text (only once!)
						grepContext.addLine(line);
					}
					if ((matchNum - submittedMatchNum) >= matchBegin.length) {
						grepContext.addMatchChunks(matchBegin, matchEnd);
						matchBegin = new int[matchBegin.length];
						matchEnd   = new int[matchEnd.length];
						submittedMatchNum += matchBegin.length;
					}
					matchBegin[matchNum - submittedMatchNum] = matcher.start();
					matchEnd[matchNum - submittedMatchNum] = matcher.end();
					matchNum++;
					// exit while loop if multiple matches on the same line are not required
					if (!multiple) break;
				}
			}
			lineNum++;
			// update progress
			readChars += line.length() + 1; // assuming 1 char for line terminators
			final int newProgress = (int) (readChars * 100 / targetLen);
			if (newProgress > progressPercent) {
				// align with the actual position in grep target
				// which might be different if the document has two chars
				// as line terminator (windows)
				readChars = target.getLineOffset(lineNum);
				progressPercent = (int) (readChars * 100 / targetLen);
				monitor.fireProgress(progressPercent);
			}
		}
		if (grepLineNum - submittedGrepLineNum > 0) {
			grepContext.addLineChunks(lineMap, matchMap, colorMap, grepLineNum - submittedGrepLineNum);
		}
		if (matchNum - submittedMatchNum > 0) {
			grepContext.addMatchChunks(matchBegin, matchEnd, matchNum - submittedMatchNum);
		}
		target.stop();
		grepContext.seal();
		if (progressPercent < 100) {
			monitor.fireProgress(100);
		}
		return grepContext;
	}
}
