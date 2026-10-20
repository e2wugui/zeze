package Zeze.Game;

import Zeze.Application;
import Zeze.Builtin.Game.Bag.BBag;
import Zeze.Builtin.Game.Bag.BItem;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestBagStackAmounts {
	private static Bag newBag(int pileMax) throws Exception {
		// Unmanaged beans exercise the arithmetic; no application start, cache or shared database.
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		var module = new Bag.Module(new Application("TestBagStackAmounts", config)) {
			@Override
			public void RegisterZezeTables(Application zeze) {
				// This fixture uses BBag directly, so no table registration is needed.
			}
		};
		module.funcItemPileMax = __ -> pileMax;
		var bag = new Bag(module, new BBag(), "items");
		bag.setCapacity(4);
		return bag;
	}

	private static BItem item(int count) {
		var item = new BItem();
		item.setId(1);
		item.setNumber(count);
		return item;
	}

	private static long total(Bag bag) {
		return bag.getBean().getItems().values().stream().mapToLong(BItem::getNumber).sum();
	}

	@Test
	void addPreservesTotalBeyondSignedIntRange() throws Exception {
		var bag = newBag(2_000_000_000);
		assertEquals(0, bag.add(1, 1_500_000_000));
		assertEquals(0, bag.add(1, 1_500_000_000));
		assertEquals(3_000_000_000L, total(bag));
		assertEquals(2, bag.getBean().getItems().size());
		assertTrue(bag.getBean().getItems().values().stream()
				.allMatch(item -> item.getNumber() > 0 && item.getNumber() <= 2_000_000_000));
	}

	@Test
	void hintedAddPreservesTotalBeyondSignedIntRange() throws Exception {
		var bag = newBag(2_000_000_000);
		assertEquals(0, bag.add(0, item(1_500_000_000)));
		assertEquals(0, bag.add(0, item(1_500_000_000)));
		assertEquals(3_000_000_000L, total(bag));
		assertEquals(2_000_000_000, requireNonNull(bag.getBean().getItems().get(0)).getNumber());
	}

	@Test
	void moveOnlyTransfersAvailableCapacity() throws Exception {
		var bag = newBag(2_000_000_000);
		bag.getBean().getItems().put(0, item(1_500_000_000));
		bag.getBean().getItems().put(1, item(1_500_000_000));
		assertEquals(0, bag.move(0, 1, -1));
		assertEquals(3_000_000_000L, total(bag));
		assertEquals(1_000_000_000, requireNonNull(bag.getBean().getItems().get(0)).getNumber());
		assertEquals(2_000_000_000, requireNonNull(bag.getBean().getItems().get(1)).getNumber());
	}

	@Test
	void sameSlotAndSmallTransfersKeepTheirSemantics() throws Exception {
		var bag = newBag(10);
		bag.getBean().getItems().put(0, item(8));
		bag.getBean().getItems().put(1, item(9));
		assertEquals(0, bag.move(0, 0, -1));
		assertEquals(8, requireNonNull(bag.getBean().getItems().get(0)).getNumber());
		assertEquals(0, bag.move(0, 1, 3));
		assertEquals(7, requireNonNull(bag.getBean().getItems().get(0)).getNumber());
		assertEquals(10, requireNonNull(bag.getBean().getItems().get(1)).getNumber());
		assertEquals(17, total(bag));
	}
}
