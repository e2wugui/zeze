package UnitTest.Zeze.Raft;

import Zeze.Raft.RaftConfig;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-14 回归：BackgroundApplyCount配置0/负数未校验——LogSequence后台apply
 * 路径tryApply(count=0)一条不应用且lastApplied==lastIndex退出条件永不成，
 * 无限yield忙轮询、applyFuture永不完成、Raft.shutdown的await挂死。
 * 修复：verify() fail-fast（对齐既有三项校验口径）。
 */
@Fast
public class TestRaftConfigBackgroundApplyVerify {

	private static String xml(String extra) {
		return """
				<?xml version="1.0" encoding="utf-8"?>
				<raft Name="fnd5_14_verify"%s>
				<node Host="127.0.0.1" Port="23001"/>
				<node Host="127.0.0.1" Port="23002"/>
				<node Host="127.0.0.1" Port="23003"/>
				</raft>
				""".formatted(extra);
	}

	@Test
	public void testZeroAndNegativeRejected() {
		Assertions.assertThrows(IllegalStateException.class,
				() -> RaftConfig.loadFromString(xml(" BackgroundApplyCount=\"0\"")).verify(),
				"BackgroundApplyCount=0 必须 fail-fast（FND5-14）");
		Assertions.assertThrows(IllegalStateException.class,
				() -> RaftConfig.loadFromString(xml(" BackgroundApplyCount=\"-1\"")).verify(),
				"BackgroundApplyCount=-1 必须 fail-fast（FND5-14）");
	}

	@Test
	public void testDefaultAndValidPass() {
		Assertions.assertDoesNotThrow(() -> RaftConfig.loadFromString(xml("")).verify(),
				"默认值（500）必须通过");
		Assertions.assertDoesNotThrow(() -> RaftConfig.loadFromString(xml(" BackgroundApplyCount=\"100\"")).verify(),
				"合法值必须通过");
	}
}
