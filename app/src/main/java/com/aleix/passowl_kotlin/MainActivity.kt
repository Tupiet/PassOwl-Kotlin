package com.aleix.passowl_kotlin

import android.content.Context
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.aleix.tupi_library.TupiLibrary
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class MainActivity : AppCompatActivity() {

    val tupiLibrary = TupiLibrary()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        generateKey() // Here we generate the key

        //val encrypted = encryptData("Hello world!")
        //val decrypted = decryptData(encrypted.first, encrypted.second)

        //tupiLibrary.alert(this, encrypted.toString(), decrypted)
    }

    // Here, we're gonna generate a key and we're gonna save it in the KeyStore
    fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder("myKeyAlias",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)                  // The block mode is CBC

            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE) // Non padding since we don't want
                                                                          // to hide the amount of information
            .build()

        keyGenerator.init(keyGenParameterSpec) // Here we init the generation of a key
        keyGenerator.generateKey() // Here we generate the key
    }

    // Here we're gonna get the secret key saved in the KeyStore
    fun getKey():SecretKey {
        val keyStore:KeyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val secretKeyEntry = keyStore.getEntry("myKeyAlias", null) as KeyStore.SecretKeyEntry
        return secretKeyEntry.secretKey
    }

    fun encryptData(data: String):Pair<ByteArray, ByteArray> {

        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        var temp = data
        while (temp.toByteArray().size % 16 != 0) // We need temp to be 16 bytes, and now er're making sure it's correct
            temp += "\u0020" // Now we're gonna pad the unused things with unnecessary spaces

        cipher.init(Cipher.ENCRYPT_MODE, getKey())

        val ivBytes = cipher.iv // This is the initialization vector. Search Wikipedia for more info

        val encryptedBytes = cipher.doFinal(temp.toByteArray(Charsets.UTF_8))

        // Now I'm saving this into a file
        val context = this.getApplicationContext()

        context.openFileOutput("savedIV.dat", Context.MODE_PRIVATE).use {
            it.write(ivBytes)
        }

        context.openFileOutput("savedData.dat", Context.MODE_PRIVATE).use {
            it.write(encryptedBytes)
        }


        return Pair(ivBytes, encryptedBytes) // Here we return the IV and the encrypted bytes, making it different to every user
    }

    fun decryptData(ivBytes: ByteArray, data:ByteArray):String {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")

        val spec = IvParameterSpec(ivBytes)

        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)
        return cipher.doFinal(data).toString(Charsets.UTF_8).trim() // The trim just skips the unnecessary spaces we have inserted
    }

    fun buttonEncrypt(view: View) {

        var input = passwordInput.text.toString()
        encryptData(input)
    }


    fun loadData(view: View) {
        //Here I'm getting the values saved
        val context = this.getApplicationContext()

        val fileIV = File(context.filesDir, "savedIV.dat")
        val contentIV = fileIV.readBytes()

        val fileData = File(context.filesDir, "savedData.dat")
        val contentData = fileData.readBytes()

        var decrypted = decryptData(contentIV, contentData)
        passTextDecrypted.setText(decrypted)
    }


}
