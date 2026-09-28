package Zeze.Services.GlobalCacheManager;

import Zeze.Net.Binary;
import Zeze.Net.Rpc;

/**
 * Acquire协议：向GCM申请全局键所有权（Share/Modify），state=StateInvalid时为释放；
 * Argument/Result均为键与状态。
 */
public class Acquire extends Rpc<BGlobalKeyState, BGlobalKeyState> {
	public static final int ProtocolId_ = Zeze.Transaction.Bean.hash32(Acquire.class.getName()); // 1225831619
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 1225831619

	static {
		register(TypeId_, Acquire.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public Acquire() {
		Argument = new BGlobalKeyState();
		Result = new BGlobalKeyState();
	}

	public Acquire(Binary gkey, int state) {
		Argument = new BGlobalKeyState();
		Result = new BGlobalKeyState();
		Argument.globalKey = gkey;
		Argument.state = state;
	}
}
