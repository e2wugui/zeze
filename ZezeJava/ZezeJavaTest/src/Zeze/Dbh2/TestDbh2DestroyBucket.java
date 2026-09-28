package Zeze.Dbh2;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * DestroyBucket（建桶半失败回滚协议）的manager侧语义：对真实在册raft销毁——
 * 摘账（dbh2s/proxyServer派发）、关raft、删桶目录；桶目录里的raft.xml残留会被
 * Dbh2Manager.start()的目录扫描复活成幽灵raft，删除必须确定。幂等：已销毁（或
 * 从未建）重发同样成功。直驱destroyBucket（包内）——rpc链路由MasterDatabase
 * 回滚路径与全栈用例覆盖。
 */
public class TestDbh2DestroyBucket {

	@Test
	public void testDestroyBucketRemovesDirAndIdempotent() throws Exception {
		var env = new Dbh2TestEnv();
		env.prepareNewEnvironment();
		try {
			var manager = env.managers.get(0);
			// manager落盘的raft.xml即master发给它的同款替换后配置（destroyBucket按名字解析portId）。
			var raftXmls = new ArrayList<File>();
			collectRaftXmls(env.managerHome(0).toFile(), raftXmls);
			Assertions.assertFalse(raftXmls.isEmpty(), "env prepare后manager必须已持有真实bucket raft");
			var raftXml = raftXmls.get(0);
			var raftStr = Files.readString(raftXml.toPath());
			// 布局 home/<db>/<table>/<portId>/<nodeDir>/raft.xml：向上取db/table/portId三段。
			var nodeDir = raftXml.getParentFile();
			var portDir = nodeDir.getParentFile();
			var tableDir = portDir.getParentFile();
			var dbDir = tableDir.getParentFile();

			manager.destroyBucket(dbDir.getName(), tableDir.getName(), raftStr);

			Assertions.assertFalse(portDir.exists(), "桶目录必须删除——raft.xml残留会被start()扫描复活成幽灵raft");

			// 幂等：对已销毁（磁盘无目录、内存无raft）的目标重发同样成功。
			manager.destroyBucket(dbDir.getName(), tableDir.getName(), raftStr);
			Assertions.assertFalse(portDir.exists());
		} finally {
			env.stopAll();
		}
	}

	private static void collectRaftXmls(File dir, ArrayList<File> out) {
		var list = dir.listFiles();
		if (null == list)
			return;
		for (var f : list) {
			if (f.isDirectory())
				collectRaftXmls(f, out);
			else if (f.isFile() && f.getName().equals("raft.xml"))
				out.add(f);
		}
	}
}
