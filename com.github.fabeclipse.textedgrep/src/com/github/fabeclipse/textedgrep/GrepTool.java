package com.github.fabeclipse.textedgrep;

import java.util.Formatter;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.AbstractTextEditor;

public class GrepTool {

	private static final int INCREMENT_LINE_BUFFER_COUNT = 10000;
	private static final int INITIAL_LINE_BUFFER_COUNT = 10000;

	public static interface IGrepTarget {
		// indicate start and stop of parsing
		public void start();
		public void stop();

		public int lineCount();
		// to iterate on target lines
		public boolean hasNextLine();
		public String nextLine();

		// utility: just used to get the original offset
		// TODO: should not be part of the interface
		public int getLineOffset(int line);
	}

	public static interface IGrepMonitor {
		public void setProgress(int percent);
	}
	
	public static class DocumentGrepTarget implements IGrepTarget {

		private final IDocument document;
		private Scanner scanner;
		private final int lineCount;

		public DocumentGrepTarget(AbstractTextEditor textEd) {
			IEditorInput input = textEd.getEditorInput();
			document = textEd.getDocumentProvider().getDocument(input);
			int lc = 0;
			try {
				lc = document.getLineOfOffset(document.getLength());
			} catch (BadLocationException e) {
			} finally {
				lineCount = lc;
			}
		}

		public DocumentGrepTarget(IDocument document) {
			this.document = document;
			int lc = 0;
			try {
				lc = document.getLineOfOffset(document.getLength());
			} catch (BadLocationException e) {
			} finally {
				lineCount = lc;
			}
		}

		@Override
		public void start() {
			String string = document.get();
			scanner = new Scanner(string);
		}
		@Override
		public void stop() {
			scanner.close();
			scanner = null;
		}
		
		@Override
		public int lineCount() {
			return lineCount;
		}
		@Override
		public boolean hasNextLine() {
			return scanner.hasNextLine();
		}

		@Override
		public String nextLine() {
			return scanner.nextLine();
		}
		@Override
		public int getLineOffset(int line) {
			int offset = -1;
			try {
				offset = document.getLineOffset(line);
			} catch (BadLocationException e) {
				// just return -1 as bad location
			}
			return offset;
		}
		
	}
	public static class GrepContext {
		private int[] lineMap;
		private final IGrepTarget target;
		private final String grep;
		private final int numGrepLines;
		private final int[] matchBegin;
		private final int[] matchEnd;
		private final int[] matchMap;
		private int numberOfMatches;

		public GrepContext(IGrepTarget target, StringBuilder grep, int[] lineMap, int[] matchBegin, int[] matchEnd, int[] matchMap, int numGrepLines) {
			this.target = target;
			this.grep = grep.toString();
			this.lineMap = lineMap;
			this.matchBegin = matchBegin;
			this.matchEnd = matchEnd;
			this.matchMap = matchMap;
			this.numGrepLines = numGrepLines;
		}
		
		public int getOriginalLine(int grepLine) {
			int originalLine = 0;
			// if the grep context is empty, it is
			// OK to ask for line 0 even if it 
			// is beyond the array size
			if (numGrepLines > 0)
				originalLine = lineMap[grepLine];
			return originalLine;
		}
		
		public int getMatchBeginForGrepLine(int grepLine, int index) {
			return matchBegin[matchMap[grepLine] + index];
		}
		
		public int getMatchEndForGrepLine(int grepLine, int index) {
			return matchEnd[matchMap[grepLine] + index];
		}
		
		public int getNumberOfMatches() {
			return numberOfMatches;
		}

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
				n = matchMap[grepLine + 1] - matchMap[grepLine];
			else
				n = numberOfMatches - matchMap[grepLine];
			return n;
		}
		
		public String getText() {
			return grep.toString();
		}
		
		public IGrepTarget getTarget() {
			return target;
		}
		
		public int getMaxOriginalLine() {
			// if the grep context is empty
			// return 0 as max line
			if (numGrepLines > 0)
				return lineMap[numGrepLines - 1];
			else
				return 0;
		}
		
	}

	private String regex;
	private final boolean caseSensitive;

//	private static IDocumentListener listener = new IDocumentListener() {
//		@Override
//		public void documentChanged(DocumentEvent event) {
//			System.out.println(event);
//		}
//		
//		@Override
//		public void documentAboutToBeChanged(DocumentEvent event) {
//			System.out.println(event);
//		}
//	};
	
	public GrepTool(String regex, boolean caseSensitive) {
		super();
		this.regex = regex;
		this.caseSensitive = caseSensitive;
	}

	public GrepContext grep(IGrepTarget target, boolean multiple, IGrepMonitor monitor) {
		GrepContext grepContext = null;
		// start with 10 thousands lines (in the grep result)
		int[] lineMap = new int[INITIAL_LINE_BUFFER_COUNT];
		int[] matchMap = new int[lineMap.length];
		int[] matchBegin = new int[INITIAL_LINE_BUFFER_COUNT];
		int[] matchEnd = new int[matchBegin.length];
		StringBuilder grep = new StringBuilder();

		Matcher matcher = Pattern.compile(regex, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE).matcher("");
		Formatter formatter = new Formatter(grep);
		int lineNum = 0;
		int grepLineNum = 0;
		int matchNum = 0;
		target.start();
		int lc = target.lineCount();
		while(target.hasNextLine()) {
			String line = target.nextLine();
			matcher.reset(line);
			boolean found = false;
			// look for matches on this line
			while (matcher.find()) {
				if (!found) {
					// update grep line array/count only at first match
					found = true;
					if (grepLineNum >= lineMap.length) {
						// resize lineMap adding 10 thousand elements
						int[] newLineMap = new int[lineMap.length + INCREMENT_LINE_BUFFER_COUNT];
						int oldLength = lineMap.length;
						System.arraycopy(lineMap, 0, newLineMap, 0, oldLength);
						lineMap = newLineMap;
						// resize matchMap adding 10 thousand elements
						int[] newMatchMap = new int[lineMap.length];
						System.arraycopy(matchMap, 0, newMatchMap, 0, oldLength);
						matchMap = newMatchMap;
					}
					matchMap[grepLineNum] = matchNum;
					lineMap[grepLineNum++] = lineNum;
					// add line to the grep text (only once!)
					formatter.format("%s\n", line);
				}
				if (matchNum >= matchBegin.length) {
					// resize match boundaries accordingly
					int oldLength = matchBegin.length;
					int[] newMatchBegin = new int[oldLength + INCREMENT_LINE_BUFFER_COUNT];
					System.arraycopy(matchBegin, 0, newMatchBegin, 0, oldLength);
					matchBegin = newMatchBegin;
					int[] newMatchEnd = new int[newMatchBegin.length];
					System.arraycopy(matchEnd, 0, newMatchEnd, 0, oldLength);
					matchEnd = newMatchEnd;
				}
				matchBegin[matchNum] = matcher.start();
				matchEnd[matchNum++] = matcher.end();
				// exit while loop if multiple matches on the same line are not required
				if (!multiple) break;
			}
			lineNum++;
//			if (monitor != null && lc > 0)
//				monitor.setProgress((lineNum) * 100 / lc);
		}
		formatter.close();
		// remove trailing newline
		if (grep.length() > 0)
			grep.deleteCharAt(grep.length()-1);
		grepContext = new GrepContext(target, grep, lineMap, matchBegin, matchEnd, matchMap, grepLineNum);
		grepContext.numberOfMatches = matchNum;
		target.stop();
		return grepContext;
	}
}
