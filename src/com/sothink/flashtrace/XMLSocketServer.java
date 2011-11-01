package com.sothink.flashtrace;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class XMLSocketServer implements Runnable {

	private ServerSocket server;

	private boolean running;

	private ArrayList<XMLSocketConnect> clients;
	
	private int counter;

	public XMLSocketServer() {

		running = true;
		clients = new ArrayList<XMLSocketConnect>();
		counter = 1;

		try {
			server = new ServerSocket(4444);
			server.setSoTimeout(500);
			server.setPerformancePreferences(0, 2, 1);
			new Thread(this).start();

		} catch (SocketException e) {
			TraceServer.getInstance().addTraceLog("#FF0000The port is occupied, please try again after exiting the program!");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public void stop() {

		running = false;
	}

	@Override
	public void run() {

		while (running) {
			for (int i = 0; i < clients.size(); i++) {
				XMLSocketConnect connect = clients.get(i);
				if (connect.isClosed()) {
					clients.remove(connect);
					i--;
				}
			}
			TraceServer.getInstance().setText(clients.size());

			try {
				Socket client = server.accept();
				
				XMLSocketConnect connect = new XMLSocketConnect(client, counter++);
				clients.add(connect);
				new Thread(connect).start();

			} catch (SocketTimeoutException e) {

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			for (XMLSocketConnect connect : clients) {
				connect.close();
			}
			clients = null;
			server.close();
			server = null;

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
