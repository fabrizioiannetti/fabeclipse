package fab.faber.launchview.ui;


import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

public class LaunchesView extends ViewPart {
	public static final String ID = "fab.faber.launchview.ui.LaunchesView";
	
	private static final String KEY_SHOW_PINNED_ONLY = "showPinnedOnly";
	private static final String KEY_SHOW_PUBLIC_ONLY = "showPublicOnly";
	private static final String KEY_PINNED_CONFIGS = "pinnedConfigs";
	private static final String KEY_NAME = "name";

	@Inject IWorkbench workbench;

	private final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
	
	private TableViewer viewer;
	private Action runAction;
	private Action debugAction;
	private Action pinAction;
	private Action unpinAction;
	private Action doubleClickAction;
	private Action showPinnedOnlyAction;
	private Action showPublicOnlyAction;

	private Set<String> pinnedLaunchConfigs = new TreeSet<>();
	private IMemento memento;

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setInput(new ArrayList<ILaunchConfiguration>());
		viewer.setLabelProvider(new DecoratingLabelProvider(
				DebugUITools.newDebugModelPresentation(),
				PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator()));
		viewer.setComparator(new ViewerComparator() {
			@Override
			public int category(Object element) {
				if (pinnedLaunchConfigs.contains(((ILaunchConfiguration)element).getName())) {
					return 0;
				}
				return 1;
			}
		});
		viewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return !showPinnedOnlyAction.isChecked() || pinnedLaunchConfigs.contains(((ILaunchConfiguration)element).getName());
			}
		});
		// Create the help context id for the viewer's control
		workbench.getHelpSystem().setHelp(viewer.getControl(), "fab.faber.launchview.viewer");
		getSite().setSelectionProvider(viewer);
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		updateLaunchList();
		launchManager.addLaunchConfigurationListener(new ILaunchConfigurationListener() {
			@Override
			public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
				updateLaunchList();
			}
			@Override
			public void launchConfigurationChanged(ILaunchConfiguration configuration) {
				updateLaunchList();
			}
			@Override
			public void launchConfigurationAdded(ILaunchConfiguration configuration) {
				updateLaunchList();
			}
		});
		launchManager.addLaunchListener(new ILaunchesListener() {
			
			@Override
			public void launchesRemoved(ILaunch[] launches) {
				// TODO Auto-generated method stub
			}
			
			@Override
			public void launchesChanged(ILaunch[] launches) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void launchesAdded(ILaunch[] launches) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	private void updateLaunchList() {
		try {
			ArrayList<ILaunchConfiguration> configsToShow = new ArrayList<>();
			ILaunchConfigurationType[] types = launchManager.getLaunchConfigurationTypes();
			for (ILaunchConfigurationType type : types) {
				ILaunchConfiguration[] configurations = launchManager.getLaunchConfigurations(type);
				if (type.isPublic() || !showPublicOnlyAction.isChecked()) {
					for (ILaunchConfiguration configuration : configurations) {
						configsToShow.add(configuration);
					}
				}
			}
			viewer.setInput(configsToShow);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				LaunchesView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(runAction);
		manager.add(debugAction);
		manager.add(new Separator());
		manager.add(showPinnedOnlyAction);
		manager.add(showPublicOnlyAction);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(runAction);
		manager.add(debugAction);
		manager.add(new Separator());
		ILaunchConfiguration configuration = getSelectedConfiguration();
		if (configuration != null && pinnedLaunchConfigs.contains(configuration.getName())) {
			manager.add(unpinAction);
		} else {
			manager.add(pinAction);
		}
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(runAction);
		manager.add(debugAction);
	}

	private void makeActions() {
		ActionFactory af = new ActionFactory();
		runAction = af.make("Run", "Run the selected launch configuration", () -> {
			IStructuredSelection selection = viewer.getStructuredSelection();
			ILaunchConfiguration configuration = (ILaunchConfiguration) selection.getFirstElement();
			DebugUITools.launch(configuration, ILaunchManager.RUN_MODE);
		});
		runAction.setImageDescriptor(DebugUITools.getImageDescriptor(IDebugUIConstants.IMG_ACT_RUN));;
		
		debugAction = af.make("Debug", "Debug the selected launch configuration", () -> {
			ILaunchConfiguration configuration = getSelectedConfiguration();
			DebugUITools.launch(configuration, ILaunchManager.DEBUG_MODE);
		});
		debugAction.setImageDescriptor(DebugUITools.getImageDescriptor(IDebugUIConstants.IMG_ACT_DEBUG));;

		doubleClickAction = af.make(() -> {
			ILaunchConfiguration configuration = getSelectedConfiguration();
			DebugUITools.openLaunchConfigurationDialog(workbench.getActiveWorkbenchWindow().getShell(), configuration, "org.eclipse.debug.ui.launchGroup.run", null);
		});

		pinAction = af.make("Pin", "Pin the current launch configuration", () -> {
			ILaunchConfiguration configuration = getSelectedConfiguration();
			pinnedLaunchConfigs.add(configuration.getName());
			viewer.refresh();
		});

		unpinAction = af.make("Unpin", "Unpin the current launch configuration", () -> {
			ILaunchConfiguration configuration = getSelectedConfiguration();
			pinnedLaunchConfigs.remove(configuration.getName());
			viewer.refresh();
		});

		showPinnedOnlyAction = af.makeChecked("ShowPinnedOnly", "Show only pinned launch configurations", () -> viewer.refresh());
		if (memento != null) {
			Boolean value = memento.getBoolean(KEY_SHOW_PINNED_ONLY);
			if (value != null)
				showPinnedOnlyAction.setChecked(value);
		}
		showPublicOnlyAction = af.makeChecked("ShowPublicOnly", "Show only public launch configurations", () -> updateLaunchList());
		showPublicOnlyAction.setChecked(true);
		if (memento != null) {
			Boolean value = memento.getBoolean(KEY_SHOW_PUBLIC_ONLY);
			if (value != null)
				showPublicOnlyAction.setChecked(value);
		}
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(event -> doubleClickAction.run());
	}

	private ILaunchConfiguration getSelectedConfiguration() {
		IStructuredSelection selection = viewer.getStructuredSelection();
		ILaunchConfiguration configuration = (ILaunchConfiguration) selection.getFirstElement();
		return configuration;
	}
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		if (memento != null) {
			this.memento = memento;
			IMemento[] children = memento.getChildren(KEY_PINNED_CONFIGS);
			for (IMemento child : children) {
				String name = child.getString(KEY_NAME);
				if (name != null) {
					pinnedLaunchConfigs.add(name);
				}
			}
		}
		super.init(site, memento);
	}
	@Override
	public void saveState(IMemento memento) {
		memento.putBoolean(KEY_SHOW_PINNED_ONLY, showPinnedOnlyAction.isChecked());
		for (String name : pinnedLaunchConfigs) {
			IMemento child = memento.createChild(KEY_PINNED_CONFIGS);
			child.putString(KEY_NAME, name);
		}
		super.saveState(memento);
	}
}
