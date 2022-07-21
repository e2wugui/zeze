package Zeze.Services.RocketMQ;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.JMSException;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotReadableException;
import javax.jms.MessageNotWriteableException;

public class ZezeBytesMessage extends ZezeMessage implements javax.jms.BytesMessage {
	private byte[] bytesIn;
	private DataInputStream dataAsInput;
	private ByteArrayOutputStream bytesOut;
	private DataOutputStream dataAsOutput;
	protected boolean readOnly;

	public ZezeBytesMessage() {
		this.bytesOut = new ByteArrayOutputStream();
		this.dataAsOutput = new DataOutputStream(this.bytesOut);
		this.readOnly = false;
		this.writeOnly = true;
	}

	public ZezeBytesMessage(byte[] data) {
		this.bytesIn = data;
		this.dataAsInput = new DataInputStream(new ByteArrayInputStream(data, 0, data.length));
		this.readOnly = true;
		this.writeOnly = false;
	}

	public byte[] getData() {
		if (this.readOnly)
			return this.bytesIn;
		if (this.writeOnly)
			return this.bytesOut.toByteArray();
		throw new IllegalStateRuntimeException("Message must be in write only or read only status");
	}

	protected void checkIsReadOnly() throws MessageNotReadableException {
		if (!readOnly) {
			throw new MessageNotReadableException("Not readable");
		}
		if (dataAsInput == null) {
			throw new MessageNotReadableException("No data to read");
		}
	}

	protected void checkIsWriteOnly() throws MessageNotWriteableException {
		if (!writeOnly) {
			throw new MessageNotWriteableException("Not writable");
		}
	}

	private void initializeWriteIfNecessary() {
		if (bytesOut == null) {
			bytesOut = new ByteArrayOutputStream();
		}
		if (dataAsOutput == null) {
			dataAsOutput = new DataOutputStream(bytesOut);
		}
	}

	private JMSException handleInputException(final IOException e) {
		JMSException ex;
		if (e instanceof EOFException) {
			ex = new MessageEOFException(e.getMessage());
		} else {
			ex = new MessageFormatException(e.getMessage());
		}
		ex.initCause(e);
		ex.setLinkedException(e);
		return ex;
	}

	private JMSException handleOutputException(final IOException e) {
		JMSException ex = new JMSException(e.getMessage());
		ex.initCause(e);
		ex.setLinkedException(e);
		return ex;
	}

	@Override
	public long getBodyLength() throws JMSException {
		return getData().length;
	}

	@Override
	public boolean readBoolean() throws JMSException {
		checkIsReadOnly();

		try {
			return dataAsInput.readBoolean();
		} catch (IOException e) {
			throw handleInputException(e);
		}
	}

	@Override
	public byte readByte() throws JMSException {
		checkIsReadOnly();

		try {
			return dataAsInput.readByte();
		} catch (IOException e) {
			throw handleInputException(e);
		}
	}

	@Override
	public int readUnsignedByte() throws JMSException {
		checkIsReadOnly();

		try {
			return dataAsInput.readUnsignedByte();
		} catch (IOException e) {
			throw handleInputException(e);
		}
	}

	@Override
	public short readShort() throws JMSException {
		checkIsReadOnly();

		try {
			return dataAsInput.readShort();
		} catch (IOException e) {
			throw handleInputException(e);
		}
	}

	@Override
	public int readUnsignedShort() throws JMSException {
		checkIsReadOnly();

		try {
			return dataAsInput.readUnsignedShort();
		} catch (IOException e) {
			throw handleInputException(e);
		}
	}

	@Override
	public char readChar() throws JMSException {
		checkIsReadOnly();

		try {
			return dataAsInput.readChar();
		} catch (IOException e) {
			throw handleInputException(e);
		}
	}

	@Override
	public int readInt() throws JMSException {
		checkIsReadOnly();

		try {
			return dataAsInput.readInt();
		} catch (IOException e) {
			throw handleInputException(e);
		}
	}

	@Override
	public long readLong() throws JMSException {
		checkIsReadOnly();

		try {
			return dataAsInput.readLong();
		} catch (IOException e) {
			throw handleInputException(e);
		}
	}

	@Override
	public float readFloat() throws JMSException {
		checkIsReadOnly();

		try {
			return dataAsInput.readFloat();
		} catch (IOException e) {
			throw handleInputException(e);
		}
	}

	@Override
	public double readDouble() throws JMSException {
		checkIsReadOnly();

		try {
			return dataAsInput.readDouble();
		} catch (IOException e) {
			throw handleInputException(e);
		}
	}

