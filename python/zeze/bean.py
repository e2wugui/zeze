from zeze.buffer import ByteBuffer
from zeze.util import indent


class Serializable:
    def encode(self, bb):
        raise NotImplementedError("Serializable.encode")

    def decode(self, bb):
        raise NotImplementedError("Serializable.decode")

    # noinspection PyMethodMayBeStatic
    def type_id(self):
        return 0

    # noinspection PyMethodMayBeStatic
    def get_pre_alloc_size(self):
        return 16

    def set_pre_alloc_size(self, size):
        pass


class BeanKey(Serializable):
    def type_name(self):
        raise NotImplementedError("BeanKey.type_name")

    def __hash__(self):
        raise NotImplementedError("BeanKey.__hash__")

    def __eq__(self, other):
        raise NotImplementedError("BeanKey.__eq__")

    def build_string(self, sb, level):
        raise NotImplementedError("BeanKey.build_string")


class Bean(Serializable):
    def type_name(self):
        raise NotImplementedError("Bean.type_name")

    def reset(self):
        pass

    def assign(self, other):
        raise NotImplementedError("Bean.assign")

    def copy(self):
        c = self.__new__(self.__class__)
        c.assign(self)
        return c

    def __hash__(self):
        raise NotImplementedError("Bean.__hash__")

    def __eq__(self, other):
        raise NotImplementedError("Bean.__eq__")

    def build_string(self, sb, level):
        raise NotImplementedError("Bean.build_string")


class EmptyBean(Bean):
    TYPEID = 0
    __instance = None

    def __new__(cls):
        if cls.__instance is None:
            cls.__instance = super().__new__(cls)
        return cls.__instance

    def type_id(self):
        return 0

    def type_name(self):
        return "EmptyBean"

    def assign(self, other):
        pass

    def copy(self):
        return self

    def get_pre_alloc_size(self):
        return 1

    def encode(self, bb):
        bb.write_byte(0)

    def decode(self, bb):
        bb.skip_unknown_field(ByteBuffer.BEAN)

    def __hash__(self):
        raise 0

    def __eq__(self, other):
        return self.__class__ == other.__class__

    def __str__(self):
        return "EmptyBean{}"

    def build_string(self, sb, level):
        raise sb.append(indent(level)).append("EmptyBean{}")
