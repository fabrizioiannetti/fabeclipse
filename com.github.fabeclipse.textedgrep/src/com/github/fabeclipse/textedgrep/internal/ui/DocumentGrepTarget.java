package com.github.fabeclipse.textedgrep.internal.ui;

import java.util.ArrayList;
import java.util.List;

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
	private final int startLine;
	private final int endLine;
	private final long length;
	
	public DocumentGrepTarget(AbstractTextEditor textEd) throws BadLocationException {
		this(textEd.getDocumentProvider().getDocument(textEd.getEditorInput()));
		editor = textEd;
	}

	public DocumentGrepTarget(IDocument document) throws BadLocationException {
		this(document, 0, document.getNumberOfLines());
	}

	public DocumentGrepTarget(IDocument document, int startLine, int endLine) throws BadLocationException {
		this.document = document;
		this.startLine = startLine;
		this.endLine = endLine;
		this.length = document.getLineOffset(endLine - 1) - document.getLineOffset(startLine);
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

	public static List<IGrepTarget> partitioned(IDocument document, final int numPartitions) throws BadLocationException {
		List<IGrepTarget> targets = new ArrayList<>();
		final int length = document.getLength();
		final int partitionLen = length / numPartitions;
		int[] partitionBoundaries = new int[numPartitions + 1];
		for (int i = 0; i < numPartitions; i++) {
			final int line = document.getLineOfOffset(i * partitionLen);
			partitionBoundaries[i] = line;
		}
		partitionBoundaries[numPartitions] = document.getNumberOfLines();
		for (int i = 0; i < numPartitions; i++) {
			int endLine = partitionBoundaries[i + 1];
			int startLine = partitionBoundaries[i];
			targets.add(new DocumentGrepTarget(document, startLine, endLine));
		}
		return targets;
	}

	@Override
	public void start() {
		lineIndex = startLine;
	}
	@Override
	public void stop() {
		lineIndex = -1;
	}
	@Override
	public boolean hasNextLine() {
		return lineIndex < endLine;
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
		return length;
	}
	
	@Override
	public String getTextBetweenLines(int origStartLine, int origEndLine,
			int startDelta, int endDelta) {
		String text = "";
		try {
			// include end lines
			if (origEndLine < document.getNumberOfLines())
				origEndLine++;
			int start = document.getLineOffset(origStartLine) + startDelta;
			int end = document.getLineOffset(origEndLine) - endDelta;
			text = document.get(start, end - start);
		} catch (BadLocationException e) {
			// TODO: log
		}
		return text;
	}
}