package Zeze.Services.Handshake;

import Zeze.*;
import Zeze.Services.*;
import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import java.util.*;
import java.math.*;

public final class CHandshakeDone extends Protocol<EmptyBean> {
	public final static int ProtocolId_ = Bean.Hash16(CHandshakeDone.class.FullName);

	@Override
	public int getModuleId() {
		return 0;
	}
	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}
}