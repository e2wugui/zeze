package Zeze.Onz;

import Zeze.Application;
import Zeze.Builtin.Onz.FuncProcedure;
import Zeze.Builtin.Onz.FuncSaga;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.EmptyBean;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestOnzArgumentWindow {
	private static final class Decoded extends RuntimeException {
	}

	public static final class Argument extends EmptyBean {
		@Override
		public void decode(IByteBuffer buffer) {
			assertEquals(1, buffer.size(), "only the declared argument window is available to the decoder");
			assertEquals(37, buffer.ReadByte());
			assertEquals(0, buffer.size());
			throw new Decoded(); // stop at decoding, before starting any business transaction
		}
	}

	private static Onz participant() throws Exception {
		var config = new Config();
		config.setNoDatabase(true);
		config.setServiceManager("disable");
		return new Onz(new Application("OnzArgumentWindow", config));
	}

	private static Binary argument() {
		return new Binary(new byte[]{99, 37, 88}, 1, 1);
	}

	@Test
	public void procedureDecodesOnlyItsArgumentWindow() throws Exception {
		var onz = participant();
		onz.register("procedure", (p, a, r) -> 0, Argument.class, EmptyBean.class);
		var request = new FuncProcedure();
		request.Argument.setFuncName("procedure");
		request.Argument.setFuncArgument(argument());
		assertThrows(Decoded.class, () -> onz.ProcessFuncProcedureRequest(request));
	}

	@Test
	public void sagaDecodesOnlyItsArgumentWindow() throws Exception {
		var onz = participant();
		onz.registerSaga("saga", (p, a, r) -> 0, (p, a) -> 0,
				Argument.class, EmptyBean.class, EmptyBean.class);
		var request = new FuncSaga();
		request.Argument.setFuncName("saga");
		request.Argument.setFuncArgument(argument());
		assertThrows(Decoded.class, () -> onz.ProcessFuncSagaRequest(request));
	}
}
