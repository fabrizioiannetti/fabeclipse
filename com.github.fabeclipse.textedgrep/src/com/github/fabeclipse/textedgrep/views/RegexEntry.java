package com.github.fabeclipse.textedgrep.views;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
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

/**
 * Entry line for a grep regular expression.
 * It consists of:
 * <ul>
 * <li> a combo widget for the regular expression text
 * <li> button to select the color to use for text highlight
 * </ul>
 * 
 * @author iannetti
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
		GridDataFactory.fillDefaults().grab(true, false).applyTo(regexpText);
		regexpText.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// do not start a grep here as this is called
				// when a new value is selected in the combo
				// by pressing the up and down cursor keys
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				listener.grep(getRegexpText(), RegexEntry.this);
			}
		});
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
