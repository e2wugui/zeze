package Zeze.Util;

import java.util.Map;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestCompilerRejectsStaleOutput {
	@Test
	public void missingRequestedClassCannotReusePreviousBytecode() throws Exception {
		var compiler = new InMemoryJavaCompiler();
		compiler.compileAllToByteCode(Map.of("CurrentOutput", "public class CurrentOutput {}"));
		var error = assertThrows(IllegalStateException.class, () -> compiler.compileAllToByteCode(
				Map.of("CurrentOutput", "class OtherOutput {}")));
		assertEquals("compiled class not found: CurrentOutput", error.getMessage());
		compiler.compileAllToByteCode(Map.of("CurrentOutput", "public class CurrentOutput {}"));
		assertEquals("CurrentOutput", compiler.defineCompiled("CurrentOutput").getName());
	}

	@Test
	public void anotherCompilationKeepsUnrelatedPendingClasses() throws Exception {
		var compiler = new InMemoryJavaCompiler();
		compiler.compileAllToByteCode(Map.of("PendingOutput", "public class PendingOutput {}"));
		compiler.compileAllToByteCode(Map.of("FreshOutput", "public class FreshOutput {}"));
		assertEquals("PendingOutput", compiler.defineCompiled("PendingOutput").getName());
		assertEquals("FreshOutput", compiler.defineCompiled("FreshOutput").getName());
	}
}
