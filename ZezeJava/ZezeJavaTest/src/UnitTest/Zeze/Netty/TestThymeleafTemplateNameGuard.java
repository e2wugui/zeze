package UnitTest.Zeze.Netty;

import Zeze.Netty.Thymeleaf;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND6-16：Thymeleaf模板名直接取解码后的请求路径，无穿越防护——应用组合
 * 「prefix路由+Thymeleaf渲染」时 /tpl/../../etc/foo 可越出模板目录读任意.html。
 * 修复：sendResponse前以isTraversalTemplateName拒绝（含..、:、反斜杠即FORBIDDEN），
 * 比照HttpServer.addFileHandler判例；FreeMarker侧normalizeRootBasedName天然免疫。
 */
@Fast
public class TestThymeleafTemplateNameGuard {

	@Test
	public void testTraversalNamesRejected() {
		Assertions.assertTrue(Thymeleaf.isTraversalTemplateName("../secret.html"), "相对上跳须拒绝");
		Assertions.assertTrue(Thymeleaf.isTraversalTemplateName("/tpl/../../etc/foo"), "嵌套上跳须拒绝");
		Assertions.assertTrue(Thymeleaf.isTraversalTemplateName("a/../b"), "中段上跳须拒绝");
		Assertions.assertTrue(Thymeleaf.isTraversalTemplateName("..\\..\\x"), "反斜杠上跳须拒绝（Windows形态）");
		Assertions.assertTrue(Thymeleaf.isTraversalTemplateName("C:/x"), "盘符冒号须拒绝");
		Assertions.assertTrue(Thymeleaf.isTraversalTemplateName("http://x"), "协议冒号须拒绝");
	}

	@Test
	public void testLegalTemplateNamesPass() {
		Assertions.assertFalse(Thymeleaf.isTraversalTemplateName("index"));
		Assertions.assertFalse(Thymeleaf.isTraversalTemplateName("/tpl/index"));
		Assertions.assertFalse(Thymeleaf.isTraversalTemplateName("user/profile"));
		Assertions.assertFalse(Thymeleaf.isTraversalTemplateName("a.b.c"));
	}
}
