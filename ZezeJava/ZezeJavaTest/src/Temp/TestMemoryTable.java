package Temp;

import java.lang.ref.SoftReference;
import Zeze.Util.OutInt;
import Zeze.Util.Task;
import demo.App;
import demo.Bean1;
import demo.Module2.BValue;

public class TestMemoryTable {
	public static void main(String[] args) throws Exception {
		App.Instance.Start();

		var ref = new SoftReference<>(new OutInt(1234));

		Task.call(App.Instance.Zeze.newProcedure(() -> {
			App.Instance.demo_Module1.tMemorySize().put(123L, new Bean1(1));
			App.Instance.demo_Module1.getTable5().put(123L, new BValue(1));
			return 0L;
		}, "put"));

		//App.Instance.Zeze.checkpointRun();

		try {
			var obj = new long[2_000_000_000];
		} catch (OutOfMemoryError err) {
			System.out.println("OutOfMemoryError");
		}

		System.out.println(ref.get());

		Task.call(App.Instance.Zeze.newProcedure(() -> {
			System.out.println(App.Instance.demo_Module1.tMemorySize().get(123L));
			System.out.println(App.Instance.demo_Module1.getTable5().get(123L));
			return 0L;
		}, "get"));
	}
}
