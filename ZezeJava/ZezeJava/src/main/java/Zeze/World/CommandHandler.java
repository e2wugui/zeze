package Zeze.World;

import Zeze.Builtin.World.Command;

public interface CommandHandler {
	long handle(Command c) throws Exception;
}
