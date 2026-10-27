package Zeze.Util;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestAtomicOutputAtomicMove {

	@Test
	public void unsupportedAtomicMoveKeepsTheOldVersionAndEndsTheHandle(@TempDir Path dir) throws Exception {
		var target = dir.resolve("state.bin");
		Files.writeString(target, "OLD");
		var provider = new NonAtomicProvider();
		try (var output = new AtomicOutputFile(provider.fileSystem.wrap(target))) {
			output.outputStream().write("NEW".getBytes(java.nio.charset.StandardCharsets.UTF_8));
			assertThrows(AtomicMoveNotSupportedException.class, output::commit);
			assertEquals("OLD", Files.readString(target));
			assertThrows(IOException.class, output::commit);
			assertThrows(IOException.class, () -> output.outputStream().write(1));
		}
		assertEquals(1, provider.atomicMoves);
		assertEquals(0, provider.nonAtomicMoves, "An atomic write cannot fall back to a weaker operation");
		try (var files = Files.list(dir)) {
			assertEquals(1, files.count(), "The failed write must discard its temporary file");
		}
	}

	/** A real on-disk provider whose rename explicitly lacks atomic-move support. */
	private static final class NonAtomicProvider extends FileSystemProvider {
		private final WrappedFileSystem fileSystem = new WrappedFileSystem(this);
		private int atomicMoves;
		private int nonAtomicMoves;

		private static Path unwrap(Path path) {
			return ((WrappedPath)Proxy.getInvocationHandler(path)).actual;
		}

		@Override
		public String getScheme() {
			return "non-atomic";
		}

		@Override
		public FileSystem newFileSystem(URI uri, Map<String, ?> env) {
			throw new UnsupportedOperationException();
		}

		@Override
		public FileSystem getFileSystem(URI uri) {
			return fileSystem;
		}

		@Override
		public Path getPath(URI uri) {
			throw new UnsupportedOperationException();
		}

		@Override
		public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
				FileAttribute<?>... attrs) throws IOException {
			return Files.newByteChannel(unwrap(path), options, attrs);
		}

		@Override
		public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options,
				FileAttribute<?>... attrs) throws IOException {
			return FileChannel.open(unwrap(path), options, attrs);
		}

		@Override
		public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
			Files.createDirectory(unwrap(dir), attrs);
		}

		@Override
		public void delete(Path path) throws IOException {
			Files.delete(unwrap(path));
		}

		@Override
		public void copy(Path source, Path target, CopyOption... options) throws IOException {
			Files.copy(unwrap(source), unwrap(target), options);
		}

		@Override
		public void move(Path source, Path target, CopyOption... options) throws IOException {
			for (var option : options) {
				if (option == StandardCopyOption.ATOMIC_MOVE) {
					atomicMoves++;
					throw new AtomicMoveNotSupportedException(source.toString(), target.toString(), "unsupported");
				}
			}
			nonAtomicMoves++;
			Files.move(unwrap(source), unwrap(target), options);
		}

		@Override
		public boolean isSameFile(Path path, Path path2) throws IOException {
			return Files.isSameFile(unwrap(path), unwrap(path2));
		}

		@Override
		public boolean isHidden(Path path) throws IOException {
			return Files.isHidden(unwrap(path));
		}

		@Override
		public FileStore getFileStore(Path path) throws IOException {
			return Files.getFileStore(unwrap(path));
		}

		@Override
		public void checkAccess(Path path, AccessMode... modes) throws IOException {
			var actual = unwrap(path);
			actual.getFileSystem().provider().checkAccess(actual, modes);
		}

		@Override
		public <V extends FileAttributeView> @Nullable V getFileAttributeView(Path path, Class<V> type,
				LinkOption... options) {
			return Files.getFileAttributeView(unwrap(path), type, options);
		}

		@Override
		public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type,
				LinkOption... options) throws IOException {
			return Files.readAttributes(unwrap(path), type, options);
		}

		@Override
		public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
			return Files.readAttributes(unwrap(path), attributes, options);
		}

		@Override
		public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
			Files.setAttribute(unwrap(path), attribute, value, options);
		}
	}

	private static final class WrappedFileSystem extends FileSystem {
		private final NonAtomicProvider provider;

		private WrappedFileSystem(NonAtomicProvider provider) {
			this.provider = provider;
		}

		private Path wrap(Path actual) {
			return (Path)Proxy.newProxyInstance(Path.class.getClassLoader(), new Class<?>[]{Path.class},
					new WrappedPath(this, actual));
		}

		@Override
		public FileSystemProvider provider() {
			return provider;
		}

		@Override
		public void close() {
		}

		@Override
		public boolean isOpen() {
			return true;
		}

		@Override
		public boolean isReadOnly() {
			return false;
		}

		@Override
		public String getSeparator() {
			return java.io.File.separator;
		}

		@Override
		public Iterable<Path> getRootDirectories() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterable<FileStore> getFileStores() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<String> supportedFileAttributeViews() {
			return Set.of("basic");
		}

		@Override
		public Path getPath(String first, String... more) {
			return wrap(Path.of(first, more));
		}

		@Override
		public PathMatcher getPathMatcher(String syntaxAndPattern) {
			throw new UnsupportedOperationException();
		}

		@Override
		public UserPrincipalLookupService getUserPrincipalLookupService() {
			throw new UnsupportedOperationException();
		}

		@Override
		public WatchService newWatchService() {
			throw new UnsupportedOperationException();
		}
	}

	private record WrappedPath(WrappedFileSystem fileSystem, Path actual) implements InvocationHandler {
		@Override
		public @Nullable Object invoke(Object proxy, Method method, Object @Nullable [] args) throws Throwable {
			if (method.getName().equals("getFileSystem"))
				return fileSystem;
			if (args != null) {
				args = args.clone();
				for (int i = 0; i < args.length; i++) {
					if (args[i] instanceof Path path && Proxy.isProxyClass(path.getClass()))
						args[i] = NonAtomicProvider.unwrap(path);
				}
			}
			try {
				var result = method.invoke(actual, args);
				return result instanceof Path path ? fileSystem.wrap(path) : result;
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}
	}
}
