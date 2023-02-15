package Benchmark;

import java.util.ArrayList;
import Zeze.Transaction.Collections.PMap1;
import Zeze.Util.Benchmark;
import org.junit.Test;

public class PMapLogTypeIdHash32Cache {
	public int size;
	@Test
	public void testCreatePMap() {
		var count = 1000_0000;
		var b = new Benchmark();
		var maps = new ArrayList<PMap1<String, String>>();
		for (int i = 0; i < count; ++i) {
			maps.add(new PMap1<>(String.class, String.class));
		}
		b.report("create PMap2", count);
		System.out.println("<-- create PMap2 tasks/s=5129358.05 time=1.95s cpu=1.27s concurrent=0.65");
		size = maps.size();
	}
}
