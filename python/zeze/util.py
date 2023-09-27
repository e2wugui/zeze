def assign_list(a, b):
    an = len(a)
    bn = len(b)
    n = min(an, bn)
    if n > 0:
        a[0:n] = b[0:n]
    if an < bn:
        a += b[n:bn]
    elif an > bn:
        del a[n:an]
    return a


def assign_copy_list(a, b):
    an = len(a)
    bn = len(b)
    n = min(an, bn)
    for i in range(n):
        a[i] = b[i].copy()
    if an < bn:
        for i in range(n, bn):
            a.append(b[i].copy())
    elif an > bn:
        del a[n:an]
    return a


def assign_set(a, b):
    a.clear()
    a.update(b)
    return a


def assign_copy_set(a, b):
    a.clear()
    for v in b:
        a.add(v.copy())
    return a


def assign_dict(a, b):
    a.clear()
    a.update(b)
    return a


def assign_copy_dict(a, b):
    a.clear()
    for k, v in b:
        a[k] = v.copy()
    return a


def assign_dynamic_bean(a, b):
    if a.__class__ == b.__class__:
        a.assign(b)
        return a
    return b.copy()


def hash_list(a):
    h = 0
    for v in a:
        h = h * 31 + v.__hash__()
    return h


def hash_dict(a):
    h = 0
    for k, v in a:
        h = h * 31 + k.__hash__()
        h = h * 31 + v.__hash__()
    return h


_INDENT_MAX = 64
_INDENTS = []
for indent in range(0, _INDENT_MAX):
    _INDENTS.append(" " * indent)


def indent(n):
    if n <= 0:
        return ""
    if n >= _INDENT_MAX:
        n = _INDENT_MAX - 1
    return _INDENTS[n]


def num_to_hex(n):
    return n + 0x30 + (((9 - n) >> 31) & (0x41 - 0x39 - 1))  # 'A'=0x41 无分支,比查表快


def to_string(buf, offset=0, length=-1):
    if length < 0:
        length = len(buf) - offset
    if length <= 0:
        return ""
    s = bytearray(length * 3 - 1)
    j = 0
    for i in range(0, length):
        if i > 0:
            s[j] = 0x2d  # '-'
            j += 1
        b = buf[offset + i]
        s[j] = num_to_hex((b >> 4) & 0xf)
        s[j + 1] = num_to_hex(b & 0xf)
        j += 2
    return str(s, "latin_1")


def to_string_with_limit(limit, buf, offset=0, length=-1):
    if limit < 0:
        raise Exception("limit=" + limit)
    if length < 0:
        length = len(buf) - offset
    if length <= limit:
        return to_string(buf, offset, length)
    s = bytearray(limit * 3 + 15)
    j = 0
    for i in range(0, limit):
        b = buf[offset + i]
        if i > 0:
            s[j] = 0x2d  # '-'
            j += 1
        s[j] = num_to_hex((b >> 4) & 0xf)
        s[j + 1] = num_to_hex(b & 0xf)
        j += 2
    s[j:j + 5] = b"...[+"
    j += 5
    t = str(length - limit).encode("latin_1")
    n = len(t)
    s[j:j + n] = t
    j += n
    s[j] = 0x5d  # ']'
    return str(s[0:j + 1], "latin_1")


def to_string_with_limit2(limit1, limit2, buf, offset=0, length=-1):
    limit = limit1 + limit2
    if (limit1 | limit2 | limit) < 0:
        raise Exception("limit=" + limit1 + '+' + limit2)
    if length < 0:
        length = len(buf) - offset
    if length <= limit:
        return to_string(buf, offset, length)
    s = bytearray(limit * 3 + 18)
    j = 0
    if limit1 > 0:
        for i in range(0, limit1):
            b = buf[offset + i]
            if i > 0:
                s[j] = 0x2d  # '-'
                j += 1
            s[j] = num_to_hex((b >> 4) & 0xf)
            s[j + 1] = num_to_hex(b & 0xf)
            j += 2
        s[j:j + 3] = b"..."
        j += 3
    s[j] = 0x5b  # '['
    s[j + 1] = 0x2b  # '+'
    j += 2
    t = str(length - limit).encode("latin_1")
    n = len(t)
    s[j:j + n] = t
    j += n
    s[j] = 0x5d  # ']'
    j += 1
    if limit2 > 0:
        s[j:j + 3] = b"..."
        j += 3
    offset += length - limit2
    for i in range(0, limit2):
        b = buf[offset + i]
        if i > 0:
            s[j] = 0x2d  # '-'
            j += 1
        s[j] = num_to_hex((b >> 4) & 0xf)
        s[j + 1] = num_to_hex(b & 0xf)
        j += 2
    return str(s[0:j], "latin_1")


def to_buf(hex_str, buf, offset):
    # 十六进制字符串转成二进制数组. 大小写的A~F都支持,忽略其它字符
    mask = ~0x007E_0000_007E_03FF  # 0~9;A~F;a~f
    v = 1
    for c in hex_str:
        c = ord(c) - 0x30  # '0'
        if (c & ~63) == 0 and ((mask >> c) & 1) == 0:
            v = (v << 4) + (c & 0xf) + ((c >> 4) & 1) * 9
            if v > 0xff:
                buf[offset] = v & 0xff
                offset += 1
                v = 1


def to_bytes(hex_str):
    # 十六进制字符串转成二进制数组. 大小写的A~F都支持,忽略其它字符
    mask = 0x007E_0000_007E_03FF  # 0~9;A~F;a~f
    n = 0
    for c in hex_str:
        c = ord(c) - 0x30  # '0'
        if (c & ~63) == 0:
            n += (mask >> c) & 1
    b = bytearray(n >> 1)
    to_buf(hex_str, b, 0)
    return b
