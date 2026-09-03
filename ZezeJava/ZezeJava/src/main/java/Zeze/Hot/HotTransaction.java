package Zeze.Hot;

import java.util.ArrayList;
import Zeze.Util.Action0;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HotTransaction {
	private static final Logger logger = LogManager.getLogger(HotTransaction.class);
	private final String name;
	private final ArrayList<Action0> rollbacks = new ArrayList<>();
	private final ArrayList<Action0> commits = new ArrayList<>();

	public HotTransaction(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void whileRollback(Action0 action) {
		rollbacks.add(action);
	}

	public void whileCommit(Action0 action) {
		commits.add(action);
	}

	public void commit() throws Exception {
		// commit 动作是安装已成功后的清理步骤（删除备份等）。单个动作失败（如Windows下
		// 备份jar被AV占用导致deleteIfExists抛出）不能把异常抛出去：install的catch会对
		// 全部命名空间执行rollback——前面已执行的动作（备份已被删除）的补偿必然失败，
		// 文件层面留下部分新版部分旧版的混杂状态。与rollback()的best-effort语义对称：
		// 失败仅记日志，继续执行其余清理动作。
		for (var commit : commits) {
			try {
				commit.run();
			} catch (Exception ex) {
				logger.error(name, ex);
			}
		}
		commits.clear();
		rollbacks.clear();
	}

	public void rollback() {
		for (var i = rollbacks.size() - 1; i >= 0; --i) {
			try {
				rollbacks.get(i).run();
			} catch (Exception ex) {
				logger.error(name, ex);
			}
		}
		rollbacks.clear();
	}
}
