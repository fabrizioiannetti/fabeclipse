package com.github.fabeclipse.textedgrep.internal.ui.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class Copy extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		copySelection(event);
		return null;
	}

	static void copySelection(ExecutionEvent event) {
		ISelection s = HandlerUtil.getCurrentSelection(event);
		if (s instanceof ITextSelection) {
			ITextSelection selection = (ITextSelection) s;
			String textData = selection.getText();
			IWorkbenchWindow activeWorkbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
			Clipboard clipboard = new Clipboard(activeWorkbenchWindow.getShell().getDisplay());
			if (textData != null && !textData.isEmpty()) {
				TextTransfer textTransfer = TextTransfer.getInstance();
				Transfer[] transfers = new Transfer[] { textTransfer };
				Object[] data = new Object[] { textData };
				clipboard.setContents(data, transfers);
				clipboard.dispose();
			}
		}
	}
}
