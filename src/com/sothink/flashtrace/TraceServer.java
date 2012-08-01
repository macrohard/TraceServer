package com.sothink.flashtrace;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TrayItem;

public class TraceServer {

	public String prefix = "Connection #";
	public String flashtrace = "Trace()";
	/**
	 * Output filter. 63 = 111111 Expand by bit, the highest bit is FATAL,
	 * followed by ERROR, WARN, INFO, DEBUG, TRACE
	 */
	public int outFilter = 63;
	public boolean outSequence = true;
	public boolean outTimeStamp = true;
	public boolean wordwrap = false;
	public boolean writeLog = true;
	public boolean useRegex = true;

	public String excludeRules = "";
	public String includeRules = "";
	public String timeFormat = "{HH:mm:ss:SSS} ";

	public Display display;

	private static TraceServer ts = null;

	private String dateFormat = "yyyy-MM-dd[HH.mm.ss]";

	private XMLSocketServer xss;
	private LogFileMonitor lfm;
	private LogThread lt;
	private File logDir;
	private Hashtable<Integer, BufferedWriter> writers;
	private Config config;

	private boolean searchNext = true;
	private int searchPos;

	private Shell sShell = null;
	private TrayItem trayItem;

	private Text txtSearch = null;
	private Button btnSearchNext = null;
	private Button btnSearchPrevious = null;
	private CTabFolder tabFolder = null;
	private Button btnSetting = null;
	private Button btnOpenLog = null;

	private Shell settingPanel = null;

	private Button chkLock = null;
	private Button chkCloseToMinimize = null;
	private Button chkSequence = null;
	private Button chkTimeStamp = null;
	private Button chkWrap = null;
	private Button chkSaveLog = null;
	private Button chkSearchRegular = null;
	private Text txtTimeStamp = null;
	private Text txtInclude = null;
	private Text txtExclude = null;

	private Button itemTrace = null;
	private Button itemDebug = null;
	private Button itemInfo = null;
	private Button itemWarn = null;
	private Button itemError = null;
	private Button itemFatal = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		TraceServer ts = new TraceServer();
		ts.createMainShell();
		ts.createSettingShell();
		ts.init();

