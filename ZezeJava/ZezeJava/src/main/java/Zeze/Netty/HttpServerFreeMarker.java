package Zeze.Netty;

import java.io.File;
import org.jetbrains.annotations.NotNull;

public class HttpServerFreeMarker extends HttpServer {
	protected @NotNull final FreeMarker freeMarker;

	public HttpServerFreeMarker(File templateDir) throws Exception {
		freeMarker = new FreeMarker(templateDir);
	}

	public FreeMarker getFreeMarker() {
		return freeMarker;
	}
}
