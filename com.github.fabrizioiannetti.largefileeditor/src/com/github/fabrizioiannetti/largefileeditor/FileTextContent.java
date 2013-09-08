package com.github.fabrizioiannetti.largefileeditor;

import java.util.ArrayList;

import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.custom.TextChangeListener;

import com.github.fabrizioiannetti.largefileeditor.FileTextModel.LineOffsets;

public class FileTextContent implements StyledTextContent {
	private FileTextModel model;
	private ArrayList<TextChangeListener> listeners = new ArrayList<TextChangeListener>();
	private String delimiter = System.getProperty("line.separator");

	public FileTextContent(FileTextModel model) {
		super();
		this.model = model;
		if (model.getLength() > Integer.MAX_VALUE)
			throw new IllegalArgumentException("file too long (styled text needs to fit offests in an int):" + model.getLength());
	}

	@Override
	public void addTextChangeListener(TextChangeListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}

	@Override
	public int getCharCount() {
		long length = model.getLength();
		return (int) length;
	}

	@Override
	public String getLine(int lineIndex) {
		return model.getLine(lineIndex);
	}

	@Override
	public int getLineAtOffset(int offset) {
		return model.getLineIndex(offset);
	}

	@Override
	public int getLineCount() {
		return model.getLineCount();
	}

	@Override
	public String getLineDelimiter() {
		// TODO Auto-generated method stub
		return delimiter;
	}

	@Override
	public int getOffsetAtLine(int lineIndex) {
		LineOffsets offsets = new LineOffsets();
		model.getOffsetsForLine(lineIndex, offsets);
		return (int) offsets.start;
	}

	@Override
	public String getTextRange(int start, int length) {
		return model.getText(start, length);
	}

	@Override
	public void removeTextChangeListener(TextChangeListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void replaceTextRange(int start, int replaceLength, String text) {
		// TODO unsupported (the model is read-only)
		System.out.println("FileTextContent.replaceTextRange(start=" + start + ", replaceLength =" + replaceLength + ")");
	}

	@Override
	public void setText(String text) {
		// TODO unsupported (the model is read-only)
		System.out.println("FileTextContent.setText()");
	}

}
