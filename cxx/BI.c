#include "osdefine.h"
#include "BI.h"
#include <stdio.h>
#include <stdlib.h>

#ifdef LIMAX_OS_APPLE_FAMILY
#include <malloc/malloc.h>
#else
#include <malloc.h>
#endif

#define ZERO BI_alloc(0)
#define LEN(b) (b[-1])
#define SGN(b) (b[-2])
#define CAP(b) (b[-3])

#define TOOMCOOK3_THRESHOLD 96
#define KRARTSUBA_THRESHOLD 32
#define BURNIKEL_ZIEGLER_THRESHOLD_BITS 4
#define BURNIKEL_ZIEGLER_THRESHOLD (1 << BURNIKEL_ZIEGLER_THRESHOLD_BITS)
static U32 MODEXP_WINDOW[] = { 19, 79, 223, 419, 1019, 2129, 0xFFFFFFFF };
#define STACK_ALLOC(bi, cap) { bi = (BI)alloca(((cap) + 3) * sizeof(U32)) + 3; CAP(bi) = (cap); SGN(bi) = 0; }

static BI BI_alloc(U32 cap)
{
	BI bi = (BI)calloc(cap + 3, sizeof(U32)) + 3;
	CAP(bi) = cap;
	return bi;
}

void BI_free(BI bi)
{
	free(bi - 3);
}

static U32 trim_len(BI x, U32 l)
{
	while (l && x[l - 1] == 0) l--;
	return l;
}

static BI trim(BI x)
{
	LEN(x) = trim_len(x, LEN(x));
	return x;
}

static U32 count_leading_zeros(U32 i)
{
	U32 n;
	if (i == 0)	return 32;
	n = 1;
	if (i >> 16 == 0) { n += 16; i <<= 16; }
	if (i >> 24 == 0) { n += 8; i <<= 8; }
	if (i >> 28 == 0) { n += 4; i <<= 4; }
	if (i >> 30 == 0) { n += 2; i <<= 2; }
	return n - (i >> 31);
}

static U32 count_trailing_zeros(U32 i) {
	U32 y, n;
	if (i == 0) return 32;
	n = 31;
	y = i << 16; if (y) { n = n - 16; i = y; }
	y = i << 8; if (y) { n = n - 8; i = y; }
	y = i << 4; if (y) { n = n - 4; i = y; }
	y = i << 2; if (y) { n = n - 2; i = y; }
	return n - ((i << 1) >> 31);
}

static U32 bit_count(BI x)
{
	return 32 * LEN(x) - count_leading_zeros(x[LEN(x) - 1]);
}

static U32 get_bit(BI x, U32 pos)
{
	return x[pos >> 5] & (1 << (pos & 31));
}

static void _zero(BI x, U32 l)
{
	U32 i;
	for (i = 0; i < l; i++)
		x[i] = 0;
}

static void _copy(BI x, BI y, U32 l)
{
	U32 i;
	for (i = 0; i < l; i++)
		x[i] = y[i];
}

static BI copy(BI x, BI y)
{
	_copy(y, x, LEN(y) = LEN(x));
	SGN(y) = SGN(x);
	return y;
}

static int _compare(BI x, U32 l, BI y, U32 m)
{
	if (l > m)
		return 1;
	else if (l < m)
		return -1;
	while (l-- > 0)
	{
		if (x[l] > y[l])
			return 1;
		else if (x[l] < y[l])
			return -1;
	}
	return 0;
}

static U32 _add_base(BI x, U32 l, BI y, U32 m, BI z)
{
	U64 c = 0;
	U32 i = 0;
	for (; i < m; i++)
	{
		c = c + x[i] + y[i];
		z[i] = (U32)c;
		c >>= 32;
	}
	for (; c && i < l; i++)
	{
		c = c + x[i];
		z[i] = (U32)c;
		c >>= 32;
	}
	if (x != z)
		for (; i < l; i++)
			z[i] = x[i];
	if (!c)
		return l;
	z[i] = 1;
	return l + 1;
}

static U32 _sub_base(BI x, U32 l, BI y, U32 m, BI z)
{
	U64 c = 0;
	U32 i = 0;
	for (; i < m; i++)
	{
		c = x[i] - c - y[i];
		z[i] = (U32)c;
		c >>= 63;
	}
	for (; c && i < l; i++)
	{
		c = x[i] == 0;
		z[i] = x[i] - 1;
	}
	if (x != z)
		for (; i < l; i++)
			z[i] = x[i];
	while (l && z[l - 1] == 0) l--;
	return l;
}

static U32 _add_base_s(BI x, U32 l, BI y, U32 m, BI z)
{
	U32 i;
	BI t;
	if (l < m)
	{
		i = l; l = m; m = i;
		t = x; x = y, y = t;
	}
	return _add_base(x, l, y, m, z);
}

static U32 _sub_base_s(BI x, U32 l, BI y, U32 m, BI z, U32 *s)
{
	int c = _compare(x, l, y, m);
	if (c == 0)
		return *s = 0;
	if (c > 0)
		return _sub_base(x, l, y, m, z);
	*s = ~*s;
	return _sub_base(y, m, x, l, z);
}

static U32 _add_s(BI x, U32 l, U32 p, BI y, U32 m, U32 q, BI z, U32 *s)
{
	if (p ^ q)
	{
		if (p)
		{
			*s = q;
			return _sub_base_s(y, m, x, l, z, s);
		}
		else
		{
			*s = p;
			return _sub_base_s(x, l, y, m, z, s);
		}
	}
	*s = p;
	return _add_base_s(x, l, y, m, z);
}

