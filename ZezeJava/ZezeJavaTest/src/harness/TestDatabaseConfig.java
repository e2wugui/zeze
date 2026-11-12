package harness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import Zeze.Config;
import org.jspecify.annotations.Nullable;

/**
 * 测试应用在创建 Application 前显式选择后端；生产 Config 不读取测试属性。
 * 仅切换空 url 的 Memory 配置，独立内存桶和其他后端保持原配置。
 * Memory 语义测试直接使用 Config，不调用本助手；不要依赖 setter 调用历史区分配置。
 * RocksDb 目标为每个数据库生成独立目录（最贴近 Memory 的不共享状态语义），不接受 url；
 * 多 App 测试用 keepOriginal 豁免——RocksDb 目录锁不支持同进程多 App。
 * Dbh2 目标连接外部已启动的服务器，连接串唯一来源是 ZezeJavaTest/zeze.xml（详见dbh2UrlFromZezeXml）；
 * 本地提交CommitRocks{serverId}目录锁同 RocksDb 多 App 约束，沿用 keepOriginal 豁免。
 */
public final class TestDatabaseConfig {

	/** 远程提交模式的CommitServer直连地址：与TestEnvLauncherListener.DBH2_COMMIT_SERVER_PORT及
	 * ZezeJavaTest/zeze.xml的Zeze.Dbh2.Commit Acceptor一致（自测漂移守卫校验三处同源）。 */
	static final String COMMIT_SERVER_ADDRESS =
			"127.0.0.1:" + TestEnvLauncherListener.DBH2_COMMIT_SERVER_PORT;

	private TestDatabaseConfig() {
	}

	/** 豁免重定向的Config登记：弱引用跟随Config生命周期，不阻碍回收。
	 * 值=豁免后端集合：空集=无条件豁免（兼容形态）；非空=仅当所选后端∈集合时豁免
	 * （如Infinite.Simulate仅RocksDb豁免——MySQL/PG/MongoDb/Dbh2共享库正是多App集群
	 * 的生产拓扑，放行重定向）。 */
	private static final WeakHashMap<Config, Set<Config.DbType>> keepOriginals = new WeakHashMap<>();

	/** 多App测试豁免后端重定向（无条件形态）：RocksDb 等后端不支持同进程多 App（目录锁），登记后保持原配置。 */
	public static Config keepOriginal(Config config) {
		synchronized (keepOriginals) {
			keepOriginals.put(config, Collections.emptySet());
		}
		return config;
	}

	/** 条件豁免：仅当所选后端∈onlyWhen（或未选择后端）时豁免。返回**当前选择下是否实际豁免**，
	 * 调用方据此做配套决策——如Infinite.App仅在豁免（保持Memory）时设置独占Memory桶，
	 * 放行重定向时保持空url让apply统一切换到目标后端共享库。 */
	public static boolean keepOriginal(Config config, Config.DbType... onlyWhen) {
		var backends = Set.of(onlyWhen);
		synchronized (keepOriginals) {
			keepOriginals.put(config, backends);
		}
		return isExempt(config, System.getProperty("zeze.test.database"));
	}

	private static boolean isExempt(Config config, @Nullable String backend) {
		Set<Config.DbType> onlyWhen;
		synchronized (keepOriginals) {
			onlyWhen = keepOriginals.get(config);
		}
		if (onlyWhen == null)
			return false;
		if (onlyWhen.isEmpty() || backend == null)
			return true; // 无条件登记，或未选择后端（Memory形态）
		for (var t : onlyWhen)
			if (t.name().equals(backend))
				return true;
		return false;
	}

	public static Config apply(Config config) {
		return apply(config, System.getProperty("zeze.test.database"), System.getProperty("zeze.test.database.url"));
	}

