package Zeze.Dbh2;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库网络管理服务。
 */
public class DatabaseService {
	private final ConcurrentHashMap<Integer, Database> databases = new ConcurrentHashMap<>();

}
