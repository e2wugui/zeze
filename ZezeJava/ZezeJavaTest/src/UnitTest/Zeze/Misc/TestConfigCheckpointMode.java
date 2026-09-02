package UnitTest.Zeze.Misc;

import javax.xml.parsers.DocumentBuilderFactory;
import harness.Fast;
import Zeze.Config;
import Zeze.Transaction.CheckpointMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

@Fast
public class TestConfigCheckpointMode {
	@Test
	public void testDefaultAndTable() {
		var conf = new Config();
		Assertions.assertEquals(CheckpointMode.Table, conf.getCheckpointMode());
		conf.setCheckpointMode(null); // null 归一为 Table
		Assertions.assertEquals(CheckpointMode.Table, conf.getCheckpointMode());
		conf.setCheckpointMode(CheckpointMode.Table);
		Assertions.assertEquals(CheckpointMode.Table, conf.getCheckpointMode());
	}

	@Test
	public void testImmediatelyRejectedBySetter() {
		var conf = new Config();
		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> conf.setCheckpointMode(CheckpointMode.Immediately));
		Assertions.assertEquals(CheckpointMode.Table, conf.getCheckpointMode()); // 拒绝后保持原值
	}

	@Test
	public void testImmediatelyRejectedByXml() throws Exception {
		var conf = new Config();
		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> conf.parse(parseElement("<zeze CheckpointMode='Immediately'/>")));
	}

	private static Element parseElement(String xml) throws Exception {
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
		return doc.getDocumentElement();
	}

	@Test
	public void testTableAcceptedByXml() throws Exception {
		var conf = new Config();
		conf.parse(parseElement("<zeze CheckpointMode='Table'/>"));
		Assertions.assertEquals(CheckpointMode.Table, conf.getCheckpointMode());
	}
}
