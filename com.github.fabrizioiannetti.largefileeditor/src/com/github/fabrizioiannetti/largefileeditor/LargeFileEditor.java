package com.github.fabrizioiannetti.largefileeditor;

import java.io.File;
import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class LargeFileEditor extends EditorPart {

	private File textFile;
	private FileTextViewer viewer;
	private IPropertyChangeListener fFontPropertyChangeListener= new FontPropertyChangeListener();

	/**
	 * Internal property change listener for handling workbench font changes.
	 * @since 2.1
	 */
	class FontPropertyChangeListener implements IPropertyChangeListener {
		/*
		 * @see IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent event) {
			if (viewer == null)
				return;

			String property= event.getProperty();

			if (JFaceResources.TEXT_FONT.equals(property)) {
				viewer.setFont(JFaceResources.getTextFont());
				return;
			}
		}
	}

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
		setSite(site);
		if (input instanceof IURIEditorInput) {
			setInput(input);
			IURIEditorInput fileInput = (IURIEditorInput) input;
			URI uri = fileInput.getURI();
			textFile = new File(uri);
			// TODO: only allow local files?
		}
		if (textFile == null) {
			throw new PartInitException("Could not read input:" + input.getName());
		}
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
		parent.setLayout(GridLayoutFactory.fillDefaults().create());
		viewer = new FileTextViewer(textFile, parent, SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		viewer.setFont(JFaceResources.getTextFont());
		JFaceResources.getFontRegistry().addListener(fFontPropertyChangeListener);
	}

	@Override
	public void setFocus() {
		viewer.setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		JFaceResources.getFontRegistry().removeListener(fFontPropertyChangeListener);
	}
}
