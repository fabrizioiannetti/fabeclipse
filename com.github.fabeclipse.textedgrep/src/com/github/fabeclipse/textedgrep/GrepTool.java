package com.github.fabeclipse.textedgrep;

import java.util.Formatter;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.AbstractTextEditor;

public class GrepTool {

	public static class GrepContext {
		private int[] lineMap;
		private final IDocument document;
		private final StringBuilder grep;
		private final int numGrepLines;
		public GrepContext(IDocument document, StringBuilder grep, int[] lineMap, int numGrepLines) {
			this.document = document;
			this.grep = grep;
			this.lineMap = lineMap;
			this.numGrepLines = numGrepLines;
		}
		
		public int getOriginalLine(int grepLine) {
			int originalLine = lineMap[grepLine];
			return originalLine;
		}
		
		public String getText() {
			return grep.toString();
		}
		
		public IDocument getDocument() {
			return document;
		}
		
		public int getMaxOriginalLine() {
			return lineMap[numGrepLines - 1];
		}
		
	}

	private String regex;

	private static IDocumentListener listener = new IDocumentListener() {
		@Override
		public void documentChanged(DocumentEvent event) {
			System.out.println(event);
		}
		
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
			System.out.println(event);
		}
	};
	
	public GrepTool(String regex) {
		super();
		this.regex = regex;
	}

	public GrepContext grepCurrentEditor(IWorkbenchWindow window) {
		GrepContext grepContext = null;
		// start with 10 thousands lines (in the grep result)
		int[] lineMap = new int[10000];
		StringBuilder grep = new StringBuilder();
		IEditorPart activeEditor = window.getActivePage().getActiveEditor();
		if (activeEditor instanceof AbstractTextEditor) {
			AbstractTextEditor textEd = (AbstractTextEditor) activeEditor;
			IEditorInput input = textEd.getEditorInput();
			IDocument document = textEd.getDocumentProvider().getDocument(input);
			String string = document.get();
			document.addDocumentListener(listener );
			
			Scanner s = new Scanner(string);
			Matcher matcher = Pattern.compile(regex).matcher("");
			Formatter formatter = new Formatter(grep);
			int lineNum = 0;
			int grepLineNum = 0;
			while(s.hasNextLine()) {
				String line = s.nextLine();
				if (matcher.reset(line).find()) {
					formatter.format("%s\n", line);
					if (grepLineNum >= lineMap.length) {
						// resize lineMap adding 10 thousand elements
						int[] newLineMap = new int[lineMap.length + 10000];
						System.arraycopy(lineMap, 0, newLineMap, 0, lineMap.length);
						lineMap = newLineMap;
					}
					lineMap[grepLineNum++] = lineNum;
				}
				lineNum++;
			}
			// remove trailing newline
			grep.deleteCharAt(grep.length()-1);
			grepContext = new GrepContext(document, grep, lineMap, grepLineNum);
		}
		return grepContext;
	}
}
