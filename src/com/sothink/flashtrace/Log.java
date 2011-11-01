package com.sothink.flashtrace;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class Log {

	public static int TRACE_COLOR = 12776645;
	public static int DEBUG_COLOR = 11195647;
	public static int INFO_COLOR = 16777109;
	public static int WARN_COLOR = 16750899;
	public static int ERROR_COLOR = 16716563;
	public static int FATAL_COLOR = 14222083;

	private static Pattern patternColor = Pattern.compile("#[A-F0-9a-f]{6}");
	private static Pattern patternFilter = Pattern.compile("^([\\w\\.\\:]*)\\: ");
	private static Pattern patternElse = Pattern
			.compile("(\\d+\\.)(\\{[\\d:]*\\} )([\\s\\S]*)");
	public static Pattern patternSOS = Pattern
			.compile("(\\d+\\.)(\\{[\\d:]*\\} )!SOS<showMessage key=\\\"(\\w+)\\\"><!\\[CDATA\\[([\\s\\S]*?)\\]\\]></showMessage>");

	private static String postfix_cur = ".*";
	private static String postfix_cur_sub = ".**";

	public String log;
	public int lineIndex;

	private int id;
	private int color = 16777215;
	private boolean valid = true;

	public Log(int id) {

		this.id = id;
	}

	public Log() {
		this(0);
	}

	public Log(String text, int id) {

		this.id = id;
		this.log = text;

		Matcher m = patternSOS.matcher(text);
		if (m.find())
			process(m.group(1), m.group(2), m.group(3), m.group(4));
		else {
			m = patternElse.matcher(text);
			if (m.find()) {
				TraceServer trace = TraceServer.getInstance();
				StringBuilder temp = new StringBuilder();
				if (trace.outSequence)
					temp.append(m.group(1));
				if (trace.outTimeStamp)
					temp.append(m.group(2));
				temp.append(m.group(3));
				temp.append("\n");
				log = temp.toString();
			}
			setCustomColor();
		}
	}

	public int getLineCount() {
		return log.split("\n").length;
	}

	public int getID() {
		return this.id;
	}

	public boolean getValid() {
		return this.valid;
	}

	/**
	 * According to log level processing the background color
	 * 
	 * @param level
	 * @param content
	 * @param outFilter
	 * @return
	 */
	public void process(String sn, String timestamp, String level,
			String content) {

		Matcher n = patternFilter.matcher(content);
		if (n.find() && filterQualifier(n.group(1))) {
			valid = false;
			return;
		}

		if (filterLevel(level)) {
			valid = false;
			return;
		}

		// Build format text
		TraceServer trace = TraceServer.getInstance();
		StringBuilder temp = new StringBuilder();
		if (trace.outSequence)
			temp.append(sn);
		if (trace.outTimeStamp)
			temp.append(timestamp);
		temp.append("【");
		temp.append(level);
		temp.append("】");
		temp.append(content);
		temp.append("\n");
		log = temp.toString();

		setCustomColor();
	}

	/**
	 * According to custom color string processing the background color
	 */
	private void setCustomColor() {

		Matcher n = patternColor.matcher(log);
		if (n.find()) {
			String s = n.group();
			color = Integer.parseInt(s.substring(1), 16);
			log = n.replaceFirst("");
		}
	}

	/**
	 * Rules dealing with include and exclude tip: Do not use regular, step by
	 * step by the recursive test string. <br>
	 * .* The current package does not include sub-packages <br>
	 * .** The current package and sub package
	 * 
	 * @param qualifier
	 * @return Return true should be filtered
	 */
	private boolean filterQualifier(String qualifier) {

		TraceServer trace = TraceServer.getInstance();
		qualifier = qualifier.replace("::", ".");
		if (trace.excludeRules.indexOf(qualifier) != -1)
			return true;

		if (trace.includeRules.length() == 0)
			return false;
		else if (trace.includeRules.indexOf(qualifier) != -1)
			return false;

		qualifier = getParentString(qualifier);
		if (qualifier == null)
			return true;

		String test = qualifier + postfix_cur;
		if (trace.excludeRules.indexOf(test) != -1)
			return true;

		if (trace.includeRules.indexOf(test) != -1)
			return false;

		return recursive(getParentString(qualifier));
	}

	private boolean recursive(String qualifier) {
		
		if (qualifier == null)
			return true;
		
		String test = qualifier + postfix_cur_sub;
		TraceServer trace = TraceServer.getInstance();
		if (trace.excludeRules.indexOf(test) != -1)
			return true;

		if (trace.includeRules.indexOf(test) != -1)
			return false;
		
		return recursive(getParentString(qualifier));
	}

	private String getParentString(String qualifier) {
		int pos = qualifier.lastIndexOf(".");
		if (pos != -1)
			return qualifier.substring(0, pos);
		else
			return null;
	}

	private boolean filterLevel(String level) {

		TraceServer trace = TraceServer.getInstance();
		if (level.equals("TRACE")) {
			if ((trace.outFilter & 1) != 1) {
				return true;
			}

			color = Log.TRACE_COLOR;
		} else if (level.equals("DEBUG")) {
			if ((trace.outFilter & 2) != 2) {
				return true;
			}

			color = Log.DEBUG_COLOR;
		} else if (level.equals("INFO")) {
			if ((trace.outFilter & 4) != 4) {
				return true;
			}

			color = Log.INFO_COLOR;
		} else if (level.equals("WARN")) {
			if ((trace.outFilter & 8) != 8) {
				return true;
			}

			color = Log.WARN_COLOR;
		} else if (level.equals("ERROR")) {
			if ((trace.outFilter & 16) != 16) {
				return true;
			}

			color = Log.ERROR_COLOR;
		} else if (level.equals("FATAL")) {
			if ((trace.outFilter & 32) != 32) {
				return true;
			}

			color = Log.FATAL_COLOR;
		}
		return false;
	}

	public Color getColor(Display display) {
		return new Color(display, color >> 16, color >> 8 & 0xFF, color & 0xFF);
	}
}
