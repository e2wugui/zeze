package Zeze.World.Mmo;

import Zeze.Builtin.World.BCommand;
import Zeze.Builtin.World.BMove;
import Zeze.Builtin.World.Command;
import Zeze.Transaction.Procedure;
import Zeze.World.ICommand;
import Zeze.World.IComponent;
import Zeze.World.World;

public class MoveSimple implements IComponent, ICommand {
	public final World world;

	public MoveSimple(World world) {
		this.world = world;
	}

	@Override
	public void install(World world) throws Exception {
		world.internalRegisterCommand(BCommand.eMoveMmo, this);
	}

	@Override
	public void start(World world) throws Exception {

	}

	@Override
	public long handle(String account, String playerId, Command c) throws Exception {
		switch (c.Argument.getCommandId()) {
		case BCommand.eMoveMmo:
			return onMove(account, playerId, c.Argument.getMapInstanceId(), ICommand.decode(new BMove.Data(), c));
		}
		return 0;
	}

	private long onMove(String account, String playerId, long mapInstanceId, BMove.Data move) throws Exception {
		var map = world.getMapManager().getMap(mapInstanceId);
		if (null == map)
			return Procedure.LogicError;
		// todo 测试环境目前是roleId，但是测试代码没有实现role的登录登出，所以这些先用account。
		var entity = map.players.get(account);
		if (null == entity)
			return Procedure.LogicError;
		map.getAoi().moveTo(entity, move);
		return 0;
	}
}
