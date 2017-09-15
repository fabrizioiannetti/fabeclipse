package fab.image.viewer.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

public class ImageFactory {

	public enum RawDataFormat {
		GRAY_U8,
		GRAY_U16,
		XRGB32,
		BGRX888,
	}

	private ByteBuffer data;
	private int width  = 1;
	private int height = 1;
	private int depth  = 1;
	private int scanlinePad = 0;
	
	private int alphaShift = 24;
	private int redShift = 16;
	private int greenShift = 8;
	private int blueShift = 0;
	private RawDataFormat format;
	
	private ImageFactory() {
	}
	
	public static ImageFactory loadRawData(Path file) throws IOException {
		ImageFactory factory = new ImageFactory();
		byte[] bytes = Files.readAllBytes(file);
		factory.data = ByteBuffer.wrap(bytes);
		return factory;
	}

	public static ImageFactory wrap(byte[] bytes) throws IOException {
		ImageFactory factory = new ImageFactory();
		factory.data = ByteBuffer.wrap(bytes);
		return factory;
	}

	public static ImageFactory use(ByteBuffer data) throws IOException {
		ImageFactory factory = new ImageFactory();
		factory.data = data;
		return factory;
	}

	public ImageFactory size(int width, int height) {
		this.width = width;
		this.height = height;
		return this;
	}
	
	public ImageFactory format(RawDataFormat fmt) {
		this.format = fmt;
		return this;
	}

	public Image createSWTImage(Device device) {
		RawDataFormat fmt = format;
		byte[] bytes;
		if (format == RawDataFormat.GRAY_U16) {
			bytes = convertU16ToXRGB32();
			fmt = RawDataFormat.XRGB32;
		} else {
			bytes = data.array();
		}
		switch (fmt) {
		case XRGB32:
			break;

		default:
			break;
		}
		int redMask = 0x00FF << (adjustForAlpha(redShift, alphaShift));
		int greenMask = 0x00FF << (adjustForAlpha(greenShift, alphaShift));
		int blueMask = 0x00FF << (adjustForAlpha(blueShift, alphaShift));
		new PaletteData(redMask, greenMask, blueMask);
		ImageData imageData = new ImageData(width, height, depth, null, scanlinePad, bytes);
		Image img = new Image(device, imageData);
		return img;
	}

	private byte[] convertU16ToXRGB32() {
		int numPixels = width*height;
		ByteBuffer dst = ByteBuffer.allocateDirect(numPixels*4);
		ShortBuffer src = data.asShortBuffer();
		for (int i = 0; i < numPixels; i++) {
			byte grey = (byte) (src.get() >>> 8);
			dst.put((byte)0).put(grey).put(grey).put(grey); // 0RGB
		}
		return dst.array();
	}

	private int adjustForAlpha(int cshift, int alpha) {
		if (cshift < alpha)
			return cshift;
		else
			return cshift + 8; // add alpha channel size
	}

}
