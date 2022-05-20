package Zeze.Arch;

import Zeze.Builtin.Online.BLocal;
import Zeze.Util.EventDispatcher;

public class LocalRemoveEventArgument implements EventDispatcher.EventArgument {
    public String Account;
    public String ClientId;
    public BLocal LocalData;
}
