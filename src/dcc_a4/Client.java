package dcc_a4;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import dcc_a4.ClientLib;

public class Client {
	public static void main(String[] args) {
		boolean debug = false;
		String inputFile;
		Integer port = 2181;
		ClientLib lib;

		if (args.length == 0) {
			System.out.println("Params: input_file [zoo_port]");
			return;
		}
		inputFile = args[0];
		
		if (args.length > 2) {
			debug = true;
			System.out.println("Debug enabled");
		}
		
		if (args.length > 1) {
			port = Integer.parseInt(args[1]);
		}
		
		if (debug) {
			System.out.println("Using port " + port + " for local zookeper");
		}
		
		try {
			lib = new ClientLib(port, debug);
		} catch (Exception e) {
			System.out.println("Cannot create client library");
			System.out.println(e);
			return;
		}
		
		File f;
		Scanner scanner;
		
		try {
			f = new File(inputFile);
			scanner = new Scanner(f);
		} catch (Exception e) {
			System.out.println("Cannot open input file");
			System.out.println(e);
			return;
		}

		while (scanner.hasNext()) {
			String command = scanner.next().toLowerCase();
			
			if (command.equals("get")) {
				String key = scanner.next();
				String value;
				
				value = lib.getValue(key);
				if (value != null) {
					if (debug) {
						System.out.println("Client read key: " + key + " value: " + value);
					} else {
						System.out.println(value);
					}
				}
			} else if (command.equals("set")) {
				String key = scanner.next();
				String value = scanner.next();
				boolean rc;
				
				rc = lib.setValue(key, value);
				if (debug) {
					if (rc != true) {
						System.out.println("Client couldn't set key: " + key + " value: " + value);
					} else {
						System.out.println("Client set key: " + key + " value: " + value);
					}
				}
			} else if (command.equals("sleep")) {
				Integer value = Integer.parseInt(scanner.next());

				try {
					TimeUnit.MILLISECONDS.sleep(value);
				} catch (InterruptedException e) {
					if (debug) {
						System.out.println("Sleep interrupted");
						System.out.println(e);
					}
				}
			}
		}
		
		scanner.close();
		lib.quit();
	}
}
