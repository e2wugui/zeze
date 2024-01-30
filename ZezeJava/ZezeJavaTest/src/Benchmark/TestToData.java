package Benchmark;

import Zeze.Serialize.ByteBuffer;
import demo.Module1.BValue;
import org.junit.Test;

public class TestToData {

	@Test
	public void testToData() {
		{
			System.out.println("BValue PMap");
			var bValue = new BValue();
			for (long i = 0; i < 100; ++i) {
				bValue.getMap15().put(i, i);
			}
			testCodec(bValue);
		}
		{
			System.out.println("BValueData PMap");
			var bValueData = new BValue.Data();
			for (long i = 0; i < 100; ++i) {
				bValueData.getMap15().put(i, i);
			}
			testCodec(bValueData);
		}
		{
			System.out.println("BValue PList");
			var bValue = new BValue();
			for (long i = 0; i < 100; ++i) {
				bValue.getArray29().add((float)i);
			}
			testCodec(bValue);
		}
		{
			System.out.println("BValueData PList");
			var bValueData = new BValue.Data();
			for (long i = 0; i < 100; ++i) {
				bValueData.getArray29().add((float)i);
			}
			testCodec(bValueData);
		}
		{
			System.out.println("BValue PSet");
			var bValue = new BValue();
			for (int i = 0; i < 100; ++i) {
				bValue.getSet10().add(i);
			}
			testCodec(bValue);
		}
		{
			System.out.println("BValueData PSet");
			var bValueData = new BValue.Data();
			for (int i = 0; i < 100; ++i) {
				bValueData.getSet10().add(i);
			}
			testCodec(bValueData);
		}
	}

	public static void testCodec(BValue bValue) {
		long sum = 0;
		// encode
		var b = new Zeze.Util.Benchmark();
		for (var i = 0; i < 3_0000; ++i) {
			var bb = ByteBuffer.Allocate();
			bValue.encode(bb);
			sum += bb.size();
		}
		var seconds = b.report("encode", 3_0000);
		System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s");

		// decode
		var b2 = new Zeze.Util.Benchmark();
		var encoded = ByteBuffer.Allocate();
		bValue.encode(encoded);
		var dummy = 0;
		for (var i = 0; i < 3_0000; ++i) {
			var bb = ByteBuffer.Wrap(encoded.Bytes, encoded.ReadIndex, encoded.size());
			var value = new BValue();
			value.decode(bb);
			dummy += value.getArray29().size() + value.getMap15().size() + value.getSet10().size();
		}
		seconds = b2.report("decode", 3_0000);
		System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s");
		System.out.println("dummy=" + dummy);
	}

	public static void testCodec(BValue.Data bValueData) {
		long sum = 0;
		var count = 5000;
		var b = new Zeze.Util.Benchmark();
		for (var i = 0; i < count; ++i) {
			var bb = ByteBuffer.Allocate();
			bValueData.encode(bb);
			sum += bb.size();
		}
		var seconds = b.report("encode", count);
		System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s");

		// decode
		var count2 = 5000;
		var b2 = new Zeze.Util.Benchmark();
		var encoded = ByteBuffer.Allocate();
		bValueData.encode(encoded);
		var dummy = 0;
		for (var i = 0; i < count2; ++i) {
			var bb = ByteBuffer.Wrap(encoded.Bytes, encoded.ReadIndex, encoded.size());
			var value = new BValue();
			value.decode(bb);
			dummy += value.getArray29().size() + value.getMap15().size() + value.getSet10().size();
		}
		seconds = b2.report("decode", count2);
		System.out.println("sum=" + sum + " bytes; speed=" + sum / seconds / 1024 / 1024 + "M/s");
		System.out.println("dummy=" + dummy);
	}

}
