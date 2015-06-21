fabeclipse
==========

This project contains eclipse plugins I develop for my pesonal use and that can hopefully be useful for others.

TextEditor Grep
---------------

This plugin contributes a view that allows to enter a regular expression, used to produce a grep-like output
(shown in the view) of the current text editor.

The view shows the original line numbers and when the cursor is moved in the grep output, the cursor in the
text editor is moved to the matching line.
Any editor that subclasses the AbstractTextEditor should be supported.

The view can optionally link to the current editor (press the double link icon in the view), in this mode
the grep is refreshed whenever an editor is activated.

Key bindings:
* when in an editor press CTRL+ALT+G (CMD+ALT+G) to open/activate the grep view, if text is selected, it is copied in the grep expression box and it is preselected
* TAB to move from grep result to grep text box and vice versa
* CTRL+F in grep result opens an incremental search box
* Ctrl+Space activates completion in the grep text box
* Enter/Mouse double click show the part where the grep was done

inside the search box:
* ESC closes the find bar, arrow down searches forwards, arrow up backwards
* search is always case insensitive and incremental (find as you type)

Marketplace site: http://marketplace.eclipse.org/content/texteditor-grep