	static Config apply(Config config, @Nullable String backend, @Nullable String url) {
		if (backend == null || isExempt(config, backend))
			return config;
		var databases = config.getDatabaseConfMap().values().stream()
				.filter(db -> db.getDatabaseType() == Config.DbType.Memory && db.getDatabaseUrl().isEmpty())
				.toList();
		if (databases.isEmpty())
			return config;
		if (Config.DbType.RocksDb.name().equals(backend)) {
			// RocksDb非JDBC：每个数据库独立目录（Memory式不共享状态，目录锁不互斥），
			// 不动druidConf（DatabaseRocksDb不消费连接池，显式RocksDb测试同款惯例）。
			if (url != null && !url.isBlank())
				throw new IllegalStateException("zeze.test.database=RocksDb 不接受 zeze.test.database.url"
						+ "：每个数据库自动独立目录，保持 Memory 式不共享状态");
			for (var db : databases) {
				db.setDatabaseType(Config.DbType.RocksDb);
				db.setDatabaseUrl(newRocksDbDir());
			}
			// 连Global配置一起摘：RocksDb与GlobalCacheManager互斥（Config.createDatabase硬闸），
			// hasGlobal/hasGlobalRaft均由本字段派生，置空即单机本地锁模式。
			// 依赖分布式锁语义的测试（Takeover/跨App对撞/多App）必须keepOriginal豁免。
			config.setGlobalCacheManagerHostNameOrAddress(null);
			return config;
		}
		if (Config.DbType.MongoDb.name().equals(backend)) {
			// MongoDb非JDBC：连接串必填（库默认zeze_mongodb，共享库持久语义对齐JDBC批次）；
			// 不动druidConf；不摘Global（createDatabase的MongoDb分支无硬闸，可与GCM共存）。
			if (url == null || url.isBlank())
				throw new IllegalStateException("zeze.test.database=MongoDb requires zeze.test.database.url");
			if (!url.startsWith("mongodb://") && !url.startsWith("mongodb+srv://"))
				throw new IllegalStateException("zeze.test.database=MongoDb requires mongodb:// URL prefix, got: " + url);
			for (var db : databases) {
				db.setDatabaseType(Config.DbType.MongoDb);
				db.setDatabaseUrl(url);
			}
			return config;
		}
		if (Config.DbType.Dbh2.name().equals(backend)) {
			// Dbh2非JDBC：连接串唯一来源是ZezeJavaTest/zeze.xml的Dbh2 DatabaseConf（外部dbh2服务器
			// 须已启动；master直连url的ip:port，不经SM发现），不接受-P url。
			// 不动druidConf；不摘Global（createDatabase的Dbh2分支无硬闸，可与GCM共存）；
			// databaseName不触碰（共享库持久语义对齐JDBC/MongoDb批次）。
			// 应用需要ServiceManager（Dbh2AgentManager的tidAutoKey）；多App测试沿用keepOriginal豁免。
			// 远程提交模式：2pc协调端点为环境内CommitServer（TestEnvLauncherListener按需启动或
			// 复用外部，见COMMIT_SERVER_ADDRESS），app自身无CommitService与CommitRocks——被中断
			// 的2pc由CommitServer自治恢复，桶侧悬挂事务探测有稳定目标（本地提交模式下app从不启动
			// manager，探测端点无人监听→事务锁死桶键→后来者撞键红，soak两批次实锤）。
			if (url != null && !url.isBlank())
				throw new IllegalStateException("zeze.test.database=Dbh2 不接受 zeze.test.database.url"
						+ "：连接串以 ZezeJavaTest/zeze.xml 的 Dbh2 DatabaseConf 为准");
			if ("disable".equals(config.getServiceManager()))
				throw new IllegalStateException("zeze.test.database=Dbh2 需要 ServiceManager（tidAutoKey分配），"
						+ "当前配置为 disable；多App测试请用 keepOriginal 豁免");
			var dbh2Url = dbh2UrlFromZezeXml(Path.of("zeze.xml"));
			for (var db : databases) {
				db.setDatabaseType(Config.DbType.Dbh2);
				db.setDatabaseUrl(dbh2Url);
			}
			config.setDbh2LocalCommit(false);
			config.getCustomizes().putIfAbsent("Dbh2Config",
					dbh2CommitServerConf(COMMIT_SERVER_ADDRESS));
			return config;
		}
		for (var db : databases) {
			var jdbc = jdbcConf(backend, url);
			db.setDatabaseType(jdbc.getDatabaseType());
			db.setDatabaseUrl(jdbc.getDatabaseUrl());
			db.setDruidConf(jdbc.getDruidConf());
		}
		return config;
	}

	/** 独立RocksDb目录：build/下集中存放（gitignored、gradle clean可扫除；rocksdb数据留取证，对齐cleanTestResidue惯例）。 */
	private static String newRocksDbDir() {
		try {
			var root = Path.of("build", "test-rocksdb");
			Files.createDirectories(root);
			return Files.createTempDirectory(root, "db_").toString();
		} catch (IOException e) {
			throw new IllegalStateException("create rocksdb test directory failed", e);
		}
	}

