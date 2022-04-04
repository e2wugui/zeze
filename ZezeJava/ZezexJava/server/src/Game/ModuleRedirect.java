package Game;

import Zeze.Arch.ProviderUserSession;
import Zeze.Arch.RedirectBase;
import Zeze.Transaction.Transaction;

/**
 * ModuleRedirect 实现，
 * 需要在App.Start的最开头初始化，需要在Create之前。
 * Zeze.Application.setModuleRedirect(new ModuleRedirect());
 */
public class ModuleRedirect extends RedirectBase {
	@Override
	public int GetChoiceHashCode() {
		String account = ((ProviderUserSession) Transaction.getCurrent().getTopProcedure().getUserState()).getAccount();
		return Zeze.Serialize.ByteBuffer.calc_hashnr(account);
	}

	public ModuleRedirect(Zeze.Arch.ProviderApp app) {
		super(app);
	}
}