static U32 _sub_s(BI x, U32 l, U32 p, BI y, U32 m, U32 q, BI z, U32 *s)
{
	*s = p;
	return p ^ q ? _add_base_s(x, l, y, m, z) : _sub_base_s(x, l, y, m, z, s);
}

static void _add(BI x, BI y, BI z)
{
	LEN(z) = _add_s(x, LEN(x), SGN(x), y, LEN(y), SGN(y), z, &SGN(z));
}

static void _sub(BI x, BI y, BI z)
{
	LEN(z) = _sub_s(x, LEN(x), SGN(x), y, LEN(y), SGN(y), z, &SGN(z));
}

static U32 _lshift_base(BI x, U32 l, U32 s, BI z)
{
	U32 w;
	if (l == 0)
		return 0;
	w = s >> 5;
	s &= 31;
	if (s)
	{
		U32 i, j, h;
		if (count_leading_zeros(x[l - 1]) < s)
			h = 0, i = l++;
		else
			h = x[i = l - 1];
		for (; i > 0; i--)
		{
			j = x[i - 1];
			z[i + w] = (h << s) | (j >> (32 - s));
			h = j;
		}
		z[i + w] = h << s;
	}
	else
		_copy(z + w, x, l);
	_zero(z, w);
	return l + w;
}

static U32 _rshift_base(BI x, U32 l, U32 s, BI z)
{
	U32 w;
	if (l == 0)
		return 0;
	w = s >> 5;
	if (l <= w)
		return 0;
	s &= 31;
	if (s)
	{
		U32 i, h, j = x[w];
		for (i = w; i < l - 1; i++)
		{
			h = x[i + 1];
			z[i - w] = (j >> s) | (h << (32 - s));
			j = h;
		}
		return (z[i - w] = j >> s) ? i - w + 1 : i - w;
	}
	_copy(z, x + w, l - w);
	return l - w;
}

static void _lshift(BI x, U32 s, BI z)
{
	LEN(z) = _lshift_base(x, LEN(x), s, z);
}

static void _rshift(BI x, U32 s, BI z)
{
	LEN(z) = _rshift_base(x, LEN(x), s, z);
}

static U32 _mul_dispatch(BI x, U32 l, BI y, U32 m, BI z);
static U32 _sqr_dispatch(BI x, U32 l, BI z);
static U32 _mul_base(BI x, U32 l, BI y, U32 m, BI z)
{
	U32 i, j;
	U64 c = 0, v = y[0];
	for (j = 0; j < l; j++)
	{
		c += v * x[j];
		z[j] = (U32)c;
		c >>= 32;
	}
	z[j] = (U32)c;
	for (i = 1; i < m; i++)
	{
		c = 0, v = y[i];
		for (j = 0; j < l; j++)
		{
			c += v * x[j] + z[i + j];
			z[i + j] = (U32)c;
			c >>= 32;
		}
		z[i + j] = (U32)c;
	}
	return l + m - (c ? 0 : 1);
}

static U32 _sqr_base(BI x, U32 l, BI z)
{
	U32 i, j, k;
	U64 c, v;
	for (i = l, k = 0; i > 0;)
	{
		c = x[--i];
		c *= c;
		z[i * 2] = (U32)(c >> 1);
		z[i * 2 + 1] = (U32)(c >> 33) | k;
		k = (U32)(c << 31);
	}
	for (i = 0; i < l - 1; i++)
	{
		c = 0; v = x[i];
		for (j = i + 1; j < l; j++)
		{
			c += v * x[j] + z[i + j];
			z[i + j] = (U32)c;
			c >>= 32;
		}
		for (; c; j++)
		{
			v = c + z[i + j];
			z[i + j] = (U32)v;
			c = v >> 32;
		}
	}
	for (l <<= 1, i = 0, k = 0; i < l; i++)
	{
		j = z[i];
		z[i] = (j << 1) | k;
		k = j >> 31;
	}
	z[0] |= x[0] & 1;
	return z[l - 1] == 0 ? l - 1 : l;
}

static void _toomcook3_divide_3(BI x)
{
	U32 c, v, q, i, l = LEN(x);
	if (l == 0)
		return;
	for (c = 0, i = 0; i < l; i++)
	{
		v = x[i];
		q = x[i] = (v - c) * 0xAAAAAAABULL;
		c = c > v ? 1 : 0;
		if (q >= 0x55555556ULL)
		{
			c++;
			if (q >= 0xAAAAAAABULL)
				c++;
		}
	}
	LEN(x) = x[l - 1] ? l : l - 1;
}

