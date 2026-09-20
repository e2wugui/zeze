package Zeze.Onz;

import Zeze.Builtin.Onz.BSavedCommits;
import demo.Module1.BKuafu;
import demo.Module1.BKuafuResult;

/**
 * OH1-F4测试事务（package Zeze.Onz以覆写包私有的commit捕获快照）：
 * perform内先同步注册参与方zeze2，再进入pendingAsync窗口，异步线程内注册迟到参与方
 * zeze1（业务异步放大的公开模式），窗口结束返回0。OnzServer.perform在waitPendingAsync
 * 之后重建快照再调用本commit——捕获传入的state即为eCommitting持久化内容。
 */
public class LateRegisterTransaction extends OnzTransaction<BKuafu.Data, BKuafuResult.Data> {
	public volatile BSavedCommits.Data capturedCommitState;

	@Override
	protected long perform() throws Exception {
		var a2 = new BKuafu.Data();
		a2.setAccount(200);
		a2.setMoney(10);
		callProcedureAsync("zeze2", "cp1f4CommitSnap", a2, new BKuafuResult.Data()).get();

		setPendingAsync(true);
		var finisher = new Thread(() -> {
			try {
				var a1 = new BKuafu.Data();
				a1.setAccount(201);
				a1.setMoney(20);
				// 异步线程内注册迟到参与方（公开API允许的业务异步放大模式）
				callProcedureAsync("zeze1", "cp1f4CommitSnap", a1, new BKuafuResult.Data()).get();
			} finally {
				setPendingAsync(false);
			}
		});
		finisher.setDaemon(true);
		finisher.start();
		return 0;
	}

	@Override
	void commit(byte[] tidBytes, BSavedCommits.Data state) {
		this.capturedCommitState = state; // 捕获重建后的快照（eCommitting持久化内容）
		super.commit(tidBytes, state);
	}
}
