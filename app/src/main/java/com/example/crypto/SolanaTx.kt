package com.example.crypto

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.io.ByteArrayOutputStream

/**
 * Solana legacy transaction builder and ed25519 signer.
 *
 * Implements compact-u16 (shortvec) encoding and System Program Transfer instruction.
 */
object SolanaTx {

    /** System Program id: 11111111111111111111111111111111 (32 zero bytes). */
    val SYSTEM_PROGRAM_ID: ByteArray = ByteArray(32)

    private const val INSTRUCTION_TRANSFER = 2

    /** Legacy transfer transaction. */
    data class Transfer(
        val senderPubKey: ByteArray,
        val recipientPubKey: ByteArray,
        val lamports: Long,
        val recentBlockhash: ByteArray
    ) {
        init {
            require(senderPubKey.size == 32) { "sender public key must be 32 bytes" }
            require(recipientPubKey.size == 32) { "recipient public key must be 32 bytes" }
            require(recentBlockhash.size == 32) { "blockhash must be 32 bytes" }
            require(lamports > 0) { "lamports must be positive" }
        }
    }

    /** Serializes legacy transaction message (the bytes that are signed). */
    fun buildMessage(transfer: Transfer): ByteArray {
        val out = ByteArrayOutputStream()
        writeCompactU16(out, 1)
        writeCompactU16(out, 0)
        writeCompactU16(out, 0)
        writeCompactU16(out, 2)
        out.write(transfer.senderPubKey)
        out.write(transfer.recipientPubKey)
        out.write(transfer.recentBlockhash)
        writeCompactU16(out, 1)
        out.write(SYSTEM_PROGRAM_ID)
        writeCompactU16(out, 2)
        out.write(transfer.senderPubKey)
        out.write(transfer.recipientPubKey)
        val data = ByteArrayOutputStream()
        data.write(INSTRUCTION_TRANSFER)
        writeLittleEndianLong(data, transfer.lamports)
        writeCompactU16(out, data.size())
        out.write(data.toByteArray())
        return out.toByteArray()
    }

    /** Signs message with ed25519 private key. Returns 64-byte signature. */
    fun sign(message: ByteArray, privateKey: ByteArray): ByteArray {
        require(privateKey.size == 32) { "ed25519 private key must be 32 bytes" }
        val signer = Ed25519Signer()
        signer.init(true, Ed25519PrivateKeyParameters(privateKey, 0))
        signer.update(message, 0, message.size)
        return signer.generateSignature()
    }

    /** Builds, signs and base58-encodes full transaction for JSON-RPC submission. */
    fun buildSignedTransfer(transfer: Transfer, privateKey: ByteArray): String {
        val message = buildMessage(transfer)
        val signature = sign(message, privateKey)
        val out = ByteArrayOutputStream()
        writeCompactU16(out, 1)
        out.write(signature)
        out.write(message)
        return Base58.encode(out.toByteArray())
    }

    /** Decodes base58 blockhash to raw bytes. */
    fun decodeBlockhash(blockhash: String): ByteArray {
        val decoded = Base58.decode(blockhash)
        require(decoded.size == 32) { "Invalid blockhash length" }
        return decoded
    }

    /** Encodes 32-byte public key to base58 Solana address. */
    fun encodeAddress(publicKey: ByteArray): String {
        require(publicKey.size == 32) { "public key must be 32 bytes" }
        return Base58.encode(publicKey)
    }

    // ========== Primitives ==========

    internal fun writeCompactU16(out: ByteArrayOutputStream, value: Int) {
        require(value in 0..0xffff) { "compact-u16 out of range: $value" }
        var v = value
        while (true) {
            val chunk = v and 0x7f
            v = v shr 7
            if (v == 0) {
                out.write(chunk)
                break
            }
            out.write(chunk or 0x80)
        }
    }

    internal fun writeLittleEndianLong(out: ByteArrayOutputStream, value: Long) {
        var v = value
        for (i in 0 until 8) {
            out.write((v and 0xff).toInt())
            v = v shr 8
        }
    }
}
