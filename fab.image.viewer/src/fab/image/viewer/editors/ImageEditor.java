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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
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
	private int bytePerPixel = 4;
	private boolean swap16 = false;
	private Text hexDump;

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

//		int[] knownSize = isWellKnownSize(size, bytePerPixel);
//		if (knownSize != null) {
//			width = knownSize[0];
//			height = knownSize[1];
//		}
		if (width == 0)
			width = 720;
		int size = buffer.limit();
		int height = size / bytePerPixel / width;
		
		// clamp size to used image data
		

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
		buffer.order(endianess);
		ByteBuffer temp = ByteBuffer.wrap(data);
		// correct for endianess if necessary
//		if (!endianess.equals(ByteOrder.nativeOrder())) {
//			temp.order(ByteOrder.nativeOrder());
//		}
		for (int i = 0; i < size; i+=4) {
			int v = buffer.getInt();
			if (swap16)
				v = (v >> 16) & (0x0000FFFF) | (v & (0x0000FFFF)) << 16 ;
			temp.putInt(v);
			
		}
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
		private int w;
		public SizeAction(int width, int height) {
			super(Integer.toString(width));
			w = width;
		}
		@Override
		public void run() {
			width = w;
			createImage();
		}
	}
	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(parent);
		description = new Label(parent, SWT.SINGLE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(description);

		ToolBarManager toolBarManager = new ToolBarManager();
		Action tAction = new Action("Size", IAction.AS_DROP_DOWN_MENU) {
		};
		tAction.setMenuCreator(new IMenuCreator() {
			
			private Menu menu;
			private MenuManager mm;

			@Override
			public Menu getMenu(Menu parent) {
				return null;
			}
			
			@Override
			public Menu getMenu(Control parent) {
				if (mm == null) {
					mm = new MenuManager();
					for (int[] s : WELL_KNOWN_SIZES)
						mm.add(new SizeAction(s[0], s[1]));
					menu = mm.createContextMenu(parent);
				}
				return menu;
			}
			
			@Override
			public void dispose() {
				mm.dispose();
				mm = null;
				menu = null;
			}
		});
		toolBarManager.add(tAction);
		toolBarManager.createControl(parent);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(toolBarManager.getControl());
		canvas = new Canvas(parent, SWT.NO_REDRAW_RESIZE | SWT.V_SCROLL);
		canvas.addPaintListener(this);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(canvas);
		
		canvas.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent e) {
			}
			@Override
			public void mouseDown(MouseEvent e) {
				if (e.button == 1) {
					String hd = String.format("X:%d Y:%d\n", e.x, e.y);
					int index = (e.x + e.y * width) * bytePerPixel;
					for (int i = 0 ; i < 16 ; i++) {
						byte v = buffer.get(index + i);
						hd += String.format("%02x ", v);
					}
					hd += "\n";
					for (int i = 0 ; i < 4 ; i++) {
						int v = buffer.getInt(index + i * 4);
						hd += String.format("   %08x ", v);
					}
					hexDump.setText(hd);
				}
			}
			@Override
			public void mouseDoubleClick(MouseEvent e) {
			}
		});
		
		hexDump = new Text(parent, SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(hexDump);

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
		mm.add(new Action("RGB32") {
			@Override
			public void run() {
				bytePerPixel = 4;
				createImage();
			}
		});
		mm.add(new Action("RGB16") {
			@Override
			public void run() {
				bytePerPixel = 2;
				createImage();
			}
		});
		mm.add(new Action("SWAP16") {
			@Override
			public void run() {
				swap16 = !swap16;
				createImage();
			}
		});
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

		int redMask = 0x00FF << (adjustForAlpha(redShift, alphaShift));
		int greenMask = 0x00FF << (adjustForAlpha(greenShift, alphaShift));
		int blueMask = 0x00FF << (adjustForAlpha(blueShift, alphaShift));
		int alphaMask = 0x00FF << alphaShift;

		if (description != null && !description.isDisposed()) {
			String string = "Format: " + new String(d);
			string += " w:" + width;
			string += " e:" + endianess;
			string += " bpp:" + bytePerPixel;
			string += String.format("redMask:%08x ", redMask);
			string += String.format("greenMask:%08x ", greenMask);
			string += String.format("blueMask:%08x ", blueMask);
			string += String.format("alphaMask:%08x ", alphaMask);
			string += String.format(" swap16:%b", swap16);
			description.setText(string);
		}
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
