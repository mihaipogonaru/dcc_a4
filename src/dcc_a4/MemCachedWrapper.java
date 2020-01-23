package dcc_a4;

import dcc_a4.ServerList;

import java.util.Scanner;

import libmemcached.wrapper.MemcachedBehavior;
import libmemcached.wrapper.MemcachedClient;
import libmemcached.wrapper.type.DistributionType;
import libmemcached.wrapper.type.ReturnType;

import libmemcached.exception.LibMemcachedException;

public class MemCachedWrapper {
	private boolean debug;
    private MemcachedClient client;
	private ServerList servers;
   
	public MemCachedWrapper(boolean debug) {
		client = new MemcachedClient();
		servers = new ServerList();
		
		this.debug = debug;
	}
	
	public void resetAllConnections() {
		client.free();
		client = new MemcachedClient(); 
	}
	
	public boolean addAllConnections() {
		ReturnType rc;
		
		for (Server srv : servers.getServers()) {
			String hostname = srv.getHostame();
			Integer port = srv.getPort();
			
			rc = client.addServer(hostname, port);
			if (!rc.equals(ReturnType.SUCCESS)) {
				if (debug) {
					System.out.println("Error adding connections");
					System.out.println(rc);
				}
				return false;
			}
		}
		
		return true;
	}
	
	/* The methods are sync since the calls etc come from async events */
	
    public synchronized boolean addServer(String hostname, Integer port) {
    	if (!servers.addServer(hostname, port)) {
    		if (debug) {
    			System.out.println("Server already exists");
    		}
    		return true;
    	}
    	resetAllConnections();
    	
    	return addAllConnections();
    }
    
    public synchronized boolean removeServer(String hostname, Integer port) {
    	if (!servers.removeServer(hostname, port)) {
    		if (debug) {
    			System.out.println("Server doesn't exist");
    		}
    		return true;
    	}
    	resetAllConnections();
    	
    	return addAllConnections();
    }
    
    public synchronized boolean setKetamaDistribution() {
    	ReturnType rc;
    	
    	MemcachedBehavior behavior = client.getBehavior();
    	rc = behavior.setDistribution(DistributionType.CONSISTENT_KETAMA);
    	
    	if (!rc.equals(ReturnType.SUCCESS)) {
    		if (debug) {
	    		System.out.println("Error setting ketama distribution");
				System.out.println(rc);
    		}
    	}
    	
    	return true;
    }
    
    public synchronized boolean setValue(String key, String value) {
    	ReturnType rc;
    	try {
    		rc = client.getStorage().set(key, value, 0, 0);
    	
	    	if (!rc.equals(ReturnType.SUCCESS)) {
	    		if (debug) {
		    		System.out.println("Error setting key: " + key + " value: " + value);
					System.out.println(rc);
	    		}
				return false;
	    	}
    	} catch (Exception e) {
    		if (debug) {
	    		System.out.println("Error setting key: " + key + " value: " + value);
				System.out.println(e);
    		}
			return false;
    	}
    	
    	return true;
    }
    
    public synchronized String getValue(String key) {
    	String res;
    	
    	try {
    		res = client.getStorage().get(key);
    		if (res == null && debug) {
    			System.out.println("Error getting key: " + key + ". Key not found");
    		}
    	} catch (LibMemcachedException e) {
    		if (debug) {
	    		System.out.println("Error getting key: " + key);
				System.out.println(e);
    		}
    		res = null;
    	}
    	
    	return res;
    }

	public static void main(String[] args) {
		boolean rc;
		MemCachedWrapper watcher = new MemCachedWrapper(true);
		Scanner scanner = new Scanner(System.in);
        
        while (true)
        {
            String command = scanner.next();
            System.out.println("Command " + command);
            
            if (command.equals("exit"))
            	break;
            
            if (command.equals("set")) {
            	String key = scanner.next();
            	String value = scanner.next();
            	
            	rc = watcher.setValue(key, value);
            	if (rc)
            		System.out.println("Set");
            }
            
            if (command.equals("get")) {
            	String key = scanner.next();
            	String value = watcher.getValue(key);
            	
            	if (value != null)
            		System.out.println("Got " + value);
            }
            
            if (command.equals("add")) {
            	String server = scanner.next();
            	Integer port = scanner.nextInt();
            	
            	rc = watcher.addServer(server, port);
            	if (rc)
            		System.out.println("Added server");
            }
            
            if (command.equals("remove")) {
            	String server = scanner.next();
            	Integer port = scanner.nextInt();
            	
            	rc = watcher.removeServer(server, port);
            	if (rc)
            		System.out.println("Removed server");
            }
        }
        
        scanner.close();
	}

}
