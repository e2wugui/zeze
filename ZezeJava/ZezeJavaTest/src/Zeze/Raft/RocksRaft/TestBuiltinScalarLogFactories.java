package Zeze.Raft.RocksRaft;

import java.math.BigDecimal;
import Zeze.Raft.RocksRaft.Log1.*;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Quaternion;
import Zeze.Serialize.Vector2;
import Zeze.Serialize.Vector2Int;
import Zeze.Serialize.Vector3;
import Zeze.Serialize.Vector3Int;
import Zeze.Serialize.Vector4;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestBuiltinScalarLogFactories {
	@Test public void decodesInt() throws Exception { roundTrip(new LogInt(null, 1, 42)); }
	@Test public void decodesDecimal() throws Exception {
		roundTrip(new LogDecimal(null, 1, new BigDecimal("123456789012345678901234567890123456789.012345")));
	}
	@Test public void decodesVector2() throws Exception { roundTrip(new LogVector2(null, 1, new Vector2(1, 2))); }
	@Test public void decodesVector2Int() throws Exception { roundTrip(new LogVector2Int(null, 1, new Vector2Int(1, 2))); }
	@Test public void decodesVector3() throws Exception { roundTrip(new LogVector3(null, 1, new Vector3(1, 2, 3))); }
	@Test public void decodesVector3Int() throws Exception { roundTrip(new LogVector3Int(null, 1, new Vector3Int(1, 2, 3))); }
	@Test public void decodesVector4() throws Exception { roundTrip(new LogVector4(null, 1, new Vector4(1, 2, 3, 4))); }
	@Test public void decodesQuaternion() throws Exception { roundTrip(new LogQuaternion(null, 1, new Quaternion(1, 2, 3, 4))); }

	private static void roundTrip(Log field) throws Exception {
		// Generated RegisterRocksTables registers collections/BeanKeys; scalar factories belong to Rocks.
		Class.forName(Rocks.class.getName());
		var source = new LogBean();
		source.getVariablesOrNew().put(field.getVariableId(), field);
		var wire = ByteBuffer.Allocate();
		source.encode(wire);
		var decoded = new LogBean();
		decoded.decode(ByteBuffer.Wrap(wire.Copy()));
		assertNotNull(decoded.getVariables());
		assertInstanceOf(field.getClass(), decoded.getVariables().get(field.getVariableId()));
		var reencoded = ByteBuffer.Allocate();
		decoded.encode(reencoded);
		assertArrayEquals(wire.Copy(), reencoded.Copy());
	}
}
