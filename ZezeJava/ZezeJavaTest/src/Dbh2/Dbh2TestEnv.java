package Dbh2;

import java.io.File;
import java.util.ArrayList;
import Zeze.Application;
import Zeze.Config;
import Zeze.Dbh2.Database;
import Zeze.Dbh2.Dbh2AgentManager;
import Zeze.Dbh2.Dbh2Manager;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Database.AbstractKVTable;
import Zeze.Util.Task;

/**
 * Dbh2 系测试的进程内组网脚手架：Master + 3个Dbh2Manager(raft多bucket) + Dbh2AgentManager + 4张KV测试表的启停。
 * 消费方：Dbh2FullTest、Benchmark.BenchDbh2FullTransaction（提取自两者的重复副本）。
 * 注意：拓扑固定绑定 Master 11000 端口、bucket 起始端口 18000（Dbh2MasterDefaultBucketPortId），
 * 	两个消费方不能在同一 JVM 内同时运行（integrationTest 与 bench 分属不同车道/进程，不会冲突）。
 */
public final class Dbh2TestEnv {
	private static final String BUCKET_PORT_ID_PROPERTY = "Dbh2MasterDefaultBucketPortId";

	public Zeze.Dbh2.Master.Main master;
	public final ArrayList<Dbh2Manager> managers = new ArrayList<>();
	public Dbh2AgentManager dbh2AgentManager;
	public Database database;
	public final ArrayList<AbstractKVTable> tables = new ArrayList<>();
	public final ByteBuffer value = ByteBuffer.Wrap(new byte[]{1, 2, 3, 4});

	private AbstractAgent serviceManager;
	private String priorBucketPortId;
	private boolean bucketPortIdTouched;

	private static Database newDatabase(Dbh2AgentManager dbh2AgentManager, @SuppressWarnings("SameParameterValue") String dbName) {
		var databaseConf = new Config.DatabaseConf();
		databaseConf.setDatabaseType(Config.DbType.Dbh2);
		databaseConf.setDatabaseUrl("dbh2://127.0.0.1:11000/" + dbName);
		databaseConf.setName("dbh2");
		return new Database(null, dbh2AgentManager, databaseConf);
	}

	public void prepareNewEnvironment() throws Exception {
		priorBucketPortId = System.getProperty(BUCKET_PORT_ID_PROPERTY);
		System.setProperty(BUCKET_PORT_ID_PROPERTY, "18000");
		bucketPortIdTouched = true;
		Task.tryInitThreadPool();
		Zeze.Net.Selectors.getInstance().add(7); // 全局selector扩容，无法安全还原（原因见stopAll注释），保持拆分前行为

		master = new Zeze.Dbh2.Master.Main("zeze.xml");
		serviceManager = Application.createServiceManager(Config.load(), "Dbh2ServiceManager");
		assert serviceManager != null;
		serviceManager.start();
		serviceManager.waitReady();

		Application.renameAndDeleteDirectory(new File("CommitRocks"));
		dbh2AgentManager = new Dbh2AgentManager(serviceManager, null, 100);
		try {
			master.start();
			for (int i = 0; i < 3; ++i)
				managers.add(new Zeze.Dbh2.Dbh2Manager("manager" + i, "zeze" + i + ".xml"));
			for (var manager : managers)
				manager.start();
			dbh2AgentManager.start();

			database = newDatabase(dbh2AgentManager, "dbh2TestDb");
			for (int i = 0; i < 4; ++i) {
				var tableName = "table" + i;
				tables.add((Database.AbstractKVTable)database.openTable(tableName, Bean.hash32(tableName)));
			}
			for (var table : tables)
				table.waitReady();
		} catch (Throwable e) {
			stopAll(); // 半启动（中途失败）时回收已启动的部分，避免泄漏端口/线程
			throw e;
		}
	}

	public void stopAll() throws Exception {
		try {
			if (master != null)
				master.stop();
			for (var manager : managers)
				manager.stop();
			if (database != null)
				database.close();
			if (dbh2AgentManager != null)
				dbh2AgentManager.stop();
			// serviceManager 保持拆分前行为：不停止（拆分前两个用例的 finally 均不停它）。
		} finally {
			// 还原 JVM 全局状态：bucket 起始端口属性恢复为启动前的值（启动前没有则清除）。
			if (bucketPortIdTouched) {
				bucketPortIdTouched = false;
				if (priorBucketPortId != null)
					System.setProperty(BUCKET_PORT_ID_PROPERTY, priorBucketPortId);
				else
					System.clearProperty(BUCKET_PORT_ID_PROPERTY);
				priorBucketPortId = null;
			}
			// Selectors.getInstance().add(7) 不还原：Selectors 只有整体 close()（关掉共享单例的全部 selector 线程），
			// 没有按个数移除的 API；getInstance() 是 JVM 级共享单例，close 会波及同 JVM 其他测试的网络，
			// 拆分前两个用例同样不关闭，保持原行为。
			managers.clear();
			tables.clear();
			master = null;
			dbh2AgentManager = null;
			database = null;
			serviceManager = null;
		}
	}
}
