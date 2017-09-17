package fab.image.viewer.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

public class ImageFactory {

	public static class RGBDesc {
		public RGBDesc(int redShift, int greenShift, int blueShift) {
			super();
			this.alphaShift = 0;
			this.redShift = redShift;
			this.greenShift = greenShift;
			this.blueShift = blueShift;
			bitWidth = 24;
		}

		public RGBDesc(int alphaShift, int redShift, int greenShift, int blueShift) {
			super();
			this.alphaShift = alphaShift;
			this.redShift = redShift;
			this.greenShift = greenShift;
			this.blueShift = blueShift;
			bitWidth = 32;
		}

		public int bitWidth;
		public int alphaShift;
		public int redShift;
		public int greenShift;
		public int blueShift;
	}

	public enum RawDataFormat {
		/**
		 * 8 bit (unsigned) per pixel, luminance only.
		 */
		GRAY_U8,
		/**
		 * 16 bit (unsigned) per pixel, luminance only
		 */
		GRAY_U16,
		/**
		 * 24 bit per pixel, interleaved. 8 bit per channel.
		 */
		RGB24, //
		RBG24, //
		GBR24, //
		GRB24, //
		BRG24, //
		BGR24, //
		/**
		 * 32 bit per pixel, interleaved. 8 bit per channel.
		 */
		ARGB32, //
		ARBG32, //
		AGBR32, //
		AGRB32, //
		ABRG32, //
		ABGR32, //
		RGBA32, //
		RBGA32, //
		GBRA32, //
		GRBA32, //
		BRGA32, //
		BGRA32 //
		;

		public synchronized RGBDesc getRGBDesc() {
			if (formatDesc == null) {
				formatDesc = new EnumMap<>(RawDataFormat.class);
				RawDataFormat[] rawDataFormats = RawDataFormat.values();
				for (RawDataFormat rawDataFormat : rawDataFormats) {
					putRGBFormat(rawDataFormat);
				}
			}
			return formatDesc.get(this);
		}

		private static EnumMap<RawDataFormat, RGBDesc> formatDesc;

		private static void putRGBFormat(RawDataFormat df) {
			String name = df.name();
			if (name.matches("A[RGB]{3}32")) {
				formatDesc.put(df, new RGBDesc(24, //
						(3 - name.indexOf('R')) * 8, //
						(3 - name.indexOf('G')) * 8, //
						(3 - name.indexOf('B')) * 8));
			} else if (name.matches("[RGB]{3}A32")) {
				formatDesc.put(df, new RGBDesc(0, //
						(3 - name.indexOf('R')) * 8, //
						(3 - name.indexOf('G')) * 8, //
						(3 - name.indexOf('B')) * 8));
			} else if (name.matches("[RGB]{3}24")) {
				formatDesc.put(df, new RGBDesc( //
						(2 - name.indexOf('R')) * 8, //
						(2 - name.indexOf('G')) * 8, //
						(2 - name.indexOf('B')) * 8));
			}
		}
	}

	private ByteBuffer data;
	private int width = 1;
	private int height = 1;
	private RawDataFormat format;

	private ImageFactory() {
	}

	public static ImageFactory loadRawData(Path file) throws IOException {
		ImageFactory factory = new ImageFactory();
		byte[] bytes = Files.readAllBytes(file);
		factory.data = ByteBuffer.wrap(bytes);
		factory.data.order(ByteOrder.LITTLE_ENDIAN);
		return factory;
	}

	public static ImageFactory wrap(byte[] bytes) throws IOException {
		ImageFactory factory = new ImageFactory();
		factory.data = ByteBuffer.wrap(bytes);
		factory.data.order(ByteOrder.LITTLE_ENDIAN);
		return factory;
	}

	public static ImageFactory use(ByteBuffer data) throws IOException {
		ImageFactory factory = new ImageFactory();
		factory.data = data.asReadOnlyBuffer();
		factory.data.rewind();
		return factory;
	}

	public ImageFactory order(ByteOrder order) {
		data.order(order);
		return this;
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
			fmt = RawDataFormat.ARGB32;
		} else if (format == RawDataFormat.GRAY_U8) {
			bytes = convertU8ToXRGB32();
			fmt = RawDataFormat.ARGB32;
		} else {
			bytes = new byte[data.limit()];
			data.get(bytes);
		}
		RGBDesc rgbDesc = fmt.getRGBDesc();

		int redMask = 0x00FF << rgbDesc.redShift;
		int greenMask = 0x00FF << rgbDesc.greenShift;
		int blueMask = 0x00FF << rgbDesc.blueShift;
		PaletteData paletteData = new PaletteData(redMask, greenMask, blueMask);
		ImageData imageData = new ImageData(width, height, rgbDesc.bitWidth, paletteData, width, bytes);
		Image img = new Image(device, imageData);
		return img;
	}

	private byte[] convertU16ToXRGB32() {
		int numPixels = width * height;
		ByteBuffer dst = ByteBuffer.allocate(numPixels * 4);
		ShortBuffer src = data.asShortBuffer();
		for (int i = 0; i < numPixels; i++) {
			byte grey = (byte) (src.get() >>> 8);
			dst.put((byte) 0).put(grey).put(grey).put(grey); // 0RGB
		}
		return dst.array();
	}

	private byte[] convertU8ToXRGB32() {
		int numPixels = width * height;
		ByteBuffer dst = ByteBuffer.allocate(numPixels * 4);
		ByteBuffer src = data;
		for (int i = 0; i < numPixels; i++) {
			byte grey = src.get();
			dst.put((byte) 0).put(grey).put(grey).put(grey); // 0RGB
		}
		return dst.array();
	}
}
