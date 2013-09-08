package com.github.fabrizioiannetti.largefileeditor;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

public class FileTextViewer extends Composite {

	private StyledText text;
	private FileTextModel model;

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
		text = new StyledText(parent, style | SWT.MULTI | SWT.READ_ONLY);
		text.setText("parsing");
		GridLayoutFactory.fillDefaults().generateLayout(parent);
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
}
