package dcc_a4;

import java.util.Collection;
import java.util.HashMap;

class Server {
	private String hostname;
	private Integer port;
	
	public Server(String hostname, Integer port) {
		this.hostname = hostname;
		this.port = port;
	}
	
	public String getHostame() {
		return hostname;
	}
	
	public Integer getPort() {
		return port;
	}
}

public class ServerList {
	private HashMap<String, Server> servers;
	
	public ServerList() {
		servers = new HashMap<String, Server>();
	}
	
	public static String makeKey(String hostname, Integer port) {
		return hostname + "_" + port;
	}
	
	public boolean addServer(String hostname, Integer port) {
		String key = makeKey(hostname, port);
		
		if (servers.containsKey(key)) {
			return false;
		}
		servers.put(key, new Server(hostname, port));
		
		return true;
	}
	
	public boolean removeServer(String hostname, Integer port) {
		String key = makeKey(hostname, port);
		
		if (!servers.containsKey(key)) {
			return false;
		}
		servers.remove(key);
		
		return true;
	}
	
	public Collection<Server> getServers() {
		return servers.values();
	}
}
