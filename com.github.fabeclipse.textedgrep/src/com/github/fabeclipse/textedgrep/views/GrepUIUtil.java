/**
 * Copyright 2015 Fabrizio Iannetti.
 */
package com.github.fabeclipse.textedgrep.views;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

public class GrepUIUtil {
	public static Image colorImage(Device display, Color color) {
		Image newImage = new Image(display, 16, 16);
		GC gc = new GC(newImage);
		gc.setBackground(color);
		gc.fillRectangle(0, 0, 16, 16);
		gc.drawRectangle(0, 0, 15, 15);
		gc.dispose();
		return newImage;
	}

	public static RGB intToRGB(int color) {
		return new RGB((color >> 16) & 0x0FF, (color >> 8) & 0x0FF, color & 0x0FF);
	}

	public static int rgbToInt(RGB rgb) {
		return rgb.red << 16 | rgb.green << 8 | rgb.blue;
	}
}
