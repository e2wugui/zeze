package Zeze.Raft;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

	public static class ConnectorEx extends Connector {
		private long Term;
		public final long getTerm() {
			return Term;
		}
		public final void setTerm(long value) {
			Term = value;
		}


		public ConnectorEx(String host) {
			this(host, 0);
		}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public ConnectorEx(string host, int port = 0)
		public ConnectorEx(String host, int port) {
			super(host, port);
		}

		@Override
		public void OnSocketClose(AsyncSocket closed) {
			// 先关闭重连，防止后面重发收集前又连上。
			// see Agent.NetClient
			super.setAutoReconnect(false);
			super.OnSocketClose(closed);
		}
	}