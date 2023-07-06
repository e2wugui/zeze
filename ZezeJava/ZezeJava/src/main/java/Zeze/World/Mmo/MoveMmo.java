package Zeze.World.Mmo;

import Zeze.Builtin.World.BCommand;
import Zeze.Builtin.World.BMoveMmo;
import Zeze.Builtin.World.Command;
import Zeze.Serialize.ByteBuffer;
import Zeze.World.ICommand;
import Zeze.World.IComponent;
import Zeze.World.World;

public class MoveMmo implements IComponent, ICommand {
	public final World world;

	public MoveMmo(World world) {
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
	public long handle(Command c) throws Exception {
		switch (c.Argument.getCommandId()) {
		case BCommand.eMoveMmo:
			return onMove(ICommand.decode(new BMoveMmo.Data(), c));
		}
		return 0;
	}

	private long onMove(BMoveMmo.Data move) {
		return 0;
	}
}
