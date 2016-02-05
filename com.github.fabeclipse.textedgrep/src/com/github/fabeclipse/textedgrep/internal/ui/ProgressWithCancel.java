package com.github.fabeclipse.textedgrep.internal.ui;

import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class ProgressWithCancel extends Composite {

	/** stop image descriptor */
	private static ImageDescriptor fgStopImage = ImageDescriptor
			.createFromFile(GrepView.class, "images/stop.png");//$NON-NLS-1$

	static {
		JFaceResources.getImageRegistry().put(
				"com.github.fabeclipse.textedgrep.stopImage", fgStopImage);//$NON-NLS-1$
	}
	private static final Runnable NULL_CANCEL_FUNC = new Runnable() {
		@Override
		public void run() {}
	};

	private int currentProgress;
	private ProgressIndicator progressIndicator;
	private Runnable cancelFunc = NULL_CANCEL_FUNC;

	private Button stopButton;

	public ProgressWithCancel(Composite parent, int style) {
		super(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(this);
		progressIndicator = new ProgressIndicator(this);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(progressIndicator);
		stopButton = new Button(this, SWT.FLAT | SWT.PUSH);
		stopButton.setImage(fgStopImage.createImage());
		stopButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cancelFunc.run();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	public ProgressWithCancel onCancel(Runnable cancelFunc) {
		if (cancelFunc != null) {
			this.cancelFunc = cancelFunc;
			stopButton.setEnabled(true);
		} else {
			this.cancelFunc = NULL_CANCEL_FUNC;
			stopButton.setEnabled(false);
		}
		return this;
	}

	public void setProgress(int value, boolean start) {
		if (value == 0 || start) {
			progressIndicator.beginTask(value);
		} else {
			if (value > currentProgress) {
				progressIndicator.worked(value - currentProgress);
				currentProgress = value;
			}
		}
	}
}
