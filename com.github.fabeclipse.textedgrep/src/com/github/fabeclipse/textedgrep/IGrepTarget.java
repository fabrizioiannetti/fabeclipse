package com.github.fabeclipse.textedgrep;

import org.eclipse.ui.IWorkbenchPart;

public interface IGrepTarget {
	// indicate start and stop of parsing
	public void start();
	public void stop();

	// to iterate on target lines
	public boolean hasNextLine();
	public String nextLine();

	// to sync grep view selection with target's one
	public void select(int start, int length);
	
	// utility: just used to get the original offset
	// TODO: should not be part of the interface
	public int getLineOffset(int line);

	public boolean isSame(IWorkbenchPart part);
	public String getTitle();
}