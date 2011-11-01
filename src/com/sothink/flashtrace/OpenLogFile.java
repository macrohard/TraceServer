package com.sothink.flashtrace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Vector;
import java.util.regex.Matcher;

import org.eclipse.swt.custom.StyledText;

public class OpenLogFile {

	public static void open(StyledText text, File file) {

		StyledText logTxt = text;
		Vector<Log> logContents = new Vector<Log>();

		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
			String log = r.readLine();
			StringBuilder contents = new StringBuilder();
			
			while (log != null) {
				contents.append(log + "\n");
				log = r.readLine();
			}
			r.close();
			
			Matcher m = Log.patternSOS.matcher(contents);
			while (m.find()) {
				Log l = new Log();
				l.process(m.group(1), m.group(2), m.group(3), m.group(4));
				if (l.getValid())
					logContents.add(l);
			}
			
			if (logContents.size() > 0) {

				StringBuilder sb = new StringBuilder();

				int c = 1;
				for (int i = 0; i < logContents.size(); i++) {
					Log l = logContents.get(i);
					l.lineIndex = c - 1;
					c += l.getLineCount();
					sb.append(l.log);
				}

				logTxt.append(sb.toString());
				TraceServer trace = TraceServer.getInstance();
				for (int i = 0; i < logContents.size(); i++) {
					Log l = logContents.get(i);
					logTxt.setLineBackground(l.lineIndex, l.getLineCount(), l.getColor(trace.display));
				}

			}
			else
			{
				logTxt.append(contents.toString());
			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