static U32 _toomcook3(BI x, U32 l, BI y, U32 m, BI z4)
{
	U32 i = (l + 2) / 3, ll = i, lm = i, lh = l - 2 * i;
	U32 ml, mm, mh, l0 = i * 2 + 2, l4;
	BI xl, yl, xm, ym, xh, yh, p, q, z1, z2, z3;
	if (m > i)
	{
		ml = i;
		if (m > i * 2)
			mm = i, mh = m - i * 2;
		else
			mm = m - i, mh = 0;
	}
	else
		ml = m, mm = 0, mh = 0;
	xl = x, xm = xl + ll, xh = xm + lm;
	yl = y, ym = yl + ml, yh = ym + mm;
	ll = trim_len(xl, ll);
	lm = trim_len(xm, lm);
	ml = trim_len(yl, ml);
	mm = trim_len(ym, mm);
	STACK_ALLOC(p, l0);
	STACK_ALLOC(q, i + 1);
	STACK_ALLOC(z1, l0);
	STACK_ALLOC(z2, l0);
	STACK_ALLOC(z3, l0);
	l0 = _add_base_s(xl, ll, xh, lh, z4);
	LEN(z3) = _add_base_s(yl, ml, yh, mh, z3);
	LEN(p) = _add_base_s(z4, l0, xm, lm, p);
	LEN(q) = _add_base_s(z3, LEN(z3), ym, mm, q);
	SGN(z1) = 0;
	LEN(z1) = _mul_dispatch(p, LEN(p), q, LEN(q), z1);
	LEN(p) = _sub_s(z4, l0, 0, xm, lm, 0, p, &SGN(p));
	LEN(q) = _sub_s(z3, LEN(z3), 0, ym, mm, 0, q, &SGN(q));
	SGN(z2) = SGN(p) ^ SGN(q);
	LEN(z2) = _mul_dispatch(p, LEN(p), q, LEN(q), z2);
	LEN(p) = _add_s(p, LEN(p), SGN(p), xh, lh, 0, p, &SGN(p));
	_lshift(p, 1, p);
	LEN(p) = _sub_s(p, LEN(p), SGN(p), xl, ll, 0, p, &SGN(p));
	LEN(q) = _add_s(q, LEN(q), SGN(q), yh, mh, 0, q, &SGN(q));
	_lshift(q, 1, q);
	LEN(q) = _sub_s(q, LEN(q), SGN(q), yl, ml, 0, q, &SGN(q));
	SGN(z3) = SGN(p) ^ SGN(q);
	LEN(z3) = _mul_dispatch(p, LEN(p), q, LEN(q), z3);
	l0 = _mul_dispatch(xl, ll, yl, ml, z4);
	l4 = _mul_dispatch(xh, lh, yh, mh, z4 + i * 4);
	_zero(z4 + l0, (l4 ? i * 4 : l + m) - l0);
	_sub(z3, z1, z3);
	_toomcook3_divide_3(z3);
	_sub(z1, z2, z1);
	_rshift(z1, 1, z1);
	LEN(z2) = _sub_s(z2, LEN(z2), SGN(z2), z4, l0, 0, z2, &SGN(z2));
	_sub(z2, z3, z3);
	_rshift(z3, 1, z3);
	LEN(p) = _lshift_base(z4 + i * 4, l4, 1, p);
	SGN(p) = 0;
	_add(z3, p, z3);
	_add(z2, z1, z2);
	LEN(z2) = _sub_s(z2, LEN(z2), SGN(z2), z4 + i * 4, l4, 0, z2, &SGN(z2));
	_sub(z1, z3, z1);
	l4 = l4 ? l4 + i * 4 : l + m;
	_add_base_s(z4 + i, l4 - i, z1, LEN(z1), z4 + i);
	_add_base_s(z4 + i * 2, l4 - i * 2, z2, LEN(z2), z4 + i * 2);
	_add_base_s(z4 + i * 3, l4 - i * 3, z3, LEN(z3), z4 + i * 3);
	while (z4[l4 - 1] == 0) l4--;
	return l4;
}

static U32 _toomcook3_sqr(BI x, U32 l, BI z4)
{
	U32 i = (l + 2) / 3, ll = i, lm = i, lh = l - 2 * i;
	BI xl = x, xm = xl + ll, xh = xm + lm, p, z1, z2, z3;
	U32 l0 = l * 2 + 2, l4;
	ll = trim_len(xl, ll);
	lm = trim_len(xm, lm);
	STACK_ALLOC(p, l0);
	STACK_ALLOC(z1, l0);
	STACK_ALLOC(z2, l0);
	STACK_ALLOC(z3, l0);
	l0 = _add_base_s(xl, ll, xh, lh, z4);
	LEN(p) = _add_base_s(z4, l0, xm, lm, p);
	SGN(z1) = 0;
	LEN(z1) = _sqr_dispatch(p, LEN(p), z1);
	LEN(p) = _sub_s(z4, l0, 0, xm, lm, 0, p, &SGN(p));
	SGN(z2) = 0;
	LEN(z2) = _sqr_dispatch(p, LEN(p), z2);
	LEN(p) = _add_s(p, LEN(p), SGN(p), xh, lh, 0, p, &SGN(p));
	_lshift(p, 1, p);
	LEN(p) = _sub_s(p, LEN(p), SGN(p), xl, ll, 0, p, &SGN(p));
	SGN(z3) = 0;
	LEN(z3) = _sqr_dispatch(p, LEN(p), z3);
	l0 = _sqr_dispatch(xl, ll, z4);
	l4 = _sqr_dispatch(xh, lh, z4 + i * 4);
	_zero(z4 + l0, i * 4 - l0);
	_sub(z3, z1, z3);
	_toomcook3_divide_3(z3);
	_sub(z1, z2, z1);
	_rshift(z1, 1, z1);
	LEN(z2) = _sub_s(z2, LEN(z2), SGN(z2), z4, l0, 0, z2, &SGN(z2));
	_sub(z2, z3, z3);
	_rshift(z3, 1, z3);
	LEN(p) = _lshift_base(z4 + i * 4, l4, 1, p);
	SGN(p) = 0;
	_add(z3, p, z3);
	_add(z2, z1, z2);
	LEN(z2) = _sub_s(z2, LEN(z2), SGN(z2), z4 + i * 4, l4, 0, z2, &SGN(z2));
	_sub(z1, z3, z1);
	l4 += i * 4;
	_add_base_s(z4 + i, l4 - i, z1, LEN(z1), z4 + i);
	_add_base_s(z4 + i * 2, l4 - i * 2, z2, LEN(z2), z4 + i * 2);
	_add_base_s(z4 + i * 3, l4 - i * 3, z3, LEN(z3), z4 + i * 3);
	return l4;
}

