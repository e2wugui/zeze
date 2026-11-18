package Zeze.Onz;

import Zeze.Application;
import Zeze.Builtin.Onz.FuncProcedure;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestOnzReadyResultEncoding {
	@Test
	public void failedEncodingLeavesTidAvailable() throws Exception {
		var config = new Config();
		config.setServiceManager("disable");
		config.setNoDatabase(true);
		var onz = new Onz(new Application("TestOnzReadyResultEncoding", config));
		var stub = new OnzProcedureStub<>(onz, "encodeResult", (p, a, r) -> 0L,
				EmptyBean.class, EmptyBean.class);
		var request = new FuncProcedure();
		request.Argument.setOnzTid(1);
		var failure = new IllegalStateException("result encoding failed");
		var result = new EmptyBean() {
			@Override
			public void encode(ByteBuffer bb) {
				throw failure;
			}
		};
		var procedure = new OnzProcedure(request, request.Argument, stub, new EmptyBean(), result);

		assertSame(failure, assertThrows(IllegalStateException.class, procedure::sendReadyAndWait));
		var replacement = new OnzProcedure(request, request.Argument, stub, new EmptyBean(), new EmptyBean());
		assertDoesNotThrow(() -> onz.markReadyProcedure(replacement));
	}
}
