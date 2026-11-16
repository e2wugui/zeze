package Zeze.Arch.Gen;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import Zeze.Arch.RedirectResult;
import Zeze.Builtin.Collections.Queue.BQueueNodeKey;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.InMemoryJavaCompiler;
import Zeze.Util.StringBuilderCs;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRedirectContainerReplacement {
	public static class Result extends RedirectResult {
		public List<Long> values = new LinkedList<>(List.of(999L));
		public Map<String, Long> mapping = new LinkedHashMap<>(Map.of("default", 999L));
		public List<BQueueNodeKey> keys = new LinkedList<>();
		public Map<BQueueNodeKey, BQueueNodeKey> keyMapping = new LinkedHashMap<>();
	}

	@Test
	public void generatedDecodeReplacesConstructorContentsAndPreservesTheImplementation() throws Exception {
		var fields = List.of(Result.class.getField("values"), Result.class.getField("mapping"),
				Result.class.getField("keys"), Result.class.getField("keyMapping"));
		var encode = new StringBuilderCs();
		var decode = new StringBuilderCs();
		Gen.instance.genEncode(encode, "", "bb", "mask", "r.", fields, null);
		Gen.instance.genDecode(decode, "", "bb", "mask", "r.", fields);
		var resultType = Result.class.getCanonicalName();
		var className = "Zeze.Arch.Gen.ContainerReplacementCodec";
		var source = "package Zeze.Arch.Gen; import Zeze.Serialize.ByteBuffer; public class ContainerReplacementCodec {"
				+ "public static ByteBuffer encode(" + resultType + " r) {var bb=ByteBuffer.Allocate();"
				+ encode + "return bb;} public static void decode(ByteBuffer bb," + resultType + " r) {"
				+ decode + "}}";
		var classpath = new File(ByteBuffer.class.getProtectionDomain().getCodeSource().getLocation().toURI())
				+ File.pathSeparator + new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
				+ File.pathSeparator + System.getProperty("java.class.path");
		var compiler = new InMemoryJavaCompiler(getClass().getClassLoader()).ignoreWarnings()
				.useOptions("-classpath", classpath);
		compiler.compileAllToByteCode(Map.of(className, source));
		var codec = compiler.defineCompiled(className);
		var encodeMethod = codec.getMethod("encode", Result.class);
		var decodeMethod = codec.getMethod("decode", ByteBuffer.class, Result.class);
		var sender = new Result();
		sender.values.clear();
		sender.values.add(7L);
		sender.mapping.clear();
		sender.mapping.put("wire", 8L);
		var key = new BQueueNodeKey("queue", 42);
		var value = new BQueueNodeKey("queue", 43);
		sender.keys.add(key);
		sender.keyMapping.put(key, value);
		var receiver = new Result();
		var originalList = receiver.values;
		var originalMap = receiver.mapping;
		decodeMethod.invoke(null, encodeMethod.invoke(null, sender), receiver);
		assertEquals(sender.values, receiver.values);
		assertEquals(sender.mapping, receiver.mapping);
		assertEquals(sender.keys, receiver.keys);
		assertEquals(sender.keyMapping, receiver.keyMapping);
		assertNotSame(key, receiver.keys.getFirst());
		assertSame(originalList, receiver.values);
		assertSame(originalMap, receiver.mapping);

		sender.values.clear();
		sender.mapping.clear();
		sender.keys.clear();
		sender.keyMapping.clear();
		decodeMethod.invoke(null, encodeMethod.invoke(null, sender), receiver);
		assertTrue(receiver.values.isEmpty());
		assertTrue(receiver.mapping.isEmpty());
		assertTrue(receiver.keys.isEmpty());
		assertTrue(receiver.keyMapping.isEmpty());
	}
}
