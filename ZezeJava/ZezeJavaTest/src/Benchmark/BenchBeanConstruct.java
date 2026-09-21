package Benchmark;

import demo.Module1.BValue;
import harness.Bench;
import org.junit.jupiter.api.Test;
import Zeze.Transaction.Collections.Map2Meta;
import Zeze.Transaction.Collections.PMap2;

/**
 * bean 构造热路径 meta 获取对比（调用点静态化/生成代码常量化）：
 * 1. bvalue-new：78 个集合字段的生成 bean 构造风暴（端到端，regen 前走 Class 构造器，
 *    regen 后走静态 meta）；
 * 2. pmap2-class-ctor：旧路径代理，每次构造走 meta 工厂（check + 双层 CHM）；
 * 3. pmap2-meta-ctor：新路径代理，getstatic 静态常量 + (Meta) 构造器。
 */
@Bench
@SuppressWarnings("unused")
public class BenchBeanConstruct {
	private static final int totalCount = 100_0000;

	// 对齐生成代码常量化的静态形态（与 BValue._map11 同一 (K,V) 元组，共享工厂缓存实例）。
	private static final Map2Meta<Long, demo.Module2.BValue> meta2Proxy
			= Map2Meta.get(Long.class, demo.Module2.BValue.class);

	// 防止 JIT 对未逃逸对象做标量替换/死码消除，三列同付一次写，保持公平。
	private static volatile Object last;

	@Test
	public void testBvalueNew() {
		var b = new Zeze.Util.Benchmark();
		for (int i = 0; i < totalCount; i++) {
			last = new BValue();
		}
		b.report(getClass().getName() + " bvalue-new", totalCount);
	}

	@Test
	public void testPmap2ClassCtor() {
		var b = new Zeze.Util.Benchmark();
		for (int i = 0; i < totalCount; i++) {
			last = new PMap2<>(Long.class, demo.Module2.BValue.class);
		}
		b.report(getClass().getName() + " pmap2-class-ctor", totalCount);
	}

	@Test
	public void testPmap2MetaCtor() {
		var b = new Zeze.Util.Benchmark();
		for (int i = 0; i < totalCount; i++) {
			last = new PMap2<>(meta2Proxy);
		}
		b.report(getClass().getName() + " pmap2-meta-ctor", totalCount);
	}
}
