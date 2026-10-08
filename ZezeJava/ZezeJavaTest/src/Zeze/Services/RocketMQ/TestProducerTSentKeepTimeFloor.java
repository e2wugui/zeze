package Zeze.Services.RocketMQ;

import harness.Extra;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * tSent 保留时长的下界约束：保留时长必须覆盖 broker 事务回查总窗口
 * （transactionTimeOut + transactionCheckMax × transactionCheckInterval，默认参数约15分钟），
 * 否则每日清理会删掉仍在回查恢复窗口内的 COMMIT 行——回查对已删行答 UNKNOW，
 * COMMIT 丢失的半消息失去兜底而灭失。低于下限的配置须被钳到下限；
 * 下限之上（含默认值）的合法配置须原样生效。
 */
@Fast
@Extra
public class TestProducerTSentKeepTimeFloor {

	// 下限取 broker 回查总窗口的上界估计：默认窗口约15分钟，1小时（默认窗口的4倍）
	// 仍容忍 broker 调大回查次数/间隔。
	private static final long FLOOR_MILLIS = 60L * 60 * 1000;
	private static final long DEFAULT_MILLIS = 7L * 24 * 60 * 60 * 1000;
	private static final String KEY = "RocketMQ.Producer.tSentKeepTimeMillis";

	@Test
	public void keepTimeBelowFloorClampedToFloor() {
		try {
			// 0/负值：deadline>=now，除当毫秒新行外全表命中删除
			for (var bad : new String[]{"0", "-1"}) {
				System.setProperty(KEY, bad);
				Assertions.assertEquals(FLOOR_MILLIS, Producer.tSentKeepTimeMillis(), () -> "value=" + bad);
			}
			// 小于下限的正值：保留窗口被收窄进回查窗口以内，同样击穿兜底
			System.setProperty(KEY, Long.toString(FLOOR_MILLIS - 1));
			Assertions.assertEquals(FLOOR_MILLIS, Producer.tSentKeepTimeMillis(), "below floor");
			// 恰等于下限：合法，不调整
			System.setProperty(KEY, Long.toString(FLOOR_MILLIS));
			Assertions.assertEquals(FLOOR_MILLIS, Producer.tSentKeepTimeMillis(), "at floor");
		} finally {
			System.clearProperty(KEY);
		}
	}

	@Test
	public void keepTimeAboveFloorAndUnsetRespected() {
		try {
			var twoHours = 2 * FLOOR_MILLIS;
			System.setProperty(KEY, Long.toString(twoHours));
			Assertions.assertEquals(twoHours, Producer.tSentKeepTimeMillis(), "valid override");
			System.clearProperty(KEY);
			Assertions.assertEquals(DEFAULT_MILLIS, Producer.tSentKeepTimeMillis(), "unset -> default 7 days");
		} finally {
			System.clearProperty(KEY);
		}
	}
}