static U32 _karatsuba(BI x, U32 l, BI y, U32 m, BI z2)
{
	U32 i = (l + 1) / 2, ll = i, lh = l - i, ml, mh;
	U32 t0, t1, l2, l0;
	BI xl, xh, yl, yh, z1;
	if (m > i)
		ml = i, mh = m - i;
	else
		ml = m, mh = 0;
	xl = x, xh = xl + ll, yl = y, yh = yl + ml;
	ll = trim_len(xl, ll);
	ml = trim_len(yl, ml);
	t0 = _add_base_s(xl, ll, xh, lh, z2);
	t1 = _add_base_s(yl, ml, yh, mh, z2 + ll + 1);
	STACK_ALLOC(z1, t0 + t1);
	LEN(z1) = _mul_dispatch(z2, t0, z2 + ll + 1, t1, z1);
	l2 = _mul_dispatch(xh, lh, yh, mh, z2 + i * 2);
	l0 = _mul_dispatch(xl, ll, yl, ml, z2);
	_zero(z2 + l0, (l2 ? i * 2 : l + m) - l0);
	LEN(z1) = _sub_base(z1, LEN(z1), z2 + i * 2, l2, z1);
	LEN(z1) = _sub_base(z1, LEN(z1), z2, l0, z1);
	l2 = l2 ? l2 + i * 2 : l + m;
	_add_base_s(z2 + i, l2 - i, z1, LEN(z1), z2 + i);
	while (z2[l2 - 1] == 0) l2--;
	return l2;
}

static U32 _karatsuba_sqr(BI x, U32 l, BI z2)
{
	U32 i = (l + 1) / 2, ll = i, lh = l - i;
	BI xl = x, xh = xl + ll, z1;
	U32 l2, l0, t0;
	ll = trim_len(xl, ll);
	t0 = _add_base_s(xl, ll, xh, lh, z2);
	STACK_ALLOC(z1, t0 * 2);
	LEN(z1) = _sqr_dispatch(z2, t0, z1);
	l2 = _sqr_dispatch(xh, lh, z2 + i * 2);
	l0 = _sqr_dispatch(xl, ll, z2);
	_zero(z2 + l0, i * 2 - l0);
	LEN(z1) = _sub_base(z1, LEN(z1), z2 + i * 2, l2, z1);
	LEN(z1) = _sub_base(z1, LEN(z1), z2, l0, z1);
	l2 += i * 2;
	_add_base_s(z2 + i, l2 - i, z1, LEN(z1), z2 + i);
	return l2;
}

static U32 _mul_dispatch(BI x, U32 l, BI y, U32 m, BI z)
{
	if (l < m)
	{
		BI t;
		U32 i;
		i = l; l = m; m = i;
		t = x; x = y, y = t;
	}
	if (m == 0)
		return 0;
	if (m < KRARTSUBA_THRESHOLD)
		return _mul_base(x, l, y, m, z);
	if (m < TOOMCOOK3_THRESHOLD)
		return _karatsuba(x, l, y, m, z);
	return _toomcook3(x, l, y, m, z);
}

static U32 _sqr_dispatch(BI x, U32 l, BI z)
{
	if (l == 0)
		return 0;
	if (l < KRARTSUBA_THRESHOLD)
		return _sqr_base(x, l, z);
	if (l < TOOMCOOK3_THRESHOLD)
		return _karatsuba_sqr(x, l, z);
	return _toomcook3_sqr(x, l, z);
}

static void _mul(BI x, BI y, BI z)
{
	U32 l = LEN(x);
	U32 m = LEN(y);
	SGN(z) = SGN(x) ^ SGN(y);
	if (l < m)
	{
		BI t;
		U32 i;
		i = l; l = m; m = i;
		t = x; x = y, y = t;
	}
	if (m == 0)
		LEN(z) = 0, SGN(z) = 0;
	else
		LEN(z) = _mul_dispatch(x, l, y, m, z);
}

static void _sqr(BI x, BI z)
{
	U32 l = LEN(x);
	SGN(z) = 0;
	LEN(z) = l ? _sqr_dispatch(x, l, z) : 0;
}

static void _submul(BI x, U32 l, BI y, U32 m, U64 w)
{
	U64 c = 0, p = 0;
	U32 i = 0;
	for (; i < m; i++)
	{
		p += w * y[i];
		c = x[i] - c - (U32)p;
		x[i] = (U32)c;
		p >>= 32;
		c >>= 63;
	}
	if (p)
	{
		c = x[i] - c - (U32)p;
		x[i] = (U32)c;
		c >>= 63;
	}
	for (; c && i < l; c = x[i++]-- == 0);
}

