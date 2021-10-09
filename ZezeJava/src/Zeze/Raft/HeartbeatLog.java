package Zeze.Raft;

import Zeze.Serialize.*;
import Zeze.Transaction.*;
import RocksDbSharp.*;
import Zeze.Net.*;
import Zeze.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;

public final class HeartbeatLog extends Log {
	public static final int SetLeaderReadyEvent = 1;

	private int Operate;
	public int getOperate() {
		return Operate;
	}
	private void setOperate(int value) {
		Operate = value;
	}


	public HeartbeatLog() {
		this(0);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public HeartbeatLog(int operate = 0)
	public HeartbeatLog(int operate) {
		super("", 0);
		setOperate(operate);
	}

	@Override
	public void Apply(StateMachine stateMachine) {
		switch (getOperate()) {
			case SetLeaderReadyEvent:
				stateMachine.getRaft().SetLeaderReady();
				break;
		}
	}

	@Override
	public void Decode(ByteBuffer bb) {
		super.Decode(bb);
		setOperate(bb.ReadInt());
	}

	@Override
	public void Encode(ByteBuffer bb) {
		super.Encode(bb);
		bb.WriteInt(getOperate());
	}
}