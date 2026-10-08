package Zeze.Services;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SM-Raft/GCM-Raft 测试的 fatalKill 守卫：Raft 致命路径（同term双Leader、appendLog失败等）
 * 默认 halt(-1) 杀死整个进程——测试 JVM 即 gradle worker，连带跳过同 worker 的后续测试类
 * （本机多次同签名事故：exit value -1 + worker 回环连接复位）。未注入钩子的 Raft 系测试
 * 一律经 {@link #install} 包裹构造，触发点以 stack trace 记录进测试输出并计数。
 * 对齐 TestSmRaftCleanupNotifyAfterCommit 的既有惯例（记录+末尾断言 count==0）。
 */
public final class SmRaftFatalGuard {
	public final AtomicInteger fatalKills = new AtomicInteger();

	/** 注入钩子并原样返回服务实例（构造行链式包裹用）。 */
	public <T> T install(T raftServer) {
		try {
			var rocks = findRocks(raftServer);
			var raft = rocks.getClass().getMethod("getRaft").invoke(rocks);
			var hookMethod = Class.forName("Zeze.Raft.Raft")
					.getDeclaredMethod("setFatalKillHookForTest", Runnable.class);
			hookMethod.setAccessible(true);
			var name = (String)raft.getClass().getMethod("getName").invoke(raft);
			hookMethod.invoke(raft, (Runnable)() -> {
				//noinspection CallToPrintStackTrace
				new Throwable("fatalKill captured on " + name).printStackTrace();
				fatalKills.incrementAndGet();
			});
		} catch (Exception e) {
			throw new RuntimeException("SmRaftFatalGuard.install fail on " + raftServer, e);
		}
		return raftServer;
	}

	/** ServiceManagerWithRaft 与 GlobalCacheManagerWithRaft 同构的 rocks 字段（沿父类链查找）。 */
	private static Object findRocks(Object server) throws Exception {
		for (var c = server.getClass(); c != null; c = c.getSuperclass()) {
			Field f;
			try {
				f = c.getDeclaredField("rocks");
			} catch (NoSuchFieldException ignore) {
				continue;
			}
			f.setAccessible(true);
			return f.get(server);
		}
		throw new NoSuchFieldException("rocks not found on " + server.getClass().getName());
	}
}
