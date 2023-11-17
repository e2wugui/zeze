package UnitTest.Zeze.Collections;

import java.util.ArrayList;
import Game.Equip.BEquipExtra;
import Zeze.Collections.CsQueue;
import demo.App;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestCsQueue {

	@Before
	public final void testInit() throws Exception {
		demo.App.getInstance().Start();
	}

	private static java.util.List<Integer> walk(CsQueue<BEquipExtra> csq) {
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
		App.getInstance().Zeze.newProcedure(() -> {
			while (csq.poll() != null) {
				// nothing.
			}
			return 0;
		}, "csq0.clear");
	}

	@Test
	public void testCsQueue() {
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

		Assert.assertEquals(java.util.List.of(0, 1, 2), walk(csq0));

		var csq1 = new CsQueue<>(qm, "TestCsQueue", 1, BEquipExtra.class, 100);
		clear(csq1);
		demo.App.getInstance().Zeze.newProcedure(() -> {
			csq1.add(new BEquipExtra(3, 3, 3));
			csq1.add(new BEquipExtra(4, 4, 4));
			csq1.add(new BEquipExtra(5, 5, 5));
			return 0;
		}, "csq1.add").call();
		Assert.assertEquals(java.util.List.of(3, 4, 5), walk(csq1));

		csq0.splice(1, csq0.getLoadSerialNo());
		Assert.assertEquals(java.util.List.of(), walk(csq1));
		Assert.assertEquals(java.util.List.of(3, 4, 5, 0, 1, 2), walk(csq0));
	}

}
