package Zeze.History;

import Zeze.Application;
import Zeze.Serialize.ByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * tHistory 物理表归属校验：gid 数值空间绑定 history 发号名（Config.getHistory），不同
 * 发号名的 gid 数值重叠，而 tHistory 主键裸用 gid——两个不同发号名的 app 解析到同一物理
 * 存储时，History.writeOnly 的 replace 会以数值相等的 gid 静默互覆（历史行丢失，回放端
 * 与账本双双无感）。
 *
 * 收口为启动期部署契约校验（不改盘上格式）：在 tHistory 所属数据库的 DirectOperates 区
 * 写归属标记（键 {@link #OWNER_KEY}，值为发号名），Application 启动时校验——同库多 app
 * 必须同一发号名（同名 gid 从同一 SM 取号天然不重叠）或为 tHistory 显式分库；换名重启
 * 同样拒绝（新名从零发号，对存量行即"他名覆盖"，须显式迁移：清理旧名数据并删除标记）。
 *
 * 并发首启的认领竞态由"写后重读终值"裁决——后写者胜，败者见他名即失败，至多一方通过。
 * disableOperates（NullOperates）读不到标记，校验跳过（info 留痕）。
 */
public final class OwnerCheck {
	private static final Logger logger = LogManager.getLogger(OwnerCheck.class);

	/** DirectOperates 区内的归属标记键（per tHistory 所属数据库）。 */
	static final String OWNER_KEY = "zeze.History.tHistory.owner";

	private OwnerCheck() {
	}

	public static void verify(@NotNull Application zeze) {
		// 经表的实际注册解析数据库（Table.getDatabase）：tHistory 可能被改挂到非配置库
		//（如测试的 FlakyDatabase），归属检测必须锚定实际物理存储而非配置名。
		var table = (Zeze.Transaction.Table)zeze.getHistoryModule().getHistoryTable();
		var db = table.getDatabase();
		if (db == null)
			return; // 表未注册到任何库（防御）：无可检面。
		var name = zeze.getConfig().getHistory();
		var key = ByteBuffer.Allocate(64);
		key.WriteString(OWNER_KEY);
		var ops = db.getDirectOperates();
		var exist = ops.getDataWithVersion(key);
		if (exist == null || exist.data == null) {
			// 首见：认领（version=0 插入）。并发认领由随后的重读终值裁决——后写者胜，
			// 先写者重读到他名即 fail-fast，至多一方通过。
			ops.saveDataWithSameVersion(key, encodeName(name), 0);
			exist = ops.getDataWithVersion(key);
			if (exist == null || exist.data == null) {
				logger.info("tHistory owner check skipped: direct operates unavailable (disableOperates?). db={}",
						db.getDatabaseUrl());
				return;
			}
		}
		var owner = exist.data.ReadString();
		if (!owner.equals(name))
			throw new IllegalStateException("tHistory physical table owner mismatch: db(url="
					+ db.getDatabaseUrl() + ") is owned by history name '" + owner + "' but this app uses '"
					+ name + "'. Different history names have overlapping gid number spaces; sharing one"
					+ " physical tHistory silently overwrites history rows (writeOnly replace). Use the"
					+ " same history name for all apps sharing this database, or move tHistory to its own"
					+ " database. 换名重启同理：需先迁移/清理旧名数据并删除归属标记(" + OWNER_KEY + ")。");
		logger.info("tHistory owner verified: db={} historyName={}", db.getDatabaseUrl(), name);
	}

	private static @NotNull ByteBuffer encodeName(@NotNull String name) {
		var bb = ByteBuffer.Allocate();
		bb.WriteString(name);
		return bb;
	}
}
