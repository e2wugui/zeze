package UnitTest.Zeze.Component;

import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerSpec;
import Zeze.Transaction.Procedure;
import demo.App;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestTimer {

	@BeforeEach
	public final void testInit() throws Exception {
		System.out.println("Timer Test Init");
		demo.App.getInstance().Start();
		App.Instance.Zeze.getTimer().start();
	}

	@AfterEach
	public final void testCleanup() throws Exception {
		//System.out.println("Timer Test Cleanup");
		//demo.App.getInstance().Stop();
	}

	public static class TestTimerHandle1 implements TimerHandle {
		@Override
		public void onTimer(@NonNull TimerContext timerContext) {
			System.out.println(">> Name: " + timerContext.timerName
					+ " ID: " + timerContext.timerId
					+ " Now: " + System.currentTimeMillis()
					+ " Expected: " + timerContext.expectedTimeMills
					+ " Next: " + timerContext.nextExpectedTimeMills);
		}
	}

	public static class TestTimerHandle2 implements TimerHandle {
		@Override
		public void onTimer(@NonNull TimerContext timerContext) {
			TestBean bean = (TestBean)timerContext.customData;
			//noinspection DataFlowIssue
			bean.addValue();
			System.out.println(">> Name: " + timerContext.timerName
					+ " ID: " + timerContext.timerId
					+ " Now: " + System.currentTimeMillis()
					+ " Expected: " + timerContext.expectedTimeMills
					+ " Next: " + timerContext.nextExpectedTimeMills
					+ " Bean Value: " + bean.getTestValue());
		}
	}

	public static class TestTimerHandle3 implements TimerHandle {
		@Override
		public void onTimer(@NonNull TimerContext timerContext) {
			TestBean bean = (TestBean)timerContext.customData;
			//noinspection DataFlowIssue
			if (bean.checkLiving()) {
				bean.addValue();
				System.out.println(">> Name: " + timerContext.timerName
						+ " ID: " + timerContext.timerId
						+ " Now: " + System.currentTimeMillis()
						+ " Expected: " + timerContext.expectedTimeMills
						+ " Next: " + timerContext.nextExpectedTimeMills
						+ " Bean Value: " + bean.getTestValue());
			} else {
				timerContext.timer.cancel("3");
				System.out.println(">> Schedule Canceled");
			}
		}
	}

	@Test
	public final void test1BasicTimer() throws Exception {
		System.out.println("========== Testing Basic Timer ==========");
		var timer = App.getInstance().Zeze.getTimer();
		// 定时器周期 50ms：逻辑与原 200ms 版一致，只等比压缩等待墙钟（period×times、睡眠窗口同步缩小）。
		final int periodMs = 50;
		final int times = 10;

		// Test schedule timer
		Assertions.assertEquals(Procedure.Success, App.getInstance().Zeze.newProcedure(() -> {
			//timer.schedule(1, 200, 10, TestTimerHandle1.class, null);
			timer.schedule(TimerSpec.ofDelay(1).period(periodMs).times(times), TestTimerHandle1.class);
			return Procedure.Success;
		}, "test_CommonSchedule").call());

		// to prevent thread from being killed
		// 窗口比定时器总时长多~30%（与原200ms版的余量比例相同），吸收调度抖动
		int sleepCircle = 14;
		for (int i = 0; i < sleepCircle; ++i) {
			Thread.sleep(periodMs);
			System.out.println(">> sleep " + i);
		}
		System.out.println("========== Test1 Passed ==========");

		// Test with customBean
		TestBean testBean1 = new TestBean();

		Assertions.assertEquals(Procedure.Success, App.getInstance().Zeze.newProcedure(() -> {
			//timer.schedule(1, 200, 10, TestTimerHandle2.class, testBean1);
			timer.schedule(TimerSpec.ofDelay(1).period(periodMs).times(times), TestTimerHandle2.class, testBean1);
			return Procedure.Success;
		}, "test_ScheduleWithCustomBean").call());

		for (int i = 0; i < sleepCircle; ++i) {
			Thread.sleep(periodMs);
			System.out.println(">> sleep " + i);
		}

		Assertions.assertEquals(times, testBean1.getTestValue());
		System.out.println("========== Test2 Passed ==========");

		// Test canceling schedule
		TestBean testBean2 = new TestBean();
		Assertions.assertEquals(Procedure.Success, App.getInstance().Zeze.newProcedure(() -> {
			//timer.schedule(1, 200, 10, TestTimerHandle3.class, testBean2);
			timer.schedule(TimerSpec.ofDelay(1).period(periodMs), TestTimerHandle3.class, testBean2);
			return Procedure.Success;
		}, "test_CancelSchedule").call());

		for (int i = 0; i < sleepCircle; ++i) {
			Thread.sleep(periodMs);
			if (i == 5) {
				testBean2.loseConnection();
			}
			System.out.println(">> sleep " + i);
		}

		// 验证取消语义：i==5 loseConnection 后，TestTimerHandle3 下次触发即 cancel。
		// 先确认已触发过（排除定时器根本没跑导致空转通过），再等 4 个周期确认计数不再增长（未取消则每 50ms 还会 +1）。
		var canceledValue = testBean2.getTestValue();
		Assertions.assertTrue(canceledValue > 0);
		Thread.sleep(periodMs * 4L);
		Assertions.assertEquals(canceledValue, testBean2.getTestValue());
		System.out.println("========== Test3 Passed ==========");
	}
}
