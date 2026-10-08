package Zeze.MQ;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestMQPartitionSocketCloseResubscribe {

	// Pause at the iterator's old-entry snapshot: the replacement subscription arrives
	// after the closing thread has observed the old socket, before it removes the entry.
	private static final class ResubscribingMap extends ConcurrentHashMap<Long, AsyncSocket> {
		private Runnable afterNext;

		@Override
		public Set<Map.Entry<Long, AsyncSocket>> entrySet() {
			var entries = super.entrySet();
			return new AbstractSet<>() {
				@Override
				public Iterator<Map.Entry<Long, AsyncSocket>> iterator() {
					var iterator = entries.iterator();
					return new Iterator<>() {
						@Override
						public boolean hasNext() {
							return iterator.hasNext();
						}

						@Override
						public Map.Entry<Long, AsyncSocket> next() {
							var entry = iterator.next();
							var action = afterNext;
							afterNext = null;
							if (action != null)
								action.run();
							return entry;
						}

						@Override
						public void remove() {
							iterator.remove();
						}
					};
				}

				@Override
				public int size() {
					return entries.size();
				}
			};
		}
	}

	@Test
	public void socketCloseKeepsReplacementSubscription() throws Exception {
		var partition = new MQPartition(null); // no Application, database, or listening port
		var subscriptions = new ResubscribingMap();
		var field = MQPartition.class.getDeclaredField("subscribes");
		field.setAccessible(true);
		field.set(partition, subscriptions);
		var service = new Service("TestMQPartitionSocketCloseResubscribe");
		var oldSocket = new MqTestSupport.FakeSocket(service);
		var newSocket = new MqTestSupport.FakeSocket(service);
		partition.subscribe(oldSocket, 1);
		subscriptions.afterNext = () -> partition.subscribe(newSocket, 1);

		partition.onSocketClose(oldSocket);

		Assertions.assertSame(newSocket, subscriptions.get(1L),
				"closing an old connection must not remove a concurrent replacement subscription");
		partition.onSocketClose(newSocket);
		Assertions.assertTrue(subscriptions.isEmpty(), "closing the current connection removes its subscription");
	}
}
