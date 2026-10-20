package Zeze.Netty;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestDirectoryListingLinks {
	private static final Pattern LINK = Pattern.compile("<a href=\"([^\"]+)\">");

	private static List<String> links(String html) {
		var result = new ArrayList<String>();
		var matcher = LINK.matcher(html);
		while (matcher.find())
			if (!matcher.group(1).equals("../"))
				result.add(matcher.group(1));
		return result;
	}

	private static HttpResponse<String> get(HttpClient client, URI uri) throws Exception {
		return client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).build(),
				HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void listedNamesRoundTripThroughRealDownloads(@TempDir Path root) throws Exception {
		var names = List.of("report#1.txt", "percent%.txt", "a+b.txt", "中文 name.txt", "a&b.txt");
		for (var name : names)
			Files.writeString(root.resolve(name), name);
		var directory = "folder #+%";
		Files.createDirectory(root.resolve(directory));
		Files.writeString(root.resolve(directory).resolve("child.txt"), "child");

		Task.tryInitThreadPool();
		try (var netty = new Netty(1); var server = new HttpServer();
			 var client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
			server.addFileHandler("/files/", root.toString(), true);
			int port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			var base = URI.create("http://127.0.0.1:" + port + "/files/");
			var listing = get(client, base);
			assertEquals(200, listing.statusCode());
			var hrefs = links(listing.body());
			assertEquals(names.size() + 1, hrefs.size());
			for (var href : hrefs) {
				var target = base.resolve(href);
				assertNull(target.getFragment(), href);
				assertNull(target.getQuery(), href);
				assertEquals(base.getAuthority(), target.getAuthority(), href);
				var response = get(client, target);
				assertEquals(200, response.statusCode(), href);
				var name = target.getPath().substring("/files/".length());
				if (name.endsWith("/")) {
					assertEquals(directory + "/", name);
					assertTrue(response.body().contains("child.txt"));
				} else {
					assertTrue(names.contains(name), name);
					assertEquals(name, response.body());
				}
			}
		}
	}

	@Test
	void schemeLikeFilenameRemainsARelativePath() throws Exception {
		// File metadata fixture models a legal POSIX filename on Windows as well.
		var child = new File("entry") {
			@Override public String getName() { return "javascript:alert(1)"; }
			@Override public boolean isHidden() { return false; }
			@Override public boolean isDirectory() { return false; }
		};
		var directory = new File("listing") {
			@Override public boolean isDirectory() { return true; }
			@Override public boolean isHidden() { return false; }
			@Override public File[] listFiles() { return new File[]{child}; }
		};
		Task.tryInitThreadPool();
		try (var netty = new Netty(1); var server = new HttpServer();
			 var client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
			server.addHandler("/listing/", 0, TransactionLevel.None, DispatchMode.Direct,
					x -> x.sendPath(directory));
			int port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			var listing = get(client, URI.create("http://127.0.0.1:" + port + "/listing/"));
			assertEquals(200, listing.statusCode());
			var link = URI.create(links(listing.body()).getFirst());
			assertNull(link.getScheme(), "a filename must never become an executable URL");
			assertEquals("javascript:alert(1)", link.getPath());
		}
	}
}
