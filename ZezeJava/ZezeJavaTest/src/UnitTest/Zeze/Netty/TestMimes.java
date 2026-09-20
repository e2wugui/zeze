package UnitTest.Zeze.Netty;

import Zeze.Netty.Mimes;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * NY2-F4回归：MIME扩展名匹配大小写敏感——键全小写，大写扩展名（Windows常见，如LOGO.PNG）
 * 命不中回text/plain。修复：查找前对扩展名lowercase(Locale.ROOT)归一化。
 */
@Fast
public class TestMimes {

	@Test
	public void testFromFileExtensionCaseInsensitive() {
		Assertions.assertEquals("image/png", Mimes.fromFileExtension("PNG"));
		Assertions.assertEquals("image/png", Mimes.fromFileExtension("Png"));
		Assertions.assertEquals("image/png", Mimes.fromFileExtension("png"), "小写键路径不得回归");
		Assertions.assertEquals("text/html", Mimes.fromFileExtension("HTML"));
		Assertions.assertEquals("application/zip", Mimes.fromFileExtension("ZIP"));
	}

	@Test
	public void testFromFileNameCaseInsensitive() {
		Assertions.assertEquals("image/png", Mimes.fromFileName("LOGO.PNG"));
		Assertions.assertEquals("image/jpeg", Mimes.fromFileName("dir/Pic.JPEG"));
		Assertions.assertEquals("text/plain", Mimes.fromFileName("noext"), "无扩展名保持默认");
		Assertions.assertEquals("text/plain", Mimes.fromFileName("unknown.xyz"), "未知扩展名保持默认");
	}
}
