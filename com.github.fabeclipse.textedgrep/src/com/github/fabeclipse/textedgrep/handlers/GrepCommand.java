package com.github.fabeclipse.textedgrep.handlers;

import org.eclipse.core.commands.AbstractHandler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.UIPlugin;

import com.github.fabeclipse.textedgrep.views.GrepView;

public class GrepCommand extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// TODO open grep
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(GrepView.VIEW_ID);
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
