package UnitTest.Zeze;

import harness.Fast;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import Zeze.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// FND2-A1-7：GlobalCacheManagerHostNameOrAddress="GlobalCacheManagersConf"但缺少
// <GlobalCacheManagersConf>子节点（笔误/复制不完整）时，parse必须抛带名
// IllegalStateException而非裸NPE，启动期即指明缺什么。
@Fast
public class TestConfigGlobalCacheManagersConfMissing {

	private static Config parseXml(String xml) throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml)));
		var config = new Config();
		config.parse(doc.getDocumentElement());
		return config;
	}

	@Test
	public void testMissingNodeThrows() {
		var ex = assertThrows(IllegalStateException.class, () ->
				parseXml("<zeze GlobalCacheManagerHostNameOrAddress=\"GlobalCacheManagersConf\"/>"));
		assertEquals("GlobalCacheManagersConf node missing", ex.getMessage());
	}

	@Test
	public void testNodePresentJoinsHosts() throws Exception {
		var config = parseXml("<zeze GlobalCacheManagerHostNameOrAddress=\"GlobalCacheManagersConf\">"
				+ "<GlobalCacheManagersConf>"
				+ "<host name=\"127.0.0.1:11000\"/><host name=\"127.0.0.1:11001\"/>"
				+ "</GlobalCacheManagersConf></zeze>");
		assertEquals("127.0.0.1:11000;127.0.0.1:11001", config.getGlobalCacheManagerHostNameOrAddress());
	}
}
