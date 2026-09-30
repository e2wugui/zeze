package Zeze.History;

import Zeze.Application;
import Zeze.Serialize.ByteBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * tHistory 物理表归属校验（FND33 history-02）：gid（GlobalSerialId）的数值空间绑定
 * history 发号名（Config.getHistory），不同发号名的 gid 数值重叠；而 tHistory 主键裸用
 * gid，两个不同发号名的 app 若解析到同一物理存储（默认表配置即同库），History.writeOnly
 * 的 replace 会以数值相等的 gid 静默互覆——历史行丢失，且两套序列各自连续（空洞检测的
 * 前提是单序列连续），回放端与账本双双无感。对比：ApplyDatabaseZeze 对 apply 库误配已
 * 显式 fail-fast；PendingGidLedger 只做了账本侧按 Application 隔离，表侧无对称防护。
 *
 * 收口为部署契约的启动期校验（KISS，不改盘上格式）：在 tHistory 所属数据库的
 * DirectOperates 区写入归属标记（键 {@link #OWNER_KEY}，值为发号名），Application 启动
 * （atomicOpenDatabase）时校验——同库多 app 必须同一发号名（多 app 协作的既有语义：
 * 同名 gid 从同一 SM 取号天然不重叠），或为 tHistory 显式分库；换名重启同样被拦截
 * （新名从零发号，对存量行就是"他名覆盖"，必须显式迁移：清理旧名数据并删除归属标记）。
 * 把发号名维度纳入 tHistory 键编码属盘上格式迁移，另立专项，不在本收口。
 *
 * 并发首启：两个不同名 app 同时首启（标记缺失）存在认领竞态，由"写后重读终值"裁决
 * ——后写者胜出，败者在重读中看到他名即失败，至多一方通过。disableOperates 的
 * NullOperates 降级读不到标记，校验跳过（info 留痕），该配置本身即放弃直接操作面。
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
