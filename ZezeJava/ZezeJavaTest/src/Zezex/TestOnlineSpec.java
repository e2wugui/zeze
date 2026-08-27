package Zezex;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ClientGame.Equip.SEquipement;
import ClientGame.Fight.AreYouFight;
import Zeze.Builtin.Game.Online.SReliableNotify;
import Zeze.Game.OnlineSpec;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * OnlineSpec 的回归测试。
 * harness 用 ZezexTestEnv 脚手架：loginQueue + linkd + server + client 全进程内组网。
 *
 * 关于 §8-6（P0 回归，ofAllOnline 多 OnlineSet）：本 harness 只有默认单 OnlineSet，
 * 无法覆盖多 set 场景；这里断言单 set 下每个目标恰好收到一次。
 * 多 set 的正确性由 OnlineTarget.AllRoles.send 的代码走查保证：
 * foreachOnline 的 lambda 参数命名为 o，方法体只允许使用 o，禁止引用外层任何名为 online 的引用。
 *
 * Arch 侧（Zeze.Arch.OnlineSpec）本 harness 拿不到 Online 实例（Game 服务器挂的是
 * Zeze.Game.Online，与 Zeze.Arch.Online 是两棵独立继承树），单元测试见 Zeze.Arch.TestArchOnlineSpec。
 */
public class TestOnlineSpec {
	private static final @NotNull Logger logger = LogManager.getLogger(TestOnlineSpec.class);

	final ZezexTestEnv env = new ZezexTestEnv();

	// 客户端接收计数：SEquipement 是普通协议计数器，SReliableNotify 是可靠通知计数器。
	private final AtomicInteger sEquipCount0 = new AtomicInteger();
	private final AtomicInteger sEquipCount1 = new AtomicInteger();
	private final AtomicInteger sReliableCount0 = new AtomicInteger();
	private final AtomicInteger sReliableCount1 = new AtomicInteger();

	/** 把客户端已注册的协议 handle 替换为计数 handle（AddFactoryHandle 重复注册会抛异常，只能替换）。 */
	@SuppressWarnings("unchecked")
	private static void installCountHandle(ClientGame.App client, long typeId, AtomicInteger counter) {
		var fh = (Service.ProtocolFactoryHandle<Protocol<?>>)client.ClientService.findProtocolFactoryHandle(typeId);
		Assertions.assertNotNull(fh);
		fh.Handle = p -> {
			counter.incrementAndGet();
			return Procedure.Success;
		};
	}

	private static void awaitCount(AtomicInteger counter, int expected) throws InterruptedException {
		for (int i = 0; i < 500 && counter.get() < expected; ++i) // 最多等 5 秒
			Thread.sleep(10);
		Assertions.assertEquals(expected, counter.get());
	}

