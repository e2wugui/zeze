
package Zeze.Arch;

import Zeze.Builtin.Online.BOnline;
import Zeze.Util.EventDispatcher;

public class LogoutEventArgument implements EventDispatcher.EventArgument {
    public String Account;
    public String ClientId;
    public BOnline OnlineData;
}
