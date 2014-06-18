package com.github.fabrizioiannetti.largefileeditor;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import com.github.fabrizioiannetti.largefileeditor.FileTextModel.LineOffsets;

public class FileTextViewer extends Composite {

	private StyledText text;
	private FileTextModel model;

	/*
	 * For the time being this monitor does not report
	 * file scan progress to the user (as it should if
	 * the scan lasts long): it only sets the document
	 * at the end of the scan.
	 */
	private IProgressMonitor scanMonitor = new NullProgressMonitor() {
		@Override
		public void done() {
			getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (!isDisposed()) {
						text.setContent(new FileTextContent(model));
					}
				}
			});
		}
	};
	
	public FileTextViewer(File textFile, Composite parent, int style) {
		super(parent, SWT.NONE);
		setLayout(GridLayoutFactory.fillDefaults().create());
		text = new StyledText(this, style | SWT.MULTI | SWT.READ_ONLY);
		text.setText("parsing...");
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		// create the model, and start file scan
		// the end of the scan is handled by the monitor
		model = new FileTextModel(textFile, scanMonitor);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		// cancel file parsing
		scanMonitor.setCanceled(true);
	}
	
	@Override
	public void setFont(Font font) {
		super.setFont(font);
		text.setFont(font);
	}

	public Point getSelectionMaxOneLine() {
		Point selection = text.getSelection();
		int lineIndex = model.getLineIndex(selection.x);
		LineOffsets offsets = new LineOffsets();
		model.getOffsetsForLine(lineIndex, offsets);
		if (selection.y > offsets.end)
			selection.y = (int) offsets.end;
		selection.y -= selection.x;
		return selection;
	}
	
	public String getSelectionTextMaxOneLine() {
		Point selection = text.getSelection();
		if (selection.x < 0 || selection.y < 0)
			return "";
		int index = model.getLineIndex(selection.x);
		LineOffsets offsets = new LineOffsets();
		model.getOffsetsForLine(index, offsets);
		String line = model.getLine(index);
		int start = selection.x - (int) offsets.start;
		if (selection.y > offsets.end)
			selection.y = (int) offsets.end;
		int end = selection.y - (int) offsets.start;
		if (end > line.length())
			return line.substring(start);
		return line.substring(start, end);
	}

	void setSelection(int start, int end) {
		text.setSelection(start, end);
	}

	public FileTextModel getModel() {
		return model;
	}
}