		while (!ts.isDisposed()) {
			if (!ts.readAndDispatch())
				ts.sleep();
		}
	}

	public TraceServer() {

		logDir = new File(System.getProperty("user.home") + "\\logs");
		if (!logDir.isDirectory())
			logDir.mkdir();

		writers = new Hashtable<Integer, BufferedWriter>();
		ts = this;

		config = new Config(new File(System.getProperty("user.home")
				+ "\\logs\\config.xml"));
		config.readConfig();
	}

	public static TraceServer getInstance() {
		return ts;
	}

	/**
	 * This method initializes sShell
	 */
	private void createMainShell() {

		display = Display.getDefault();

		sShell = new Shell(display, SWT.SHELL_TRIM);
		sShell.setSize(new Point(800, 450));
		sShell.setLayout(null);
		sShell.setImage(display.getSystemImage(SWT.ICON_WORKING));
		sShell.setMinimumSize(800, 200);

		sShell.addShellListener(new ShellAdapter() {

			public void shellClosed(ShellEvent e) {

				if (chkCloseToMinimize.getSelection()) {
					e.doit = false;
					minimize();
				} else {
					exit();
				}
			}

			public void shellIconified(ShellEvent e) {

				minimize();
			}
		});
		sShell.addListener(SWT.Resize, new Listener() {

			@Override
			public void handleEvent(Event e) {

				Point size = sShell.getSize();

				tabFolder.setSize(size.x - 35, size.y - 85);
				chkLock.setLocation(size.x - 230, 10);
				chkCloseToMinimize.setLocation(size.x - 120, 10);
				btnSetting.setLocation(size.x - 325, 6);
				btnOpenLog.setLocation(size.x - 420, 6);
			}
		});

		Button btnClear = new Button(sShell, SWT.NONE);
		btnClear.setBounds(10, 6, 70, 27);
		btnClear.setText("&Clear");
		btnClear.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				getTextBox(getCurrentTabItem()).setText("");
				searchPos = 0;
			}
		});

		txtSearch = new Text(sShell, SWT.BORDER);
		txtSearch.setBounds(90, 8, 150, 23);
		txtSearch.addKeyListener(new KeyAdapter() {

			public void keyPressed(KeyEvent key) {

				if (key.keyCode == 13)
					search();
			}
		});
		txtSearch.addFocusListener(new FocusAdapter() {

			public void focusGained(FocusEvent e) {
				txtSearch.selectAll();
			}
		});
		txtSearch.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				txtSearch.selectAll();
			}
		});

		btnSearchNext = new Button(sShell, SWT.NONE);
		btnSearchNext.setBounds(245, 6, 50, 27);
		btnSearchNext.setText("&Next");
		btnSearchNext.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				searchNext = true;
				search();
			}
		});

		btnSearchPrevious = new Button(sShell, SWT.NONE);
		btnSearchPrevious.setBounds(300, 6, 60, 27);
		btnSearchPrevious.setText("&Previous");
		btnSearchPrevious.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				searchNext = false;
				search();
			}
		});

		btnOpenLog = new Button(sShell, SWT.NONE);
		btnOpenLog.setBounds(380, 6, 90, 27);
		btnOpenLog.setText("Open Log &File");
		btnOpenLog.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				FileDialog fd = new FileDialog(sShell, SWT.OPEN);
				fd.setText("Open the saved log files");
				fd.setFilterPath(logDir.getAbsolutePath());
				String[] filterExt = { "*.txt" };
				fd.setFilterExtensions(filterExt);
				String openfile = fd.open();
				if (openfile != null) {
					File f = new File(openfile);
					if (f.canRead()) {
						StyledText temp = getTextBox(createTabItem("File: "
								+ f.getName()));
						OpenLogFile.open(temp, f);
					} else {
						addTraceLog("#FF0000File cannot read: " + openfile);
					}
				}
			}
		});

		btnSetting = new Button(sShell, SWT.NONE);
		btnSetting.setBounds(475, 6, 90, 27);
		btnSetting.setText("C&onfiguration");
		btnSetting.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				Rectangle r = sShell.getBounds();
				Point p = settingPanel.getSize();

				settingPanel.setLocation((int) (r.x + (r.width - p.x) * 0.5),
						(int) (r.y + (r.height - p.y) * 0.5));
				settingPanel.setVisible(true);
			}
		});

		chkLock = new Button(sShell, SWT.CHECK);
		chkLock.setBounds(570, 10, 110, 17);
		chkLock.setText("&Lock ScrollBar");
		chkLock.setSelection(true);

		chkCloseToMinimize = new Button(sShell, SWT.CHECK);
		chkCloseToMinimize.setBounds(680, 10, 120, 17);
		chkCloseToMinimize.setText("Close to &Tray");
		chkCloseToMinimize.setSelection(true);

		tabFolder = new CTabFolder(sShell, SWT.TOP | SWT.BORDER);
		tabFolder.setLocation(10, 40);
		tabFolder.setMinimizeVisible(true);
		tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {

			public void minimize(CTabFolderEvent e) {

				e.doit = false;
				CTabItem t = null;
				for (int i = 0; i < tabFolder.getItemCount(); i++) {
					t = tabFolder.getItem(i);
					if (!t.getText().equals(flashtrace)) {
						t.dispose();
						i--;
					}
				}
			}
		});

		createTabItem(flashtrace);

		// Create a system tray icon
		final Menu menu = new Menu(sShell, SWT.POP_UP);
		MenuItem showMenuItem = new MenuItem(menu, SWT.PUSH);
		showMenuItem.setText("Show &Window");
		showMenuItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				maximize();
			}
		});
		new MenuItem(menu, SWT.SEPARATOR);
		MenuItem exitMenuItem = new MenuItem(menu, SWT.PUSH);
		exitMenuItem.setText("E&xit");
		exitMenuItem.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				exit();
			}
		});
		menu.setDefaultItem(showMenuItem);

		trayItem = new TrayItem(display.getSystemTray(), SWT.NONE);
		trayItem.setToolTipText("Trace Server");
		trayItem.setVisible(false);
		trayItem.setImage(sShell.getImage());
		trayItem.addListener(SWT.DefaultSelection, new Listener() {

			@Override
			public void handleEvent(Event e) {

				maximize();
			}
		});
		trayItem.addMenuDetectListener(new MenuDetectListener() {

			@Override
			public void menuDetected(MenuDetectEvent e) {

				menu.setVisible(true);
			}
		});

	}

	private void createSettingShell() {

		settingPanel = new Shell(sShell, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		settingPanel.setSize(580, 270);
		settingPanel.setLayout(null);
		settingPanel.setText("Configuration");
		settingPanel.addShellListener(new ShellAdapter() {

			public void shellClosed(ShellEvent e) {

				e.doit = false;
				saveSettings();
			}

		});

		Group group = new Group(settingPanel, SWT.SHADOW_NONE);
		group.setBounds(10, 5, 275, 110);
		group.setText("General");

		chkSequence = new Button(group, SWT.CHECK);
		chkSequence.setBounds(30, 20, 150, 17);
		chkSequence.setText("Sequence Number");
		if (outSequence)
			chkSequence.setSelection(true);
		chkSequence.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				outSequence = chkSequence.getSelection();
			}
		});

		chkWrap = new Button(group, SWT.CHECK);
		chkWrap.setBounds(190, 20, 70, 17);
		chkWrap.setText("Wordwrap");
		if (wordwrap)
			chkWrap.setSelection(true);
		chkWrap.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				wordwrap = chkWrap.getSelection();
				for (int i = 0; i < tabFolder.getItemCount(); i++) {
					getTextBox(tabFolder.getItem(i)).setWordWrap(wordwrap);
				}
			}
		});

		chkSaveLog = new Button(group, SWT.CHECK);
		chkSaveLog.setBounds(30, 80, 70, 17);
		chkSaveLog.setText("Save Log");
		if (writeLog)
			chkSaveLog.setSelection(true);
		chkSaveLog.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				writeLog = chkSaveLog.getSelection();
			}
		});

		chkSearchRegular = new Button(group, SWT.CHECK);
		chkSearchRegular.setBounds(130, 80, 140, 17);
		chkSearchRegular.setText("Search use Regex");
		if (useRegex)
			chkSearchRegular.setSelection(true);
		chkSearchRegular.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				useRegex = chkSearchRegular.getSelection();
			}
		});

		chkTimeStamp = new Button(group, SWT.CHECK);
		chkTimeStamp.setBounds(30, 50, 80, 17);
		chkTimeStamp.setText("Timestamp");
		if (outTimeStamp)
			chkTimeStamp.setSelection(true);
		chkTimeStamp.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				outTimeStamp = chkTimeStamp.getSelection();
			}
		});

		txtTimeStamp = new Text(group, SWT.BORDER);
		txtTimeStamp.setBounds(110, 48, 140, 23);
		txtTimeStamp.setText(timeFormat);

		Group group2 = new Group(settingPanel, SWT.SHADOW_NONE);
		group2.setBounds(10, 120, 275, 80);
		group2.setText("Filter");

		itemTrace = new Button(group2, SWT.CHECK);
		itemTrace.setBounds(30, 20, 70, 17);
		itemTrace.setText("TRACE");
		if ((outFilter & 1) == 1)
			itemTrace.setSelection(true);
		itemTrace.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				if (itemTrace.getSelection())
					outFilter |= 1;
				else
					outFilter &= 62;
			}
		});
		itemDebug = new Button(group2, SWT.CHECK);
		itemDebug.setBounds(110, 20, 70, 17);
		itemDebug.setText("DEBUG");
		if ((outFilter & 2) == 2)
			itemDebug.setSelection(true);
		itemDebug.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				if (itemDebug.getSelection())
					outFilter |= 2;
				else
					outFilter &= 61;
			}
		});
		itemInfo = new Button(group2, SWT.CHECK);
		itemInfo.setBounds(190, 20, 70, 17);
		itemInfo.setText("INFO");
		if ((outFilter & 4) == 4)
			itemInfo.setSelection(true);
		itemInfo.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				if (itemInfo.getSelection())
					outFilter |= 4;
				else
					outFilter &= 59;
			}
		});
		itemWarn = new Button(group2, SWT.CHECK);
		itemWarn.setBounds(30, 50, 70, 17);
		itemWarn.setText("WARN");
		if ((outFilter & 8) == 8)
			itemWarn.setSelection(true);
		itemWarn.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				if (itemWarn.getSelection())
					outFilter |= 8;
				else
					outFilter &= 55;
			}
		});
		itemError = new Button(group2, SWT.CHECK);
		itemError.setBounds(110, 50, 70, 17);
		itemError.setText("ERROR");
		if ((outFilter & 16) == 16)
			itemError.setSelection(true);
		itemError.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				if (itemError.getSelection())
					outFilter |= 16;
				else
					outFilter &= 47;
			}
		});
		itemFatal = new Button(group2, SWT.CHECK);
		itemFatal.setBounds(190, 50, 70, 17);
		itemFatal.setText("FATAL");
		if ((outFilter & 32) == 32)
			itemFatal.setSelection(true);
		itemFatal.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				if (itemFatal.getSelection())
					outFilter |= 32;
				else
					outFilter &= 31;
			}
		});

		Label lbl = new Label(settingPanel, SWT.LEFT);
		lbl.setBounds(300, 7, 120, 14);
		lbl.setText("Include Rules: (?)");
		lbl.setToolTipText("According to Exclude Rules filter first, and then filtered under Include Rules.\n.*: The current package does not include sub-packages.\n.**: The current package and sub package.");
		txtInclude = new Text(settingPanel, SWT.BORDER | SWT.MULTI);
		txtInclude.setBounds(300, 23, 260, 80);
		txtInclude.setText(includeRules);
		
		lbl = new Label(settingPanel, SWT.LEFT);
		lbl.setBounds(300, 104, 120, 14);
		lbl.setText("Exclude Rules: (?)");
		lbl.setToolTipText("According to Exclude Rules filter first, and then filtered under Include Rules.\n.*: The current package does not include sub-packages.\n.**: The current package and sub package.");
		txtExclude = new Text(settingPanel, SWT.BORDER | SWT.MULTI);
		txtExclude.setBounds(300, 120, 260, 80);
		txtExclude.setText(excludeRules);

		Button btnClose = new Button(settingPanel, SWT.NONE);
		btnClose.setBounds(260, 210, 90, 27);
		btnClose.setText("Close");
		btnClose.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				saveSettings();
			}
		});
		settingPanel.setDefaultButton(btnClose);

		Button btnClear = new Button(settingPanel, SWT.NONE);
		btnClear.setBounds(20, 210, 110, 27);
		btnClear.setText("Clear log files");
		btnClear.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				File[] files = logDir.listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File dir, String name) {

						if (name.indexOf(".txt") != -1)
							return true;
						return false;
					}

				});

				for (File file : files)
					file.delete();
			}
		});

	}

	private void init() {

		lt = new LogThread();
		xss = new XMLSocketServer();
		lfm = new LogFileMonitor();

		addTraceLog("Log file storage directory: " + logDir.getPath());
		sShell.open();
	}

	public boolean isDisposed() {

		return sShell.isDisposed();
	}

	public boolean readAndDispatch() {

		return display.readAndDispatch();
	}

	public void sleep() {

		display.sleep();
	}

	public boolean getLockScrollBar() {
		return chkLock.getSelection();
	}

	public void setText(final int count) {

		display.syncExec(new Runnable() {

			@Override
			public void run() {

				sShell.setText("Trace Server : " + count + " Connections");
			}

		});
	}

	public void newConnect(final int id) {

		display.syncExec(new Runnable() {

			@Override
			public void run() {

				maximize();

				createTabItem(prefix + id);
			}
		});

	}

	public void closeConnect(final int id) {

		BufferedWriter w = writers.get(id);
		if (w != null) {
			try {
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			writers.remove(id);
		}
	}

	private CTabItem createTabItem(String name) {

		StyledText txtLog = new StyledText(tabFolder, SWT.V_SCROLL | SWT.BORDER
				| SWT.H_SCROLL | SWT.READ_ONLY);
		txtLog.setWordWrap(wordwrap);
		txtLog.setIndent(2);
		txtLog.setTabs(16);
		txtLog.addKeyListener(new KeyAdapter() {

			public void keyPressed(KeyEvent key) {

				if (key.stateMask == 262144 && key.keyCode == 107) {
					quickSearch(true);
				} else if (key.stateMask == 393216 && key.keyCode == 107) {
					quickSearch(false);
				}
			}
		});

		CTabItem tabItem = null;
		if (name.equals(flashtrace))
			tabItem = new CTabItem(tabFolder, SWT.NONE);
		else
			tabItem = new CTabItem(tabFolder, SWT.CLOSE);
		tabItem.setText(name);
		tabItem.setControl(txtLog);
		tabItem.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				CTabItem tabItem = (CTabItem) e.widget;
				StyledText txtLog = (StyledText) tabItem.getControl();
				txtLog.dispose();
				tabItem.dispose();
				System.gc();
			}
		});
		tabFolder.setSelection(tabItem);

		return tabItem;
	}

	private CTabItem getCurrentTabItem() {

		return tabFolder.getItem(tabFolder.getSelectionIndex());
	}

	private CTabItem getTabItem(String name) {

		CTabItem t = null;
		for (int i = 0; i < tabFolder.getItemCount(); i++) {
			t = tabFolder.getItem(i);
			if (t.getText().equals(name))
				return t;
		}
		return null;
	}

	public StyledText getTextBox(CTabItem item) {

		if (item == null)
			return null;

		return (StyledText) item.getControl();
	}

	public StyledText getTextBox(String name) {
		return getTextBox(getTabItem(name));
	}

	public void addTraceLog(String text) {

		if (!text.endsWith("\n"))
			text += "\n";
		lt.addLog(text, 0);
	}

	public void addSocketLog(String text, int id, int sn) {

		StringBuilder sb = new StringBuilder();
		sb.append(sn);
		sb.append(".");
		sb.append(getDateString(timeFormat));
		sb.append(text);
		if (!text.endsWith("\n"))
			sb.append("\n");
		text = sb.toString();

		if (writeLog) {
			BufferedWriter w = writers.get(id);
			if (w == null) {
				try {
					w = new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(logDir.getAbsolutePath()
									+ "\\" + getDateString(dateFormat)
									+ " Connect #" + id + ".txt"), "UTF8"));
					writers.put(id, w);

				} catch (UnsupportedEncodingException e) {
					addTraceLog("#FF0000Encoding format is not supported!");
				} catch (FileNotFoundException e) {
					addTraceLog("#FF0000File cannot open!");
				}
			}
			if (w != null) {
				try {
					w.write(text);
					w.flush();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		lt.addLog(text, id);
	}

	private void quickSearch(boolean down) {
		StyledText t = getTextBox(getCurrentTabItem());
		String s = t.getSelectionText();
		if (s.length() != 0) {
			txtSearch.setText(s);
			searchNext = down;
			search();
		}
	}

	private void search() {

		String keyword = txtSearch.getText();
		if (keyword.equals(""))
			return;

		StyledText t = getTextBox(getCurrentTabItem());
		Point p = t.getSelectionRange();
		if (p.y > 0) {
			if (searchNext)
				searchPos = p.x + 1;
			else
				searchPos = p.x - 1;
		} else {
			searchPos = p.x;
		}

		if (searchNext) {
			while (!searchLog(t, keyword)) {
				MessageBox messageBox = new MessageBox(sShell,
						SWT.ICON_INFORMATION | SWT.YES | SWT.NO);
				messageBox.setText("Search");
				messageBox
						.setMessage("Match is not found. Re-search from the beginning?");
				if (messageBox.open() == SWT.YES) {
					searchPos = 0;
				} else {
					break;
				}
			}
		} else {
			while (!searchLog(t, keyword)) {
				MessageBox messageBox = new MessageBox(sShell,
						SWT.ICON_INFORMATION | SWT.YES | SWT.NO);
				messageBox.setText("Search");
				messageBox
						.setMessage("Match is not found. Re-search from the end?");
				if (messageBox.open() == SWT.YES) {
					searchPos = t.getCharCount();
				} else {
					break;
				}
			}
		}
	}

	private boolean searchLog(StyledText t, String keyword) {

		int start = 0, end = 0;

		if (useRegex) {
			Pattern p = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(t.getText());

			if (searchNext) {
				if (!m.find(searchPos))
					return false;

				start = m.start();
				end = m.end();
			} else {
				while (true) {
					if (!m.find())
						break;

					if (m.end() <= searchPos) {
						start = m.start();
						end = m.end();
					} else {
						break;
					}
				}

				if (end == 0)
					return false;
			}
		} else {
			if (searchNext) {
				start = t.getText().indexOf(keyword, searchPos);
				if (start == -1)
					return false;
			} else {
				start = t.getText().lastIndexOf(keyword, searchPos);
				if (start == -1)
					return false;
			}
			end = start + keyword.length();
		}

		t.setSelection(start, end);
		t.setTopIndex(t.getLineAtOffset(start) - t.getSize().y
				/ t.getLineHeight() / 2);
		searchPos = start + 1;
		return true;
	}

	private void exit() {

		xss.stop();
		lfm.stop();
		lt.stop();
		trayItem.dispose();
		sShell.dispose();
		System.exit(0);
	}

	private void minimize() {

		sShell.setVisible(false);
		trayItem.setVisible(true);
	}

	private void maximize() {

		sShell.setVisible(true);
		sShell.setMinimized(false);
		sShell.setActive();
		trayItem.setVisible(false);
	}

	private void saveSettings() {

		timeFormat = txtTimeStamp.getText();
		excludeRules = txtExclude.getText();
		includeRules = txtInclude.getText();
		settingPanel.setVisible(false);
		config.writeConfig();
	}

	private static String getDateString(String format) {

		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(cal.getTime());
	}

}
