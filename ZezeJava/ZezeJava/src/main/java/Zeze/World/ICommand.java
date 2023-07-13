package Zeze.World;

import Zeze.Builtin.World.Command;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Data;

/**
 * 客户端服务器之间互发抽象协议，具体什么协议用二级CommandId描述。
 */
public interface ICommand {
	long handle(String account, String playerId, Command c) throws Exception;

	static <T extends Data> T decode(T data, Command c) {
		data.decode(ByteBuffer.Wrap(c.Argument.getParam()));
		return data;
	}

}
