package com.github.fabeclipse.textedgrep.views;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Entry line for a grep regular expression.
 * It consists of:
 * <ul>
 * <li> a combo widget for the regular expression text
 * <li> button to select the color to use for text highlight
 * </ul>
 * 
 * @author iannetti
 *
 */
public class RegexEntry extends Composite {

	private Combo regexpText;
	private Button chooseColor;
	private Color regexColor = new Color(getDisplay(), 255, 255, 0);
	private IRegexEntryListener listener;

	/**
	 * Create an entry line for a regular expression.
	 * 
	 * @param parent the parent container
	 * @param listener an object to receive events.
	 */
	public RegexEntry(Composite parent, IRegexEntryListener listener) {
		super(parent, SWT.NONE);
		this.listener = listener;
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(this);
		regexpText = new Combo(this, SWT.SINGLE);
		chooseColor = new Button(this, SWT.PUSH);
		chooseColor.setText(" ");
		chooseColor.setBackground(regexColor);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(regexpText);
		GridDataFactory.fillDefaults().applyTo(chooseColor);
		setupColorChooser();
		setupRegexCombo();
		setTabList(new Control[] { regexpText });
	}

	@Override
	public void dispose() {
		super.dispose();
		regexColor.dispose();
	}

	private void setupRegexCombo() {
		regexpText.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// do not start a grep here as this is called
				// when a new value is selected in the combo
				// by pressing the up and down cursor keys
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				listener.grep(getRegexpText());
			}
		});
	}

	private void setupColorChooser() {
		chooseColor.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				widgetDefaultSelected(e);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				ColorDialog colorDialog = new ColorDialog(getShell());
				RGB rgb = colorDialog.open();
				if (rgb != null) {
					Color oldColor = regexColor;
					regexColor = new Color(getDisplay(), rgb);
					chooseColor.setBackground(regexColor);
					oldColor.dispose();
				}
			}
		});
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
}
