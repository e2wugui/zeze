package Temp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import com.sun.net.httpserver.HttpServer;

public class TestHttpServer {
	public static void main(String[] args) throws IOException {
		var hs = HttpServer.create(new InetSocketAddress(80), 10);
		hs.createContext("/", he -> {
			var sb = new StringBuilder("<html><body><pre>\n");
			sb.append("LocalAddress: ").append(he.getLocalAddress()).append('\n');
			sb.append("RemoteAddress: ").append(he.getRemoteAddress()).append('\n');
			sb.append("Protocol: ").append(he.getProtocol()).append('\n');
			sb.append("RequestMethod: ").append(he.getRequestMethod()).append('\n');
			sb.append("RequestURI: ").append(he.getRequestURI()).append('\n');
			sb.append("RequestHeaders:\n");
			for (var e : he.getRequestHeaders().entrySet()) {
				sb.append("  ").append(e.getKey()).append(": ");
				for (String s : e.getValue())
					sb.append(s).append(';');
				sb.append('\n');
			}
			sb.append("RequestBody.length: ").append(he.getRequestBody().readAllBytes().length).append('\n');
			sb.append("</pre></body></html>\n");
			byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);
			he.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
			he.sendResponseHeaders(200, content.length);
			try (var os = he.getResponseBody()) {
				os.write(content);
			}
		});
		hs.start();
	}
}
/*
GET /abc/def/ HTTP/1.1
Host: 127.0.0.1
Connection: keep-alive
User-Agent: Mozilla/5.0 ......
Accept: text/html,......
Accept-Encoding: gzip, deflate, br
Accept-Language: zh-CN,zh;q=0.9
......
*/
/*
HTTP/1.1 200 OK
Date: Tue, 26 Jul 2022 06:39:14 GMT
Content-type: text/html; charset=utf-8
Content-length: 760

LocalAddress: /127.0.0.1:80
RemoteAddress: /127.0.0.1:62142
Protocol: HTTP/1.1
RequestMethod: GET
RequestURI: /abc/def/
RequestHeaders:
  Host: 127.0.0.1;
  Connection: keep-alive;
  User-agent: Mozilla/5.0 ......
  Accept: text/html,......
  Accept-encoding: gzip, deflate, br;
  Accept-language: zh-CN,zh;q=0.9;
  ......
RequestBody.length: 0
*/
