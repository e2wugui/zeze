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
	public static final String mimes = """
			3gp=video/3gpp
			3gpp=video/3gpp
			7z=application/x-7z-compressed
			ai=application/postscript
			asf=video/x-ms-asf
			asx=video/x-ms-asf
			atom=application/atom+xml
			avi=video/x-msvideo
			avif=image/avif
			bin=application/octet-stream
			bmp=image/x-ms-bmp
			cco=application/x-cocoa
			crt=application/x-x509-ca-cert
			css=text/css
			deb=application/octet-stream
			der=application/x-x509-ca-cert
			dll=application/octet-stream
			dmg=application/octet-stream
			doc=application/msword
			docx=application/vnd.openxmlformats-officedocument.wordprocessingml.document
			ear=application/java-archive
			eot=application/vnd.ms-fontobject
			eps=application/postscript
			exe=application/octet-stream
			flv=video/x-flv
			gif=image/gif
			hqx=application/mac-binhex40
			htc=text/x-component
			htm=text/html
			html=text/html
			ico=image/x-icon
			img=application/octet-stream
			iso=application/octet-stream
			jad=text/vnd.sun.j2me.app-descriptor
			jar=application/java-archive
			jardiff=application/x-java-archive-diff
			jng=image/x-jng
			jnlp=application/x-java-jnlp-file
			jpeg=image/jpeg
			jpg=image/jpeg
			js=application/javascript
			json=application/json
			kar=audio/midi
			kml=application/vnd.google-earth.kml+xml
			kmz=application/vnd.google-earth.kmz
			m3u8=application/vnd.apple.mpegurl
			m4a=audio/x-m4a
			m4v=video/x-m4v
			mid=audio/midi
			midi=audio/midi
			mml=text/mathml
			mng=video/x-mng
			mov=video/quicktime
			mp3=audio/mpeg
			mp4=video/mp4
			mpeg=video/mpeg
			mpg=video/mpeg
			msi=application/octet-stream
			msm=application/octet-stream
			msp=application/octet-stream
			odg=application/vnd.oasis.opendocument.graphics
			odp=application/vnd.oasis.opendocument.presentation
			ods=application/vnd.oasis.opendocument.spreadsheet
			odt=application/vnd.oasis.opendocument.text
			ogg=audio/ogg
			pdb=application/x-pilot
			pdf=application/pdf
			pem=application/x-x509-ca-cert
			pl=application/x-perl
			pm=application/x-perl
			png=image/png
			ppt=application/vnd.ms-powerpoint
			pptx=application/vnd.openxmlformats-officedocument.presentationml.presentation
			prc=application/x-pilot
			ps=application/postscript
			ra=audio/x-realaudio
			rar=application/x-rar-compressed
			rpm=application/x-redhat-package-manager
			rss=application/rss+xml
			rtf=application/rtf
			run=application/x-makeself
			sea=application/x-sea
			shtml=text/html
			sit=application/x-stuffit
			svg=image/svg+xml
			svgz=image/svg+xml
			swf=application/x-shockwave-flash
			tcl=application/x-tcl
			tif=image/tiff
			tiff=image/tiff
			tk=application/x-tcl
			ts=video/mp2t
			txt=text/plain
			war=application/java-archive
			wasm=application/wasm
			wbmp=image/vnd.wap.wbmp
			webm=video/webm
			webp=image/webp
			wml=text/vnd.wap.wml
			wmlc=application/vnd.wap.wmlc
			wmv=video/x-ms-wmv
			woff=font/woff
			woff2=font/woff2
			xhtml=application/xhtml+xml
			xls=application/vnd.ms-excel
			xlsx=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
			xml=text/xml
			xpi=application/x-xpinstall
			xspf=application/xspf+xml
			zip=application/zip
			""";

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
			throw Task.forceThrow(e);
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
