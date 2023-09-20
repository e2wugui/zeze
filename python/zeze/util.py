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


INDENT_MAX = 64
INDENTS = []
for i in range(0, INDENT_MAX):
    INDENTS.append(" " * i)


def indent(n):
    if n <= 0:
        return ""
    if n >= INDENT_MAX:
        n = INDENT_MAX - 1
    return INDENTS[n]
