package Zeze.World;

import Zeze.Builtin.World.Command;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Data;

public interface ICommand {
	long handle(String playerId, Command c) throws Exception;

	static <T extends Data> T decode(T data, Command c) {
		data.decode(ByteBuffer.Wrap(c.Argument.getParam()));
		return data;
	}

}
