package Zeze.World.Mmo;

import Zeze.Builtin.World.BCommand;
import Zeze.Builtin.World.BMove;
import Zeze.Builtin.World.Command;
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
	public long handle(String playerId, Command c) throws Exception {
		switch (c.Argument.getCommandId()) {
		case BCommand.eMoveMmo:
			return onMove(playerId, ICommand.decode(new BMove.Data(), c));
		}
		return 0;
	}

	private long onMove(String playerId, BMove.Data move) {
		return 0;
	}
}
