package Zeze.Arch.Gen;

import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.RedirectBase;
import Zeze.Arch.RedirectException;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectHash;
import Zeze.Arch.RedirectToServer;
import Zeze.Builtin.ProviderDirect.ModuleRedirect;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.InMemoryJavaCompiler;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import static org.junit.jupiter.api.Assertions.*;

@Fast
@Isolated // 生成器为进程级编译器，本用例生成独有类。
public class TestRedirectDecodeFailure implements IModule {
	public static final int ModuleId = 11990;
	public static final String ModuleFullName = "RedirectDecodeFailure";

	@Override public String getFullName() { return ModuleFullName; }
	@Override public String getName() { return ModuleFullName; }
	@Override public int getId() { return ModuleId; }

	@RedirectToServer
	public RedirectFuture<Long> readNumber(int serverId) {
		return RedirectFuture.finish(7L);
	}

	@RedirectHash
	public RedirectFuture<Long> readByHash(int hash) {
		return RedirectFuture.finish(11L);
	}

	@RedirectToServer
	public RedirectFuture<Long> readWithBean(int serverId, EmptyBean payload) {
		return RedirectFuture.finish(13L);
	}

	@TempDir
	Path directory;

	private static final class CaptureSocket extends AsyncSocket {
		long rpcSession;

		CaptureSocket(Service service) { super(service); }
		@Override public Type getType() { return Type.eClient; }
		@Override protected void doClose(@Nullable Throwable ex, boolean gracefully) { }
		@Override public @Nullable TimeThrottle getTimeThrottle() { return null; }
		@Override public @Nullable SocketAddress getRemoteAddress() { return null; }
		@Override public boolean Send(byte[] bytes, int offset, int length) {
			var input = ByteBuffer.Wrap(bytes, offset + 12, length - 12);
			var request = new ModuleRedirect();
			request.decode(input);
			rpcSession = request.getSessionId();
			return true;
		}
	}

	@Test
	void malformedRemoteResultCompletesTheGeneratedFutureExceptionally() throws Exception {
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		var app = new Application("RedirectDecodeFailure", config);
		var service = new Service("RedirectDecodeFailure.transport", config);
		var socket = new CaptureSocket(service);
		try {
			var provider = new ProviderApp(app);
			var prefix = ProviderApp.class.getDeclaredField("serverServiceNamePrefix");
			prefix.setAccessible(true);
			prefix.set(provider, "test");
			app.redirect = new RedirectBase(provider) {
				@Override
				public AsyncSocket choiceServer(IModule module, int serverId, boolean orOtherServer) {
					return socket;
				}
			};
			var appBase = new AppBase() {
				@Override public Application getZeze() { return app; }
			};
			GenModule.instance.generateRedirectSources(directory.toString(), appBase,
					new Class<?>[]{TestRedirectDecodeFailure.class}, false);
			var name = GenModule.REDIRECT_PREFIX + getClass().getName().replace('.', '_');
			var source = Files.readString(directory.resolve(name + ".java"));
			var compiler = new InMemoryJavaCompiler(getClass().getClassLoader()).ignoreWarnings()
					.useOptions("-classpath", System.getProperty("java.class.path"));
			compiler.compileAllToByteCode(Map.of(name, source));
			var module = (TestRedirectDecodeFailure)compiler.defineCompiled(name)
					.getConstructor(AppBase.class).newInstance(appBase);
			var future = module.readNumber(1);
			assertFalse(future.isDone());
			// 应答层成功并消费上下文，内部Long载荷截断；超时已无法再次完成此future。
			var request = (ModuleRedirect)Objects.requireNonNull(service.removeRpcContext(socket.rpcSession));
			assertDoesNotThrow(() -> Objects.requireNonNull(request.getResponseHandle()).handle(request));
			assertTrue(future.isCompletedExceptionally());
			assertThrows(CompletionException.class, future::get);

			// future 已完成后的用户回调异常仍交给派发层，不能被解码保护吞掉。
			var successFailure = new IllegalStateException("success callback");
			var success = module.readNumber(1).onSuccess(value -> { throw successFailure; });
			var successRequest = (ModuleRedirect)Objects.requireNonNull(service.removeRpcContext(socket.rpcSession));
			var encoded = ByteBuffer.Allocate();
			encoded.WriteLong(7);
			successRequest.Result.setParams(new Binary(encoded));
			assertSame(successFailure, assertThrows(IllegalStateException.class,
					() -> Objects.requireNonNull(successRequest.getResponseHandle()).handle(successRequest)));
			assertEquals(7L, success.get());

			var timeoutFailure = new IllegalStateException("timeout callback");
			var timedOut = module.readNumber(1).onFail(error -> { throw timeoutFailure; });
			var timeoutRequest = (ModuleRedirect)Objects.requireNonNull(service.removeRpcContext(socket.rpcSession));
			timeoutRequest.setIsTimeout(true);
			assertSame(timeoutFailure, assertThrows(IllegalStateException.class,
					() -> Objects.requireNonNull(timeoutRequest.getResponseHandle()).handle(timeoutRequest)));
			assertTrue(timedOut.isCompletedExceptionally());
		} finally {
			socket.close();
			service.stop();
			app.stop();
		}
	}

