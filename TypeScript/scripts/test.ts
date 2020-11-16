
import * as Zeze from "Zeze"

var bb = new Zeze.Serialize.ByteBuffer();
bb.WriteBytes(new ArrayBuffer(0));

console.log(bb.toString());

