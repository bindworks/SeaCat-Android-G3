package com.teskalabs.seacat.misc


class Base32 protected constructor(private val ALPHABET: String) {
    private val DIGITS: CharArray
    private val MASK: Int
    private val SHIFT: Int
    private val CHAR_MAP: HashMap<Char, Int>

    init {
        DIGITS = ALPHABET.toCharArray()
        MASK = DIGITS.size - 1
        SHIFT = Integer.numberOfTrailingZeros(DIGITS.size)
        CHAR_MAP = HashMap()
        for (i in DIGITS.indices) {
            CHAR_MAP[DIGITS[i]] = i
        }
    }

    @Throws(DecodingException::class)
    protected fun decodeInternal(encoded: String): ByteArray {
        // Remove whitespace and separators
        var s = encoded.trim { it <= ' ' }.replace(SEPARATOR.toRegex(), "").replace(" ".toRegex(), "")

        // Remove padding. Note: the padding is used as hint to determine how many
        // bits to decode from the last incomplete chunk (which is commented out
        // below, so this may have been wrong to start with).
        s = s.replaceFirst("[=]*$".toRegex(), "")

        // Canonicalize to all upper case
        s = s.uppercase()
        if (s.length == 0) {
            return ByteArray(0)
        }
        val encodedLength = s.length
        val outLength = encodedLength * SHIFT / 8
        val result = ByteArray(outLength)
        var buffer = 0
        var next = 0
        var bitsLeft = 0
        for (c in s.toCharArray()) {
            if (!CHAR_MAP.containsKey(c)) {
                throw DecodingException("Illegal character: $c")
            }
            buffer = buffer shl SHIFT
            buffer = buffer or (CHAR_MAP[c]!!.and(MASK))
            bitsLeft += SHIFT
            if (bitsLeft >= 8) {
                result[next++] = (buffer.shr(bitsLeft - 8)).toByte()
                bitsLeft -= 8
            }
        }
        // We'll ignore leftover bits for now.
        //
        // if (next != outLength || bitsLeft >= SHIFT) {
        //  throw new DecodingException("Bits left: " + bitsLeft);
        // }
        return result
    }

    protected fun encodeInternal(data: ByteArray): String {
        if (data.size == 0) {
            return ""
        }

        // SHIFT is the number of bits per output character, so the length of the
        // output is the length of the input multiplied by 8/SHIFT, rounded up.
        if (data.size >= 1 shl 28) {
            // The computation below will fail, so don't do it.
            throw IllegalArgumentException()
        }

        val outputLength = (data.size * 8 + SHIFT - 1) / SHIFT
        val result = StringBuilder(outputLength)

        var buffer = data[0].toInt()
        var next = 1
        var bitsLeft = 8
        while (bitsLeft > 0 || next < data.size) {
            if (bitsLeft < SHIFT) {
                if (next < data.size) {
                    buffer = buffer.shl(8)
                    buffer = buffer.or(data[next++].toInt().and(0xff))
                    bitsLeft += 8
                } else {
                    val pad = SHIFT - bitsLeft
                    buffer = buffer.shl(pad)
                    bitsLeft += pad
                }
            }
            val index = MASK.and(buffer.shr(bitsLeft - SHIFT))
            bitsLeft -= SHIFT
            result.append(DIGITS[index])
        }
        return result.toString()
    }

    class DecodingException(message: String) : Exception(message)

    companion object {
        // singleton

        internal val instance = Base32("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567") // RFC 4648/3548

        internal val SEPARATOR = "-"

        @Throws(DecodingException::class)
        fun decode(encoded: String): ByteArray {
            return instance.decodeInternal(encoded)
        }

        fun encode(data: ByteArray): String {
            return instance.encodeInternal(data)
        }
    }
}