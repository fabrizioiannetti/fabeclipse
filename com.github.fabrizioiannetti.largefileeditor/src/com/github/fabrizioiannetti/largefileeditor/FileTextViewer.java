package com.github.fabrizioiannetti.largefileeditor;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class FileTextViewer extends Composite {

	private FileTextModel model;

	public FileTextViewer(File textFile, Composite parent, int style) {
		super(parent, style);
		model = new FileTextModel(textFile, null);
		Text text = new Text(parent, SWT.MULTI | SWT.FLAT);
		text.setText(model.getLine(0));
	}
}
