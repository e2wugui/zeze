package Zeze;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestSchemasDuplicateRegistration {
	@Test
	public void rejectedBeanAndTablePreserveOriginalRegistrations() {
		var schemas = new Schemas();
		var bean = new Schemas.Bean("value", false);
		var table = new Schemas.Table("table", "long", "value");
		schemas.addBean(bean);
		schemas.addTable(table);

		assertThrows(IllegalStateException.class, () -> schemas.addBean(new Schemas.Bean("value", true)));
		assertThrows(IllegalStateException.class, () -> schemas.addTable(new Schemas.Table("table", "int", "value")));

		assertSame(bean, schemas.beans.get("value"));
		assertSame(table, schemas.tables.get("table"));
	}
}
