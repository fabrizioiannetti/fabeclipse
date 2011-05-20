package com.github.fabeclipse.textedgrep;

import java.util.Formatter;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.AbstractTextEditor;

public class GrepTool {

	private static final int INCREMENT_LINE_BUFFER_COUNT = 10000;
	private static final int INITIAL_LINE_BUFFER_COUNT = 10000;

	public static class GrepContext {
		private int[] lineMap;
		private final IDocument document;
		private final StringBuilder grep;
		private final int numGrepLines;
		private final int[] matchBegin;
		private final int[] matchEnd;
		public GrepContext(IDocument document, StringBuilder grep, int[] lineMap, int[] matchBegin, int[] matchEnd, int numGrepLines) {
			this.document = document;
			this.grep = grep;
			this.lineMap = lineMap;
			this.matchBegin = matchBegin;
			this.matchEnd = matchEnd;
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
		
		public int getMatchBeginForGrepLine(int grepLine) {
			return matchBegin[grepLine];
		}
		
		public int getMatchEndForGrepLine(int grepLine) {
			return matchEnd[grepLine];
		}
		
		public String getText() {
			return grep.toString();
		}
		
		public IDocument getDocument() {
			return document;
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

	public GrepContext grepEditor(AbstractTextEditor textEd) {
		GrepContext grepContext = null;
		// start with 10 thousands lines (in the grep result)
		int[] lineMap = new int[INITIAL_LINE_BUFFER_COUNT];
		int[] matchBegin = new int[INITIAL_LINE_BUFFER_COUNT];
		int[] matchEnd = new int[INITIAL_LINE_BUFFER_COUNT];
		StringBuilder grep = new StringBuilder();
		IEditorInput input = textEd.getEditorInput();
		IDocument document = textEd.getDocumentProvider().getDocument(input);
		String string = document.get();
//			document.addDocumentListener(listener );
		
		Scanner s = new Scanner(string);
		Matcher matcher = Pattern.compile(regex, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE).matcher("");
		Formatter formatter = new Formatter(grep);
		int lineNum = 0;
		int grepLineNum = 0;
		while(s.hasNextLine()) {
			String line = s.nextLine();
			if (matcher.reset(line).find()) {
				formatter.format("%s\n", line);
				if (grepLineNum >= lineMap.length) {
					// resize lineMap adding 10 thousand elements
					int[] newLineMap = new int[lineMap.length + INCREMENT_LINE_BUFFER_COUNT];
					int oldLength = lineMap.length;
					System.arraycopy(lineMap, 0, newLineMap, 0, oldLength);
					lineMap = newLineMap;
					// resize match boundaries accordingly
					int[] newMatchBegin = new int[lineMap.length];
					System.arraycopy(matchBegin, 0, newMatchBegin, 0, oldLength);
					matchBegin = newMatchBegin;
					int[] newMatchEnd = new int[lineMap.length];
					System.arraycopy(matchEnd, 0, newMatchEnd, 0, oldLength);
					matchEnd = newMatchEnd;
				}
				matchBegin[grepLineNum] = matcher.start();
				matchEnd[grepLineNum] = matcher.end();
				lineMap[grepLineNum++] = lineNum;
			}
			lineNum++;
		}
		// remove trailing newline
		if (grep.length() > 0)
			grep.deleteCharAt(grep.length()-1);
		grepContext = new GrepContext(document, grep, lineMap, matchBegin, matchEnd, grepLineNum);
		return grepContext;
	}
}
