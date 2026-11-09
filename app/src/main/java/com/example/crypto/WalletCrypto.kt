package com.example.crypto

import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.x9.ECNamedCurveTable
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.digests.SHA512Digest
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.crypto.MnemonicUtils
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Production wallet cryptography primitives.
 *
 * Implements:
 * - BIP-39 mnemonic generation, validation, and seed derivation
 * - BIP-32 hierarchical deterministic key derivation (secp256k1)
 * - SLIP-0010 ed25519 derivation for Solana
 * - Address derivation: EVM (EIP-55), Bitcoin P2WPKH bech32, Solana base58
 * - Signing: EIP-191 personal_sign, BIP-143 sighash with low-S DER, ed25519
 */
object WalletCrypto {
    private val secureRandom = SecureRandom()
    private val secpParams = ECNamedCurveTable.getByName("secp256k1")
    private val secpDomain = ECDomainParameters(secpParams)

    const val BTC_DERIVATION_PATH = "m/84'/0'/0'/0/0"
    const val EVM_DERIVATION_PATH = "m/44'/60'/0'/0/0"
    const val SOL_DERIVATION_PATH = "m/44'/501'/0'/0'"

    private const val HARDENED = 0x80000000.toInt()

    // ========== BIP-39 ==========

    /** Generates a BIP-39 mnemonic with valid checksum. */
    fun generateMnemonic(strengthBits: Int = 128): String {
        require(strengthBits in setOf(128, 160, 192, 224, 256)) { "Unsupported entropy size" }
        val entropy = ByteArray(strengthBits / 8).also { secureRandom.nextBytes(it) }
        return MnemonicUtils.generateMnemonic(entropy)
    }

