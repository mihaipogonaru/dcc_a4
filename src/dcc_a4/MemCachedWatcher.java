package dcc_a4;

import dcc_a4.MemCachedWrapper;
import dcc_a4.ClientLib;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.io.FileNotFoundException;

class DummyWatcher implements Watcher
{
    @Override
    public void process(WatchedEvent event)
    {
        // Do something! 
    	// No!
    }
}

public class MemCachedWatcher {
	public static String nodePrefix = "/node";
	
	private String myPath;
	private String myHostname;
	private Integer myPort;
	
	private String connString;
	private ZooKeeper zoo;
	
	public MemCachedWatcher(String configFile) throws FileNotFoundException {
		File f = new File(configFile);
		Scanner scanner = new Scanner(f);
			
		this.connString = scanner.next() + ":" + scanner.nextInt();
		this.myHostname = scanner.next();
		this.myPort = scanner.nextInt();
		scanner.close();
		
		myPath = null;
		zoo = null;
	}
	
	private void zooConnect() throws IOException  {
		if (zoo != null) {
			return;
		}
		
		zoo = new ZooKeeper(connString, 100, new DummyWatcher());
	}
	
	private void zooDisconnect() {
		if (zoo == null) {
			return;
		}
		
		try {
			zoo.close();
		} catch (Exception e) {
			/* Ignore exceptions */
		}
		
		zoo = null;
	}
	
	private void createWatchersNode() {
		try {
			if (zoo.exists(ClientLib.watchersNode, false) == null) {
				zoo.create(ClientLib.watchersNode, null, Ids.OPEN_ACL_UNSAFE,
							CreateMode.PERSISTENT);
			}
		} catch (Exception e) {
			System.out.println("Failed creating watchers node");
			System.out.println(e);
		}
	}
	
	public boolean existsMyNode () {
		if (myPath == null)
			return false;
		
		try {
			Stat stat = zoo.exists(myPath, false);
			
			if (stat == null)
				return false;
			return true;
		} catch (Exception e) {
			System.out.println("Exception when checking myPath existence. Assuming it doesn't exist");
		}
		
		return false;
	}
	
	public boolean createMyNode() {
		if (existsMyNode()) {
			//System.out.println("Node already exists");
			return true;
		}
		
		try {
			/* If no connection, create one */
			if (zoo == null) {
				zooConnect();
			}
			
			String nodePath = ClientLib.watchersNode + nodePrefix;
			String nodeData = myHostname + ":" + myPort;
			
			createWatchersNode();
			myPath = zoo.create(nodePath, nodeData.getBytes(), Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL_SEQUENTIAL);
		} catch (Exception e) {
			System.out.println("Error creating path");
			System.out.println(e);
			/* Also close the connection */
			zooDisconnect();
			
			return false;
		}
		
		return true;
	}
	
	public boolean deleteMyNode() {
		if (!existsMyNode()) {
			//System.out.println("Node doesn't exist");
			return true;
		}
		
		try {
			zoo.delete(myPath, -1);
			
			/* Also close the connection */
			zooDisconnect();
		} catch (Exception e) {
			System.out.println("Error deleting path");
			System.out.println(e);
			/* Also close the connection */
			zooDisconnect();
			
			return false;
		}
		
		return true;
	}
	
	public boolean isMemCachedAlive() {
		boolean rc;
		String value;
		MemCachedWrapper cw = new MemCachedWrapper(true);

		cw.addServer("localhost", this.myPort);
		
		rc = cw.setValue("__alive__check__private__", "true");
		if (!rc) {
			return false;
		}
		
		value = cw.getValue("__alive__check__private__");
		if (value == null || !value.equals("true")) {
			return false;
		}
		
		cw.resetAllConnections();
		
		return true;
	}
	
	public void startWatching() {
		while (true) {
			if (isMemCachedAlive()) {
				System.out.println("MemCached is alive");
				createMyNode();
			} else {
				System.out.println("MemCached is dead");
				deleteMyNode();
			}
				
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (Exception e) {
				System.out.println("Sleep interrupted");
				System.out.println(e);
			}
		}
	}
	
	public static void main(String[] args) {
		MemCachedWatcher watcher;

		if (args.length != 1) {
			System.out.println("Please provide config file");
			return;
		}
		
		try {
			watcher = new MemCachedWatcher(args[0]);
		} catch (Exception e) {
			System.out.println("Error creating watcher process");
			System.out.println(e);
			return;
		}
		
		watcher.startWatching();
	}
}