static void _div_base(BI x, U32 *l, BI y, U32 *m, BI z, U32 *n)
{
	U32 o, v, s = *m, p = y[s - 1];
	int c;
	while ((c = _compare(x, *l, y, *m)) > 0)
	{
		if (x[*l - 1] > p)
		{
			o = *l - s;
			v = x[*l - 1] / (p + 1);
		}
		else if (*l > s)
		{

			o = *l - s - 1;
			v = x[*l - 1];
			if (p != 0xFFFFFFFF)
			{
				U64 m = v;
				m = (m << 32) | x[*l - 2];
				v = (U32)(m / (p + 1));
			}
		}
		else
		{
			if (z)
			{
				v = 1;
				_add_base(z, *n, &v, 1, z);
				trim(z);
			}
			*l = _sub_base(x, *l, y, *m, x);
			return;
		}
		if (z)
			_add_base(z + o, *n - o, &v, 1, z + o);
		_submul(x + o, *l - o, y, s, v);
		*l = trim_len(x, *l);
	}
	if (!c)
	{
		if (z)
		{
			v = 1;
			_add_base(z, *n, &v, 1, z);
			*n = trim_len(z, *n);
		}
		*l = 0;
		return;
	}
	if (z)
		*n = trim_len(z, *n);
}

static void _burnikel_ziegler_2n1n(BI x, BI y, BI z, U32 n);
static void _burnikel_ziegler_3n2n(BI x, BI y, BI z, U32 n);
static void _burnikel_ziegler(BI x, BI y, BI z)
{
	U32 s, n = LEN(y), t;
	BI e, d, q;
	while (count_leading_zeros(n) + (s = count_trailing_zeros(n)) < (32 - BURNIKEL_ZIEGLER_THRESHOLD_BITS))
		n += 1 << s;
	s = n * 32 - bit_count(y);
	t = bit_count(x) + s + 1;
	t = t / (n * 32) + (t % (n * 32) ? 1 : 0);
	STACK_ALLOC(e, t * n);
	STACK_ALLOC(d, n);
	STACK_ALLOC(q, (t - 1) * n);
	_zero(e + LEN(x), t * n - LEN(x));
	_lshift(x, s, e);
	_lshift(y, s, d);
	_zero(q, LEN(q) = CAP(q));
	for (; t > 1; t--)
		_burnikel_ziegler_2n1n(e + (t - 2) * n, d, q + (t - 2) * n, n);
	_rshift(trim(e), s, x);
	if (z)
		copy(trim(q), z);
}

static void _burnikel_ziegler_2n1n(BI x, BI y, BI z, U32 n)
{
	if (n <= BURNIKEL_ZIEGLER_THRESHOLD)
	{
		U32 l = 2 * n;
		U32 m = n;
		_div_base(x, &l, y, &m, z, &n);
		return;
	}
	_burnikel_ziegler_3n2n(x + n / 2, y, z + n / 2, n / 2);
	_burnikel_ziegler_3n2n(x, y, z, n / 2);
}

static void _burnikel_ziegler_3n2n(BI x, BI y, BI z, U32 n)
{
	U32 i, j = trim_len(y, n);
	BI d;
	if (_compare(x + 2 * n, n, y + n, n) < 0)
	{
		_burnikel_ziegler_2n1n(x + n, y + n, z, n);
		i = trim_len(z, n);
	}
	else
	{
		for (i = 0; i < n; i++)
			z[i] = 0xFFFFFFFF;
		i = trim_len(y + n, n);
		_sub_base(x + 2 * n, n, y + n, i, x + 2 * n);
		_add_base(x + n, 2 * n, y + n, i, x + n);
		i = n;
	}
	STACK_ALLOC(d, i + j);
	LEN(d) = _mul_dispatch(z, i, y, j, d);
	for (i = trim_len(x, 3 * n), j = 0; _compare(x, i, d, LEN(d)) < 0; j++)
		i = _add_base_s(x, i, y, trim_len(y, 2 * n), x);
	_sub_base(x, i, d, LEN(d), x);
	if (j)
		_sub_base(z, n, &j, 1, z);
}

static void _div(BI x, BI y, BI z)
{
	U32 l = LEN(x), m = LEN(y), n;
	if (m >= BURNIKEL_ZIEGLER_THRESHOLD && l >= m + BURNIKEL_ZIEGLER_THRESHOLD / 2)
	{
		_burnikel_ziegler(x, y, z);
	}
	else
	{
		if (z)
			_zero(z, n = LEN(z) = CAP(z));
		_div_base(x, &l, y, &m, z, &n);
		LEN(x) = l;
		LEN(y) = m;
		if (z)
			LEN(z) = n;
	}
	SGN(x) ^= SGN(y);
	if (z)
		SGN(z) = SGN(x);
}

BI BI_from_string(char *str)
{
	U32 s, n, nibble, len, i = str[0] == '-' ? 1 : 0;
	BI bi;
	for (len = 0; str[len]; len++);
	len -= i;
	s = len & 7;
	len >>= 3;
	if (s)
		++len;
	else
		s = 8;
	bi = BI_alloc(len);
	LEN(bi) = len;
	SGN(bi) = i;
	for (n = 0; len > 0; bi[--len] = n, n = 0, s = 8)
	{
		while (s)
		{
			char c = str[i++];
			if (c >= '0' && c <= '9')
				nibble = c - '0';
			else if (c >= 'A' && c <= 'F')
				nibble = c - 'A' + 10;
			else
				nibble = c - 'a' + 10;
			n |= (nibble << (--s) * 4);
		}
	}
	return trim(bi);
}

