package io.keen.client.scala.util

import javax.crypto.Cipher
import javax.crypto.spec._
import javax.xml.bind.DatatypeConverter.{parseHexBinary,printHexBinary}

object ScopedKeys {

  val BLOCK_SIZE = 32

  def decrypt(apiKey: String, scopedKey: String) = {

    val hexedIv = scopedKey.substring(0, 32)
    val hexedCipherText = scopedKey.substring(32)

    val iv = parseHexBinary(hexedIv)
    val cipherText = parseHexBinary(hexedCipherText)

    val secret = new SecretKeySpec(padApiKey(apiKey), "AES")

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

    val ivParameterSpec = new IvParameterSpec(iv);
    cipher.init(Cipher.DECRYPT_MODE, secret, ivParameterSpec);

    // do the decryption
    new String(cipher.doFinal(cipherText), "UTF-8");
  }

  def encrypt(apiKey: String, options: String) = {
    val secret = new SecretKeySpec(padApiKey(apiKey), "AES")

    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, secret)

    val iv = cipher.getParameters.getParameterSpec(classOf[IvParameterSpec]).getIV
    val cipherText = cipher.doFinal(options.getBytes("UTF-8"))

    printHexBinary(iv) + printHexBinary(cipherText)
  }

  private def padApiKey(key: String) = key.padTo(BLOCK_SIZE, " ").mkString.getBytes("UTF-8")
}