    /** Validates mnemonic checksum and word list membership. */
    fun isValidMnemonic(mnemonic: String): Boolean {
        val words = mnemonic.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty() || words.size % 3 != 0) return false
        return MnemonicUtils.validateMnemonic(words.toTypedArray().joinToString(" "))
    }

    /** BIP-39 seed derivation: PBKDF2-HMAC-SHA512, 2048 iterations. */
    fun mnemonicToSeed(mnemonic: String, passphrase: String = ""): ByteArray {
        require(isValidMnemonic(mnemonic)) { "Invalid BIP-39 mnemonic" }
        return MnemonicUtils.generateSeed(mnemonic.trim(), passphrase)
    }

    // ========== BIP-32 / 44 ==========

    /** Parses derivation path string to IntArray for web3j. */
    fun parseDerivationPath(path: String): IntArray {
        val parts = path.trim().split('/')
        require(parts.isNotEmpty() && (parts[0] == "m" || parts[0] == "M")) { "Path must start with m" }
        val out = ArrayList<Int>(parts.size - 1)
        for (part in parts.drop(1)) {
            require(part.isNotEmpty()) { "Empty path element" }
            val hardened = part.endsWith("'") || part.endsWith("h") || part.endsWith("H")
            val number = if (hardened) part.dropLast(1) else part
            val index = number.toIntOrNull() ?: throw IllegalArgumentException("Invalid path index: $part")
            require(index in 0..0x7fffffff) { "Path index out of range: $part" }
            out.add(if (hardened) index or HARDENED else index)
        }
        return out.toIntArray()
    }

    /** Derives secp256k1 EC key pair from BIP-39 seed. */
    fun deriveSecp256k1(seed: ByteArray, path: String): ECKeyPair {
        val master = Bip32ECKeyPair.generateKeyPair(seed)
        return Bip32ECKeyPair.deriveKeyPair(master, parseDerivationPath(path))
    }

    /** Derives ed25519 key pair via SLIP-0010 (hardened-only). */
    fun deriveEd25519(seed: ByteArray, path: String): Ed25519KeyPair {
        val indices = parseDerivationPath(path)
        require(indices.all { (it and HARDENED) != 0 }) { "ed25519 requires hardened indices" }
        var il = hmacSha512("ed25519 seed".toByteArray(Charsets.UTF_8), seed)
        var key = il.copyOfRange(0, 32)
        var chainCode = il.copyOfRange(32, 64)
        for (index in indices) {
            val indexBytes = ByteArray(4)
            indexBytes[0] = (index ushr 24).toByte()
            indexBytes[1] = (index ushr 16).toByte()
            indexBytes[2] = (index ushr 8).toByte()
            indexBytes[3] = index.toByte()
            val data = ByteArray(1 + 32 + 4)
            data[0] = 0
            System.arraycopy(key, 0, data, 1, 32)
            System.arraycopy(indexBytes, 0, data, 33, 4)
            il = hmacSha512(chainCode, data)
            key = il.copyOfRange(0, 32)
            chainCode = il.copyOfRange(32, 64)
        }
        val priv = Ed25519PrivateKeyParameters(key, 0)
        val pub = priv.generatePublicKey()
        return Ed25519KeyPair(key, pub.encoded)
    }

    private fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
        val mac = HMac(SHA512Digest())
        mac.init(KeyParameter(key))
        mac.update(data, 0, data.size)
        val out = ByteArray(mac.macSize)
        mac.doFinal(out, 0)
        return out
    }

    // ========== Addresses ==========

    /** EVM address (EIP-55 checksummed). */
    fun evmAddress(keyPair: ECKeyPair): String {
        val raw = Keys.getAddress(keyPair)
        val checksummed = Keys.toChecksumAddress(raw).removePrefix("0x")
        return "0x$checksummed"
    }

    /** Bitcoin native SegWit P2WPKH bech32 address from compressed public key. */
    fun bitcoinAddress(compressedPublicKey: ByteArray, hrp: String = "bc"): String {
        require(compressedPublicKey.size == 33) { "Expected compressed public key" }
        val sha = MessageDigest.getInstance("SHA-256").digest(compressedPublicKey)
        val ripemd = RIPEMD160Digest()
        ripemd.update(sha, 0, sha.size)
        val hash = ByteArray(ripemd.digestSize)
        ripemd.doFinal(hash, 0)
        return Bech32.encodeSegwitV0(hrp, hash)
    }

    /** Solana address: base58 of 32-byte ed25519 public key. */
    fun solanaAddress(publicKey: ByteArray): String {
        require(publicKey.size == 32) { "Expected 32-byte ed25519 public key" }
        return Base58.encode(publicKey)
    }

    /** Compressed secp256k1 public key (33 bytes). */
    fun compressedPublicKey(privateKey: BigInteger): ByteArray {
        return secpDomain.g.multiply(privateKey).normalize().getEncoded(true)
    }

    // ========== Signing ==========

    /** EIP-191 personal_sign over bytes. Returns 65-byte signature (r||s||v). */
    fun signPersonalMessage(message: ByteArray, keyPair: ECKeyPair): ByteArray {
        val sig = org.web3j.crypto.Sign.signPrefixedMessage(message, keyPair)
        val r = sig.r
        val s = sig.s
        val v = sig.v
        val out = ByteArray(65)
        System.arraycopy(r, 0, out, 0, 32)
        System.arraycopy(s, 0, out, 32, 32)
        out[64] = v[0]
        return out
    }

    /** BIP-143 low-S DER signature with sighash type appended. */
    fun signHashLowS(hash: ByteArray, privateKey: BigInteger, sighashType: Int = 0x01): ByteArray {
        val signer = org.bouncycastle.crypto.signers.ECDSASigner()
        signer.init(true, org.bouncycastle.crypto.params.ECPrivateKeyParameters(privateKey, secpDomain))
        val components = signer.generateSignature(hash)
        var r = components[0]
        var s = components[1]
        val halfOrder = secpDomain.n.shiftRight(1)
        if (s > halfOrder) s = secpDomain.n.subtract(s)
        if (r < BigInteger.ZERO) r = r.add(secpDomain.n)
        val der = org.bouncycastle.asn1.DERSequence(
            arrayOf(org.bouncycastle.asn1.ASN1Integer(r), org.bouncycastle.asn1.ASN1Integer(s))
        ).encoded
        val result = ByteArray(der.size + 1)
        System.arraycopy(der, 0, result, 0, der.size)
        result[der.size] = sighashType.toByte()
        return result
    }

    /** keccak256 hash. */
    fun keccak256(data: ByteArray): ByteArray = Hash.sha3(data)

    /** Hex encode with 0x prefix. */
    fun hex(data: ByteArray): String = "0x" + Numeric.toHexString(data)

    /** Decode hex string to bytes. */
    fun unhex(value: String): ByteArray = Numeric.hexStringToByteArray(value.removePrefix("0x"))

    /** 32-byte big-endian private key scalar. */
    fun privateKeyBytes(keyPair: ECKeyPair): ByteArray {
        val pk = keyPair.privateKey
        val out = ByteArray(32)
        val raw = pk.toByteArray()
        val src = if (raw.size > 32) raw.copyOfRange(raw.size - 32, raw.size) else raw
        System.arraycopy(src, 0, out, 32 - src.size, src.size)
        return out
    }

    data class Ed25519KeyPair(val privateKey: ByteArray, val publicKey: ByteArray)
}
