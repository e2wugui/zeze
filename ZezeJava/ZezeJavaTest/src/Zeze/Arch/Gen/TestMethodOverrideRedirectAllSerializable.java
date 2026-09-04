package Zeze.Arch.Gen;

import harness.Fast;
import java.lang.reflect.Method;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectResult;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// FND2-A1-1：RedirectAll+Serializable结果类型必须在MethodOverride构造器即fail-fast，
// 不再放行到生成代码——All路径对Serializable结果不收集字段（resultFields为空），
// 接收端不编码、发起端不解码，分组结果会静默变成空对象且无任何诊断。
@Fast
public class TestMethodOverrideRedirectAllSerializable {

	/** 手写Serializable结果类：修复前正是触发静默数据丢失的形态。 */
	public static class SerializableResult extends RedirectResult implements Zeze.Serialize.Serializable {
		public long count;

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteLong(count);
		}

		@Override
		public void decode(IByteBuffer bb) {
			count = bb.ReadLong();
		}
	}

	/** 常规public字段结果类：标准路径，必须继续可用。 */
	public static class NormalResult extends RedirectResult {
		public long count;
	}

	public static class TestModule {
		@RedirectAll
		public RedirectAllFuture<SerializableResult> bad(int hash) {
			throw new UnsupportedOperationException();
		}

		@RedirectAll
		public RedirectAllFuture<NormalResult> good(int hash) {
			throw new UnsupportedOperationException();
		}
	}

	@Test
	public void testSerializableResultRejected() throws Exception {
		Method bad = TestModule.class.getMethod("bad", int.class);
		var ex = assertThrows(IllegalStateException.class,
				() -> new MethodOverride(bad, bad.getAnnotation(RedirectAll.class)));
		assertTrue(ex.getMessage().contains("Serializable"), ex.getMessage());
	}

	@Test
	public void testNormalResultAccepted() throws Exception {
		Method good = TestModule.class.getMethod("good", int.class);
		var mo = new MethodOverride(good, good.getAnnotation(RedirectAll.class));
		assertEquals(1, mo.resultFields.size()); // 非Serializable结果的public字段正常收集
	}
}