BI BI_from_array(U8 *v, U32 len)
{
	BI bi;
	U32 l, n, s, mask = len & 3;
	len >>= 2;
	if (mask)
		++len;
	else
		mask = 4;
	bi = BI_alloc(len);
	LEN(bi) = len;
	SGN(bi) = *v >> 7;
	for (n = 0, s = mask, l = len; l > 0; bi[--l] = n, n = 0, s = 4)
	{
		while (s)
		{
			U32 b = *v++;
			n |= b << ((--s) * 8);
		}
	}
	if (SGN(bi))
	{
		U64 c = 1;
		for (n = 0; n < len; n++)
		{
			c += ~bi[n];
			bi[n] = (U32)c;
			c >>= 32;
		}
		bi[n - 1] &= 0xffffffff >> ((4 - mask) << 3);
	}
	return trim(bi);
}

BI BI_from_u32(U32 n)
{
	if (n == 0)
		return ZERO;
	BI bi = BI_alloc(1);
	LEN(bi) = 1;
	bi[0] = n;
	return bi;
}

BI BI_from_i32(I32 n)
{
	if (n == 0)
		return ZERO;
	if (n > 0)
		return BI_from_u32((U32)n);
	BI bi = BI_from_u32((U32)-n);
	SGN(bi) = 1;
	return bi;
}

BI BI_from_u64(U64 n)
{
	if (n == 0)
		return ZERO;
	U32 h = (n >> 32) & 0xffffffff;
	if (h == 0)
		return BI_from_u32((U32)n);
	BI bi = BI_alloc(2);
	LEN(bi) = 2;
	bi[0] = (U32)n;
	bi[1] = h;
	return bi;
}

BI BI_from_i64(I64 n)
{
	if (n == 0)
		return ZERO;
	if (n > 0)
		return BI_from_u64((U64)n);
	BI bi = BI_from_u64((U64)-n);
	SGN(bi) = 1;
	return bi;
}

static void _size(BI bi, U32 *_l, U32 *_s, U32 *_p)
{
	U32 s, h, l = LEN(bi);
	h = bi[--l];
	*_l = l;
	if ((s = h >> 24)) *_s = 4;
	else if ((s = h >> 16)) *_s = 3;
	else if ((s = h >> 8)) *_s = 2;
	else s = h, *_s = 1;
	*_p = s >> 7;
	if (SGN(bi) && *_p)
	{
		switch (*_s)
		{
		case 1:	if (h != 0x80) return; break;
		case 2:	if (h != 0x8000) return; break;
		case 3:	if (h != 0x800000) return; break;
		case 4:	if (h != 0x80000000) return;
		}
		while (l--)
			if (bi[l]) return;
		*_p = 0;
	}
}

U32 BI_size(BI bi)
{
	U32 l, s, p;
	if (!LEN(bi))
		return 1;
	_size(bi, &l, &s, &p);
	return (l << 2) + s + p;
}

U32 BI_to_array(BI bi, U8 *v, U32 len)
{
	U32 l, s, p, n, i;
	BI tmp;
	_size(bi, &l, &s, &p);
	n = (l << 2) + s + p;
	if (len < n)
		return 0;
	if (SGN(bi))
	{
		U32 m = LEN(bi);
		U64 c = 1;
		STACK_ALLOC(tmp, m);
		for (i = 0; i < m; i++)
		{
			c += ~bi[i];
			tmp[i] = (U32)c;
			c >>= 32;
		}
		i = 0;
		if (p) v[i++] = 0xff;
	}
	else
	{
		tmp = bi;
		i = 0;
		if (p) v[i++] = 0;
	}
	do
	{
		U32 w = tmp[l];
		while (s--)
			v[i++] = (U8)(w >> (s * 8));
		s = 4;
	} while (l--);
	return n;
}

void BI_dump(BI bi)
{
	if (!LEN(bi))
	{
		printf("0");
	}
	else
	{
		U32 size = LEN(bi) - 1;
		if (SGN(bi))
			printf("-");
		printf("%x", bi[size]);
		while (size > 0)
			printf("%08x", bi[--size]);
	}
	printf(" LEN = %d CAP = %d\n", LEN(bi), CAP(bi));
}

BI BI_dup(BI bi)
{
	return copy(bi, BI_alloc(LEN(bi)));
}

int BI_cmp(BI x, BI y)
{
	int r = SGN(x) ^ SGN(y) ? 1 : _compare(x, LEN(x), y, LEN(y));
	return SGN(x) ? -r : r;
}

BI BI_add(BI x, BI y)
{
	BI z;
	U32 l = LEN(x);
	U32 m = LEN(y);
	STACK_ALLOC(z, (l > m ? l : m) + 1);
	_add(x, y, z);
	return BI_dup(z);
}

BI BI_sub(BI x, BI y)
{
	BI z;
	U32 l = LEN(x);
	U32 m = LEN(y);
	STACK_ALLOC(z, (l > m ? l : m) + 1);
	_sub(x, y, z);
	return BI_dup(z);
}

BI BI_lshift(BI x, U32 s)
{
	BI z;
	STACK_ALLOC(z, LEN(x) + (s >> 5) + 1);
	_lshift(x, s, z);
	return BI_dup(z);
}

BI BI_rshift(BI x, U32 s)
{
	BI z;
	STACK_ALLOC(z, LEN(x));
	_rshift(x, s, z);
	return BI_dup(z);
}

BI BI_mul(BI x, BI y)
{
	BI z;
	U32 l = LEN(x);
	U32 m = LEN(y);
	if (!l || !m)
		return ZERO;
	STACK_ALLOC(z, l + m);
	_mul(x, y, z);
	return BI_dup(z);
}

