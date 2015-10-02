package plots.wizards;

import org.eclipse.jface.wizard.Wizard;

public class ImportDataWizard extends Wizard {

	public ImportDataWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	@Override
	public void addPages() {
		addPage(new ImportDataSourcePage("Data source"));
		addPage(new ImportDataPreviewPage("PreviewPage"));
	}
	@Override
	public boolean performFinish() {
		return false;
	}

}
