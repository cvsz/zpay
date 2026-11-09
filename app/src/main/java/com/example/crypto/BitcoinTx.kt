package com.example.crypto

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.MessageDigest
import org.bouncycastle.crypto.digests.RIPEMD160Digest

/**
 * Bitcoin native SegWit (P2WPKH / BIP-84) transaction builder and BIP-143 signer.
 *
 * Implements:
 * - CompactSize (varint) encoding
 * - P2WPKH scriptPubKey generation
 * - BIP-143 sighash for native SegWit inputs
 * - RFC-6979 deterministic ECDSA with canonical low-S DER encoding
 */
object BitcoinTx {

    private const val SIGHASH_ALL = 0x01

    /** Transaction input with txid in RPC display order. */
    data class Input(
        val txid: String,
        val vout: Int,
        val valueSatoshis: Long,
        val sequence: Long = 0xfffffffL,
        val compressedPubKey: ByteArray
    )

    /** Transaction output. */
    data class Output(
        val valueSatoshis: Long,
        val scriptPubKey: ByteArray
    )

    /** P2WPKH scriptPubKey for 20-byte witness program hash. */
    fun p2wpkhScript(witnessProgramHash160: ByteArray): ByteArray {
        require(witnessProgramHash160.size == 20) { "witness program must be 20 bytes" }
        val out = ByteArrayOutputStream()
        out.write(0x00)
        out.write(0x14)
        out.write(witnessProgramHash160)
        return out.toByteArray()
    }

    /** Computes hash160 (RIPEMD160(SHA256)) for compressed public key. */
    fun witnessProgramHash160(compressedPublicKey: ByteArray): ByteArray {
        require(compressedPublicKey.size == 33) { "Expected compressed public key" }
        val sha = MessageDigest.getInstance("SHA-256").digest(compressedPublicKey)
        val ripemd = RIPEMD160Digest()
        ripemd.update(sha, 0, sha.size)
        val hash = ByteArray(ripemd.digestSize)
        ripemd.doFinal(hash, 0)
        return hash
    }

    /** Builds unsigned transaction prefix and computes BIP-143 sighash. */
    fun buildUnsignedP2wpkh(input: Input, outputs: List<Output>, lockTime: Long = 0): UnsignedTransaction {
        require(outputs.isNotEmpty()) { "At least one output required" }

        val outpoints = ByteArrayOutputStream()
        outpoints.write(reverseBytes(hexToBytes(input.txid)))
        writeLittleEndianInt(outpoints, input.vout)

        val sequences = ByteArrayOutputStream()
        sequences.write(leBytes(input.sequence, 4))

        val outputsSer = ByteArrayOutputStream()
        for (output in outputs) {
            writeLittleEndianLong(outputsSer, output.valueSatoshis)
            writeVarInt(outputsSer, output.scriptPubKey.size)
            outputsSer.write(output.scriptPubKey)
        }

        val hashPrevouts = doubleSha256(outpoints.toByteArray())
        val hashSequence = doubleSha256(sequences.toByteArray())
        val hashOutputs = doubleSha256(outputsSer.toByteArray())

        val scriptCode = p2wpkhScript(witnessProgramHash160(input.compressedPubKey))

        val preimage = ByteArrayOutputStream()
        writeLittleEndianInt(preimage, 2)
        preimage.write(hashPrevouts)
        preimage.write(hashSequence)
        preimage.write(reverseBytes(hexToBytes(input.txid)))
        writeLittleEndianInt(preimage, input.vout)
        writeVarInt(preimage, scriptCode.size)
        preimage.write(scriptCode)
        writeLittleEndianLong(preimage, input.valueSatoshis)
        preimage.write(leBytes(input.sequence, 4))
        preimage.write(hashOutputs)
        writeLittleEndianInt(preimage, lockTime.toInt())
        writeLittleEndianInt(preimage, SIGHASH_ALL)

        val sighash = doubleSha256(preimage.toByteArray())

        val prefix = ByteArrayOutputStream()
        writeLittleEndianInt(prefix, 2)
        prefix.write(0x00) // marker
        prefix.write(0x01) // flag (SegWit)
        writeVarInt(prefix, 1)
        prefix.write(reverseBytes(hexToBytes(input.txid)))
        writeLittleEndianInt(prefix, input.vout)
        writeVarInt(prefix, 0) // empty scriptSig
        prefix.write(leBytes(input.sequence, 4))
        writeVarInt(prefix, outputs.size)
        for (output in outputs) {
            writeLittleEndianLong(prefix, output.valueSatoshis)
            writeVarInt(prefix, output.scriptPubKey.size)
            prefix.write(output.scriptPubKey)
        }

        return UnsignedTransaction(prefix.toByteArray(), sighash, scriptCode, lockTime)
    }

    /** Signs BIP-143 sighash and returns final raw transaction hex. */
    fun signP2wpkh(unsigned: UnsignedTransaction, privateKey: BigInteger): String {
        val signature = WalletCrypto.signHashLowS(unsigned.sighash, privateKey, SIGHASH_ALL)
        val witness = ByteArrayOutputStream()
        writeVarInt(witness, 1)
        writeVarInt(witness, signature.size)
        witness.write(signature)

        val out = ByteArrayOutputStream()
        out.write(unsigned.prefix)
        out.write(witness.toByteArray())
        writeLittleEndianInt(out, unsigned.lockTime.toInt())
        return out.toByteArray().joinToString("") { "%02x".format(it) }
    }

    data class UnsignedTransaction(
        val prefix: ByteArray,
        val sighash: ByteArray,
        val scriptCode: ByteArray,
        val lockTime: Long = 0
    )

    // ========== Primitives ==========

    internal fun doubleSha256(data: ByteArray): ByteArray {
        val sha = MessageDigest.getInstance("SHA-256")
        return sha.digest(sha.digest(data))
    }

    internal fun writeVarInt(out: ByteArrayOutputStream, value: Int) {
        when {
            value < 0xfd -> out.write(value)
            value <= 0xffff -> {
                out.write(0xfd)
                out.write(value and 0xff)
                out.write((value shr 8) and 0xff)
            }
            value <= 0xffffffff -> {
                out.write(0xfe)
                out.write(value and 0xff)
                out.write((value shr 8) and 0xff)
                out.write((value shr 16) and 0xff)
                out.write((value shr 24) and 0xff)
            }
            else -> throw IllegalArgumentException("varint out of range: $value")
        }
    }

    internal fun writeLittleEndianInt(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xff)
        out.write((value shr 8) and 0xff)
        out.write((value shr 16) and 0xff)
        out.write((value shr 24) and 0xff)
    }

    internal fun writeLittleEndianLong(out: ByteArrayOutputStream, value: Long) {
        var v = value
        for (i in 0 until 8) {
            out.write((v and 0xff).toInt())
            v = v shr 8
        }
    }

    internal fun leBytes(value: Long, size: Int): ByteArray {
        val out = ByteArray(size)
        var v = value
        for (i in 0 until size) {
            out[i] = (v and 0xff).toByte()
            v = v shr 8
        }
        return out
    }

    internal fun hexToBytes(hex: String): ByteArray {
        val clean = hex.removePrefix("0x").trim()
        require(clean.length % 2 == 0) { "Invalid hex length" }
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    internal fun reverseBytes(input: ByteArray): ByteArray {
        val out = ByteArray(input.size)
        for (i in input.indices) out[i] = input[input.size - 1 - i]
        return out
    }
}
