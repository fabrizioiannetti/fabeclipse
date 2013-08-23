package com.github.fabrizioiannetti.largefileeditor;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class LargeFileEditor extends EditorPart {

	private File textFile;
	private FileTextViewer viewer;

	@Override
	public void doSave(IProgressMonitor monitor) {
		// this editor is read only : save does not do anything
		if (monitor != null) {
			monitor.done();
		}
	}

	@Override
	public void doSaveAs() {
		// TODO allow copy?
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isDirty() {
		// this editor is read-only
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO allow copy?
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new FileTextViewer(textFile, parent, SWT.NONE);
	}

	@Override
	public void setFocus() {
		viewer.setFocus();
	}

}
