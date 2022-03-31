package Zeze.Arch;

public class KnownSerializer {
	public Zeze.Util.Action4<Zeze.Util.StringBuilderCs, String, String, String> Encoder;
	public Zeze.Util.Action4<Zeze.Util.StringBuilderCs, String, String, String> Decoder;
	public Zeze.Util.Action3<Zeze.Util.StringBuilderCs, String, String> Define;
	public Zeze.Util.Func0<String> TypeName;

	public KnownSerializer(Zeze.Util.Action4<Zeze.Util.StringBuilderCs, String, String, String> enc,
						   Zeze.Util.Action4<Zeze.Util.StringBuilderCs, String, String, String> dec,
						   Zeze.Util.Action3<Zeze.Util.StringBuilderCs, String, String> def,
						   Zeze.Util.Func0<String> typeName) {
		Encoder = enc;
		Decoder = dec;
		Define = def;
		TypeName = typeName;
	}
}