	@Test
	void routingFailurePreservesServerNotFoundWhileEncodeFailureRemainsLocalExecution() throws Exception {
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		var app = new Application("RedirectExceptionCodePreservation", config);
		var service = new Service("RedirectExceptionCodePreservation.transport", config);
		var socket = new CaptureSocket(service);
		try {
			var provider = new ProviderApp(app);
			var prefix = ProviderApp.class.getDeclaredField("serverServiceNamePrefix");
			prefix.setAccessible(true);
			prefix.set(provider, "test");
			var serverMissing = new RedirectException(RedirectException.SERVER_NOT_FOUND, "missing target server");
			var hashMissing = new RedirectException(RedirectException.SERVER_NOT_FOUND, "missing hash target");
			app.redirect = new RedirectBase(provider) {
				@Override
				public AsyncSocket choiceServer(IModule module, int serverId, boolean orOtherServer) {
					if (serverId == 404)
						throw serverMissing;
					return socket;
				}

				@Override
				public AsyncSocket choiceHash(IModule module, int hash, int dataConcurrentLevel) {
					throw hashMissing;
				}
			};
			var appBase = new AppBase() {
				@Override public Application getZeze() { return app; }
			};
			GenModule.instance.generateRedirectSources(directory.toString(), appBase,
					new Class<?>[]{TestRedirectDecodeFailure.class}, false);
			var name = GenModule.REDIRECT_PREFIX + getClass().getName().replace('.', '_');
			var source = Files.readString(directory.resolve(name + ".java"));
			var compiler = new InMemoryJavaCompiler(getClass().getClassLoader()).ignoreWarnings()
					.useOptions("-classpath", System.getProperty("java.class.path"));
			compiler.compileAllToByteCode(Map.of(name, source));
			var module = (TestRedirectDecodeFailure)compiler.defineCompiled(name)
					.getConstructor(AppBase.class).newInstance(appBase);

			var onFailError = new AtomicReference<@Nullable Throwable>();
			var serverFuture = module.readNumber(404).onFail(onFailError::set);
			var serverEx = assertInstanceOf(RedirectException.class,
					assertThrows(CompletionException.class, serverFuture::get).getCause());
			assertEquals(RedirectException.SERVER_NOT_FOUND, serverEx.getCode());
			assertSame(serverMissing, serverEx);
			assertSame(serverMissing, onFailError.get());

			var hashFuture = module.readByHash(7);
			var hashEx = assertInstanceOf(RedirectException.class,
					assertThrows(CompletionException.class, hashFuture::get).getCause());
			assertEquals(RedirectException.SERVER_NOT_FOUND, hashEx.getCode());
			assertSame(hashMissing, hashEx);

			var encodeFailure = new IllegalStateException("param encode failed");
			var badBean = new EmptyBean() {
				@Override
				public void encode(ByteBuffer bb) {
					throw encodeFailure;
				}
			};
			var encodeFuture = module.readWithBean(1, badBean);
			var encodeEx = assertInstanceOf(RedirectException.class,
					assertThrows(CompletionException.class, encodeFuture::get).getCause());
			assertEquals(RedirectException.LOCAL_EXECUTION, encodeEx.getCode());
			assertSame(encodeFailure, encodeEx.getCause());
		} finally {
			socket.close();
			service.stop();
			app.stop();
		}
	}
}
