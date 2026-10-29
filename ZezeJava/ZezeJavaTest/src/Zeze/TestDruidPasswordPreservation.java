package Zeze;

import javax.xml.parsers.DocumentBuilderFactory;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Fast
public class TestDruidPasswordPreservation {

	private static Element databaseElement() throws Exception {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().createElement("DatabaseConf");
	}

	@Test
	public void passwordKeepsLeadingAndTrailingWhitespace() throws Exception {
		var element = databaseElement();
		element.setAttribute("Password", "  example secret\t ");
		assertEquals("  example secret\t ", new Config.DruidConf(element).password);
	}

	@Test
	public void whitespaceOnlyPasswordRemainsAnExplicitPassword() throws Exception {
		var element = databaseElement();
		element.setAttribute("Password", " \t ");
		assertEquals(" \t ", new Config.DruidConf(element).password);
	}

	@Test
	public void missingOrEmptyPasswordKeepsTheUrlFallback() throws Exception {
		var element = databaseElement();
		assertNull(new Config.DruidConf(element).password);
		element.setAttribute("Password", "");
		assertNull(new Config.DruidConf(element).password);
	}
}
