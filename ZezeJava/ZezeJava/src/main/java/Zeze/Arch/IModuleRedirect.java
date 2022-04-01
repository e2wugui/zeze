package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.NotImplementedException;

public abstract class IModuleRedirect {
	public ConcurrentHashMap<String, RedirectHandle> Handles = new ConcurrentHashMap <>();

	public int GetDefaultChoiceType() {
		return Zeze.Beans.Provider.BModule.ChoiceTypeHashAccount;
	}

	public int GetChoiceHashCode() {
		throw new NotImplementedException("GetChoiceHashCode By Context");
	}

	public void DispatchResponse() {

	}
}
