/**
 * Copyright 2015 Fabrizio Iannetti.
 */
package com.github.fabeclipse.textedgrep.internal.ui.cmd;

import org.eclipse.core.commands.AbstractHandler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.github.fabeclipse.textedgrep.internal.ui.GrepView;

public class GrepCommand extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String text = null;
		ISelection selection = HandlerUtil.getActiveMenuSelection(event);
		selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof ITextSelection) {
			ITextSelection textSelection = (ITextSelection) selection;
			text = textSelection.getText();
		}
		try {
			IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(GrepView.VIEW_ID);
			if (text != null && text.length() > 0)
				((GrepView)view).setGrepRegularExpression(text);
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
