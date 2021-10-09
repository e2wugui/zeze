package Zeze.Util;

import Zeze.*;
import java.util.*;
import java.io.*;

// binary: msgsize4bytes + tag4bytes + others
public final class ChatHistoryMessage implements Zeze.Serialize.Serializable {
	public static final int TagDeleted = 1;
	public static final int TagSeparate = 2;

	public static final int TypeString = 0; // >0 user defined, <0 reserved

	private int Tag;
	public int getTag() {
		return Tag;
	}
	public void setTag(int value) {
		Tag = value;
	}
	private long Id;
	public long getId() {
		return Id;
	}
	public void setId(long value) {
		Id = value;
	}
	private long TimeTicks;
	public long getTimeTicks() {
		return TimeTicks;
	}
	public void setTimeTicks(long value) {
		TimeTicks = value;
	}
	private String Sender;
	public String getSender() {
		return Sender;
	}
	public void setSender(String value) {
		Sender = value;
	}
	private int Type;
	public int getType() {
		return Type;
	}
	public void setType(int value) {
		Type = value;
	}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private byte[] Content;
	private byte[] Content;
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public byte[] getContent()
	public byte[] getContent() {
		return Content;
	}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void setContent(byte[] value)
	public void setContent(byte[] value) {
		Content = value;
	}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private Dictionary<int, byte[]> Properties = new Dictionary<int, byte[]> ();
	private HashMap<Integer, byte[]> Properties = new HashMap<Integer, byte[]> ();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public Dictionary<int, byte[]> getProperties()
	public HashMap<Integer, byte[]> getProperties() {
		return Properties;
	}
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void setProperties(Dictionary<int, byte[]> value)
	public void setProperties(HashMap<Integer, byte[]> value) {
		Properties = value;
	}
	public String getContentStr() {
		return (getType() == TypeString) ? System.Text.Encoding.UTF8.GetString(getContent()) : BitConverter.toString(getContent());
	}
	public boolean isDeleted() {
		return (getTag() & TagDeleted) != 0;
	}
	public boolean isSeparate() {
		return (getTag() & TagSeparate) != 0;
	}

	public void SaveContentToFile(String path) {
		try (System.IO.FileStream fs = System.IO.File.Create(path)) {
			fs.Write(this.getContent(), 0, this.getContent().length);
		}
	}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public static byte[] LoadContentFromFile(string path)
	public static byte[] LoadContentFromFile(String path) {
		if (false == (new File(path)).isFile()) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: return Array.Empty<byte>();
			return Array.<Byte>Empty();
		}

		try (System.IO.FileStream fs = System.IO.File.Open(path, System.IO.FileMode.Open)) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] bytes = new byte[fs.Length];
			byte[] bytes = new byte[fs.Length];
			fs.Read(bytes, 0, bytes.length);
    
			return bytes;
		}
	}

	public void Decode(Zeze.Serialize.ByteBuffer bb) {
		this.setTag(bb.ReadInt4());
		this.setId(bb.ReadLong());
		this.setTimeTicks(bb.ReadLong());
		this.setSender(bb.ReadString());
		this.setType(bb.ReadInt());
		this.setContent(bb.ReadBytes());

		int propertiesSize = bb.ReadInt();
		for (int i = 0; i < propertiesSize; ++i) {
			int key = bb.ReadInt();
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] value = bb.ReadBytes();
			byte[] value = bb.ReadBytes();
			getProperties().put(key, value);
		}
	}

	public void Encode(Zeze.Serialize.ByteBuffer bb) {
		bb.WriteInt4(this.getTag());
		bb.WriteLong(this.getId());
		bb.WriteLong(this.getTimeTicks());
		bb.WriteString(this.getSender());
		bb.WriteInt(this.getType());
		bb.WriteBytes(this.getContent());

		bb.WriteInt(getProperties().size());
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: foreach (KeyValuePair<int, byte[]> pair in Properties)
		for (Map.Entry<Integer, byte[]> pair : getProperties().entrySet()) {
			bb.WriteInt(pair.getKey());
			bb.WriteBytes(pair.getValue());
		}
	}

	public int SizeHint() {
		return 4 + 9 + 9 + 128 + 5 + 5 + getContent().length; // tag + id + time + sender + type + contentsize + content
	}
}