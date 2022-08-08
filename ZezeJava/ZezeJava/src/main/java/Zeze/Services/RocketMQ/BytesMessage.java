package Zeze.Services.RocketMQ;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import javax.jms.JMSException;

public class BytesMessage extends Message implements javax.jms.BytesMessage {

	protected transient DataOutputStream dataOut;
	protected transient ByteArrayOutputStream byteOut;
	protected transient DataInputStream dataIn;
	protected transient int length;

	@Override
	public long getBodyLength() throws JMSException {
		// TODO: initialize reading
		return length;
	}

	@Override
	public boolean readBoolean() throws JMSException {
		// TODO: initialize reading
		try {
			return this.dataIn.readBoolean();
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public byte readByte() throws JMSException {
		// TODO: initialize reading
		try {
			return this.dataIn.readByte();
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public int readUnsignedByte() throws JMSException {
		// TODO: initialize reading
		try {
			return this.dataIn.readUnsignedByte();
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public short readShort() throws JMSException {
		// TODO: initialize reading
		try {
			return this.dataIn.readShort();
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public int readUnsignedShort() throws JMSException {
		// TODO: initialize reading
		try {
			return this.dataIn.readUnsignedShort();
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public char readChar() throws JMSException {
		// TODO: initialize reading
		try {
			return this.dataIn.readChar();
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public int readInt() throws JMSException {
		// TODO: initialize reading
		try {
			return this.dataIn.readInt();
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public long readLong() throws JMSException {
		// TODO: initialize reading
		try {
			return this.dataIn.readLong();
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public float readFloat() throws JMSException {
		// TODO: initialize reading
		try {
			return this.dataIn.readFloat();
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public double readDouble() throws JMSException {
		// TODO: initialize reading
		try {
			return this.dataIn.readDouble();
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public String readUTF() throws JMSException {
		// TODO: initialize reading
		try {
			return this.dataIn.readUTF();
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public int readBytes(byte[] value) throws JMSException {
		return readBytes(value, value.length);
	}

	@Override
	public int readBytes(byte[] value, int length) throws JMSException {
		// TODO: initialize reading
		try {
			int n = 0;
			while (n < length) {
				int count = this.dataIn.read(value, n, length - n);
				if (count < 0) {
					break;
				}
				n += count;
			}
			if (n == 0 && length > 0) {
				n = -1;
			}
			return n;
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public void writeBoolean(boolean value) throws JMSException {
		// TODO: initialize writing
		try {
			this.dataOut.writeBoolean(value);
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public void writeByte(byte value) throws JMSException {
		// TODO: initialize writing
		try {
			this.dataOut.writeByte(value);
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public void writeShort(short value) throws JMSException {
		// TODO: initialize writing
		try {
			this.dataOut.writeShort(value);
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public void writeChar(char value) throws JMSException {
		// TODO: initialize writing
		try {
			this.dataOut.writeChar(value);
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public void writeInt(int value) throws JMSException {
		// TODO: initialize writing
		try {
			this.dataOut.writeInt(value);
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public void writeLong(long value) throws JMSException {
		// TODO: initialize writing
		try {
			this.dataOut.writeLong(value);
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public void writeFloat(float value) throws JMSException {
		// TODO: initialize writing
		try {
			this.dataOut.writeFloat(value);
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public void writeDouble(double value) throws JMSException {
		// TODO: initialize writing
		try {
			this.dataOut.writeDouble(value);
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public void writeUTF(String value) throws JMSException {
		// TODO: initialize writing
		try {
			this.dataOut.writeUTF(value);
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public void writeBytes(byte[] value) throws JMSException {
		// TODO: initialize writing
		try {
			this.dataOut.write(value);
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Override
	public void writeBytes(byte[] value, int offset, int length) throws JMSException {
		// TODO: initialize writing
		try {
			this.dataOut.write(value, offset, length);
		} catch (Exception e) {
			throw new JMSException(e.getMessage());
		}
	}

	@Deprecated
	@Override
	public void writeObject(Object value) throws JMSException {

	}

	@Override
	public void reset() throws JMSException {
		// TODO: 
	}
}
