package com.github.fabrizioiannetti.largefileeditor;

import org.eclipse.ui.IWorkbenchPart;

import com.github.fabeclipse.textedgrep.IGrepTarget;
import com.github.fabrizioiannetti.largefileeditor.FileTextModel.LineOffsets;

public class LargeFileGrepTarget implements IGrepTarget {

	final private LargeFileEditor editor;
	final private FileTextModel model;
	private int grepLine;
	private int grepLineCount;

	public LargeFileGrepTarget(LargeFileEditor editor) {
		this.editor = editor;
		model = editor.getViewer().getModel();
	}
	@Override
	public void start() {
		grepLine = 0;
		grepLineCount = model.getLineCount();
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean hasNextLine() {
		return grepLine < grepLineCount;
	}

	@Override
	public String nextLine() {
		String line = null;
		if (grepLine < grepLineCount) {
			line = model.getLine(grepLine);
			grepLine++;
		}
		return line;
	}

	@Override
	public void select(int start, int length) {
		editor.select(start, length);
	}

	@Override
	public int getLineOffset(int line) {
		final LineOffsets offsets = new LineOffsets();
		editor.getViewer().getModel().getOffsetsForLine(line, offsets);
		return (int) offsets.start;
	}

	@Override
	public boolean isSame(IWorkbenchPart part) {
		return part != null && part.equals(editor);
	}

	@Override
	public String getTitle() {
		return editor.getTitle();
	}
	@Override
	public long getLength() {
		return editor.getViewer().getModel().getLength();
	}

	@Override
	public String getTextBetweenLines(int origStartLine, int origEndLine) {
		String text = model.getTextBetweenLines(origStartLine, origEndLine);
		return text;
	}
}
