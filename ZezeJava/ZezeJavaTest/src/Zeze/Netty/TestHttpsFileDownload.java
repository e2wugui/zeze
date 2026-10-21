package Zeze.Netty;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Cert;
import Zeze.Util.Task;
import Zeze.Util.TestCert;
import harness.Fast;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.jspecify.annotations.Nullable;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
@Isolated
public class TestHttpsFileDownload {
	private record TlsMaterial(PrivateKey privateKey, X509Certificate certificate, SSLContext clientContext) {
	}

	private static @Nullable TlsMaterial tlsMaterial;

	@BeforeAll
	static void createCertificate() throws Exception {
		Task.tryInitThreadPool();
		var keyPair = Cert.generateRsaKeyPair();
		var privateKey = keyPair.getPrivate();
		var certificate = TestCert.generateRsaCert("localhost", keyPair.getPublic(), "localhost", privateKey, 1);
		var trusted = KeyStore.getInstance(KeyStore.getDefaultType());
		trusted.load(null, null);
		trusted.setCertificateEntry("local-test-server", certificate);
		var trustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManager.init(trusted);
		var clientContext = SSLContext.getInstance("TLS");
		clientContext.init(null, trustManager.getTrustManagers(), null);
		tlsMaterial = new TlsMaterial(privateKey, certificate, clientContext);
	}

	@Test
	void httpsGetSendsEntireFile(@TempDir Path directory) throws Exception {
		try (var fixture = new DownloadServer(directory, true)) {
			// A successful TLS text response proves certificate trust and the ordinary HTTP pipeline work.
			assertArrayEquals(new byte[]{1, 2, 3}, fixture.request("/control", "GET", null).body());
			var response = fixture.request("/file", "GET", null);
			assertEquals(200, response.statusCode());
			assertEquals(fixture.bytes.length, response.headers().firstValueAsLong("Content-Length").orElseThrow());
			assertArrayEquals(fixture.bytes, response.body());
		}
	}

	@Test
	void httpsRangeSendsOnlyRequestedBytes(@TempDir Path directory) throws Exception {
		try (var fixture = new DownloadServer(directory, true)) {
			var response = fixture.request("/file", "GET", "bytes=31-98352");
			assertEquals(206, response.statusCode());
			assertEquals("bytes 31-98352/" + fixture.bytes.length,
					response.headers().firstValue("Content-Range").orElseThrow());
			assertArrayEquals(Arrays.copyOfRange(fixture.bytes, 31, 98353), response.body());
		}
	}

	@Test
	void httpsHeadSendsHeadersWithoutBody(@TempDir Path directory) throws Exception {
		try (var fixture = new DownloadServer(directory, true)) {
			var response = fixture.request("/file", "HEAD", null);
			assertEquals(200, response.statusCode());
			assertEquals(fixture.bytes.length, response.headers().firstValueAsLong("Content-Length").orElseThrow());
			assertEquals(0, response.body().length);
		}
	}

	@Test
	void plaintextGetStillSendsEntireFile(@TempDir Path directory) throws Exception {
		try (var fixture = new DownloadServer(directory, false)) {
			var response = fixture.request("/file", "GET", null);
			assertEquals(200, response.statusCode());
			assertArrayEquals(fixture.bytes, response.body());
		}
	}

	private static final class DownloadServer implements AutoCloseable {
		private final byte[] bytes = new byte[192 * 1024 + 37];
		private final Netty netty = new Netty(1);
		private final HttpServer server = new HttpServer();
		private final HttpClient client;
		private final Path file;
		private final String address;

		DownloadServer(Path directory, boolean tls) throws Exception {
			var material = Objects.requireNonNull(tlsMaterial);
			for (int i = 0; i < bytes.length; i++)
				bytes[i] = (byte)(i * 31 + (i >>> 8));
			file = Files.write(directory.resolve("download.bin"), bytes);
			if (tls)
				server.setSsl(material.privateKey(), null, material.certificate());
			server.addHandler("/file", 0, TransactionLevel.None, DispatchMode.Direct, x -> x.sendFile(file.toFile()));
			server.addHandler("/control", 0, TransactionLevel.None, DispatchMode.Direct,
					x -> x.sendPlainText(HttpResponseStatus.OK, new byte[]{1, 2, 3}));
			var channel = server.start(netty, "127.0.0.1", 0).sync().channel();
			address = (tls ? "https" : "http") + "://localhost:"
					+ ((InetSocketAddress)channel.localAddress()).getPort();
			client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
					.sslContext(material.clientContext()).connectTimeout(Duration.ofSeconds(5)).build();
		}

		HttpResponse<byte[]> request(String path, String method, @Nullable String range) throws Exception {
			var builder = HttpRequest.newBuilder(URI.create(address + path)).timeout(Duration.ofSeconds(5))
					.method(method, HttpRequest.BodyPublishers.noBody());
			if (range != null)
				builder.header("Range", range);
			// Bound completion even if a broken server writes headers then leaves the TLS body unfinished.
			return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofByteArray()).get(8, TimeUnit.SECONDS);
		}

		@Override
		public void close() throws Exception {
			client.shutdownNow();
			server.close();
			netty.close();
			Files.delete(file); // Temporary-file cleanup only; Windows shared-delete does not prove handle closure.
		}
	}
}
