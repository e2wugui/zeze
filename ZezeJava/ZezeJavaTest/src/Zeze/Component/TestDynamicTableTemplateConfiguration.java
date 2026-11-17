package Zeze.Component;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.TableDynamic;
import demo.Module1.Table3;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@Fast
public class TestDynamicTableTemplateConfiguration {
	@Test
	public void templateControlsCacheAndCommitDurability() throws Exception {
		var conf = TakeoverTestEnv.newConf("off", 600_000, 600_000);
		var template = new Table3();
		var tableConf = new Config.TableConf();
		tableConf.setCheckpointWhenCommit(true);
		tableConf.setCacheCapacity(7);
		conf.getTableConfMap().put(template.getName(), tableConf);
		var app = new Application("DynamicTableTemplateConfiguration." + conf.getServerId(), conf);
		app.addTable("", template);
		try {
			app.start();
			var table = new TableDynamic<>(app, "dynamic_template_configuration", template);
			assertEquals(0, app.newProcedure(() -> {
				table.getOrAdd(1L).setInt_1(42);
				return 0;
			}, "DynamicTableTemplateConfiguration.write").call());
			var persisted = table.selectFromDatabase(1L);
			assertNotNull(persisted, "template CheckpointWhenCommit must persist before returning");
			assertEquals(42, persisted.getInt_1());
			assertSame(tableConf, table.getTableConf());
			assertEquals(7, table.getTableConf().getCacheCapacity());
		} finally {
			app.stop();
		}
	}
}
