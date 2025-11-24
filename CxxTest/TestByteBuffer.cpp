#include <climits>
#include <cstring>
#include <cstdlib>
#include <cstdio>
#include <exception>
#include "zeze/cxx/ByteBuffer.h"

using Zeze::ByteBuffer;

static void assertEquals(const char* e, const char* c)
{
	if (strcmp(e, c))
	{
		fprintf(stderr, "assertEquals failed:\n  expected: '%s'\n,   but was: '%s'\n", e, c);
		throw std::exception();
	}
}

static void assertEquals(int64_t e, int64_t c)
{
	if (e != c)
	{
		fprintf(stderr, "assertEquals failed:\n  expected: %lld\n,   but was: %lld\n", (long long)e, (long long)c);
		throw std::exception();
	}
}

static void testBasic()
{
	ByteBuffer bb;
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	{
		bool v = true;
		bb.WriteBool(v);
		assertEquals(1, bb.Size());
		assertEquals(1, bb.Bytes[bb.ReadIndex]);
		assertEquals(v, bb.ReadBool());
		assertEquals(bb.ReadIndex, bb.WriteIndex);
	}
	{
		char v = 1;
		bb.WriteByte(v);
		assertEquals(1, bb.Size());
		assertEquals(1, bb.Bytes[bb.ReadIndex]);
		assertEquals(v, bb.ReadByte());
		assertEquals(bb.ReadIndex, bb.WriteIndex);
	}
}

static void testUInt()
{
	ByteBuffer bb;
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	int v = 1;
	bb.WriteUInt(v);
	assertEquals(1, bb.Size());
//	assertEquals("01", bb.toString());
	assertEquals(v, bb.ReadUInt());
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	v = 0x80;
	bb.WriteUInt(v);
	assertEquals(2, bb.Size());
//	assertEquals("80-80", bb.toString());
	assertEquals(v, bb.ReadUInt());
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	v = 0x4000;
	bb.WriteUInt(v);
	assertEquals(3, bb.Size());
//	assertEquals("C0-40-00", bb.toString());
	assertEquals(v, bb.ReadUInt());
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	v = 0x200000;
	bb.WriteUInt(v);
	assertEquals(4, bb.Size());
//	assertEquals("E0-20-00-00", bb.toString());
	assertEquals(v, bb.ReadUInt());
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	v = 0x10000000;
	bb.WriteUInt(v);
	assertEquals(5, bb.Size());
//	assertEquals("F0-10-00-00-00", bb.toString());
	assertEquals(v, bb.ReadUInt());
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	v = -1;
	bb.WriteUInt(v);
	assertEquals(5, bb.Size());
//	assertEquals("F0-FF-FF-FF-FF", bb.toString());
	assertEquals(v, bb.ReadUInt());
	assertEquals(bb.ReadIndex, bb.WriteIndex);
}

static void testLong()
{
	ByteBuffer bb;
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	int64_t v = 1;
	bb.WriteLong(v);
	assertEquals(1, bb.Size());
//	assertEquals("01", bb.toString());
	assertEquals(v, bb.ReadLong());
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	v = 0x80L;
	bb.WriteLong(v);
	assertEquals(2, bb.Size());
//	assertEquals("40-80", bb.toString());
	assertEquals(v, bb.ReadLong());
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	v = 0x4000L;
	bb.WriteLong(v);
	assertEquals(3, bb.Size());
//	assertEquals("60-40-00", bb.toString());
	assertEquals(v, bb.ReadLong());
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	v = 0x200000LL;
	bb.WriteLong(v);
	assertEquals(4, bb.Size());
//	assertEquals("70-20-00-00", bb.toString());
	assertEquals(v, bb.ReadLong());
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	v = 0x10000000LL;
	bb.WriteLong(v);
	assertEquals(5, bb.Size());
//	assertEquals("78-10-00-00-00", bb.toString());
	assertEquals(v, bb.ReadLong());
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	v = 0x800000000LL;
	bb.WriteLong(v);
	assertEquals(6, bb.Size());
//	assertEquals("7C-08-00-00-00-00", bb.toString());
	assertEquals(v, bb.ReadLong());
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	v = 0x40000000000LL;
	bb.WriteLong(v);
	assertEquals(7, bb.Size());
//	assertEquals("7E-04-00-00-00-00-00", bb.toString());
	assertEquals(v, bb.ReadLong());
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	v = 0x2000000000000LL;
	bb.WriteLong(v);
	assertEquals(8, bb.Size());
//	assertEquals("7F-02-00-00-00-00-00-00", bb.toString());
	assertEquals(v, bb.ReadLong());
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	v = 0x100000000000000LL;
	bb.WriteLong(v);
	assertEquals(9, bb.Size());
//	assertEquals("7F-81-00-00-00-00-00-00-00", bb.toString());
	assertEquals(v, bb.ReadLong());
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	v = 0x8000000000000000LL;
	bb.WriteLong(v);
	assertEquals(9, bb.Size());
//	assertEquals("80-00-00-00-00-00-00-00-00", bb.toString());
	assertEquals(v, bb.ReadLong());
	assertEquals(bb.ReadIndex, bb.WriteIndex);

	v = -1;
	bb.WriteLong(v);
	assertEquals(1, bb.Size());
//	assertEquals("FF", bb.toString());
	assertEquals(v, bb.ReadLong());
	assertEquals(bb.ReadIndex, bb.WriteIndex);
}

