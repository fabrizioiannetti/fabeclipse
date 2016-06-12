package com.github.fabeclipse.textedgrep.internal.ui.cmd;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.github.fabeclipse.textedgrep.internal.ui.GrepView;

public class CopyOriginalRange extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		IWorkbenchWindow activeWorkbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
		Clipboard clipboard = new Clipboard(activeWorkbenchWindow.getShell().getDisplay());
		String textData = getContentFromGrepView(part);
		if (textData != null && !textData.isEmpty()) {
			TextTransfer textTransfer = TextTransfer.getInstance();
			Transfer[] transfers = new Transfer[] { textTransfer };
			Object[] data = new Object[] { textData };
			clipboard.setContents(data, transfers);
			clipboard.dispose();
		}
		return null;
	}

	private String getContentFromGrepView(IWorkbenchPart part) {
		if (part instanceof GrepView) {
				GrepView gv = (GrepView) part;
				return (gv.getOriginalForCurrentSelection());
		}
		return null;
	}
}
