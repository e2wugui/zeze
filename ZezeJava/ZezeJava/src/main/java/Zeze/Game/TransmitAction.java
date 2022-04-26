package Zeze.Game;

import Zeze.Net.Binary;

@FunctionalInterface
public interface TransmitAction {
	long call(long sender, long target, Binary parameter);
}
