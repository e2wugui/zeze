#pragma once

namespace limax {

	class RFC2118Encode: public Codec {
		std::shared_ptr<Codec> sink;
		int32_t pos;
		int32_t rem;
		int8_t dict[8192];
		int16_t hash[65536];
		int32_t idx;
		int32_t match_idx;
		int32_t match_off;
		int32_t match_len;
		bool	flushed;

		void putBits(int32_t val, int32_t nbits);
		void putLiteral(int8_t c);
		void putTuple(int32_t off, int32_t len);
		void _flush();
	public:
		RFC2118Encode(std::shared_ptr<Codec> _sink);
		void update(int8_t c);
		void update(int8_t data[], int32_t off, int32_t len);
		void flush();
	};

	class RFC2118Decode: public Codec {
		std::shared_ptr<Codec> sink;
		int32_t rem;
		int32_t pos;
		int32_t off;
		int32_t len;
		int8_t hist[8192 * 3];
		int32_t hpos;

		void drain();
		void copy(int32_t dstPos, int32_t srcPos, int32_t length);
		void output(int8_t c);
		void output(int32_t off, int32_t len);
		int32_t bitCompute();
		void process();
	public:
		class UncompressException: public CodecException {
		};
		RFC2118Decode(std::shared_ptr<Codec> _sink);
		void update(int8_t c);
		void update(int8_t data[], int32_t off, int32_t len);
		void flush();
	};

} // namespace limax {
