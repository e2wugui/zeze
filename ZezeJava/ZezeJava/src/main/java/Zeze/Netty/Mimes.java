package Zeze.Netty;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Properties;
import Zeze.Util.StringSpan;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Mimes {
	// from mime.types in nginx-1.25.2
	@SuppressWarnings("TextBlockMigration")
	public static final String mimes = //@formatter:off
			"3gp=video/3gpp\n" +
			"3gpp=video/3gpp\n" +
			"7z=application/x-7z-compressed\n" +
			"ai=application/postscript\n" +
			"asf=video/x-ms-asf\n" +
			"asx=video/x-ms-asf\n" +
			"atom=application/atom+xml\n" +
			"avi=video/x-msvideo\n" +
			"avif=image/avif\n" +
			"bin=application/octet-stream\n" +
			"bmp=image/x-ms-bmp\n" +
			"cco=application/x-cocoa\n" +
			"crt=application/x-x509-ca-cert\n" +
			"css=text/css\n" +
			"deb=application/octet-stream\n" +
			"der=application/x-x509-ca-cert\n" +
			"dll=application/octet-stream\n" +
			"dmg=application/octet-stream\n" +
			"doc=application/msword\n" +
			"docx=application/vnd.openxmlformats-officedocument.wordprocessingml.document\n" +
			"ear=application/java-archive\n" +
			"eot=application/vnd.ms-fontobject\n" +
			"eps=application/postscript\n" +
			"exe=application/octet-stream\n" +
			"flv=video/x-flv\n" +
			"gif=image/gif\n" +
			"hqx=application/mac-binhex40\n" +
			"htc=text/x-component\n" +
			"htm=text/html\n" +
			"html=text/html\n" +
			"ico=image/x-icon\n" +
			"img=application/octet-stream\n" +
			"iso=application/octet-stream\n" +
			"jad=text/vnd.sun.j2me.app-descriptor\n" +
			"jar=application/java-archive\n" +
			"jardiff=application/x-java-archive-diff\n" +
			"jng=image/x-jng\n" +
			"jnlp=application/x-java-jnlp-file\n" +
			"jpeg=image/jpeg\n" +
			"jpg=image/jpeg\n" +
			"js=application/javascript\n" +
			"json=application/json\n" +
			"kar=audio/midi\n" +
			"kml=application/vnd.google-earth.kml+xml\n" +
			"kmz=application/vnd.google-earth.kmz\n" +
			"m3u8=application/vnd.apple.mpegurl\n" +
			"m4a=audio/x-m4a\n" +
			"m4v=video/x-m4v\n" +
			"mid=audio/midi\n" +
			"midi=audio/midi\n" +
			"mml=text/mathml\n" +
			"mng=video/x-mng\n" +
			"mov=video/quicktime\n" +
			"mp3=audio/mpeg\n" +
			"mp4=video/mp4\n" +
			"mpeg=video/mpeg\n" +
			"mpg=video/mpeg\n" +
			"msi=application/octet-stream\n" +
			"msm=application/octet-stream\n" +
			"msp=application/octet-stream\n" +
			"odg=application/vnd.oasis.opendocument.graphics\n" +
			"odp=application/vnd.oasis.opendocument.presentation\n" +
			"ods=application/vnd.oasis.opendocument.spreadsheet\n" +
			"odt=application/vnd.oasis.opendocument.text\n" +
			"ogg=audio/ogg\n" +
			"pdb=application/x-pilot\n" +
			"pdf=application/pdf\n" +
			"pem=application/x-x509-ca-cert\n" +
			"pl=application/x-perl\n" +
			"pm=application/x-perl\n" +
			"png=image/png\n" +
			"ppt=application/vnd.ms-powerpoint\n" +
			"pptx=application/vnd.openxmlformats-officedocument.presentationml.presentation\n" +
			"prc=application/x-pilot\n" +
			"ps=application/postscript\n" +
			"ra=audio/x-realaudio\n" +
			"rar=application/x-rar-compressed\n" +
			"rpm=application/x-redhat-package-manager\n" +
			"rss=application/rss+xml\n" +
			"rtf=application/rtf\n" +
			"run=application/x-makeself\n" +
			"sea=application/x-sea\n" +
			"shtml=text/html\n" +
			"sit=application/x-stuffit\n" +
			"svg=image/svg+xml\n" +
			"svgz=image/svg+xml\n" +
			"swf=application/x-shockwave-flash\n" +
			"tcl=application/x-tcl\n" +
			"tif=image/tiff\n" +
			"tiff=image/tiff\n" +
			"tk=application/x-tcl\n" +
			"ts=video/mp2t\n" +
			"txt=text/plain\n" +
			"war=application/java-archive\n" +
			"wasm=application/wasm\n" +
			"wbmp=image/vnd.wap.wbmp\n" +
			"webm=video/webm\n" +
			"webp=image/webp\n" +
			"wml=text/vnd.wap.wml\n" +
			"wmlc=application/vnd.wap.wmlc\n" +
			"wmv=video/x-ms-wmv\n" +
			"woff=font/woff\n" +
			"woff2=font/woff2\n" +
			"xhtml=application/xhtml+xml\n" +
			"xls=application/vnd.ms-excel\n" +
			"xlsx=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\n" +
			"xml=text/xml\n" +
			"xpi=application/x-xpinstall\n" +
			"xspf=application/xspf+xml\n" +
			"zip=application/zip\n"; //@formatter:on

	private static final String mimeDefault = "text/plain";
	private static final HashMap<String, String> mimesMap = new HashMap<>();

	private static void load(@NotNull Reader input) throws IOException {
		var p = new Properties();
		p.load(input);
		for (var e : p.entrySet())
			mimesMap.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
	}

	static {
		try {
			// load default
			load(new StringReader(mimes));
			// try load config
			var file = new File("mimes.properties");
			if (file.exists())
				load(new FileReader(file, StandardCharsets.UTF_8));
		} catch (IOException e) {
			Task.forceThrow(e);
		}
	}

	// 注意：扩展名不包含字符'.'
	public static @NotNull String fromFileExtension(@NotNull Object extName) {
		//noinspection SuspiciousMethodCalls
		var mime = mimesMap.get(extName);
		return mime != null ? mime : mimeDefault;
	}

	public static @NotNull String fromFileName(@Nullable String file) {
		if (file != null) {
			var index = file.lastIndexOf('.');
			if (index >= 0)
				return fromFileExtension(new StringSpan(file, index + 1, file.length() - index - 1));
		}
		return mimeDefault;
	}
}
