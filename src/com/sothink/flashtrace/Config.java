package com.sothink.flashtrace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class Config {

	private File configuration;

	public Config(File file) {
		configuration = file;
	}

	public void readConfig() {
		if (configuration.isFile()) {

			try {
				XMLInputFactory inputFactory = XMLInputFactory.newInstance();
				inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
				XMLStreamReader r = inputFactory
						.createXMLStreamReader(new FileInputStream(
								configuration));

				TraceServer ts = TraceServer.getInstance();
				String localname = "";
				String temp;
				while (r.hasNext()) {
					r.next();
					if (r.isWhiteSpace())
						continue;
					
					switch (r.getEventType()) {
					case XMLStreamConstants.START_ELEMENT:
						localname = r.getLocalName();
						if (localname.equals("config")) {
							for (int i = 0, n = r.getAttributeCount(); i < n; i++) {
								temp = r.getAttributeLocalName(i);
								if (temp.equals("outFilter")) {
									ts.outFilter = Integer.parseInt(r
											.getAttributeValue(i));
								} else if (temp.equals("outSequence")) {
									ts.outSequence = Boolean.parseBoolean(r
											.getAttributeValue(i));
								} else if (temp.equals("outTimestamp")) {
									ts.outTimeStamp = Boolean.parseBoolean(r
											.getAttributeValue(i));
								} else if (temp.equals("wordwrap")) {
									ts.wordwrap = Boolean.parseBoolean(r
											.getAttributeValue(i));
								} else if (temp.equals("writeLog")) {
									ts.writeLog = Boolean.parseBoolean(r
											.getAttributeValue(i));
								} else if (temp.equals("useRegex")) {
									ts.useRegex = Boolean.parseBoolean(r
											.getAttributeValue(i));
								} else if (temp.equals("timeFormat")) {
									ts.timeFormat = r.getAttributeValue(i);
								}
							}
						}
						break;
					case XMLStreamConstants.CDATA:
					case XMLStreamConstants.CHARACTERS:
						temp = r.getText();
						if (localname.equals("includeRules")) {
							ts.includeRules = temp;
						} else if (localname.equals("excludeRules")) {
							ts.excludeRules = temp;
						}
						break;
					}
				}

				r.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (XMLStreamException e) {
				e.printStackTrace();
			}

		}
	}

	public void writeConfig() {
		if (!configuration.isFile()) {
			try {
				configuration.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (configuration.canWrite()) {

			TraceServer ts = TraceServer.getInstance();

			try {
				XMLOutputFactory output = XMLOutputFactory.newInstance();
				XMLStreamWriter writer = output
						.createXMLStreamWriter(new FileOutputStream(
								configuration));
				writer.writeStartDocument();
				writer.writeCharacters("\n");
				writer.writeStartElement("config");
				writer
						.writeAttribute("outFilter", String
								.valueOf(ts.outFilter));
				writer.writeAttribute("outSequence", String
						.valueOf(ts.outSequence));
				writer.writeAttribute("outTimestamp", String
						.valueOf(ts.outTimeStamp));
				writer.writeAttribute("wordwrap", String.valueOf(ts.wordwrap));
				writer.writeAttribute("writeLog", String.valueOf(ts.writeLog));
				writer.writeAttribute("useRegex", String.valueOf(ts.useRegex));
				writer.writeAttribute("timeFormat", ts.timeFormat);
				writer.writeCharacters("\n");
				writer.writeStartElement("includeRules");
				writer.writeCData(ts.includeRules);
				writer.writeEndElement();
				writer.writeCharacters("\n");
				writer.writeStartElement("excludeRules");
				writer.writeCData(ts.excludeRules);
				writer.writeEndElement();
				writer.writeCharacters("\n");
				writer.writeEndDocument();
				writer.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (XMLStreamException e) {
				e.printStackTrace();
			}

		}
	}
}
