package Zeze.Arch.Gen;

import java.io.File;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.RedirectToServer;
import Zeze.Arch.RedirectFuture;
import Zeze.Util.InMemoryJavaCompiler;
import Zeze.Util.StringBuilderCs;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRedirectNullability {

	public static class Module {
		@RedirectToServer
		public void send(int serverId, @Nullable String text, List<@Nullable String> values,
				byte @Nullable [] data, @Nullable String[] elements, Map.@Nullable Entry<String, Long> entry) {
		}

		@RedirectToServer
		public RedirectFuture<@Nullable Long> fetch(int serverId) {
			return new RedirectFuture<>();
		}

		@RedirectToServer
		public void names(int serverId, String account, @Nullable String instanceName) {
		}
	}

	@TempDir
	Path tempDir;

	@Test
	public void generatedParameterTypesRetainNullableAtItsOriginalLocation() throws Exception {
		var method = Module.class.getMethod("send", int.class, String.class, List.class,
				byte[].class, String[].class, Map.Entry.class);
		var override = new MethodOverride(method, method.getAnnotation(RedirectToServer.class));
		var className = "Zeze.Arch.Gen.NullableParameters";
		var source = "package Zeze.Arch.Gen; import org.jspecify.annotations.Nullable; "
				+ "public class NullableParameters { public void send(" + override.getDefineString() + ") {} }";
		var classpath = new File(Nullable.class.getProtectionDomain().getCodeSource().getLocation().toURI())
				+ File.pathSeparator + System.getProperty("java.class.path");
		var compiler = new InMemoryJavaCompiler(getClass().getClassLoader()).ignoreWarnings()
				.useOptions("-classpath", classpath);
		compiler.compileAllToByteCode(Map.of(className, source));
		var generated = compiler.defineCompiled(className).getMethod("send", method.getParameterTypes());
		var types = generated.getAnnotatedParameterTypes();
		assertFalse(types[0].isAnnotationPresent(Nullable.class));
		assertTrue(types[1].isAnnotationPresent(Nullable.class));
		assertFalse(types[2].isAnnotationPresent(Nullable.class));
		assertTrue(((AnnotatedParameterizedType)types[2]).getAnnotatedActualTypeArguments()[0]
				.isAnnotationPresent(Nullable.class));
		assertTrue(types[3].isAnnotationPresent(Nullable.class));
		assertFalse(((AnnotatedArrayType)types[3]).getAnnotatedGenericComponentType()
				.isAnnotationPresent(Nullable.class));
		assertFalse(types[4].isAnnotationPresent(Nullable.class));
		assertTrue(((AnnotatedArrayType)types[4]).getAnnotatedGenericComponentType()
				.isAnnotationPresent(Nullable.class));
		assertTrue(types[5].isAnnotationPresent(Nullable.class));
	}

	@Test
	public void generatedFutureRetainsItsNullableResultType() throws Exception {
		var method = Module.class.getMethod("fetch", int.class);
		var override = new MethodOverride(method, method.getAnnotation(RedirectToServer.class));
		var className = "Zeze.Arch.Gen.NullableFuture";
		var source = "package Zeze.Arch.Gen; import org.jspecify.annotations.Nullable; "
				+ "public class NullableFuture { public " + Gen.instance.getTypeName(method.getAnnotatedReturnType())
				+ " fetch(int serverId) { return new Zeze.Arch.RedirectFuture<" + override.resultTypeName + ">(); }}";
		var classpath = new File(Nullable.class.getProtectionDomain().getCodeSource().getLocation().toURI())
				+ File.pathSeparator + new File(RedirectFuture.class.getProtectionDomain().getCodeSource().getLocation().toURI())
				+ File.pathSeparator + System.getProperty("java.class.path");
		var compiler = new InMemoryJavaCompiler(getClass().getClassLoader()).ignoreWarnings()
				.useOptions("-classpath", classpath);
		compiler.compileAllToByteCode(Map.of(className, source));
		var generated = compiler.defineCompiled(className).getMethod("fetch", int.class);
		var returnType = (AnnotatedParameterizedType)generated.getAnnotatedReturnType();
		assertTrue(returnType.getAnnotatedActualTypeArguments()[0].isAnnotationPresent(Nullable.class));
		assertEquals("@Nullable Long", override.resultTypeName);
	}

	@Test
	public void nonNullParametersRejectNullOnTheWireAndNullableParametersStillRoundTrip() throws Exception {
		var method = Module.class.getMethod("names", int.class, String.class, String.class);
		var parameters = List.of(method.getParameters()).subList(1, 3);
		var requiredName = parameters.getFirst().getName();
		var optionalName = parameters.getLast().getName();
		var encode = new StringBuilderCs();
		var decode = new StringBuilderCs();
		Gen.instance.genEncode(encode, "", "bb", "mask", "", parameters, null);
		Gen.instance.genDecode(decode, "", "bb", "mask", "", parameters);
		assertFalse(encode.toString().contains(requiredName + " == null"));
		assertFalse(encode.toString().contains(requiredName + " != null"));
		var className = "Zeze.Arch.Gen.NullableParameterCodec";
		var source = "package Zeze.Arch.Gen; import Zeze.Serialize.ByteBuffer; "
				+ "public class NullableParameterCodec {"
				+ "public static ByteBuffer encode(String " + requiredName + ", String " + optionalName + ") {"
				+ "var bb = ByteBuffer.Allocate();" + encode + "return bb;}"
				+ "public static String[] decode(ByteBuffer bb) {String " + requiredName + "; String " + optionalName + ";"
				+ decode + "return new String[]{" + requiredName + ", " + optionalName + "};}}";
		var classpath = new File(ByteBuffer.class.getProtectionDomain().getCodeSource().getLocation().toURI())
				+ File.pathSeparator + System.getProperty("java.class.path");
		var compiler = new InMemoryJavaCompiler(getClass().getClassLoader()).ignoreWarnings()
				.useOptions("-classpath", classpath);
		compiler.compileAllToByteCode(Map.of(className, source));
		var codec = compiler.defineCompiled(className);
		var encodeMethod = codec.getMethod("encode", String.class, String.class);
		var decodeMethod = codec.getMethod("decode", ByteBuffer.class);
		for (var instanceName : new @Nullable String[]{null, "game"}) {
			var encoded = (ByteBuffer)encodeMethod.invoke(null, "account", instanceName);
			var expected = ByteBuffer.Allocate();
			expected.WriteULong(instanceName == null ? 2 : 0);
			expected.WriteString("account");
			if (instanceName != null)
				expected.WriteString(instanceName);
			assertArrayEquals(expected.Copy(), encoded.Copy());
			assertArrayEquals(new @Nullable String[]{"account", instanceName}, (String[])decodeMethod.invoke(null, encoded));
		}
		var invalid = ByteBuffer.Allocate();
		invalid.WriteULong(1);
		var failure = assertThrows(InvocationTargetException.class, () -> decodeMethod.invoke(null, invalid));
		assertInstanceOf(IllegalArgumentException.class, failure.getCause());
	}

	@Test
	public void builtInOnlineRedirectRetainsItsNullableParameterAndNullMarkedScope() throws Exception {
		var app = new AppBase() {
			@Override
			public @Nullable Application getZeze() {
				return null;
			}
		};
		GenModule.instance.generateRedirectSources(tempDir.toString(), app,
				new Class<?>[]{Zeze.Game.Online.class}, false);
		var source = Files.readString(tempDir.resolve("Redirect_Zeze_Game_Online.java"));
		assertTrue(source.contains("@NullMarked"));
		assertTrue(source.contains("redirectRemoveLocal(int serverId, long roleId, @Nullable String instanceName)"));
		assertTrue(source.contains("setServiceNamePrefix(java.util.Objects.requireNonNull("));
	}
}
