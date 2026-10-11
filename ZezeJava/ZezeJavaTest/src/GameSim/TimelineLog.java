package GameSim;

import Zeze.Util.PropertiesHelper;

/**
 * 诊断时间线（休眠旋钮 {@code game.slowLogMs>0} 才激活，默认关闭零成本）：慢操作完成事件与
 * 窗口/fanout/踢线/世界起止生命周期事件统一走 log4j2（"GameSim.Timeline"），与
 * ZezeJavaTest/log/zeze.log 的毫秒墙钟 pattern 同流——hb p99 尖峰与 checkpoint 轮次
 * （{@code zeze.checkpointTrace}）/系统邮件 fan-out 的相位相关裁决在单文件内完成
 * （BENCHMARK.md L 节双证协议的时间线证据侧）。
 *
 * <p>仅裁决/诊断跑开启，基线与 A/B 跑保持默认关闭：{@code enabled} 为 static final，
 * 关闭时调用点分支可被 JIT 消除，热路径无额外开销。
 */
public final class TimelineLog {
	public static final boolean enabled;
	public static final long thresholdUs;

	private static final org.apache.logging.log4j.Logger log =
			org.apache.logging.log4j.LogManager.getLogger("GameSim.Timeline");

	static {
		int slowLogMs = PropertiesHelper.getInt("game.slowLogMs", 0);
		enabled = slowLogMs > 0;
		thresholdUs = slowLogMs * 1000L;
	}

	private TimelineLog() {
	}

	/** 慢操作完成（GameWorld.call 统一入口；us=本次事务耗时） */
	public static void slowOp(String op, long us, int windowId) {
		log.info("slow op={} us={} win={}", op, us, windowId);
	}

	/** 生命周期事件（窗口起止/fanout 起止/踢线/世界起停） */
	public static void event(String what) {
		log.info("evt {}", what);
	}
}
