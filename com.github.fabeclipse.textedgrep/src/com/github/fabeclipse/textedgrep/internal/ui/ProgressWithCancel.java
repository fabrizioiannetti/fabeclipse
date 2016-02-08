package com.github.fabeclipse.textedgrep.internal.ui;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ProgressBar;

import com.github.fabeclipse.textedgrep.GrepMonitor;

public class ProgressWithCancel extends Composite {

	/** stop image descriptor */
	private static ImageDescriptor fgStopImage = ImageDescriptor
			.createFromFile(GrepView.class, "images/stop.png");//$NON-NLS-1$

	static {
		JFaceResources.getImageRegistry().put(
				"com.github.fabeclipse.textedgrep.stopImage", fgStopImage);//$NON-NLS-1$
	}

	private ProgressBar progressIndicator;
	private Button stopButton;
	private GrepMonitor monitor;

	public ProgressWithCancel(Composite parent, int style) {
		super(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(this);
		progressIndicator = new ProgressBar(this, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(progressIndicator);
		progressIndicator.setMinimum(0);
		progressIndicator.setMaximum(100);
		stopButton = new Button(this, SWT.FLAT | SWT.PUSH);
		stopButton.setImage(fgStopImage.createImage());
		stopButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (monitor != null)
					monitor.cancel();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	public ProgressWithCancel onCancel(GrepMonitor monitor) {
		this.monitor = monitor;
		return this;
	}

	public void setProgress(int value, boolean start) {
		progressIndicator.setSelection(value);
	}
}
