package Zeze.World.Mmo;

import Zeze.Builtin.World.BCommand;
import Zeze.Builtin.World.BMoveMmo;
import Zeze.Builtin.World.Command;
import Zeze.Serialize.ByteBuffer;
import Zeze.World.ICommandHandler;
import Zeze.World.IComponent;
import Zeze.World.World;

public class MoveMmo implements IComponent, ICommandHandler {
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
			var moveArg = new BMoveMmo();
			moveArg.decode(ByteBuffer.Wrap(c.Argument.getParam()));
			return onMove(moveArg);
		}
		return 0;
	}

	private long onMove(BMoveMmo move) {
		return 0;
	}
}