	@Test
	public void testOnlineSpec() throws Exception {
		Task.tryInitThreadPool();
		try {
			env.prepareNewEnvironment(2, 1, 1);
			var client0 = env.clients.get(0);
			var client1 = env.clients.get(1);
			var server0 = env.servers.getFirst();

			ZezexTestEnv.auth(client0.onLinkConnectedFuture.get(), client0, "account0");
			var role0 = ZezexTestEnv.getRole(client0);
			var roleId0 = null != role0 ? role0.getId() : ZezexTestEnv.createRole(client0, "role0");
			ZezexTestEnv.login(client0, roleId0);

			ZezexTestEnv.auth(client1.onLinkConnectedFuture.get(), client1, "account1");
			var role1 = ZezexTestEnv.getRole(client1);
			var roleId1 = null != role1 ? role1.getId() : ZezexTestEnv.createRole(client1, "role1");
			ZezexTestEnv.login(client1, roleId1);

			var online = server0.Provider.getOnline();
			// 登录流程结束后再替换 handle，避免登录期间的推送干扰计数基准。
			installCountHandle(client0, SEquipement.TypeId_, sEquipCount0);
			installCountHandle(client1, SEquipement.TypeId_, sEquipCount1);
			installCountHandle(client0, SReliableNotify.TypeId_, sReliableCount0);
			installCountHandle(client1, SReliableNotify.TypeId_, sReliableCount1);

			// ---- §8-1 空目标：不编码、不发送 ----
			logger.info("=== 1 empty target");
			var encodeBomb = new SEquipement() {
				@Override
				public void encode(@NotNull ByteBuffer bb) {
					throw new AssertionError("empty target must not encode");
				}
			};
			OnlineSpec.ofRoles(online, List.of()).send(encodeBomb);
			OnlineSpec.ofRoles(online, List.of()).send(encodeBomb.getTypeId(), new Binary(new byte[0]));
			OnlineSpec.ofAllOnline(online, List.of()).send(encodeBomb);
			Thread.sleep(200); // 负向稳定窗
			Assertions.assertEquals(0, sEquipCount0.get());
			Assertions.assertEquals(0, sEquipCount1.get());

			// ---- §8-2 单发/批发路径 ----
			logger.info("=== 2 roles dispatch");
			OnlineSpec.ofRoles(online, List.of(roleId0)).send(new SEquipement()); // size==1 单发
			awaitCount(sEquipCount0, 1);
			Assertions.assertEquals(0, sEquipCount1.get());
			OnlineSpec.ofRoles(online, List.of(roleId0, roleId1)).send(new SEquipement()); // size>1 批发
			awaitCount(sEquipCount0, 2);
			awaitCount(sEquipCount1, 1);
			// withContext 在无协议上下文时回退默认 Online（顺带覆盖 resolveOnline 路径）。
			OnlineSpec.ofRole(online, roleId0).withContext().send(new SEquipement());
			awaitCount(sEquipCount0, 3);

			// ---- §8-3 事务内 send：commit 前未收到，commit 后收到；rollback 后不收到 ----
			logger.info("=== 3 txn send");
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				OnlineSpec.ofRoles(online, List.of(roleId0)).send(new SEquipement());
				Assertions.assertEquals(3, sEquipCount0.get()); // commit 前未收到
				return Procedure.Success;
			}, "testTxnSend").call());
			awaitCount(sEquipCount0, 4); // commit 后收到
			Assertions.assertEquals(1L, server0.Zeze.newProcedure(() -> {
				OnlineSpec.ofRoles(online, List.of(roleId0)).send(new SEquipement());
				return 1L; // 非 Success 触发回滚
			}, "testTxnSendRollback").call());
			Thread.sleep(200); // 负向稳定窗
			Assertions.assertEquals(4, sEquipCount0.get()); // rollback 后不收到

			// ---- §8-4 事务内 sendNow：立即收到（不等 commit） ----
			logger.info("=== 4 sendNow");
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				OnlineSpec.ofRoles(online, List.of(roleId0)).sendNow(new SEquipement());
				awaitCount(sEquipCount0, 5); // 事务体内、commit 前即收到
				return Procedure.Success;
			}, "testSendNow").call());

			// ---- §8-5 sendWhileRollback：rollback 后收到；commit 后不收到 ----
			logger.info("=== 5 sendWhileRollback");
			Assertions.assertEquals(1L, server0.Zeze.newProcedure(() -> {
				OnlineSpec.ofRoles(online, List.of(roleId0)).sendWhileRollback(new SEquipement());
				return 1L; // 回滚
			}, "testSendWhileRollback").call());
			awaitCount(sEquipCount0, 6); // rollback 后收到
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				OnlineSpec.ofRoles(online, List.of(roleId0)).sendWhileRollback(new SEquipement());
				return Procedure.Success;
			}, "testSendWhileRollbackCommit").call());
			Thread.sleep(200); // 负向稳定窗
			Assertions.assertEquals(6, sEquipCount0.get()); // commit 后不收到

			// ---- §8-6 P0 回归：ofAllOnline 每个目标恰好收到一次（单 set，见类注释） ----
			logger.info("=== 6 ofAllOnline");
			var base0 = sEquipCount0.get();
			var base1 = sEquipCount1.get();
			OnlineSpec.ofAllOnline(online, List.of(roleId0, roleId1)).send(new SEquipement());
			awaitCount(sEquipCount0, base0 + 1);
			awaitCount(sEquipCount1, base1 + 1);
			Thread.sleep(200); // 负向稳定窗
			Assertions.assertEquals(base0 + 1, sEquipCount0.get()); // 恰好一次，无重复
			Assertions.assertEquals(base1 + 1, sEquipCount1.get());

			// ---- §8-7 ofReliableNotify：send / sendWhileRollback 正常投递 ----
			logger.info("=== 7 reliableNotify");
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				online.addReliableNotifyMark(roleId0, "testListener");
				return Procedure.Success;
			}, "addReliableNotifyMark").call());
			OnlineSpec.ofReliableNotify(online, roleId0, "testListener").send(new SEquipement());
			awaitCount(sReliableCount0, 1);
			Assertions.assertEquals(1L, server0.Zeze.newProcedure(() -> {
				OnlineSpec.ofReliableNotify(online, roleId0, "testListener").sendWhileRollback(new SEquipement());
				return 1L;
			}, "testReliableWhileRollback").call());
			awaitCount(sReliableCount0, 2);
			Assertions.assertEquals(0, sReliableCount1.get());

			// ---- §8-8 ofTransmit：未知 actionName 抛错；已知 action 正常路由 ----
			logger.info("=== 8 transmit");
			Assertions.assertThrows(UnsupportedOperationException.class,
					() -> OnlineSpec.ofTransmit(online, roleId0, "unknownActionXyz", roleId1).transmit());
			var transmitCount = new AtomicInteger();
			online.getTransmitActions().put("testAction", (sender, target, param) -> {
				transmitCount.incrementAndGet();
				return 0;
			});
			OnlineSpec.ofTransmit(online, roleId0, "testAction", roleId1).transmit();
			awaitCount(transmitCount, 1);

			// ---- §8-9 事务内 sendRpcForWait → IllegalStateException ----
			logger.info("=== 9 sendRpcForWait in txn");
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				Assertions.assertThrows(IllegalStateException.class,
						() -> OnlineSpec.ofRole(online, roleId0).sendRpcForWait(new AreYouFight()));
				return Procedure.Success;
			}, "testSendRpcForWaitInTxn").call());

			// ---- §8-10 复用：同一 spec 连续 send 两个协议都送达；残留选项被静默忽略 ----
			logger.info("=== 10 reuse");
			var base11 = sEquipCount0.get();
			var spec = OnlineSpec.ofRole(online, roleId0);
			spec.send(new SEquipement());
			spec.trying(true).timeout(1000); // 残留选项：send 族忽略，不抛异常（spec 可复用）
			spec.send(new SEquipement());
			awaitCount(sEquipCount0, base11 + 2);

			// ---- §8-12 冻结：事务内 send(p1) 后改选项再 send(p2)，commit 后两者都送达 ----
			logger.info("=== 12 freeze");
			var base12 = sEquipCount0.get();
			Assertions.assertEquals(Procedure.Success, server0.Zeze.newProcedure(() -> {
				var s = OnlineSpec.ofRole(online, roleId0);
				s.trying(false).send(new SEquipement()); // p1 冻结 trying=false
				s.trying(true).send(new SEquipement()); // p2 冻结 trying=true，不影响已排队的 p1
				return Procedure.Success;
			}, "testFreeze").call());
			awaitCount(sEquipCount0, base12 + 2);
			// p1 按调用时刻选项执行由接收计数间接断言；同时走查确认 OnlineSpec.send0 等
			// 延迟闭包仅捕获局部变量（tg/tr/o），从不捕获 spec 实例。

			ZezexTestEnv.logout(client0, roleId0);
			ZezexTestEnv.logout(client1, roleId1);
		} catch (Throwable e) { // rethrow
			logger.error("", e);
			throw e;
		} finally {
			env.stopAll();
		}
	}
}
