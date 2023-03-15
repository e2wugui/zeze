
#include "Gen/demo/Bean1.hpp"
#include "Gen/demo/Module1/BValue.hpp"

int main(char* args[]) {
	Zeze::ByteBuffer bb(16);
	demo::Module1::BValue bValue;
	bValue.Int1 = 1;
	bValue.Long2 = 2;
	bValue.String3 = "3";
	bValue.Bool4 = true;
	bValue.Short5 = 5;
	bValue.Float6 = 6;
	bValue.Double7 = 7;
	bValue.Bytes8 = "8";
	bValue.List9.push_back(demo::Bean1());
	bValue.Set10.insert(10);
	bValue.Map11[11] = demo::Module2::BValue();
	bValue.Bean12.Int1 = 12;
	bValue.Byte13 = 13;
	bValue.Dynamic14.SetBean(new demo::Bean1());
	bValue.Dynamic14.SetBean(new demo::Module1::BSimple()); // set again
	bValue.Map15[15] = 15;
	demo::Module1::Key key(16);
	bValue.Map16[key] = demo::Module1::BSimple();
	bValue.Vector2.x = 17;
	bValue.Vector2Int.x = 18;
	bValue.Vector3.x = 19;
	bValue.Vector4.x = 20;
	bValue.Quaternion.x = 21;
	Zeze::Vector2Int v2i(22, 22);
	bValue.MapVector2Int[v2i] = v2i;
	bValue.ListVector2Int.push_back(v2i);
	bValue.Map25[key] = demo::Module1::BSimple();
	bValue.Map26[key] = demo::Module1::BValue::constructDynamicBean_Map26();
	bValue.Map26[key].SetBean(new demo::Module1::BSimple());
	bValue.Dynamic27.SetBean(new demo::Module1::BSimple());
	bValue.Key28.S = 28;
	bValue.Key28.Assign(demo::Module1::Key(28));
	bValue.Array29.push_back(29);
	bValue.LongList.push_back(30);
	bValue.Encode(bb);

	Zeze::ByteBuffer bb2(bb.Bytes, 0, bb.WriteIndex);
	demo::Module1::BValue bValueDecoded;
	bValueDecoded.Decode(bb2);
}
