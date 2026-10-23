package Zeze.Transaction;

import java.math.BigDecimal;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Quaternion;
import Zeze.Serialize.Vector2;
import Zeze.Serialize.Vector2Int;
import Zeze.Serialize.Vector3;
import Zeze.Serialize.Vector3Int;
import Zeze.Serialize.Vector4;

/** A field change value or a container change payload. Encoding never applies a transaction. */
public abstract class Log {
	private final int varId;

	protected Log(int varId) {
		this.varId = varId;
	}

	public final int getVariableId() {
		return varId;
	}

	public abstract int getTypeId();

	public String getTypeName() {
		return getClass().getSimpleName();
	}

	public abstract void encode(ByteBuffer bb);

	public boolean booleanValue() {
		return longValue() != 0;
	}

	public byte byteValue() {
		return (byte)longValue();
	}

	public short shortValue() {
		return (short)longValue();
	}

	public int intValue() {
		return (int)longValue();
	}

	public long longValue() {
		throw new UnsupportedOperationException(getTypeName());
	}

	public float floatValue() {
		return (float)doubleValue();
	}

	public double doubleValue() {
		throw new UnsupportedOperationException(getTypeName());
	}

	public Binary binaryValue() {
		throw new UnsupportedOperationException(getTypeName());
	}

	public String stringValue() {
		throw new UnsupportedOperationException(getTypeName());
	}

	public BigDecimal decimalValue() {
		throw new UnsupportedOperationException(getTypeName());
	}

	public Vector2 vector2Value() {
		return new Vector2(floatValue(), 0);
	}

	public Vector2Int vector2IntValue() {
		return new Vector2Int(intValue(), 0);
	}

	public Vector3 vector3Value() {
		return new Vector3(floatValue(), 0, 0);
	}

	public Vector3Int vector3IntValue() {
		return new Vector3Int(intValue(), 0, 0);
	}

	public Vector4 vector4Value() {
		return new Vector4(floatValue(), 0, 0, 0);
	}

	public Quaternion quaternionValue() {
		return new Quaternion(floatValue(), 0, 0, 0);
	}
}
