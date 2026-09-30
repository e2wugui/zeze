package Zeze.Transaction;

import java.util.List;
import java.util.Map;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import com.amazonaws.services.dynamodbv2.AbstractAmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestDynamoDbSchemaTableReadiness {
	@Test
	public void newlyCreatedSchemaTableCanBeReadImmediately() {
		var database = new DatabaseDynamoDb(null, config(), new CreatingSchemaClient(false));
		try {
			var result = database.getDirectOperates().getDataWithVersion(key());
			assertEquals(0, result.version);
			assertNull(result.data);
		} finally {
			database.close();
		}
	}

	@Test
	public void newlyCreatedSchemaTableCanSaveMetadataImmediately() {
		var database = new DatabaseDynamoDb(null, config(), new CreatingSchemaClient(false));
		try {
			var data = new byte[]{11, 22, 33};
			var saved = database.getDirectOperates().saveDataWithSameVersion(key(), ByteBuffer.Wrap(data), 0);
			assertTrue(saved.getValue());
			assertEquals(1, saved.getKey());
			var result = database.getDirectOperates().getDataWithVersion(key());
			assertEquals(1, result.version);
			assertArrayEquals(data, result.data.Copy());
		} finally {
			database.close();
		}
	}

	@Test
	public void schemaTableBeingCreatedByAnotherInstanceCanBeReadImmediately() {
		var database = new DatabaseDynamoDb(null, config(), new CreatingSchemaClient(true));
		try {
			var result = database.getDirectOperates().getDataWithVersion(key());
			assertEquals(0, result.version);
			assertNull(result.data);
		} finally {
			database.close();
		}
	}

	private static Config.DatabaseConf config() {
		var config = new Config.DatabaseConf();
		config.setDatabaseType(Config.DbType.DynamoDb);
		return config;
	}

	private static ByteBuffer key() {
		return ByteBuffer.Wrap(new byte[]{1});
	}

	/**
	 * CreateTable returns before the table becomes ACTIVE. The first status observation here
	 * sees creation complete; any earlier data access fails as required by the DynamoDB API.
	 * https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_CreateTable.html
	 */
	private static final class CreatingSchemaClient extends AbstractAmazonDynamoDB {
		private final boolean alreadyCreating;
		private TableStatus status = TableStatus.CREATING;
		private byte[] value;

		private CreatingSchemaClient(boolean alreadyCreating) {
			this.alreadyCreating = alreadyCreating;
		}

		@Override
		public CreateTableResult createTable(List<AttributeDefinition> attributes, String tableName,
				List<KeySchemaElement> keys, ProvisionedThroughput throughput) {
			if (alreadyCreating)
				throw new ResourceInUseException("Schema table is already being created");
			return new CreateTableResult().withTableDescription(description(tableName));
		}

		@Override
		public DescribeTableResult describeTable(String tableName) {
			status = TableStatus.ACTIVE;
			return new DescribeTableResult().withTable(description(tableName));
		}

		@Override
		public GetItemResult getItem(GetItemRequest request) {
			requireActive();
			return value == null ? new GetItemResult() : new GetItemResult().withItem(
					Map.of("value", new AttributeValue().withB(java.nio.ByteBuffer.wrap(value))));
		}

		@Override
		public PutItemResult putItem(PutItemRequest request) {
			requireActive();
			var binary = request.getItem().get("value").getB().duplicate();
			value = new byte[binary.remaining()];
			binary.get(value);
			return new PutItemResult();
		}

		@Override
		public void shutdown() {
		}

		private TableDescription description(String tableName) {
			return new TableDescription().withTableName(tableName).withTableStatus(status);
		}

		private void requireActive() {
			if (status != TableStatus.ACTIVE)
				throw new ResourceNotFoundException("Schema table is still " + status);
		}
	}
}
