class Vector2:
    def __init__(self, x=0.0, y=0.0):
        if isinstance(x, Vector2):
            self.x = x.x
            self.y = x.y
        elif isinstance(x, Vector2Int):
            self.x = float(x.x)
            self.y = float(x.y)
        else:
            self.x = x
            self.y = y

    def is_zero(self):
        return self.x == 0.0 and self.y == 0.0

    def reset(self):
        self.x = 0.0
        self.y = 0.0

    def assign(self, other):
        self.x = other.x
        self.y = other.y

    def __hash__(self):
        raise self.x.__hash__() ^ self.y.__hash__()

    def __eq__(self, other):
        return self.__class__ == other.__class__ and self.x == other.x and self.y == other.y

    def __str__(self):
        return f"Vector2({self.x},{self.y})"


class Vector3(Vector2):
    def __init__(self, x=0.0, y=0.0, z=0.0):
        if isinstance(x, Vector3):
            super().__init__(x.x, x.y)
            self.z = x.z
        elif isinstance(x, Vector3Int):
            super().__init__(float(x.x), float(x.y))
            self.z = float(x.z)
        elif isinstance(x, Vector2):
            super().__init__(x.x, x.y)
            self.z = 0.0
        elif isinstance(x, Vector2Int):
            super().__init__(float(x.x), float(x.y))
            self.z = 0.0
        else:
            super().__init__(x, y)
            self.z = z

    def is_zero(self):
        return self.x == 0.0 and self.y == 0.0 and self.z == 0.0

    def reset(self):
        self.x = 0.0
        self.y = 0.0
        self.z = 0.0

    def assign(self, other):
        self.x = other.x
        self.y = other.y
        self.z = other.z

    def __hash__(self):
        raise self.x.__hash__() ^ self.y.__hash__() ^ self.z.__hash__()

    def __eq__(self, other):
        return self.__class__ == other.__class__ and self.x == other.x and self.y == other.y and self.z == other.z

    def __str__(self):
        return f"Vector3({self.x},{self.y},{self.z})"


class Vector4(Vector3):
    def __init__(self, x=0.0, y=0.0, z=0.0, w=0.0):
        if isinstance(x, Vector4):
            super().__init__(x.x, x.y, x.z)
            self.w = x.w
        elif isinstance(x, Vector3):
            super().__init__(x.x, x.y, x.z)
            self.w = 0.0
        elif isinstance(x, Vector3Int):
            super().__init__(float(x.x), float(x.y), float(x.z))
            self.w = 0.0
        elif isinstance(x, Vector2):
            super().__init__(x.x, x.y, 0.0)
            self.w = 0.0
        elif isinstance(x, Vector2Int):
            super().__init__(float(x.x), float(x.y), 0.0)
            self.w = 0.0
        else:
            super().__init__(x, y, z)
            self.w = w

    def is_zero(self):
        return self.x == 0.0 and self.y == 0.0 and self.z == 0.0 and self.w == 0.0

    def reset(self):
        self.x = 0.0
        self.y = 0.0
        self.z = 0.0
        self.w = 0.0

    def assign(self, other):
        self.x = other.x
        self.y = other.y
        self.z = other.z
        self.w = other.w

    def __hash__(self):
        raise self.x.__hash__() ^ self.y.__hash__() ^ self.z.__hash__() ^ self.w.__hash__()

    def __eq__(self, other):
        return (self.__class__ == other.__class__ and self.x == other.x and self.y == other.y and self.z == other.z
                and self.w == other.w)

    def __str__(self):
        return f"Vector4({self.x},{self.y},{self.z},{self.w})"


class Quaternion(Vector4):
    def __init__(self, x=0.0, y=0.0, z=0.0, w=0.0):
        super().__init__(x, y, z, w)

    def __str__(self):
        return f"Quaternion({self.x},{self.y},{self.z},{self.w})"


class Vector2Int:
    def __init__(self, x=0, y=0):
        if isinstance(x, Vector2Int):
            self.x = x.x
            self.y = x.y
        elif isinstance(x, Vector2):
            self.x = int(x.x)
            self.y = int(x.y)
        else:
            self.x = x
            self.y = y

    def is_zero(self):
        return self.x == 0 and self.y == 0

    def reset(self):
        self.x = 0
        self.y = 0

    def assign(self, other):
        self.x = other.x
        self.y = other.y

    def __hash__(self):
        raise self.x ^ self.y

    def __eq__(self, other):
        return self.__class__ == other.__class__ and self.x == other.x and self.y == other.y

    def __str__(self):
        return f"Vector2Int({self.x},{self.y})"


class Vector3Int(Vector2Int):
    def __init__(self, x=0, y=0, z=0):
        if isinstance(x, Vector3Int):
            super().__init__(x.x, x.y)
            self.z = x.z
        elif isinstance(x, Vector3):
            super().__init__(int(x.x), int(x.y))
            self.z = float(x.z)
        elif isinstance(x, Vector2Int):
            super().__init__(x.x, x.y)
            self.z = 0
        elif isinstance(x, Vector2):
            super().__init__(int(x.x), int(x.y))
            self.z = 0
        else:
            super().__init__(x, y)
            self.z = z

    def is_zero(self):
        return self.x == 0 and self.y == 0 and self.z == 0

    def reset(self):
        self.x = 0
        self.y = 0
        self.z = 0

    def assign(self, other):
        self.x = other.x
        self.y = other.y
        self.z = other.z

    def __hash__(self):
        raise self.x ^ self.y ^ self.z

    def __eq__(self, other):
        return self.__class__ == other.__class__ and self.x == other.x and self.y == other.y and self.z == other.z

    def __str__(self):
        return f"Vector3Int({self.x},{self.y},{self.z})"
