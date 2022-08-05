package Temp;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.digest.DigestUtils;

public class TestZezeWebEcho {
	public static void main(String args[]) throws IOException, InterruptedException, NoSuchAlgorithmException {
		var uploadFile = "C:\\Users\\10501\\Downloads\\TortoiseSVN-1.14.2.29370-x64-svn-1.14.1.msi";
		var downloadFile = "C:\\Users\\10501\\Downloads\\echo.download";

		var httpClient = HttpClient.newHttpClient();
		var request = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1/zeze/echo"))
				.POST(HttpRequest.BodyPublishers.ofFile(Path.of(uploadFile)))
				.header("Accept", "application/json")
				.build();
		httpClient.send(request, HttpResponse.BodyHandlers.ofFile(Path.of(downloadFile)));

		var uploadMd5 = DigestUtils.md5Hex(new FileInputStream(uploadFile));
		var downloadMd5 = DigestUtils.md5Hex(new FileInputStream(downloadFile));
		System.out.println("upload=" + uploadMd5);
		System.out.println("download=" + downloadMd5);
	}
}