BI BI_sqr(BI x)
{
	BI z;
	U32 l = LEN(x);
	if (!l)
		return ZERO;
	STACK_ALLOC(z, l << 1);
	_sqr(x, z);
	return BI_dup(z);
}

BI BI_div(BI x, BI y)
{
	BI r, q;
	if (!LEN(x) || LEN(x) < LEN(y))
		return ZERO;
	if (!LEN(y))
		return NULL;
	STACK_ALLOC(r, LEN(x));
	STACK_ALLOC(q, LEN(x) + 1 - LEN(y));
	copy(x, r);
	_div(r, y, q);
	return BI_dup(q);
}

BI BI_mod(BI x, BI y)
{
	BI r;
	if (!LEN(x))
		return ZERO;
	if (!LEN(y) || SGN(y))
		return NULL;
	STACK_ALLOC(r, LEN(x));
	copy(x, r);
	_div(r, y, NULL);
	if (SGN(r))
	{
		BI z;
		U32 l = LEN(r);
		U32 m = LEN(y);
		STACK_ALLOC(z, (l > m ? l : m) + 1);
		_add(r, y, z);
		r = z;
	}
	return BI_dup(r);
}

BI BI_modinv(BI x, BI y)
{
	BI m, n, q, c, r, s, t;
	U32 i = LEN(x), j = LEN(y);
	if (!i || !j || SGN(y))
		return NULL;
	if (i < ++j) i = j;
	STACK_ALLOC(m, i);
	copy(x, m);
	STACK_ALLOC(n, j);
	copy(y, n);
	STACK_ALLOC(q, j);
	STACK_ALLOC(c, j);
	STACK_ALLOC(r, j);
	STACK_ALLOC(s, j);
	r[0] = 1;
	LEN(r) = 1;
	LEN(s) = 0;
	for (; LEN(n); t = m, m = n, n = t, t = r, r = s, s = t)
	{
		_div(m, n, q);
		_mul(q, s, c);
		_sub(r, c, r);
	}
	if (LEN(m) != 1 || m[0] != 1)
		return NULL;
	_div(r, y, NULL);
	if (SGN(r))
		_add(y, r, r);
	return BI_dup(r);
}

static U32 _modexp_pattern(BI e, U32 d, U32 *i, U32 *l)
{
	int n;
	U32 p;
	if (*i == 0)
		return 0xFFFFFFFF;
	p = --*i;
	if (!get_bit(e, p))
	{
		*l = 1;
		return 0;
	}
	for (d = p < d ? p : d - 1; !get_bit(e, p - d); d--);
	*l = d + 1;
	for (*i -= d, n = 1; d; d--)
		n = (n << 1) + (get_bit(e, --p) ? 1 : 0);
	return n;
}

static U32 _modexp_window_size(U32 l)
{
	U32 i = 0;
	while (MODEXP_WINDOW[i] < l)
		i++;
	return i + 1;
}

#ifdef LIMAX_PLAT_32

static void _modexp_direct(BI x, BI y, BI z, BI a)
{
	BI *b, p, r, t;
	U32 d, i = LEN(z) * 2, k, l, m;
	STACK_ALLOC(r, i);
	STACK_ALLOC(t, i);
	k = bit_count(y) - 1;
	d = _modexp_window_size(k);
	l = 1 << (d - 1);
	b = (BI *)alloca(l * sizeof(BI));
	if (i < LEN(x))
		i = LEN(x);
	STACK_ALLOC(p, i);
	copy(x, p);
	_div(p, z, NULL);
	STACK_ALLOC(b[0], LEN(p));
	copy(p, b[0]);
	copy(p, r);
	if (l > 1)
	{
		_sqr(b[0], p);
		_div(p, z, NULL);
		while (--l)	b[l] = NULL;
	}
	while ((m = _modexp_pattern(y, d, &k, &l)) != 0xFFFFFFFF)
	{
		while (l--)
		{
			_sqr(r, t);
			_div(t, z, NULL);
			x = r, r = t, t = x;
		}
		if (m)
		{
			m >>= 1;
			if (!b[m])
			{
				for (i = m - 1; !b[i]; i--);
				for (; i < m; i++)
				{
					_mul(p, b[i], t);
					_div(t, z, NULL);
					STACK_ALLOC(b[i + 1], LEN(t));
					copy(t, b[i + 1]);
				}
			}
			_mul(r, b[m], t);
			_div(t, z, NULL);
			x = r, r = t, t = x;
		}
	}
	copy(r, a);
}

#else // #ifdef LIMAX_PLAT_32

static void _barrett_reduction(BI a, BI n, BI m, U32 k)
{
	BI t, r;
	U32 i = LEN(m) << 1;
	STACK_ALLOC(t, i);
	STACK_ALLOC(r, i);
	_rshift(a, k - 1, r);
	_mul(m, r, t);
	_rshift(t, k + 1, r);
	_mul(n, r, t);
	_sub(a, t, a);
	while (BI_cmp(a, n) > 0)
		_sub(a, n, a);
}