static void testInt(int x)
{
	ByteBuffer bb;
	bb.WriteInt(x);
	int y = bb.ReadInt();
	assertEquals(x, y);
	assertEquals(bb.ReadIndex, bb.WriteIndex);
}

static void testLong(int64_t x)
{
	ByteBuffer bb;
	bb.WriteLong(x);
	int64_t y = bb.ReadLong();
	assertEquals(x, y);
	assertEquals(bb.ReadIndex, bb.WriteIndex);
}

static void testUInt(int x)
{
	ByteBuffer bb;
	bb.WriteUInt(x);
	int y = bb.ReadUInt();
	assertEquals(x, y);
	assertEquals(bb.ReadIndex, bb.WriteIndex);
}

static void testSkipUInt(int x)
{
	ByteBuffer bb;
	bb.WriteUInt(x);
	bb.ReadUInt();
	int ri = bb.ReadIndex;
	bb.ReadIndex = 0;
	bb.SkipUInt();
	assertEquals(ri, bb.ReadIndex);
}

static void testSkipLong(int64_t x)
{
	ByteBuffer bb;
	bb.WriteLong(x);
	bb.ReadLong();
	int ri = bb.ReadIndex;
	bb.ReadIndex = 0;
	bb.SkipLong();
	assertEquals(ri, bb.ReadIndex);
}

static void testAll(int64_t x)
{
	testInt((int)x);
	testInt((int)-x);
	testUInt((int)x);
	testUInt((int)-x);
	testSkipUInt((int)x);
	testSkipUInt((int)-x);
	testLong(x);
	testLong(-x);
//	testULong(x);
//	testULong(-x);
	testSkipLong(x);
	testSkipLong(-x);
//	testSkipULong(x);
//	testSkipULong(-x);
}

static int64_t nextLong()
{
	int64_t r = 0;
	for (int i = 0; i < 64; i += 15)
		r = (r << 15) + rand();
	return r;
}

static void testInteger()
{
	for (int i = 0; i <= 64; i++)
	{
		testAll(1LL << i);
		testAll((1LL << i) - 1);
		testAll(((1LL << i) - 1) & 0x5555555555555555LL);
		testAll(((1LL << i) - 1) & 0xaaaaaaaaaaaaaaaaLL);
	}
	for (int i = 0; i < 10000; i++)
		testAll(nextLong());
	testInt(INT_MIN);
	testInt(INT_MAX);
	testLong(INT_MIN);
	testLong(INT_MAX);
	testLong(LLONG_MIN);
	testLong(LLONG_MAX);
	testUInt(INT_MIN);
	testUInt(INT_MAX);
//	testULong(INT_MIN);
//	testULong(INT_MAX);
//	testULong(LLONG_MIN);
//	testULong(LLONG_MAX);
	testSkipLong(INT_MIN);
	testSkipLong(INT_MAX);
	testSkipLong(LLONG_MIN);
	testSkipLong(LLONG_MAX);
	testSkipUInt(INT_MIN);
	testSkipUInt(INT_MAX);
//	testSkipULong(INT_MIN);
//	testSkipULong(INT_MAX);
//	testSkipULong(LLONG_MIN);
//	testSkipULong(LLONG_MAX);
}

void TestByteBuffer()
{
	printf("TestByteBuffer ... ");
	testBasic();
	testUInt();
	testLong();
	testInteger();
	printf("OK!\n");
}
