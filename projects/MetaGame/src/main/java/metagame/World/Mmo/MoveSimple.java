package metagame.World.Mmo;

import metagame.builtin.World.BCommand;
import metagame.builtin.World.BMove;
import metagame.builtin.World.Command;
import Zeze.Transaction.Procedure;
import metagame.World.ICommand;
import metagame.World.IComponent;
import metagame.World.World;

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
		//noinspection SwitchStatementWithTooFewBranches
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

		var entity = map.players.get(playerId);
		if (null == entity)
			return Procedure.LogicError;
		map.getAoi().moveTo(entity, move);
		return 0;
	}
}
