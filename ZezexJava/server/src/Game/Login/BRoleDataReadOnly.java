package Game.Login;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BRoleDataReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public String getName();
}