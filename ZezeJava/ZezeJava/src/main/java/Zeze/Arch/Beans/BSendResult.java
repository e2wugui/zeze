package Zeze.Arch.Beans;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.LongList;
import Zeze.Util.Str;

public final class BSendResult implements Serializable {
	public static final long TYPEID = -7186434891670297524L;

	private final LongList _ErrorLinkSids = new LongList();

	public LongList getErrorLinkSids() {
		return _ErrorLinkSids;
	}

	public BSendResult() {
	}

	@Override
	public long typeId() {
		return TYPEID;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		buildString(sb, 0);
		return sb.toString();
	}

	public void buildString(StringBuilder sb, int level) {
		var i1 = Str.indent(level + 4);
		var i2 = Str.indent(level + 8);
		sb.append("Zeze.Arch.Beans.BSendResult: {\n");
		sb.append(i1).append("ErrorLinkSids=[");
		if (!_ErrorLinkSids.isEmpty()) {
			sb.append('\n');
			for (int i = 0, n = _ErrorLinkSids.size(); i < n; i++)
				sb.append(i2).append("Item=").append(_ErrorLinkSids.get(i)).append(",\n");
			sb.append(i1);
		}
		sb.append("]\n");
		sb.append(Str.indent(level)).append('}');
	}

	private static int _PRE_ALLOC_SIZE_ = 16;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}

	@SuppressWarnings("UnusedAssignment")
	@Override
	public void encode(ByteBuffer _o_) {
		int _i_ = 0;
		{
			var _x_ = _ErrorLinkSids;
			int _n_ = _x_.size();
			if (_n_ != 0) {
				_i_ = _o_.WriteTag(_i_, 1, ByteBuffer.LIST);
				_o_.WriteListType(_n_, ByteBuffer.INTEGER);
				for (int _j_ = 0; _j_ < _n_; _j_++)
					_o_.WriteLong(_x_.get(_j_));
			}
		}
		_o_.WriteByte(0);
	}

	@SuppressWarnings("UnusedAssignment")
	@Override
	public void decode(IByteBuffer _o_) {
		int _t_ = _o_.ReadByte();
		int _i_ = _o_.ReadTagSize(_t_);
		if (_i_ == 1) {
			var _x_ = _ErrorLinkSids;
			_x_.clear();
			if ((_t_ & ByteBuffer.TAG_MASK) == ByteBuffer.LIST) {
				int _n_ = _o_.ReadTagSize(_t_ = _o_.ReadByte());
				if (_x_.capacity() < _n_) {
					_x_.wraps(new long[Math.min(_n_, 0x10000)]);
					_x_.clear();
				}
				for (; _n_ > 0; _n_--)
					_x_.add(_o_.ReadLong(_t_));
			} else
				_o_.SkipUnknownFieldOrThrow(_t_, "Collection");
			_i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
		}
		while (_t_ != 0) {
			_o_.SkipUnknownField(_t_);
			_o_.ReadTagSize(_t_ = _o_.ReadByte());
		}
	}
}
