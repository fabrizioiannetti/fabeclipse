package plots.views;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class ImportDataFromTextDialog extends SelectionStatusDialog {

	private static final int BUTTON_ID = IDialogConstants.CLIENT_ID;
	private Text regex;
	private TableViewer viewer;
	private DataImporter importer = new DataImporter();
	private IWorkbenchWindow ww;
	private ArrayList<String> textEditors;
	private CCombo source;

	public ImportDataFromTextDialog(Shell parent) {
		super(parent);
		setMessage("Choose content from on of the open editors listed below");
	}

	public void listOpenEditors(IWorkbenchWindow ww) {
		this.ww = ww;
		textEditors = new ArrayList<>();
		IEditorReference[] editors = ww.getActivePage().getEditorReferences();
		for (IEditorReference ep : editors) {
			try {
				textEditors.add(ep.getEditorInput().getName());
			} catch (PartInitException e) {
			}
		}
	}

	private int getActiveSourceInWorkbench() {
		IEditorPart ed = ww.getActivePage().getActiveEditor();
		if (ed != null) {
			String name = ed.getEditorInput().getName();
			for (int i = 0; i < textEditors.size(); i++) {
				if (name.equals(textEditors.get(i)))
					return i;
			}
		}
		return -1;
	}

	private String[] getSources() {
		return textEditors.toArray(new String[textEditors.size()]);
	}
	private IDocument getSelectedEditorDocument(String name) {
		AbstractTextEditor te = null;
		if (ww == null)
			return null;
		IEditorReference[] editors = ww.getActivePage().getEditorReferences();
		for (IEditorReference ep : editors) {
			IEditorPart editor = ep.getEditor(false);
			if (editor != null && editor.getEditorInput().getName().equals(name) && editor instanceof AbstractTextEditor) {
				te = (AbstractTextEditor) editor;
			}
		}
		if (te == null)
			return null;
		IDocumentProvider documentProvider = te.getDocumentProvider();
		IDocument document = documentProvider.getDocument(te.getEditorInput());
		return document;
	}


	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		// add combo to select the editor
		source = new CCombo(area, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(source);
		String[] sources = getSources();
		source.setItems(sources);
		int selected = getActiveSourceInWorkbench();
		if (selected < 0 && sources.length > 0)
			selected = 0;
		source.select(0);
		// add regular expression field to be used to extract data
		Label label = new Label(area, SWT.NONE);
		label.setText("Regular expression to extract data");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
		regex = new Text(area, SWT.SINGLE | SWT.BORDER);
		regex.setToolTipText("This expression is used to extract integer data from the text");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(regex);
		viewer = new TableViewer(area, SWT.FULL_SELECTION | SWT.BORDER);
		viewer.setContentProvider(new IStructuredContentProvider() {
			private DataImporter input;

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				if (newInput instanceof DataImporter) {
					input = (DataImporter) newInput;
				}
			}
			
			@Override
			public void dispose() {
			}
			
			@Override
			public Object[] getElements(final Object inputElement) {
				if (input == null)
					return new String[0];
				final String[] elements = new String[input.getData(0).length];
				final int[] data = input.getData(0);
				for (int i = 0; i < elements.length; i++) {
					elements[i] = Integer.toString(data[i]);
				}
				return elements;
			}
		});
		viewer.setInput(new String[] {"first", "second", "third", "fourth"});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(viewer.getControl());

		regex.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Button button = getButton(BUTTON_ID);
				if (button == null || button.isDisposed())
					return;
				try {
					Pattern.compile(regex.getText());
					button.setEnabled(true);
				} catch (Exception e1) {
					button.setEnabled(false);
				}
			}
		});
		
		return area;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		createButton(parent, BUTTON_ID, "Preview", false);
		getButton(BUTTON_ID).setEnabled(false);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		try {
			if (buttonId == BUTTON_ID)
				importData(100);
			else if (buttonId == IDialogConstants.OK_ID)
				importData(0);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.buttonPressed(buttonId);
	}

	private void importData(int lines) throws BadLocationException {
		String selected = source.getText();
		IDocument document = getSelectedEditorDocument(selected);
		if (document != null) {
			String text;
			if (lines > 0) {
				int offset;
				if (lines >= document.getNumberOfLines())
					offset = document.getLength();
				else
					offset = document.getLineOffset(lines);
				text = document.get(0, offset);
			}
			else
				text = document.get(); // TODO: this is BAD: it returns the whole editor content!
			importer.importFromText(text, regex.getText());
			viewer.setInput(importer);
		}
	}

	@Override
	protected void computeResult() {
		// TODO Auto-generated method stub
		
	}

	public int[] getData() {
		return importer.getData(0);
	}
}
