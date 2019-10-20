/**
 * Copyright 2015 Fabrizio Iannetti.
 */
package com.github.fabeclipse.textedgrep;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.IWorkbenchPart;

/**
 * @since 2.0
 */
public interface IGrepTarget {
	// indicate start and stop of parsing
	public void start();
	public void stop();

	// to iterate on target lines
	public boolean hasNextLine();
	public String nextLine();

	/**
	 * Get the size in characters of the target content
	 * 
	 * @return the target length.
	 * 
	 * @since 3.0
	 */
	public long getLength();

	// to sync grep view selection with target's one
	public void select(int start, int length);
	
	// utility: just used to get the original offset
	// TODO: should not be part of the interface
	public int getLineOffset(int line);

	public boolean isSame(IWorkbenchPart part);
	public String getTitle();

	/**
	 * Get a portion of the original document as text.
	 * 
	 * Implementation may decide to return less text than
	 * requested if deemed too much.
	 * 
	 * @param origStartLine start line of the portion (includes this line)
	 * @param origEndLine end line of the portion (includes this line)
	 * @param origStartOffset offset of portion start in first line (0 means from the beginning)
	 * @param origEndOffset length of endline to include in portion
	 * @since 3.0
	 */
	public String getTextBetweenLines(int origStartLine, int origEndLine, int origStartOffset, int origEndOffset);
}