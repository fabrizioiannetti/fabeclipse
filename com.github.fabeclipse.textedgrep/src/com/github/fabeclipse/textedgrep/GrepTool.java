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

	public String grepCurrentEditor(IWorkbenchWindow window) {
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
			while(s.hasNextLine()) {
				String line = s.nextLine();
				if (matcher.reset(line).find()) {
					formatter.format("%5d %s\n", lineNum, line);
				}
				lineNum++;
			}
		}
		return grep.toString();
	}
}
