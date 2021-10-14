package Game.Buf;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BBufReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getId();
	public long getAttachTime();
	public long getContinueTime();
	public Zeze.Transaction.DynamicBeanReadOnly getExtra();

	public Game.Buf.BBufExtraReadOnly getExtraGameBufBBufExtra();
}