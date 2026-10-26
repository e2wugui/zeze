package Zeze.Transaction;

import com.amazonaws.services.dynamodbv2.AbstractAmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import harness.Fast;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Zeze.Config;

@Fast
public class TestDynamoClientConstruction {
	private static final class Client extends AbstractAmazonDynamoDB {
		final RuntimeException failure = new IllegalStateException("metadata initialization failed");
		final @Nullable RuntimeException closeFailure;
		final boolean failWhenCreating;
		int closes;

		Client(boolean failWhenCreating, @Nullable RuntimeException closeFailure) {
			this.failWhenCreating = failWhenCreating;
			this.closeFailure = closeFailure;
		}

		@Override
		public CreateTableResult createTable(List<AttributeDefinition> attributes, String tableName,
				List<KeySchemaElement> keys, ProvisionedThroughput throughput) {
			if (failWhenCreating)
				throw failure;
			return new CreateTableResult();
		}

		@Override
		public DescribeTableResult describeTable(String tableName) {
			throw failure;
		}

		@Override
		public void shutdown() {
			closes++;
			if (closeFailure != null)
				throw closeFailure;
		}
	}

	private static void verifyFailure(boolean duringCreate, @Nullable RuntimeException closeFailure) {
		var client = new Client(duringCreate, closeFailure);
		var thrown = Assertions.assertThrows(IllegalStateException.class,
				() -> new DatabaseDynamoDb(null, new Config.DatabaseConf(), client));
		Assertions.assertSame(client.failure, thrown);
		Assertions.assertEquals(1, client.closes, "unpublished client must be shut down");
		if (closeFailure != null)
			Assertions.assertArrayEquals(new Throwable[]{closeFailure}, thrown.getSuppressed());
	}

	@Test
	public void createMetadataFailureShutsDownTheClient() {
		verifyFailure(true, null);
	}

	@Test
	public void waitForMetadataFailureShutsDownTheClient() {
		verifyFailure(false, null);
	}

	@Test
	public void shutdownFailureIsSuppressedByTheInitializationFailure() {
		verifyFailure(false, new IllegalStateException("shutdown failed"));
	}
}
