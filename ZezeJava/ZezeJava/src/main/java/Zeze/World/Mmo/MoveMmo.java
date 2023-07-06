package Zeze.World.Mmo;

import Zeze.Builtin.World.BCommand;
import Zeze.Builtin.World.Command;
import Zeze.World.CommandHandler;
import Zeze.World.Component;
import Zeze.World.World;

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
