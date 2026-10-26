package Zeze.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Fast
public class TestCyclicBarrierRunCancellation {

	@Test
	public void queueBarrierCannotCompensateAnAlreadyExecutedAction() {
		var queue = new TaskOneByOneQueue(Runnable::run);
		var runs = new AtomicInteger();
		var cancels = new AtomicInteger();
		var barrier = new TaskOneByOneQueue.BarrierAction("complete", runs::incrementAndGet,
				1, cancels::incrementAndGet);
		submit(queue, new TaskOneByOneQueue.TaskBarrierAction(barrier, 1, null));
		barrier.cancel();
		assertEquals(1, runs.get());
		assertEquals(0, cancels.get());
		assertEquals(0, queue.size());
	}

	@Test
	public void reentrantCancellationKeepsFollowingQueueTaskBehindTheBarrierBody() {
		var queue = new TaskOneByOneQueue(Runnable::run);
		var events = new ArrayList<String>();
		var cancels = new AtomicInteger();
		var reference = new OutObject<TaskOneByOneQueue.BarrierAction>();
		var barrier = new TaskOneByOneQueue.BarrierAction("running", () -> {
			events.add("start");
			submit(queue, new TaskOneByOneQueue.TaskBodyTask(new TaskBody.OfAction(() -> events.add("next")),
					null, null, null));
			assert reference.value != null;
			reference.value.cancel();
			events.add("end");
		}, 1, cancels::incrementAndGet);
		reference.value = barrier;
		submit(queue, new TaskOneByOneQueue.TaskBarrierAction(barrier, 1, null));
		assertEquals(List.of("start", "end", "next"), events);
		assertEquals(0, cancels.get());
		assertFalse(queue.isLocked());
		assertEquals(0, queue.size());
	}

	@Test
	public void reentrantCancellationKeepsFollowingKey2TaskBehindTheBarrierBody() {
		var engine = new TaskOneByOneByKey2(1, Runnable::run);
		var queue = engine.new TaskOneByOne();
		var events = new ArrayList<String>();
		var reference = new OutObject<TaskOneByOneByKey2.BarrierAction>();
		var barrier = new TaskOneByOneByKey2.BarrierAction("running", () -> {
			events.add("start");
			queue.execute(new TaskBody.OfAction(() -> events.add("next")), null, null);
			assert reference.value != null;
			reference.value.cancel();
			events.add("end");
		}, 1);
		reference.value = barrier;
		queue.executeBarrier(barrier, 1, null);
		assertEquals(List.of("start", "end", "next"), events);
		assertEquals("[]", queue.toString());
	}

	@Test
	public void arrivalDuringCancellationWaitsForCompensationToFinish() {
		var queue = new TaskOneByOneQueue(Runnable::run);
		var events = new ArrayList<String>();
		var reference = new OutObject<TaskOneByOneQueue.BarrierAction>();
		var barrier = new TaskOneByOneQueue.BarrierAction("canceling", () -> events.add("unexpected-body"), 1, () -> {
			events.add("cancel-start");
			assert reference.value != null;
			submit(queue, new TaskOneByOneQueue.TaskBarrierAction(reference.value, 1, null));
			submit(queue, new TaskOneByOneQueue.TaskBodyTask(new TaskBody.OfAction(() -> events.add("next")),
					null, null, null));
			events.add("cancel-end");
		});
		reference.value = barrier;
		barrier.cancel();
		assertEquals(List.of("cancel-start", "cancel-end", "next"), events);
		assertEquals(0, queue.size());
	}

	private static void submit(TaskOneByOneQueue queue, TaskOneByOneQueue.Task task) {
		Runnable dispatch;
		queue.lock();
		try {
			dispatch = queue.submit(task);
		} finally {
			queue.unlock();
		}
		if (dispatch != null)
			dispatch.run();
	}
}
