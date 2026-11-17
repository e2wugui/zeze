package Zeze.Arch;

import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Builtin.Provider.BModule;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestModuleBindLoadChoice {
	@Test
	void configuredLoadAndRequestChoicesRetainTheirIdentity(@TempDir Path directory) throws Exception {
		var file = directory.resolve("provider.module.binds.xml");
		Files.writeString(file, """
				<ProviderModuleBinds>
				  <module name="Load" ChoiceType="ChoiceTypeLoad"/>
				  <module name="Request" ChoiceType="ChoiceTypeRequest"/>
				  <module name="Default"/>
				</ProviderModuleBinds>
				""");
		var modules = ProviderModuleBinds.load(file.toString()).getModules();
		assertEquals(BModule.ChoiceTypeLoad, modules.get("Load").getChoiceType());
		assertEquals(BModule.ChoiceTypeRequest, modules.get("Request").getChoiceType());
		assertEquals(BModule.ChoiceTypeDefault, modules.get("Default").getChoiceType());
	}
}
