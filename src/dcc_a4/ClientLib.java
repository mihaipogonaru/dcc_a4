package dcc_a4;

import dcc_a4.MemCachedWrapper;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;

import java.io.IOException;

public class ClientLib implements Watcher {
	public static String watchersNode = "/dcc_a4";
	
	private boolean debug;
	private MemCachedWrapper cw;
	private ZooKeeper zoo;
	private ConcurrentHashMap<String, Server> servers;
	
	public ClientLib(Integer port, boolean debug) throws IOException, KeeperException, InterruptedException {
		this.debug = debug;
		
		zoo = new ZooKeeper("localhost:" + port, 1000, null);
		
		cw = new MemCachedWrapper(debug);
		cw.setKetamaDistribution();

		/* Create node */
		createWatchersNode();
		
		servers = new ConcurrentHashMap<String, Server>();
		/* Find existing server and register watch */
		findExitingServers();
	}
	
	private void createWatchersNode() {
		try {
			if (zoo.exists(ClientLib.watchersNode, null) == null) {
				zoo.create(ClientLib.watchersNode, null, Ids.OPEN_ACL_UNSAFE,
							CreateMode.PERSISTENT);
			}
		} catch (Exception e) {
			System.out.println("Failed creating watchers node");
			System.out.println(e);
		}
	}
	
	private void findExitingServers() {
		try {
			List<String> children = zoo.getChildren(watchersNode, this);
			
			for (String child : children) {
				String childNode = watchersNode + "/" + child;
				byte data[] = zoo.getData(childNode, false, null);
				addServer(childNode, new String(data));
			}
		} catch (Exception e) {
			/* Ignore exceptions, for now */
			if (debug) {
				System.out.println("Failed finding existing servers");
				System.out.println(e);
			}
		}
	}
	
	private void replaceExistingServers() {
		try {
			List<String> children = zoo.getChildren(watchersNode, this);

			/* Remove nodes that don't exist anymore */
			for (String child : servers.keySet()) {
				if (!children.contains(child.split(watchersNode + "/")[1])) {
					String childNode = child;
					removeServer(childNode);
				}
			}
			
			/* Add new nodes */
			for (String child : children) {
				if (!servers.containsKey(child)) {
					String childNode = watchersNode + "/" + child;
					byte data[] = zoo.getData(childNode, false, null);
					addServer(childNode, new String(data));
				}
			}
		} catch (Exception e) {
			/* Ignore exceptions, for now */
			if (debug) {
				System.out.println("Error replacing existing servers");
				System.out.println(e);
			}
		}
	}
	
	private boolean addServer(String path, String data) {
		if (path == null || data == null || servers.containsKey(path))
			return false;
		
		String hostname = data.split(":")[0];
		Integer port = Integer.parseInt(data.split(":")[1]);
		
		if (!cw.addServer(hostname, port))
			return false;
		
		if (debug) {
			System.out.println("Added server: " + data);
		}
		servers.put(path, new Server(hostname, port));
		
		return true;
	}
	
	private boolean removeServer(String path) {
		if (path == null || !servers.containsKey(path))
			return false;

		Server srv = servers.get(path);
		
		if (!cw.removeServer(srv.getHostame(), srv.getPort()))
			return false;
		
		if (debug) {
			System.out.println("Removed server: " +	srv.getHostame() + ":" + srv.getPort());
		}
		servers.remove(path);
		
		return true;
	}
	
	@Override
	public void process(WatchedEvent event)
	{
		if (debug) {
			System.out.println("Got event " + event.getType() + " path: " + event.getPath());
		}
		
		if (event.getType() == Event.EventType.NodeChildrenChanged) {
			replaceExistingServers();
		}
	}
	
	public boolean setValue(String key, String value) {   	
    	return cw.setValue(key, value);
    }
    
    public String getValue(String key) {    	
    	return cw.getValue(key);
    }
    
    public void quit() {
    	cw.resetAllConnections();
    	try {
    		zoo.close();
    	} catch (Exception e) {
    		/* Ignore exception, for now */
    	}
    }
}
