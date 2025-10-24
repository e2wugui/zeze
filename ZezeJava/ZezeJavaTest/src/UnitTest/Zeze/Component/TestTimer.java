package UnitTest.Zeze.Component;

import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Transaction.Procedure;
import demo.App;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTimer {

	@Before
	public final void testInit() throws Exception {
		System.out.println("Timer Test Init");
		demo.App.getInstance().Start();
		App.Instance.Zeze.getTimer().start();
	}

	@After
	public final void testCleanup() throws Exception {
		//System.out.println("Timer Test Cleanup");
		//demo.App.getInstance().Stop();
	}

	public static class TestTimerHandle1 implements TimerHandle {
		@Override
		public void onTimer(TimerContext timerContext) {
			System.out.println(">> Name: " + timerContext.timerName
					+ " ID: " + timerContext.timerId
					+ " Now: " + System.currentTimeMillis()
					+ " Expected: " + timerContext.expectedTimeMills
					+ " Next: " + timerContext.nextExpectedTimeMills);
		}
	}

	public static class TestTimerHandle2 implements TimerHandle {
		@Override
		public void onTimer(TimerContext timerContext) {
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
		public void onTimer(TimerContext timerContext) {
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

		// Test schedule timer
		Assert.assertEquals(Procedure.Success, App.getInstance().Zeze.newProcedure(() -> {
			timer.schedule(1, 200, 10, TestTimerHandle1.class, null);
			return Procedure.Success;
		}, "test_CommonSchedule").call());

		// to prevent thread from being killed
		int sleepCircle = 12;
		for (int i = 0; i < sleepCircle; ++i) {
			Thread.sleep(200);
			System.out.println(">> sleep " + i);
		}
		System.out.println("========== Test1 Passed ==========");

		// Test with customBean
		TestBean testBean1 = new TestBean();

		Assert.assertEquals(Procedure.Success, App.getInstance().Zeze.newProcedure(() -> {
			timer.schedule(1, 200, 10, TestTimerHandle2.class, testBean1);
			return Procedure.Success;
		}, "test_ScheduleWithCustomBean").call());

		for (int i = 0; i < sleepCircle; ++i) {
			Thread.sleep(200);
			System.out.println(">> sleep " + i);
		}

		Assert.assertSame(10, testBean1.getTestValue());
		System.out.println("========== Test2 Passed ==========");

		// Test canceling schedule
		TestBean testBean2 = new TestBean();
		Assert.assertEquals(Procedure.Success, App.getInstance().Zeze.newProcedure(() -> {
			timer.schedule(1, 200, 10, TestTimerHandle3.class, testBean2);
			return Procedure.Success;
		}, "test_CancelSchedule").call());

		for (int i = 0; i < sleepCircle; ++i) {
			Thread.sleep(200);
			if (i == 5) {
				testBean2.loseConnection();
			}
			System.out.println(">> sleep " + i);
		}

		Assert.assertTrue(testBean2.getTestValue() <= 10);
		System.out.println("========== Test3 Passed ==========");
	}
}
