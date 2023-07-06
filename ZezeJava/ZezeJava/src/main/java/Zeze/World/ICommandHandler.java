package Zeze.World;

import Zeze.Builtin.World.Command;

public interface ICommandHandler {
	long handle(Command c) throws Exception;
}
