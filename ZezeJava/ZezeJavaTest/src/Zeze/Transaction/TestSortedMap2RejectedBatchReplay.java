package Zeze.Transaction;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.History.Helper;
import Zeze.Serialize.ByteBuffer;
import dynamicred.Demo.Amount;
import dynamicred.Demo.Order;
import harness.Fast;
import org.junit.jupiter.api.Test;

@Fast
public class TestSortedMap2RejectedBatchReplay {
	@Test
	public void rejectedBatchPreservesSourceEntryForReplay() throws Exception {
		var app = Map2TestSupport.newApp("SortedMap2RejectedBatchReplay");
		try {
			var orders = new TableDynamic<Long, Order>(app, "sorted_map2_rejected_batch_orders", key -> {
				var bb = ByteBuffer.Allocate();
				bb.WriteLong(key);
				return bb;
			}, ByteBuffer::ReadLong, Order::new, false, null, Long.class, Order.class);
			Helper.registerLogs();
			Helper.registerTableLogs(orders);
			var capturedEdit = new AtomicReference<byte[]>();
			orders.getChangeListenerMap().addListener((key, record) -> {
				if (key.equals(1L) && record.getState() == Changes.Record.Edit) {
					var bb = ByteBuffer.Allocate();
					record.encode(bb);
					capturedEdit.set(bb.Copy());
				}
			});

			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var order = new Order();
				var item = Order.newDynamicBean_AmountSorted();
				item.setBean(new Amount(10));
				order.getAmountSorted().put(1, item);
				orders.put(1L, order);
				return Procedure.Success;
			}, "seedAmount").call());
			var follower = requireNonNull(orders.selectCopy(1L));
			assertEquals(10, amount(follower).getAmount());

			var destination = new Order().getAmountSorted();
			var sourceItem = new AtomicReference<DynamicBean>();
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var source = requireNonNull(orders.get(1L));
				var item = requireNonNull(source.getAmountSorted().get(1));
				var batch = new LinkedHashMap<Integer, DynamicBean>();
				batch.put(2, item);
				batch.put(3, Order.newDynamicBean_DirectAmount());
				assertThrows(IllegalArgumentException.class, () -> destination.putAll(batch));
				amount(source).setAmount(51);
				sourceItem.set(item);
				return Procedure.Success;
			}, "rejectBatchThenEditAmount").call());

			assertTrue(destination.isEmpty());
			assertEquals(51, amount(requireNonNull(orders.selectCopy(1L))).getAmount());
			var decoded = new Changes.Record(orders);
			var bytes = ByteBuffer.Wrap(requireNonNull(capturedEdit.get(), "missing source Edit"));
			decoded.decode(bytes);
			assertTrue(bytes.isEmpty());
			assertEquals(Changes.Record.Edit, decoded.getState());
			follower.followerApply(requireNonNull(decoded.getLogBean()));
			assertEquals(51, amount(follower).getAmount(), "the source entry edit must survive replay");
			assertEquals(1, requireNonNull(sourceItem.get()).mapKey());
		} finally {
			app.stop();
		}
	}

	private static Amount amount(Order order) {
		return (Amount)requireNonNull(order.getAmountSorted().get(1)).getBean();
	}
}
