package zephyropen.device.beamscan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import zephyropen.api.ZephyrOpen;
import zephyropen.util.Utils;

import gnu.io.*;

/** @author brad.zdanivsky@gmal.com */
public class Find {
	
	/* framework configuration */
	public static ZephyrOpen constants = ZephyrOpen.getReference();

	/* serial port configuration parameters */
	public static final int BAUD_RATE = 115200;
	public static final int TIMEOUT = 2000;
	public static final int DATABITS = SerialPort.DATABITS_8;
	public static final int STOPBITS = SerialPort.STOPBITS_1;
	public static final int PARITY = SerialPort.PARITY_NONE;
	public static final int FLOWCONTROL = SerialPort.FLOWCONTROL_NONE;
	private static final long RESPONCE_DELAY = 300;

	/* reference to the underlying serial port */
	private SerialPort serialPort = null;
	private InputStream inputStream = null;
	private OutputStream outputStream = null;

	/* list of all free ports */
	private Vector<String> ports = new Vector<String>();
	
	/* constructor makes a list of available ports */
	public Find() {
		getAvailableSerialPorts();
	}

	/** */
	private void getAvailableSerialPorts() {
		@SuppressWarnings("rawtypes")
		Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
		while (thePorts.hasMoreElements()) {
			CommPortIdentifier com = (CommPortIdentifier) thePorts.nextElement();
			if (com.getPortType() == CommPortIdentifier.PORT_SERIAL)
				ports.add(com.getName());
		}
	}

	/** connects on start up, return true is currently connected */
	private boolean connect(String address) {
		try {

			/* construct the serial port */
			serialPort = (SerialPort) CommPortIdentifier.getPortIdentifier(address).open("FindPort", TIMEOUT);

			/* configure the serial port */
			serialPort.setSerialPortParams(BAUD_RATE, DATABITS, STOPBITS, PARITY);
			serialPort.setFlowControlMode(FLOWCONTROL);

			/* extract the input and output streams from the serial port */
			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();
			
		} catch (Exception e) {
			constants.info("error connecting to: " + address);
			close();
			return false;
		}

		// be sure
		if (inputStream == null) return false;
		if (outputStream == null) return false;
		
		return true;
	}

	/** Close the serial port streams */
	private void close() {
		if (serialPort != null) {
			serialPort.close();
			serialPort = null;
		}
		try {
			if (inputStream != null) {
				inputStream.close();
			}
		} catch (Exception e) {
			System.err.println("input stream close():" + e.getMessage());
		}
		try {
			if (outputStream != null)
				outputStream.close();
		} catch (Exception e) {
			System.err.println("output stream close():" + e.getMessage());
		}
	}

	/**
	 * Loop through all available serial ports and ask for product id's
	 */
	public String search(String target) {
		
		constants.info("searching for: " + target);
		for (int i = ports.size() - 1; i >= 0; i--) {
			if (connect(ports.get(i))) {
				Utils.delay(TIMEOUT);
				String id = getProduct();
				System.out.println("discovered : " + id);
				if (id.equalsIgnoreCase(target)) {
					close();
					return ports.get(i);
				}

				// close on each loop
				close();
			}
		}
		close();
		return null;
	}

	/** send command to get product id */
	public String getProduct() {

		byte[] buffer = new byte[32];
		String device = "";

		// be sure there is no old bytes in our reply
		try {
			inputStream.skip(inputStream.available());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// send command to arduino
		try {
			outputStream.write(new byte[] { 'x', 13 });
		} catch (IOException e) {
			e.printStackTrace();
		}

		// wait for reply
		Utils.delay(RESPONCE_DELAY);

		// read it
		int read = 0;
		try {
			read = inputStream.read(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int j = 0; j < read; j++)
			device += (char) buffer[j];

		return device.trim();
	}
} 
