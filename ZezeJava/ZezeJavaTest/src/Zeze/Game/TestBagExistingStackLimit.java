package Zeze.Game;

import Zeze.Application;
import Zeze.Builtin.Game.Bag.BBag;
import Zeze.Builtin.Game.Bag.BItem;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@Fast
public class TestBagExistingStackLimit {
	@Test
	void existingStackAboveCurrentLimitKeepsItsItems() throws Exception {
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		var app = new Application("TestBagExistingStackLimit", config);
		try {
			var module = new Bag.Module(app) {
				@Override
				public void RegisterZezeTables(Application zeze) {
					// This fixture uses an unmanaged bag.
				}
			};
			module.funcItemPileMax = __ -> 1;
			var bag = new Bag(module, new BBag(), "items");
			bag.setCapacity(2);
			var oldStack = item(Integer.MAX_VALUE);
			var source = item(1);
			bag.getBean().getItems().put(0, oldStack);
			bag.getBean().getItems().put(1, source);

			assertEquals(2, bag.add(0, item(2)));
			assertEquals(0, bag.move(1, 0, 1));
			assertSame(source, bag.getBean().getItems().get(1));
			assertEquals(1, source.getNumber());
			assertEquals(Integer.MAX_VALUE, oldStack.getNumber());
		} finally {
			app.stop();
		}
	}

	private static BItem item(int number) {
		var item = new BItem();
		item.setId(1);
		item.setNumber(number);
		return item;
	}
}
