package com.github.fabeclipse.timetrim;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.nebula.widgets.calendarcombo.CalendarCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

public class TimeToolWorkbenchWindowControl extends
		WorkbenchWindowControlContribution {
	private final DateFormat df = new SimpleDateFormat("HH:mm");
	private final DateFormat ttdf = new SimpleDateFormat("yyyy:MM:dd 'week' w");

	public TimeToolWorkbenchWindowControl() {
	}

	public TimeToolWorkbenchWindowControl(String id) {
		super(id);
	}

	@Override
	protected Control createControl(Composite parent) {
//		final ToolItem item = new ToolItem((ToolBar) parent, SWT.DROP_DOWN);
		final Button item = new Button(parent, SWT.ARROW | SWT.FLAT);
//		item.setText(df.format(new Date()));
//		final DateTime item = new DateTime(parent, SWT.TIME);
//		final DateTime item = new DateTime(parent, SWT.TIME);
//		final CalendarCombo item = new CalendarCombo(parent, SWT.READ_ONLY | SWT.FLAT);
//		item.setDate(new Date());

		Timer timeUpdater = new Timer();
		TimerTask task = new TimerTask() {
			private String shown = "";
			private String ttShown = "";
			@Override
			public void run() {
				// if widget is disposed, cancel the timer
				if (item.isDisposed()) {
					this.cancel();
					return;
				}

				Date date = new Date();
				final String dateToShow = df.format(date);
				if (!shown.equals(dateToShow)) {
					shown = dateToShow;
					item.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							if(!item.isDisposed())
//								item.setText(dateToShow);
								;
						}
					});
				}
				final String ttDateToShow = ttdf.format(date);
				if (!ttShown.equals(ttDateToShow)) {
					ttShown = ttDateToShow;
					item.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							if(!item.isDisposed())
								item.setToolTipText(ttDateToShow);
						}
					});
				}
			}
		};
		timeUpdater.scheduleAtFixedRate(task, 1000, 1000);
		return item;
	}

}
