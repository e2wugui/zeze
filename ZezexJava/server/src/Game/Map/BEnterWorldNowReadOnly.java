package Game.Map;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public interface BEnterWorldNowReadOnly {
	public long getTypeId();
	public void Encode(ByteBuffer _os_);
	public boolean NegativeCheck();
	public Zeze.Transaction.Bean CopyBean();

	public int getMapInstanceId();
	public float getX();
	public float getY();
	public float getZ();
	public int getResouceId();
}