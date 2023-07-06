package Zeze.World;

import Zeze.Builtin.World.BCommand;
import Zeze.Builtin.World.Command;

public class MoveMmo implements Component, CommandHandler {
	private World world;

	@Override
	public void install(World world) throws Exception {
		world.internalRegisterCommand(BCommand.eMoveMmo, this);

		this.world = world;
	}

	@Override
	public void start(World world) throws Exception {

	}

	@Override
	public long handle(Command c) throws Exception {
		return 0;
	}
}
