package Zeze.Dbh2;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理桶（即部分表内容）
 */
public class Database {
	private final String name;
	private final int serverId; // todo 移到父类去

	private final ConcurrentHashMap<String, Table> buckets = new ConcurrentHashMap<>();

	public Database(int serverId, String name) {
		this.serverId = serverId;
		this.name = name;
	}

	public Table getOrAdd(String tableName) {
		return buckets.computeIfAbsent(tableName, (key) -> new Table(this, tableName));
	}

	public String getName() {
		return name;
	}

	public int getServerId() {
		return serverId;
	}
}
