package Zeze.Transaction;

import harness.Fast;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import com.amazonaws.services.dynamodbv2.AbstractAmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import Zeze.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestDatabaseBufferWindows {
	@Test
	public void prefixSliceDoesNotExposeTrailingBackingBytes() {
		var source = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5});
		var prefix = source.slice(0, 2);
		Assertions.assertArrayEquals(new byte[]{1, 2}, Database.copyIf(prefix));
	}

	@Test
	public void buffersWithoutAccessibleArraysPreserveTheirWindowAndCursor() {
		var direct = ByteBuffer.allocateDirect(5);
		direct.put(new byte[]{0, 1, 2, 3, 4}).flip();
		var heap = ByteBuffer.wrap(new byte[]{0, 1, 2, 3, 4});
		for (var source : List.of(direct, direct.asReadOnlyBuffer(), heap.asReadOnlyBuffer())) {
			source.position(1).mark().position(2);
			Assertions.assertArrayEquals(new byte[]{2, 3, 4}, Database.copyIf(source));
			Assertions.assertEquals(2, source.position());
			Assertions.assertEquals(5, source.limit());
			source.reset();
			Assertions.assertEquals(1, source.position());
			source.limit(1);
			Assertions.assertArrayEquals(new byte[0], Database.copyIf(source));
			source.reset();
			Assertions.assertEquals(1, source.position());
		}
	}

	@Test
	public void completeAccessibleArrayKeepsTheSharingContract() {
		var bytes = new byte[]{1, 2, 3};
		Assertions.assertSame(bytes, Database.copyIf(ByteBuffer.wrap(bytes)));
	}

	@Test
	public void dynamoFindUsesTheSameRemainingWindow() {
		var heap = ByteBuffer.wrap(new byte[]{9, 2, 3, 8}).position(1).limit(3);
		var direct = ByteBuffer.allocateDirect(4);
		direct.put(new byte[]{9, 2, 3, 8}).position(1).limit(3);
		var prefix = ByteBuffer.wrap(new byte[]{2, 3, 8}).slice(0, 2);
		for (var source : List.of(heap, direct, heap.asReadOnlyBuffer(), prefix)) {
			var config = new Config.DatabaseConf();
			config.setDisableOperates(true);
			var database = new DatabaseDynamoDb(null, config, new Client(source));
			int position = source.position();
			source.mark();
			try {
				var table = (Database.AbstractKVTable)database.openTable("window", 0);
				var result = table.find(Zeze.Serialize.ByteBuffer.Wrap(new byte[]{7}));
				Assertions.assertNotNull(result);
				Assertions.assertArrayEquals(new byte[]{2, 3}, result.Copy());
				Assertions.assertEquals(position, source.position());
				source.reset();
				Assertions.assertEquals(position, source.position());
			} finally {
				database.close();
			}
		}
	}

	private static final class Client extends AbstractAmazonDynamoDB {
		private final ByteBuffer value;

		Client(ByteBuffer value) {
			this.value = value;
		}

		@Override
		public CreateTableResult createTable(List<AttributeDefinition> attributes, String tableName,
				List<KeySchemaElement> keys, ProvisionedThroughput throughput) {
			throw new ResourceInUseException("existing table");
		}

		@Override
		public GetItemResult getItem(GetItemRequest request) {
			return new GetItemResult().withItem(Map.of("value", new AttributeValue().withB(value)));
		}

		@Override
		public void shutdown() {
		}
	}
}
