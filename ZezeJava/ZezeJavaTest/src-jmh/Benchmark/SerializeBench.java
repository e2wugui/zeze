package Benchmark;

import Zeze.Serialize.ByteBuffer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

// 序列化微基准：demo.Bean1（int + PMap1[int,int]×8）与 demo.web.BMap2Bean1（PMap2[int,Bean1]×10，
// 每项 Bean1 含 V2×4）的 encode/decode/roundTrip。bean 形状对齐 D 宏观场景。
// encode 每次新分配 buffer（对齐 checkpoint 真实路径的分配成本）；decode 复用 @State 里
// 预编码的 buffer，仅回退 ReadIndex。
@State(Scope.Benchmark)
@org.openjdk.jmh.annotations.BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SerializeBench {
	private demo.Bean1 bean1;
	private demo.web.BMap2Bean1 map2Bean;
	private ByteBuffer bufBean1;
	private ByteBuffer bufMap2Bean;

	@Setup(Level.Trial)
	public void setup() {
		bean1 = new demo.Bean1();
		bean1.setV1(42);
		for (int i = 0; i < 8; ++i)
			bean1.getV2().put(1000 + i, i);
		map2Bean = new demo.web.BMap2Bean1();
		for (int k = 0; k < 10; ++k)
			map2Bean.getMap2().put(k, newBean1(k));
		bufBean1 = ByteBuffer.Allocate(1024);
		bean1.encode(bufBean1);
		bufMap2Bean = ByteBuffer.Allocate(4096);
		map2Bean.encode(bufMap2Bean);
	}

	private static demo.Bean1 newBean1(int seed) {
		var b = new demo.Bean1();
		b.setV1(seed);
		for (int j = 0; j < 4; ++j)
			b.getV2().put(j, seed + j);
		return b;
	}

	@Benchmark
	public void encodeBean1(Blackhole bh) {
		var bb = ByteBuffer.Allocate(1024);
		bean1.encode(bb);
		bh.consume(bb);
	}

	@Benchmark
	public void decodeBean1(Blackhole bh) {
		var b = new demo.Bean1();
		bufBean1.ReadIndex = 0;
		b.decode(bufBean1);
		bh.consume(b);
	}

	@Benchmark
	public void roundTripBean1(Blackhole bh) {
		var bb = ByteBuffer.Allocate(1024);
		bean1.encode(bb);
		var b = new demo.Bean1();
		bb.ReadIndex = 0;
		b.decode(bb);
		bh.consume(b);
	}

	@Benchmark
	public void encodeMap2Bean(Blackhole bh) {
		var bb = ByteBuffer.Allocate(4096);
		map2Bean.encode(bb);
		bh.consume(bb);
	}

	@Benchmark
	public void decodeMap2Bean(Blackhole bh) {
		var b = new demo.web.BMap2Bean1();
		bufMap2Bean.ReadIndex = 0;
		b.decode(bufMap2Bean);
		bh.consume(b);
	}
}
