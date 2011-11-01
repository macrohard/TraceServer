package com.sothink.flashtrace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class LogFileMonitor implements Runnable {

	private File logFile;

	private long lastModifiedTime;

	private int logContentSize;

	private boolean running;

	public LogFileMonitor() {

		logFile = new File(System.getenv().get("APPDATA") + "\\Macromedia\\Flash Player\\Logs\\flashlog.txt");
		if (!logFile.exists()) {

			TraceServer.getInstance().addTraceLog("#FF0000FlashPlayer log file is not found, cannot listen trace() statement!");
		} else {
			
			lastModifiedTime = logFile.lastModified();
			running = true;
			new Thread(this).start();
			TraceServer.getInstance().addTraceLog("#00FF00FlashPlayer log file is found: " + logFile.getAbsolutePath());
		}
		
	}

	public void stop() {

		running = false;
	}

	@Override
	public void run() {

		while (running) {

			try {
				long newModifiedTime = logFile.lastModified();
				if (newModifiedTime != lastModifiedTime) {

					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), "UTF-8"));
					ArrayList<String> curContent = new ArrayList<String>();

					String temp = br.readLine();
					while (temp != null) {
						curContent.add(temp);
						temp = br.readLine();
					}

					if (curContent.size() > logContentSize) {
						TraceServer trace = TraceServer.getInstance();
						for (int i = logContentSize; i < curContent.size(); i++) {
							trace.addTraceLog(curContent.get(i));
						}
					}

					logContentSize = curContent.size();
					lastModifiedTime = newModifiedTime;
				}

				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
}
