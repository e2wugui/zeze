package Zeze.Arch.Gen;

import java.lang.reflect.Method;

import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectHash;
import Zeze.Arch.RedirectKey;
import Zeze.Arch.RedirectResult;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AG1-F2/AG1-F4回归（MethodOverride生成期校验）：
 * <ul>
 * <li>AG1-F2：redirect返回类型校验缺口——All配Future、Hash/ToServer配AllFuture、
 * raw泛型、TypeVariable/通配符实参原先生成非法源码（RedirectFuture&lt;null&gt;）或
 * 反射CCE，模块创建期晦涩崩溃。修复：配对校验+instanceof守卫，全部带方法名fail-fast。</li>
 * <li>AG1-F4：@RedirectKey作用于数组类型参数生成name.hashCode()（身份哈希），
 * 内容相同的key落入不同oneByOne串行队列。修复：生成期直接IllegalStateException。</li>
 * </ul>
 */
@Fast
public class TestAg1F2F4RedirectGenValidation {

	public static class NormalResult extends RedirectResult {
		public long count;
	}

	// ---- AG1-F2：注解种类与future种类配对 ----

	public static class PairModule {
		@RedirectAll
		public RedirectFuture<NormalResult> allWithFuture(int hash) {
			throw new UnsupportedOperationException();
		}

		@RedirectHash
		public RedirectAllFuture<NormalResult> hashWithAllFuture(int hash) {
			throw new UnsupportedOperationException();
		}

		@RedirectAll
		public RedirectAllFuture<NormalResult> allOk(int hash) {
			throw new UnsupportedOperationException();
		}

		@RedirectHash
		public RedirectFuture<NormalResult> hashOk(int hash) {
			throw new UnsupportedOperationException();
		}
	}

	@Test
	public void testFutureKindMustPairWithAnnotationKind() throws Exception {
		var ex = assertThrows(IllegalStateException.class,
				() -> override(PairModule.class, "allWithFuture", int.class));
		assertTrue(ex.getMessage().contains("allWithFuture"), "错误必须带方法名: " + ex.getMessage());

		ex = assertThrows(IllegalStateException.class,
				() -> override(PairModule.class, "hashWithAllFuture", int.class));
		assertTrue(ex.getMessage().contains("hashWithAllFuture"), "错误必须带方法名: " + ex.getMessage());

		assertDoesNotThrow(() -> override(PairModule.class, "allOk", int.class));
		assertDoesNotThrow(() -> override(PairModule.class, "hashOk", int.class));
	}

	// ---- AG1-F2：raw泛型返回 ----

	public static class RawModule {
		@SuppressWarnings("rawtypes")
		@RedirectHash
		public RedirectFuture rawFuture(int hash) {
			throw new UnsupportedOperationException();
		}
	}

	@Test
	public void testRawFutureRejected() throws Exception {
		var ex = assertThrows(IllegalStateException.class,
				() -> override(RawModule.class, "rawFuture", int.class));
		assertTrue(ex.getMessage().contains("raw"), "raw泛型必须被拒绝: " + ex.getMessage());
	}

	// ---- AG1-F2：TypeVariable/通配符实参 ----

	public static class GenericModule<T> {
		@RedirectHash
		public RedirectFuture<T> typeVarFuture(int hash) {
			throw new UnsupportedOperationException();
		}

		@RedirectHash
		public RedirectFuture<?> wildcardFuture(int hash) {
			throw new UnsupportedOperationException();
		}
	}

	@Test
	public void testTypeVariableAndWildcardRejected() throws Exception {
		var ex = assertThrows(IllegalStateException.class,
				() -> override(GenericModule.class, "typeVarFuture", int.class));
		assertTrue(ex.getMessage().contains("typeVarFuture"), "错误必须带方法名: " + ex.getMessage());

		ex = assertThrows(IllegalStateException.class,
				() -> override(GenericModule.class, "wildcardFuture", int.class));
		assertTrue(ex.getMessage().contains("wildcardFuture"), "错误必须带方法名: " + ex.getMessage());
	}

	// ---- AG1-F4：数组@RedirectKey ----

	public static class ArrayKeyModule {
		@RedirectHash
		public void arrayKey(int hash, @RedirectKey byte[] key) {
			throw new UnsupportedOperationException();
		}

		@RedirectHash
		public void binaryKeyOk(int hash, @RedirectKey Zeze.Net.Binary key) {
			throw new UnsupportedOperationException();
		}

		@RedirectHash
		public void intKeyOk(int hash, @RedirectKey int key) {
			throw new UnsupportedOperationException();
		}
	}

	@Test
	public void testArrayRedirectKeyRejected() throws Exception {
		var ex = assertThrows(IllegalStateException.class,
				() -> override(ArrayKeyModule.class, "arrayKey", int.class, byte[].class));
		assertTrue(ex.getMessage().contains("array"), "数组key必须被拒绝: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("Binary"), "报错须提示改用Binary: " + ex.getMessage());

		assertDoesNotThrow(() -> override(ArrayKeyModule.class, "binaryKeyOk", int.class, Zeze.Net.Binary.class));
		assertDoesNotThrow(() -> override(ArrayKeyModule.class, "intKeyOk", int.class, int.class));
	}

	private static MethodOverride override(Class<?> cls, String methodName, Class<?>... paramTypes)
			throws Exception {
		Method method = cls.getMethod(methodName, paramTypes);
		return new MethodOverride(method, method.getAnnotations()[0]);
	}
}
