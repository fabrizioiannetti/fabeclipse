package com.github.fabeclipse.textedgrep.internal.ui;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import com.github.fabeclipse.textedgrep.IGrepTarget;

/**
 * Bridge between a grep target and an IDocument object.
 * Used to grep text editors.
 * 
 * @author Fabrizio Iannetti
 * @since 3.0
 *
 */
public class DocumentGrepTarget implements IGrepTarget {

	private final IDocument document;
	int lineIndex = -1;
	private AbstractTextEditor editor;
	
	public DocumentGrepTarget(AbstractTextEditor textEd) {
		this(textEd.getDocumentProvider().getDocument(textEd.getEditorInput()));
		editor = textEd;
	}

	public DocumentGrepTarget(IDocument document) {
		this.document = document;
		// TODO: add a document listener, this should update
		// grep results
		document.addDocumentListener(new IDocumentListener() {
			@Override
			public void documentChanged(DocumentEvent event) {
			}
			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
			}
		});
	}

	@Override
	public void start() {
		lineIndex = 0;
	}
	@Override
	public void stop() {
		lineIndex = -1;
	}
	@Override
	public boolean hasNextLine() {
		return lineIndex < document.getNumberOfLines();
	}

	@Override
	public String nextLine() {
		try {
			IRegion information = document.getLineInformation(lineIndex);
			lineIndex++;
			return document.get(information.getOffset(), information.getLength());
		} catch (BadLocationException e) {
			return null;
		}
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

	@Override
	public void select(int start, int length) {
		editor.selectAndReveal(start, length);
	}

	@Override
	public boolean isSame(IWorkbenchPart part) {
		return part == editor;
	}

	@Override
	public String getTitle() {
		return editor.getTitle();
	}

	@Override
	public long getLength() {
		return document.getLength();
	}
	
}