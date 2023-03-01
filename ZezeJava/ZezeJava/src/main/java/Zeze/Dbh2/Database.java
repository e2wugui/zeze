package Zeze.Dbh2;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理桶（即部分表内容）
 */
public class Database {
	private final String name;
	private final int serverId;

	private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

	public Database(int serverId, String name) {
		this.serverId = serverId;
		this.name = name;
	}

	public Bucket getOrAdd(String tableName) {
		return buckets.computeIfAbsent(tableName, (key) -> new Bucket(this, tableName));
	}

	public String getName() {
		return name;
	}

	public int getServerId() {
		return serverId;
	}
}