	@Override
	public String readUTF() throws JMSException {
		checkIsReadOnly();

		try {
			return dataAsInput.readUTF();
		} catch (IOException e) {
			throw handleInputException(e);
		}
	}

	@Override
	public int readBytes(byte[] value) throws JMSException {
		checkIsReadOnly();

		return readBytes(value, value.length);
	}

	@Override
	public int readBytes(byte[] value, int length) throws JMSException {
		checkIsReadOnly();

		if (length > value.length) {
			throw new IndexOutOfBoundsException("length must be smaller than the length of value");
		}
		if (dataAsInput == null) {
			throw new MessageNotReadableException("Message is not readable! ");
		}
		try {
			int offset = 0;
			while (offset < length) {
				int read = dataAsInput.read(value, offset, length - offset);
				if (read < 0) {
					break;
				}
				offset += read;
			}

			if (offset == 0 && length != 0) {
				return -1;
			}
			return offset;
		} catch (IOException e) {
			throw handleInputException(e);
		}
	}

	@Override
	public void writeBoolean(boolean value) throws JMSException {
		checkIsWriteOnly();
		initializeWriteIfNecessary();

		try {
			dataAsOutput.writeBoolean(value);
		} catch (IOException e) {
			throw handleOutputException(e);
		}
	}

	@Override
	public void writeByte(byte value) throws JMSException {
		checkIsWriteOnly();
		initializeWriteIfNecessary();

		try {
			dataAsOutput.writeByte(value);
		}
		catch (IOException e) {
			throw handleOutputException(e);
		}
	}

	@Override
	public void writeShort(short value) throws JMSException {
		checkIsWriteOnly();
		initializeWriteIfNecessary();

		try {
			dataAsOutput.writeShort(value);
		}
		catch (IOException e) {
			throw handleOutputException(e);
		}
	}

	@Override
	public void writeChar(char value) throws JMSException {
		checkIsWriteOnly();
		initializeWriteIfNecessary();

		try {
			dataAsOutput.writeChar(value);
		}
		catch (IOException e) {
			throw handleOutputException(e);
		}
	}

	@Override
	public void writeInt(int value) throws JMSException {
		checkIsWriteOnly();
		initializeWriteIfNecessary();

		try {
			dataAsOutput.writeInt(value);
		}
		catch (IOException e) {
			throw handleOutputException(e);
		}
	}

	@Override
	public void writeLong(long value) throws JMSException {
		checkIsWriteOnly();
		initializeWriteIfNecessary();

		try {
			dataAsOutput.writeLong(value);
		}
		catch (IOException e) {
			throw handleOutputException(e);
		}
	}

	@Override
	public void writeFloat(float value) throws JMSException {
		checkIsWriteOnly();
		initializeWriteIfNecessary();

		try {
			dataAsOutput.writeFloat(value);
		}
		catch (IOException e) {
			throw handleOutputException(e);
		}
	}

	@Override
	public void writeDouble(double value) throws JMSException {
		checkIsWriteOnly();
		initializeWriteIfNecessary();

		try {
			dataAsOutput.writeDouble(value);
		}
		catch (IOException e) {
			throw handleOutputException(e);
		}
	}

	@Override
	public void writeUTF(String value) throws JMSException {
		checkIsWriteOnly();
		initializeWriteIfNecessary();

		try {
			dataAsOutput.writeUTF(value);
		}
		catch (IOException e) {
			throw handleOutputException(e);
		}
	}

	@Override
	public void writeBytes(byte[] value) throws JMSException {
		checkIsWriteOnly();
		initializeWriteIfNecessary();

		if (dataAsOutput == null) {
			throw new MessageNotWriteableException("Message is not writable! ");
		}
		try {
			dataAsOutput.write(value);
		}
		catch (IOException e) {
			throw handleOutputException(e);
		}
	}

	@Override
	public void writeBytes(byte[] value, int offset, int length) throws JMSException {
		checkIsWriteOnly();
		initializeWriteIfNecessary();

		if (dataAsOutput == null) {
			throw new MessageNotWriteableException("Message is not writable! ");
		}
		try {
			dataAsOutput.write(value, offset, length);
		}
		catch (IOException e) {
			throw handleOutputException(e);
		}
	}

	@Override
	public void writeObject(Object value) throws JMSException {
		checkIsWriteOnly();
		initializeWriteIfNecessary();

		throw new UnsupportedOperationException("Operation unsupported!");
	}

	@Override
	public void reset() throws JMSException {
		try {
			if (bytesOut != null) {
				bytesOut.reset();
			}
			if (this.dataAsInput != null) {
				this.dataAsInput.reset();
			}

			this.readOnly = true;
		}
		catch (IOException e) {
			throw new JMSException(e.getMessage());
		}
	}
}
