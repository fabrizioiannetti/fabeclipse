package com.github.fabeclipse.textedgrep;

/**
 * @since 3.0
 */
public interface IGrepContext {

	int getOriginalLine(int grepLine);

	int getMatchBeginForGrepLine(int grepLine, int index);

	int getMatchEndForGrepLine(int grepLine, int index);

	int getColorForGrepLine(int grepLine);

	int getNumberOfMatches();

	int getNumberOfMatchesForGrepLine(int grepLine);

	int getMaxOriginalLine();

	String getText();

	IGrepTarget getTarget();
}