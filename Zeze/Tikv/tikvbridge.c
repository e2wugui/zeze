
#include "tikvbridge.h"

#include <stdio.h>

typedef int (__stdcall *Walker) (void * key, int keylen, void * value, int valuelen);

int BridgeWalker(void* walker, void * key, int keylen, void * value, int valuelen)
{
	Walker _w = (Walker)walker;
	return _w(key, keylen, value, valuelen);
}
