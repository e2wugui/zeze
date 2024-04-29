package Zeze.History;

import Zeze.Application;
import Zeze.Builtin.HistoryModule.ZezeHistoryTable_m_a_g_i_c;

public class HistoryModule extends AbstractHistoryModule {
	private final Application zeze;

	public HistoryModule(Application zeze) {
		this.zeze = zeze;
	}

	public Application getZeze() {
		return zeze;
	}

	public ZezeHistoryTable_m_a_g_i_c getTable() {
		return _ZezeHistoryTable_m_a_g_i_c;
	}
}
