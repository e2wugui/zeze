package Zeze.History;

import Zeze.Application;
import Zeze.Serialize.ByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * tHistory 物理表归属校验：在 tHistory 所属库的 DirectOperates 中记录并校验发号名（{@link #OWNER_KEY}），
 * 防止不同 history 发号名共享同一物理表导致 gid 数值重叠而静默互覆；并发首启通过版本推进封存归属。
 */
public final class OwnerCheck {
	private static final Logger logger = LogManager.getLogger(OwnerCheck.class);

	/** DirectOperates 区内的归属标记键（per tHistory 所属数据库）。 */
	static final String OWNER_KEY = "zeze.History.tHistory.owner";

	private OwnerCheck() {
	}

	public static void verify(Application zeze) {
		verify(HistoryModule.get(zeze));
	}

	@SuppressWarnings("ConstantConditions")
	static void verify(HistoryModule module) {
		// 锚定表的实际物理存储库而非配置名。
		var table = (Zeze.Transaction.Table)module.getHistoryTable();
		var db = table.getDatabase();
		if (db == null)
			return;
		var name = module.getHistoryName();
		var key = ByteBuffer.Allocate(64);
		key.WriteString(OWNER_KEY);
		var ops = db.getDirectOperates();
		var exist = ops.getDataWithVersion(key);
		if (exist == null || exist.data == null) {
			// 首见以 version=0 认领，后续需再推进版本封存，避免被携带旧空读的并发认领覆盖。
			ops.saveDataWithSameVersion(key, encodeName(name), 0);
			exist = ops.getDataWithVersion(key);
			if (exist == null || exist.data == null) {
				logger.info("tHistory owner check skipped: direct operates unavailable (disableOperates?). db={}",
						db.getDatabaseUrl());
				return;
			}
		}
		var owner = exist.data.ReadString();
		if (owner.equals(name) && exist.version == 0) {
			// 推进 version 脱离 0 态并回读，确保并发空读无法再以 version=0 覆写。
			ops.saveDataWithSameVersion(key, encodeName(name), 0);
			exist = ops.getDataWithVersion(key);
			if (exist == null || exist.data == null || exist.version == 0)
				throw new IllegalStateException("tHistory owner could not be stabilized: db=" + db.getDatabaseUrl());
			owner = exist.data.ReadString();
		}
		if (!owner.equals(name))
			throw new IllegalStateException("tHistory physical table owner mismatch: db(url="
					+ db.getDatabaseUrl() + ") is owned by history name '" + owner + "' but this app uses '"
					+ name + "'. Different history names have overlapping gid number spaces; sharing one"
					+ " physical tHistory silently overwrites history rows (writeOnly replace). Use the"
					+ " same history name for all apps sharing this database, or move tHistory to its own"
					+ " database. 换名重启同理：需先迁移/清理旧名数据并删除归属标记(" + OWNER_KEY + ")。");
		logger.info("tHistory owner verified: db={} historyName={}", db.getDatabaseUrl(), name);
	}

	private static ByteBuffer encodeName(String name) {
		var bb = ByteBuffer.Allocate();
		bb.WriteString(name);
		return bb;
	}
}