	/**
	 * 从测试配置zeze.xml提取Dbh2连接串（外部dbh2服务器的唯一事实来源）。
	 * 条目位于注释块内亦有效（按原文正则匹配，不经XML解析器）：zeze.xml的注释块是"可用后端示例菜单"，
	 * Dbh2行即当前部署的连接事实。
	 */
	static String dbh2UrlFromZezeXml(Path zezeXml) {
		String text;
		try {
			text = Files.readString(zezeXml);
		} catch (IOException e) {
			throw new IllegalStateException("读取 " + zezeXml + " 失败：Dbh2连接串取自该文件", e);
		}
		var elementPattern = java.util.regex.Pattern.compile("<DatabaseConf\\b[^>]*>");
		var urlPattern = java.util.regex.Pattern.compile("DatabaseUrl=\"([^\"]*)\"");
		for (var elementMatcher = elementPattern.matcher(text); elementMatcher.find(); ) {
			var element = elementMatcher.group();
			if (!element.contains("DatabaseType=\"Dbh2\""))
				continue;
			var urlMatcher = urlPattern.matcher(element);
			if (!urlMatcher.find())
				throw new IllegalStateException(zezeXml + " 的 Dbh2 DatabaseConf 缺少 DatabaseUrl");
			var value = urlMatcher.group(1);
			if (!value.startsWith("dbh2://"))
				throw new IllegalStateException(zezeXml + " 的 Dbh2 DatabaseUrl 需要 dbh2:// 前缀，当前: " + value);
			return value;
		}
		throw new IllegalStateException(zezeXml + " 中未找到 DatabaseType=\"Dbh2\" 的 DatabaseConf（含DatabaseUrl）");
	}

	/** 合成Dbh2Config的CustomizeConf元素（CommitServerAddress），经getCustomizes注入，
	 * Dbh2AgentManager构造期parseCustomize消费；配置文件中已有同名条目时不覆盖（putIfAbsent）。 */
	private static org.w3c.dom.Element dbh2CommitServerConf(String address) {
		try {
			var xml = "<CustomizeConf Name=\"Dbh2Config\" CommitServerAddress=\"" + address + "\"/>";
			var builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
			var doc = builder.parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
			return doc.getDocumentElement();
		} catch (Exception e) {
			throw new IllegalStateException("create Dbh2Config CustomizeConf element failed", e);
		}
	}

	/** 直接测试数据库适配器时也使用同一选择；null表示未选择本后端，调用方须明确中止测试。 */
	public static Config.@Nullable DatabaseConf jdbcTestConf(Config.DbType expected) {
		return jdbcTestConf(expected, System.getProperty("zeze.test.database"),
				System.getProperty("zeze.test.database.url"));
	}

	static Config.@Nullable DatabaseConf jdbcTestConf(Config.DbType expected, @Nullable String backend,
			@Nullable String url) {
		if (backend == null) {
			if (url != null && !url.isBlank())
				throw new IllegalStateException("zeze.test.database.url requires zeze.test.database");
			return null;
		}
		var conf = jdbcConf(backend, url); // 先验证显式配置，不能把错误配置当成未选择后端。
		return conf.getDatabaseType() == expected ? conf : null;
	}

	static Config.DatabaseConf jdbcConf(String backend, @Nullable String url) {
		var selected = JdbcTestBackend.parse(backend);
		if (url == null || url.isBlank())
			throw new IllegalStateException("zeze.test.database=" + backend + " requires zeze.test.database.url");
		if (!url.startsWith(selected.urlPrefix))
			throw new IllegalStateException("zeze.test.database=" + backend + " requires URL prefix " + selected.urlPrefix);
		var druid = new Config.DruidConf();
		druid.driverClassName = selected.driverClassName;
		// 多应用测试不常驻闲置连接，保留并发余量；回收时间须大于 Druid 默认清理周期60s。
		druid.initialSize = 0;
		druid.minIdle = 0;
		druid.maxActive = 4;
		druid.minEvictableIdleTimeMillis = 61_000L;
		druid.maxWait = 10_000L;
		var conf = new Config.DatabaseConf();
		conf.setDatabaseType(selected.type);
		conf.setDatabaseUrl(url);
		conf.setDruidConf(druid);
		return conf;
	}
}
