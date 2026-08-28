package UnitTest.Zeze.Collections;

import java.util.ArrayList;
import java.util.List;
import Game.Equip.BEquipExtra;
import Zeze.Collections.CsQueue;
import demo.App;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestCsQueue {
	@BeforeEach
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	private static List<Integer> walk(CsQueue<BEquipExtra> csq) throws Exception {
		//System.out.println(csq.getInnerName());
		var out = new ArrayList<Integer>();
		csq.walk((k, v) -> {
			//System.out.println(k.getName() + " " + v.getAttack());
			out.add(v.getAttack());
			return true;
		});
		return out;
	}

	private static void clear(CsQueue<BEquipExtra> csq) {
		Assertions.assertEquals(0, App.getInstance().Zeze.newProcedure(() -> {
			while (csq.poll() != null) {
				// nothing.
			}
			return 0;
		}, "csq0.clear").call());
	}

	// Queue.size()走TableX.get，必须在事务内调用。
	private static long size(CsQueue<BEquipExtra> csq) {
		var out = new Zeze.Util.OutLong();
		App.getInstance().Zeze.newProcedure(() -> {
			out.value = csq.size();
			return 0;
		}, "csq.size").call();
		return out.value;
	}

	@Test
	public void testCsQueue() throws Exception {
		var qm = demo.App.getInstance().Zeze.getQueueModule();
		var csq0 = new CsQueue<>(qm, "TestCsQueue", 0, BEquipExtra.class, 100);
		// clear
		clear(csq0);

		demo.App.getInstance().Zeze.newProcedure(() -> {
			csq0.add(new BEquipExtra(0, 0, 0));
			csq0.add(new BEquipExtra(1, 1, 1));
			csq0.add(new BEquipExtra(2, 2, 2));
			return 0;
		}, "csq0.add").call();

		Assertions.assertEquals(List.of(0, 1, 2), walk(csq0));

		var csq1 = new CsQueue<>(qm, "TestCsQueue", 1, BEquipExtra.class, 100);
		clear(csq1);
		demo.App.getInstance().Zeze.newProcedure(() -> {
			csq1.add(new BEquipExtra(3, 3, 3));
			csq1.add(new BEquipExtra(4, 4, 4));
			csq1.add(new BEquipExtra(5, 5, 5));
			return 0;
		}, "csq1.add").call();
		Assertions.assertEquals(List.of(3, 4, 5), walk(csq1));

		csq0.splice(1, csq0.getLoadSerialNo());
		Assertions.assertEquals(List.of(), walk(csq1));
		Assertions.assertEquals(List.of(3, 4, 5, 0, 1, 2), walk(csq0));
		Assertions.assertEquals(6, size(csq0)); // splice需要合并count
	}

	@Test
	public void testCsQueueSpliceEmpty() throws Exception {
		// 接管到空队列后继续add：新数据必须可达（修复前：dst.tail未被设置，新节点成为孤岛，poll/walk永远看不到）。
		var qm = demo.App.getInstance().Zeze.getQueueModule();
		var csq0 = new CsQueue<>(qm, "TestCsQueueSpliceEmpty", 0, BEquipExtra.class, 100);
		clear(csq0); // dst 保持为空队列

		var csq1 = new CsQueue<>(qm, "TestCsQueueSpliceEmpty", 1, BEquipExtra.class, 100);
		clear(csq1);
		demo.App.getInstance().Zeze.newProcedure(() -> {
			csq1.add(new BEquipExtra(3, 3, 3));
			csq1.add(new BEquipExtra(4, 4, 4));
			return 0;
		}, "csq1.add").call();

		csq0.splice(1, csq0.getLoadSerialNo());
		Assertions.assertEquals(List.of(), walk(csq1));

		demo.App.getInstance().Zeze.newProcedure(() -> {
			csq0.add(new BEquipExtra(9, 9, 9));
			return 0;
		}, "csq0.addAfterSplice").call();

		Assertions.assertEquals(List.of(3, 4, 9), walk(csq0));
		Assertions.assertEquals(3, size(csq0));
	}
}
