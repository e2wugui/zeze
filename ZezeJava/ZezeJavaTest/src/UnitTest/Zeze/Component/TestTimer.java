package UnitTest.Zeze.Component;

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
	public final void testInit() throws Throwable {
		System.out.println("Timer Test Init");
		demo.App.getInstance().Start();
	}

	@After
	public final void testCleanup() throws Throwable {
		System.out.println("Timer Test Cleanup");
		demo.App.getInstance().Stop();
	}

	@Test
	public final void test1BasicTimer() throws Throwable {
		System.out.println("========== Testing Basic Timer ==========");
		var timer = App.getInstance().Zeze.getTimer();

		// Test schedule timer
		timer.addHandle("print1", timerContext -> {
			System.out.println(">> Name: " + timerContext.timerName + " ID: " + timerContext.timerId + " Now: " + timerContext.curTimeMills + " Expected: " + timerContext.expectedTimeMills + " Next: " + timerContext.nextExpectedTimeMills);
		});

		Assert.assertEquals(Procedure.Success, App.getInstance().Zeze.newProcedure(() -> {
			timer.schedule(1, 200, 10, "print1", null);
			return Procedure.Success;
		}, "test_CommonSchedule").Call());

		// to prevent thread from being killed
		int sleepCircle = 12;
		for (int i = 0; i < sleepCircle; ++i) {
			Thread.sleep(200);
			System.out.println(">> sleep " + i);
		}
		System.out.println("========== Test1 Passed ==========");

		// Test with customBean
		TestBean testBean1 = new TestBean();
		timer.addHandle("addTestBean", timerContext -> {
			TestBean bean = (TestBean)timerContext.customData;
			bean.addValue();
			System.out.println(">> Name: " + timerContext.timerName + " ID: " + timerContext.timerId + " Now: " + timerContext.curTimeMills + " Expected: " + timerContext.expectedTimeMills + " Next: " + timerContext.nextExpectedTimeMills + " Bean Value: " + bean.getTestValue());
		});

		Assert.assertEquals(Procedure.Success, App.getInstance().Zeze.newProcedure(() -> {
			timer.schedule(1, 200, 10, "addTestBean", testBean1);
			return Procedure.Success;
		}, "test_ScheduleWithCustomBean").Call());

		for (int i = 0; i < sleepCircle; ++i) {
			Thread.sleep(200);
			System.out.println(">> sleep " + i);
		}

		Assert.assertSame(testBean1.getTestValue(), 10);
		System.out.println("========== Test2 Passed ==========");

		// Test canceling schedule
		TestBean testBean2 = new TestBean();
		timer.addHandle("cancelSchedule", timerContext -> {
			TestBean bean = (TestBean)timerContext.customData;
			if (bean.checkLiving()) {
				bean.addValue();
				System.out.println(">> Name: " + timerContext.timerName + " ID: " + timerContext.timerId + " Now: " + timerContext.curTimeMills + " Expected: " + timerContext.expectedTimeMills + " Next: " + timerContext.nextExpectedTimeMills + " Bean Value: " + bean.getTestValue());
			} else {
				timer.cancel("3");
				System.out.println(">> Schedule Canceled");
			}
		});
		Assert.assertEquals(Procedure.Success, App.getInstance().Zeze.newProcedure(() -> {
			timer.schedule(1, 200, 10, "cancelSchedule", testBean2);
			return Procedure.Success;
		}, "test_CancelSchedule").Call());

		for (int i = 0; i < sleepCircle; ++i) {
			Thread.sleep(200);
			if (i == 5) {
				testBean2.loseConnection();
			}
			System.out.println(">> sleep " + i);
		}

		Assert.assertTrue(testBean2.getTestValue() < 10);
		System.out.println("========== Test3 Passed ==========");
	}
}
