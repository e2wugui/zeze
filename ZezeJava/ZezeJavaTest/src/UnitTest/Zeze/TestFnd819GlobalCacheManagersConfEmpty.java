package UnitTest.Zeze;

import harness.Fast;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import Zeze.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// FND8-19：GlobalCacheManagersConf节点存在但无<host>（或host name全空白）时，
// toString产出""或";"静默降级为无GCM模式——明确opt-in的部署失去跨进程锁协调，
// 且多空白host（";"）连主流后端的setInUse启动互斥兜底都被绕过。parse的转换点
// 必须按消费语义过滤后判空fail-fast；直接属性空串（单机标准写法）不受影响。
@Fast
public class TestFnd819GlobalCacheManagersConfEmpty {

	private static Config parseXml(String xml) throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml)));
		var config = new Config();
		config.parse(doc.getDocumentElement());
		return config;
	}

	/** 节点存在但无任何<host>子节点：修复前toString()==""静默降级为单机模式。 */
	@Test
	public void testEmptyNodeThrows() {
		var ex = assertThrows(IllegalStateException.class, () ->
				parseXml("<zeze GlobalCacheManagerHostNameOrAddress=\"GlobalCacheManagersConf\">"
						+ "<GlobalCacheManagersConf/>"
						+ "</zeze>"));
		assertEquals("GlobalCacheManagersConf has no non-blank <host> children", ex.getMessage());
	}

	/** 单个空白name：toString()==""，与漏写host同型。 */
	@Test
	public void testSingleBlankHostThrows() {
		assertThrows(IllegalStateException.class, () ->
				parseXml("<zeze GlobalCacheManagerHostNameOrAddress=\"GlobalCacheManagersConf\">"
						+ "<GlobalCacheManagersConf><host name=\"  \"/></GlobalCacheManagersConf>"
						+ "</zeze>"));
	}

	/** >=2个空白name：toString()==";"非blank，isBlank检查封不住的变体（更静默：连
	 * setInUse互斥都被绕过），过滤后判空必须拒绝。 */
	@Test
	public void testMultipleBlankHostsThrows() {
		assertThrows(IllegalStateException.class, () ->
				parseXml("<zeze GlobalCacheManagerHostNameOrAddress=\"GlobalCacheManagersConf\">"
						+ "<GlobalCacheManagersConf>"
						+ "<host name=\"\"/><host name=\" \"/>"
						+ "</GlobalCacheManagersConf>"
						+ "</zeze>"));
	}

	/** 正常host不受影响：hasGlobal()==true且值按";"连接。 */
	@Test
	public void testValidHostsUnaffected() throws Exception {
		var config = parseXml("<zeze GlobalCacheManagerHostNameOrAddress=\"GlobalCacheManagersConf\">"
				+ "<GlobalCacheManagersConf>"
				+ "<host name=\" 127.0.0.1:11000 \"/><host name=\"127.0.0.1:11001\"/>"
				+ "</GlobalCacheManagersConf></zeze>");
		assertEquals("127.0.0.1:11000;127.0.0.1:11001", config.getGlobalCacheManagerHostNameOrAddress());
		assertTrue(config.hasGlobal());
	}

	/** 直接属性空串/缺属性=单机模式标准写法：不得抛（fail-fast只在opt-in路径触发）。 */
	@Test
	public void testDirectEmptyAttributeUnaffected() throws Exception {
		var config = parseXml("<zeze GlobalCacheManagerHostNameOrAddress=\"\"/>");
		assertFalse(config.hasGlobal());
		var config2 = parseXml("<zeze/>");
		assertFalse(config2.hasGlobal());
	}
}
