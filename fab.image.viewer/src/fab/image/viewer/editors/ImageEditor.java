package fab.image.viewer.editors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class ImageEditor extends EditorPart implements PaintListener {

	private static final int[][] WELL_KNOWN_SIZES = {
		{ 854, 480 },
		{ 864, 480 },
		{ 320, 240 },
		{ 640, 480 },
		{ 720, 38 },
		{ 720, 1280 },
		{ 1280, 720 },
		{ 720, 1280-38 },
		{ 1920, 1080 },
		{ 1920, 1088 },
	};
	private Canvas canvas;
	private MappedByteBuffer buffer;

	private ByteOrder endianess = ByteOrder.LITTLE_ENDIAN;

	private int alphaShift = 24;
	private int redShift = 16;
	private int greenShift = 8;
	private int blueShift = 0;

	private Image image;
	private Label description;
	private int width = 320;

	public ImageEditor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		
		if (input instanceof IURIEditorInput) {
			IURIEditorInput uriInput = (IURIEditorInput) input;
			URI uri = uriInput.getURI();
			File file = new File(uri);
			try {
				RandomAccessFile f = new RandomAccessFile(file, "r");
				FileChannel channel = f.getChannel();
				buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
				f.close();
			} catch (FileNotFoundException e) {
				throw new PartInitException("File not found", e);
			} catch (IOException e) {
				throw new PartInitException("Error reading file", e);
			}
		}
	}

	private synchronized void createImage() {
		Display device = getEditorSite().getWorkbenchWindow().getShell().getDisplay();

		int bytePerPixel = 2;
		int size = buffer.limit();
		int height = size/ bytePerPixel / width;
		int[] knownSize = isWellKnownSize(size, bytePerPixel);
		if (knownSize != null) {
			width = knownSize[0];
			height = knownSize[1];
		}
		width = 1920;
		height = 1088;

		byte[] data;
		data = new byte[buffer.limit()];
		buffer.position(0);
//		buffer.get(data);

		Image newImage;
		if (bytePerPixel == 4)
			newImage = create32bitImage(device, size, height, data);
		else
			newImage = create16bitImage(device, size, height, data);
		if (image != null)
			image.dispose();
		image = newImage;
		if (canvas != null && !canvas.isDisposed())
			canvas.redraw();
		updateDescription();
	}

	private Image create32bitImage(Display device, int size, int height, byte[] data) {
		// correct for endianess if necessary
//		if (!endianess.equals(ByteOrder.nativeOrder())) {
			buffer.order(endianess);
			ByteBuffer temp = ByteBuffer.wrap(data);
			temp.order(ByteOrder.nativeOrder());
			for (int i = 0; i < size; i+=4) {
				temp.putInt(buffer.getInt());
			}
//		}
		int redMask = 0x00FF << (adjustForAlpha(redShift, alphaShift));
		int greenMask = 0x00FF << (adjustForAlpha(greenShift, alphaShift));
		int blueMask = 0x00FF << (adjustForAlpha(blueShift, alphaShift));
		PaletteData pd = new PaletteData(redMask, greenMask, blueMask);
		ImageData id = new ImageData(width, height, 32, pd, width, data);
		Image newImage = new Image(device, id);
		return newImage;
	}

	private Image create16bitImage(Display device, int size, int height, byte[] data) {
		// correct for endianess if necessary
//		if (!endianess.equals(ByteOrder.nativeOrder())) {
			buffer.order(endianess);
			ByteBuffer temp = ByteBuffer.wrap(data);
			temp.order(ByteOrder.nativeOrder());
			for (int i = 0; i < size; i+=4) {
				temp.putShort(buffer.getShort());
			}
//		}
		int redMask = maskFor16bit(redShift);
		int greenMask = maskFor16bit(greenShift);
		int blueMask = maskFor16bit(blueShift);
		PaletteData pd = new PaletteData(redMask, greenMask, blueMask);
		ImageData id = new ImageData(width, height, 16, pd, width, data);
		Image newImage = new Image(device, id);
		return newImage;
	}


	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	private int adjustForAlpha(int cshift, int alpha) {
		if (cshift < alpha)
			return cshift;
		else
			return cshift + 8; // add alpha channel size
	}

	private int maskFor16bit(int cshift) {
		switch (cshift) {
		case 0: return 0x1F << 0;
		case 8: return 0x3F << 5;
		case 16: return 0x1F << 11;
		}
		return 0;
	}

	private class RGBAction extends Action {
		private int rshift;
		private int gshift;
		private int bshift;
		public RGBAction(String name) {
			super(name);
			rshift = (2 - name.indexOf('R')) * 8;
			gshift = (2 - name.indexOf('G')) * 8;
			bshift = (2 - name.indexOf('B')) * 8;
		}
		@Override
		public void run() {
			redShift = rshift;
			greenShift = gshift;
			blueShift = bshift;
			createImage();
		}
	}

	private class AlphaAction extends Action {
		private int shift;
		public AlphaAction(String name) {
			super(name);
			shift = (3 - name.indexOf("A")) * 8;
		}
		@Override
		public void run() {
			alphaShift = shift;
			createImage();
		}
	}
	
	private class EndianAction extends Action {
		ByteOrder byteOrder;
		public EndianAction(ByteOrder bo) {
			super(bo.toString());
			byteOrder = bo;
		}
		@Override
		public void run() {
			endianess = byteOrder;
			createImage();
		}
	}

	private class SizeAction extends Action {
		public SizeAction(int width, int height) {
			super(width + "x" + height);
		}
	}
	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);
		description = new Label(parent, SWT.SINGLE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(description);

		ToolBarManager toolBarManager = new ToolBarManager();
		toolBarManager.createControl(parent);
		toolBarManager.add(new Action("T") {
		});
		for (int[] s : WELL_KNOWN_SIZES)
			toolBarManager.add(new SizeAction(s[0], s[1]));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(toolBarManager.getControl());
		canvas = new Canvas(parent, SWT.NO_REDRAW_RESIZE);
		canvas.addPaintListener(this);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(canvas);
		
		MenuManager mm = new MenuManager();
		mm.add(new RGBAction("RGB"));
		mm.add(new RGBAction("RBG"));
		mm.add(new RGBAction("GBR"));
		mm.add(new RGBAction("GRB"));
		mm.add(new RGBAction("BGR"));
		mm.add(new RGBAction("BRG"));
		mm.add(new Separator());
		mm.add(new AlphaAction("Axxx"));
		mm.add(new AlphaAction("xxxA"));
		mm.add(new Separator());
		mm.add(new EndianAction(ByteOrder.LITTLE_ENDIAN));
		mm.add(new EndianAction(ByteOrder.BIG_ENDIAN));
		Menu menu = mm.createContextMenu(canvas);
		canvas.setMenu(menu);
		
		createImage();
	}

	@Override
	public void setFocus() {
		canvas.setFocus();
	}

	@Override
	public void dispose() {
		if (image != null)
			image.dispose();
		super.dispose();
	}
	@Override
	public void paintControl(PaintEvent e) {
		if (image != null && !image.isDisposed())
			e.gc.drawImage(image, 0, 0);
	}

	private void updateDescription() {
		char[] d = new char[4];
		d[3 - adjustForAlpha(redShift, alphaShift)/8] = 'R';
		d[3 - adjustForAlpha(greenShift, alphaShift)/8] = 'G';
		d[3 - adjustForAlpha(blueShift, alphaShift)/8] = 'B';
		d[3 - alphaShift/8] = 'A';
		if (description != null && !description.isDisposed())
			description.setText("Format: " + new String(d) + " width: " + width);
	}
	
	private int[] isWellKnownSize(int size, int bytePerPixel) {
		for (int i = 0; i < WELL_KNOWN_SIZES.length ; i++) {
			int[] dims = WELL_KNOWN_SIZES[i]; 
			if (dims[0] * dims[1] * bytePerPixel == size)
				return dims;
		}
		return null;
	}
}
