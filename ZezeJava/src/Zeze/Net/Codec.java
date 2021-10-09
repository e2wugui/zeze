package Zeze.Net;

import Zeze.Serialize.*;
import Zeze.*;
import java.io.*;

public interface Codec extends Closeable {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void update(byte c);
	public void update(byte c);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public void update(byte[] data, int off, int len);
	public void update(byte[] data, int off, int len);
	public void flush();
}