package Zeze.Net;

import javax.xml.parsers.DocumentBuilderFactory;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestServiceConfPublication {
	@Test
	public void publishesOnlyCompleteNamedAndDefaultConfigurations() throws Exception {
		var config = new Config();
		var originalDefault = config.getDefaultServiceConf();
		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		var service = document.createElement("ServiceConf");
		var acceptor = document.createElement("Acceptor");
		service.appendChild(acceptor);
		var unknown = document.createElement("Unknown");
		service.appendChild(unknown);

		assertThrows(IllegalStateException.class, () -> new ServiceConf(config, service));
		assertSame(originalDefault, config.getDefaultServiceConf());

		service.setAttribute("Name", "configured");
		assertThrows(IllegalStateException.class, () -> new ServiceConf(config, service));
		assertFalse(config.getServiceConfMap().containsKey("configured"));

		service.removeChild(unknown);
		var complete = new ServiceConf(config, service);
		assertSame(complete, config.getServiceConfMap().get("configured"));
		assertEquals(1, complete.acceptorCount());
	}
}
