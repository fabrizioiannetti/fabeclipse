package com.github.fabrizioiannetti.largefileeditor;

import java.io.File;

import org.eclipse.swt.widgets.Composite;

public class FileTextViewer extends Composite {

	private File textFile;

	public FileTextViewer(File textFile, Composite parent, int style) {
		super(parent, style);
		this.textFile = textFile;
	}
	
}
