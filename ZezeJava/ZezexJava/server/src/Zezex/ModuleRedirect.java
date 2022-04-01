package Zezex;

import Zeze.Arch.IModuleRedirect;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.Transaction;

/**
 * ModuleRedirect 实现，
 * 需要在App.Start的最开头初始化，需要在Create之前。
 * Zeze.Application.setModuleRedirect(new ModuleRedirect());
 */
public class ModuleRedirect extends IModuleRedirect {
	@Override
	public int GetChoiceHashCode() {
		String account = ((Game.Login.Session) Transaction.getCurrent().getTopProcedure().getUserState()).getAccount();
		return Zeze.Serialize.ByteBuffer.calc_hashnr(account);
	}

	public static AsyncSocket RandomLink() {
		return Game.App.Instance.Server.RandomLink();
	}
}
