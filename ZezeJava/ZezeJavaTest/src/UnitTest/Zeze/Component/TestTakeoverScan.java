package UnitTest.Zeze.Component;

import UnitTest.Zeze.BMyBean;
import Zeze.Application;
import Zeze.Builtin.Collections.Queue.BQueue;
import Zeze.Collections.CsQueue;
import Zeze.Transaction.TableX;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 步骤②扫描兜底回归（AnnounceServers的功能替代）：不依赖Suspect提示，
 * 周期扫描发现过期租约→自动tryTransfer完成接管；未过期租约不动。
 */
@Fast
public class TestTakeoverScan {

	@SuppressWarnings("unchecked")
	private static TableX<String, BQueue> tQueues(Application app) {
		var t = app.getTable("Zeze_Builtin_Collections_Queue_tQueues");
		Assertions.assertNotNull(t);
		return (TableX<String, BQueue>)t;
	}

	@Test
	public void testScanTakesOverExpired() throws Exception {
		var conf = TakeoverTestEnv.newConf("on", 600_000, 200); // 扫描周期200ms
		var app = new Application("TestTakeoverScan", conf);
		try {
			app.start();
			var csq = new CsQueue<>(app.getQueueModule(), "TestTakeoverScanQ", conf.getServerId(), BMyBean.class, 10);

			// 死者777：队列数据+root stamp=5。不手动tryTransfer，等扫描发现。
			// 死者serverId直接构造CsQueue（ctor走_open，无'@'限制；module.open公开接口拒绝'@'名字）；
			// 其scope按插入序在live实例之后执行，轮到时src已被live scope清空立碑，直接返回0。
			var deadCsq = new CsQueue<>(app.getQueueModule(), "TestTakeoverScanQ", 777, BMyBean.class, 10);
			var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var v = new BMyBean();
				v.setI(77);
				deadCsq.add(v); // 此刻root.serial==myEpoch（ctor晚注册stamp），fence通过
				tQueues(app).getOrAdd("TestTakeoverScanQ@777").setLoadSerialNo(5);
				return 0L;
			}, "TestTakeoverScan.forgeDead")).call();
			Assertions.assertEquals(0L, rc);
			// 租约最后伪造：lease存在之前扫描walk会跳过777，避免【空数据+过期租约】竞态被立空碑。
			TakeoverTestEnv.forgeLease(app, 777, 5, System.currentTimeMillis() - 1_000);
			// TableX.walk只看底层存储（未见checkpoint的新增记录不可见）：伪造后必须强制checkpoint，
			// 否则扫描通道永远看不见租约行，测试只能靠跨App共享存储的污染行"借光"通过。
			app.checkpointRun();

			// 等扫描周期接管完成（最多5秒）。
			var deadline = System.currentTimeMillis() + 5_000;
			while (TakeoverTestEnv.readLease(app, 777)[1] != 0 && System.currentTimeMillis() < deadline)
				Thread.sleep(50);
			Assertions.assertEquals(0L, TakeoverTestEnv.readLease(app, 777)[1], "扫描应发现过期租约并完成接管立碑");

			var got = new int[] {-1};
			var rcPoll = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var v = csq.poll();
				got[0] = v != null ? v.getI() : -1;
				return 0L;
			}, "TestTakeoverScan.poll")).call();
			Assertions.assertEquals(0L, rcPoll);
			Assertions.assertEquals(77, got[0], "扫描接管的数据应可取回");

			// 未过期的租约：扫描不动（flap保护由tryTransfer内部实现，这里验证扫描入口不误报）。
			var liveExpireAt = System.currentTimeMillis() + 60_000;
			TakeoverTestEnv.forgeLease(app, 888, 5, liveExpireAt);
			app.checkpointRun(); // 同上：让888对扫描walk可见，断言"可见但不误动"才有意义
			Thread.sleep(500); // 至少两个扫描周期
			var lease888 = TakeoverTestEnv.readLease(app, 888);
			Assertions.assertEquals(liveExpireAt, lease888[1], "未过期租约不得被扫描立碑");
		} finally {
			app.stop();
		}
	}
}
