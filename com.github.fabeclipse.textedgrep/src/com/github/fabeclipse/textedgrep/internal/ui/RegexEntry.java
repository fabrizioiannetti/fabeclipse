/**
 * Copyright 2015 Fabrizio Iannetti.
 */
package com.github.fabeclipse.textedgrep.internal.ui;

import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;

/**
 * Entry line for a grep regular expression.
 * It consists of:
 * <ul>
 * <li> a combo widget for the regular expression text
 * <li> button to select the color to use for text highlight
 * </ul>
 * 
 * @since 2.0
 *
 */
public class RegexEntry extends Composite {

	private Combo regexpText;
	private Button chooseColor;
	private Color regexColor;
	private IRegexEntryListener listener;
	private boolean showColorChooser = false;
	
	// default colour for foreground of the line
	private static final RGB DEFAULT_REGEX_COLOR = new RGB(0, 0, 0);

	/**
	 * Create an entry line for a regular expression.
	 * 
	 * @param parent the parent container
	 * @param listener an object to receive events.
	 * @param color the color to use for regular expression (XRGB 32 bit format)
	 *        -1 for default
	 */
	public RegexEntry(Composite parent, IRegexEntryListener listener, int color) {
		this(parent, listener, color == -1 ? null : GrepUIUtil.intToRGB(color));
	}

	/**
	 * Create an entry line for a regular expression.
	 * 
	 * @param parent the parent container
	 * @param listener an object to receive events.
	 * @param rgb the color to use for regular expression, null for default
	 */
	public RegexEntry(Composite parent, IRegexEntryListener listener, RGB rgb) {
		super(parent, SWT.NONE);
		this.listener = listener;
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(this);
		regexpText = new Combo(this, SWT.SINGLE);
		if (showColorChooser) {
			if (rgb == null)
				rgb = DEFAULT_REGEX_COLOR;
			regexColor = new Color(getDisplay(), rgb);
			chooseColor = new Button(this, SWT.PUSH);
			updateColorChooser();
			GridDataFactory.fillDefaults().applyTo(chooseColor);
			setupColorChooser();
		}
		setupRegexCombo();
		setTabList(new Control[] { regexpText });
	}

	private void updateColorChooser() {
		if (chooseColor != null && !chooseColor.isDisposed()) {
			Image newImage = GrepUIUtil.colorImage(getDisplay(), regexColor);
			chooseColor.setImage(newImage);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (regexColor != null)
			regexColor.dispose();
	}

	private void setupRegexCombo() {
		// compute space for content assist decoration
		// (code from platform's FindAndReplaceDialog)
		FieldDecoration dec= FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		int hIndent = dec.getImage().getBounds().width;

		GridDataFactory.fillDefaults().grab(true, false).indent(hIndent, 0).applyTo(regexpText);
		regexpText.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// do not start a grep here as this is called
				// when a new value is selected in the combo
				// by pressing the up and down cursor keys
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				String text = getRegexpText();
				if (text != null && !text.isEmpty())
					listener.grep(text, RegexEntry.this);
			}
		});
		
		// add content assist (code from platform's FindAndReplaceDialog)
		ComboContentAdapter contentAdapter= new ComboContentAdapter();
		FindReplaceDocumentAdapterContentProposalProvider findProposer= new FindReplaceDocumentAdapterContentProposalProvider(true);
		new ContentAssistCommandAdapter(
				regexpText,
				contentAdapter,
				findProposer,
				IWorkbenchCommandConstants.EDIT_CONTENT_ASSIST,
				new char[0],
				true);
	}

	private void setupColorChooser() {
		chooseColor.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ColorDialog colorDialog = new ColorDialog(getShell());
				colorDialog.setText("Highlight color");
				colorDialog.setRGB(regexColor.getRGB());
				RGB rgb = colorDialog.open();
				if (rgb != null) {
					Color oldColor = regexColor;
					regexColor = new Color(getDisplay(), rgb);
					updateColorChooser();
					oldColor.dispose();
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		chooseColor.setToolTipText("Choose highlight color");
	}

	public Color getRegexColor() {
		return regexColor;
	}
	
	public String getRegexpText() {
		return regexpText.getText();
	}
	
	public void setRegexpText(String regex) {
		setRegexpText(regex, false);
	}
	
	public void setRegexpText(String regex, boolean selected) {
		regexpText.setText(regex);
		if (selected)
			regexpText.setSelection(new Point(0, regex.length()));;
	}

	public void setRegexHistory(String[] history) {
		regexpText.setItems(history);
	}

	public void remove() {
		// TODO: implement
//		Composite parent = regexpText.getParent();
	}
}
