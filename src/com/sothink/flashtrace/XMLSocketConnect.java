package com.sothink.flashtrace;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class XMLSocketConnect implements Runnable {

	private Socket client;

	private int id;
	
	private int count;

	public XMLSocketConnect(Socket client, int id) {

		this.client = client;
		this.id = id;
	}

	@Override
	public void run() {

		boolean firstLog = true;
		TraceServer trace = TraceServer.getInstance();

		try {
			InputStreamReader input = new InputStreamReader(client.getInputStream(), "UTF-8");
			BufferedReader reader = new BufferedReader(input);

			StringBuilder data = new StringBuilder();
			int c = reader.read();
			while (c != -1) {

				if (c == 0) {
					String str = data.toString();
					if (firstLog) {
						firstLog = false;
						if (str.indexOf("<policy-file-request/>") != -1) {
							OutputStreamWriter output = new OutputStreamWriter(client.getOutputStream(), "UTF-8");
							BufferedWriter writer = new BufferedWriter(output);
							String xml = "<cross-domain-policy> " + "<allow-access-from domain=\"*\" to-ports=\"*\"/>"
									+ "</cross-domain-policy> ";
							writer.write(xml + "\0");
							writer.flush();
							writer.close();
							return;
						} else {
							trace.newConnect(id);
							trace.addSocketLog(
									"!SOS<showMessage key=\"CONNECT\"><![CDATA[#555555Connection is OPEN]]></showMessage>", id, ++count);
						}
					}
					trace.addSocketLog(str, id, ++count);
					data = new StringBuilder();

				} else {
					data.append((char) c);
				}

				if (client.isConnected())
					c = reader.read();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		close();
		if (!firstLog) {
			trace.addSocketLog("!SOS<showMessage key=\"CONNECT\"><![CDATA[#555555Connection is CLOSE]]></showMessage>", id, ++count);
			trace.closeConnect(id);
		}
	}

	public boolean isClosed() {

		return client.isClosed();
	}

	public void close() {

		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