static void _modexp_barrett(BI x, BI y, BI z, BI a)
{
	BI n, m, *b, t, r, p;
	U32 d, i, j;
	U32 k = bit_count(z);
	U32 l = (k >> 4) + 1;
	U32 c = (k << 1) & 31;
	STACK_ALLOC(n, l);
	_zero(n, (LEN(n) = l) - 1);
	n[l - 1] = 1 << c;
	STACK_ALLOC(m, l + 1 - LEN(z));
	_div(n, z, m);
	l = LEN(z) * 2;
	STACK_ALLOC(t, l);
	STACK_ALLOC(r, l);
	if (l < LEN(x))
		l = LEN(x);
	STACK_ALLOC(p, l);
	copy(x, p);
	_div(p, z, NULL);
	_barrett_reduction(p, z, m, k);
	i = bit_count(y) - 1;
	d = _modexp_window_size(i);
	l = 1 << (d - 1);
	b = (BI *)alloca(l * sizeof(BI));
	STACK_ALLOC(b[0], LEN(p));
	copy(p, b[0]);
	copy(p, r);
	if (l > 1)
	{
		_sqr(b[0], p);
		_barrett_reduction(p, z, m, k);
		while (--l)	b[l] = NULL;
	}
	while ((c = _modexp_pattern(y, d, &i, &l)) != 0xFFFFFFFF)
	{
		while (l--)
		{
			_sqr(r, t);
			_barrett_reduction(t, z, m, k);
			x = r, r = t, t = x;
		}
		if (c)
		{
			c >>= 1;
			if (!b[c])
			{
				for (j = c - 1; !b[j]; j--);
				for (; j < c; j++)
				{
					_mul(p, b[j], t);
					_barrett_reduction(t, z, m, k);
					STACK_ALLOC(b[j + 1], LEN(t));
					copy(t, b[j + 1]);
				}
			}
			_mul(r, b[c], t);
			_barrett_reduction(t, z, m, k);
			x = r, r = t, t = x;
		}
	}
	copy(r, a);
}

static void _montgomery_reduction(BI x, BI y, U32 z)
{
	U32 i, j, k = 0, l = LEN(x), m = LEN(y);
	U64 c, v;
	for (i = l; i < m * 2 + 1; i++) x[i] = 0;
	for (i = 0; i < m; i++)
	{
		v = x[i] * z;
		for (c = 0, k = i, j = 0; j < m; j++, k++)
		{
			c = x[k] + c + y[j] * v;
			x[k] = (U32)c;
			c >>= 32;
		}
		while (c)
		{
			c = x[k] + c;
			x[k++] = (U32)c;
			c >>= 32;
		}
	}
	if (_compare(x + m, LEN(x) = k - m, y, m) > 0)
		LEN(x) = _sub_base(x + m, LEN(x), y, LEN(y), x);
	else
		_copy(x, x + m, LEN(x));
}

static U32 modexp32(U32 v) {
	U32 t = v;
	t *= 2 - v * t;
	t *= 2 - v * t;
	t *= 2 - v * t;
	t *= 2 - v * t;
	return t;
}

static void _modexp_montgomery(BI x, BI y, BI z, BI a)
{
	BI _r, *b, _c, t, p;
	U32 d, i = ~modexp32(z[0]) + 1, j, k, l, m = LEN(z);
	STACK_ALLOC(_r, m * 2 + 1);
	_zero(_r, (LEN(_r) = CAP(_r)) - 1);
	_r[m * 2] = 1;
	_div(_r, z, NULL);
	STACK_ALLOC(t, LEN(x));
	copy(x, t);
	_div(t, z, NULL);
	STACK_ALLOC(_c, m * 2 + 1);
	k = bit_count(y) - 1;
	d = _modexp_window_size(k);
	l = 1 << (d - 1);
	b = (BI *)alloca(l * sizeof(BI));
	_mul(t, _r, _c);
	_montgomery_reduction(_c, z, i);
	STACK_ALLOC(b[0], LEN(_c));
	copy(_c, b[0]);
	copy(_c, _r);
	if (l > 1)
	{
		STACK_ALLOC(p, m * 2 + 1);
		_sqr(b[0], p);
		_montgomery_reduction(p, z, i);
		while (--l) b[l] = NULL;
	}
	else
		p = NULL;
	while ((m = _modexp_pattern(y, d, &k, &l)) != 0xFFFFFFFF)
	{
		while (l--)
		{
			_sqr(_r, _c);
			_montgomery_reduction(_c, z, i);
			t = _c, _c = _r, _r = t;
		}
		if (m)
		{
			m >>= 1;
			if (!b[m])
			{
				for (j = m - 1; !b[j]; j--);
				for (; j < m; j++)
				{
					_mul(p, b[j], _c);
					_montgomery_reduction(_c, z, i);
					STACK_ALLOC(b[j + 1], LEN(_c));
					copy(_c, b[j + 1]);
				}
			}
			_mul(_r, b[m], _c);
			_montgomery_reduction(_c, z, i);
			t = _c, _c = _r, _r = t;
		}
	}
	_montgomery_reduction(_r, z, i);
	copy(_r, a);
}

#endif // #else // #ifdef LIMAX_PLAT_32

BI BI_modexp(BI x, BI y, BI z)
{
	BI r;
	if (!LEN(z) || SGN(z) || SGN(y))
		return NULL;
	if (!LEN(x))
		return ZERO;
	if (LEN(x) == 1 && x[0] == 1)
	{
		r = BI_from_u32(1);
	}
	else
	{
		r = BI_alloc(LEN(z));
#ifdef LIMAX_PLAT_32
		_modexp_direct(x, y, z, r);
#else
		if (z[0] & 1)
			_modexp_montgomery(x, y, z, r);
		else
			_modexp_barrett(x, y, z, r);
#endif
	}
	return r;
}
