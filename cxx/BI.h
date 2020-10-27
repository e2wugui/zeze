
#pragma once

typedef unsigned char U8;
typedef unsigned int U32;
typedef unsigned long long U64;
typedef signed int I32;
typedef signed long long I64;

typedef U32 *BI;

#ifdef __cplusplus
extern "C"
{
#endif

int BI_cmp(BI x, BI y);
BI BI_from_string(char *v);
BI BI_from_array(U8 *v, U32 s);
BI BI_from_u32(U32 n);
BI BI_from_i32(I32 n);
BI BI_from_u64(U64 n);
BI BI_from_i64(I64 n);
U32 BI_to_array(BI bi, U8 *v, U32 s);
U32 BI_size(BI bi);
void BI_free(BI bi);
void BI_dump(BI bi);
BI BI_dup(BI bi);
BI BI_add(BI x, BI y);
BI BI_sub(BI x, BI y);
BI BI_lshift(BI x, U32 s);
BI BI_rshift(BI x, U32 s);
BI BI_mul(BI x, BI y);
BI BI_sqr(BI x);
BI BI_div(BI x, BI y);
BI BI_mod(BI x, BI y);
BI BI_modinv(BI x, BI y);
BI BI_modexp(BI x, BI y, BI z);

#ifdef __cplusplus
}
#endif
