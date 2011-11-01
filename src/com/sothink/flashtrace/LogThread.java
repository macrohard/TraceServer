package com.sothink.flashtrace;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.swt.custom.StyledText;

public class LogThread implements Runnable {

	private Vector<Log> logContents;

	private boolean running;
	
	public LogThread() {

		logContents = new Vector<Log>();
		new Thread(this).start();
		running = true;
	}

	public void stop() {

		running = false;
	}

	public void addLog(String t, int i) {

		Log l = new Log(t, i);
		if (l.getValid())
			logContents.add(l);
	}
	
	@Override
	public void run() {

		while (running) {

			// System.out.println(logContents.size());
			try {

				Thread.sleep(50);

				if (logContents.size() > 0)
					TraceServer.getInstance().display.syncExec(new Runnable() {

						@Override
						public void run() {

							Hashtable<Integer, Vector<Log>> logList = new Hashtable<Integer, Vector<Log>>();

							while (logContents.size() > 0) {
								if (!running)
									return;

								Log l = logContents.remove(0);

								Vector<Log> list = logList.get(l.getID());
								if (list == null) {
									list = new Vector<Log>();
									logList.put(l.getID(), list);
								}
								list.add(l);
							}

							TraceServer trace = TraceServer.getInstance();
							StyledText txtTraceLog = trace.getTextBox(trace.flashtrace);

							Iterator<Integer> it = logList.keySet().iterator();
							while (it.hasNext()) {
								int id = it.next();
								Vector<Log> list = logList.get(id);

								StringBuilder sb = new StringBuilder();
								StyledText temp = null;
								if (id > 0)
									temp = trace.getTextBox(trace.prefix + id);
								else
									temp = txtTraceLog;

								if (temp != null) {
									int c = temp.getLineCount();
									for (int i = 0; i < list.size(); i++) {
										Log l = list.get(i);
										l.lineIndex = c - 1;
										c += l.getLineCount();
										sb.append(l.log);
									}
								} else {
									temp = txtTraceLog;
									int c = temp.getLineCount();
									for (int i = 0; i < list.size(); i++) {
										Log l = list.get(i);
										l.lineIndex = c - 1;
										c += l.getLineCount();
										sb.append("<" + id + ">" + l.log);
									}

								}

								temp.append(sb.toString());
								for (int i = 0; i < list.size(); i++) {
									Log l = list.get(i);
									temp.setLineBackground(l.lineIndex, l.getLineCount(), l.getColor(trace.display));
								}

								if (trace.getLockScrollBar())
									temp.setTopIndex(temp.getLineCount() - 1);

							}

						}

					});

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	
}
