package Zeze.Arch;

import java.util.ArrayList;
import Zeze.Application;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Util.OutLong;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Fast
public class TestProviderLoadCapacity {

	@Test
	public void aggregateCapacityAboveIntRangeKeepsFullProvidersOutOfSelection() throws Exception {
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		var app = new Application("ProviderLoadCapacity", config);
		var sessions = new ArrayList<ProviderSession>();
		try {
			var service = mock(Service.class);
			var providers = new Agent.SubscribeState(new BSubscribeInfo("capacity", 1));
			var distribute = new ProviderDistribute(app, new LoadConfig(), service, 0);
			for (int id = 1; id <= 10; id++) {
				var session = new ProviderSession();
				sessions.add(session);
				session.sessionId = id;
				session.load.setProposeMaxOnline(Integer.MAX_VALUE);
				session.load.setOnline(id <= 2 ? 0 : Integer.MAX_VALUE);
				var socket = mock(AsyncSocket.class);
				when(socket.getUserState()).thenReturn(session);
				when(service.GetSocket(id)).thenReturn(socket);
				providers.onRegister(new BServiceInfo("capacity", Integer.toString(id), 0));
				providers.setIdentityLocalState(Integer.toString(id), new ProviderModuleState(id, 1, 0, false));
			}
			var selected = new OutLong();
			for (int i = 0; i < 64; i++) {
				assertTrue(distribute.choiceLoad(providers, selected));
				assertTrue(selected.value == 1 || selected.value == 2,
						"positive capacity remains; full provider selected: " + selected.value);
			}
		} finally {
			for (var session : sessions)
				session.timeCounter.close();
			app.stop();
		}
	}
}
