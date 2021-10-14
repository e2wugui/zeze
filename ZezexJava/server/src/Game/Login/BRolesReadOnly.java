package Game.Login;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BRolesReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public System.Collections.Generic.IReadOnlyList<Game.Login.BRoleReadOnly> getRoleList();
	public long getLastLoginRoleId();
}