package comp.proj.painter.ui.extract

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import comp.proj.painter.R
import comp.proj.painter.databinding.FragmentExtractBinding
import comp.proj.painter.ui.code.ByteSource
import comp.proj.painter.ui.code.JpegDecoder
import comp.proj.painter.ui.data.Code
import kotlinx.android.synthetic.main.fragment_extract.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File


class ExtractFragment : Fragment() {

    private lateinit var extractViewModel: ExtractViewModel

    lateinit var binding: FragmentExtractBinding

    //lateinit var password: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        extractViewModel =
            ViewModelProvider(this).get(ExtractViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_extract, container, false)
        binding = FragmentExtractBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.vm = extractViewModel

        binding.buttonSelect.setOnClickListener {
            val mediaIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(mediaIntent, Code.IMAGE_RESULT)
        }

        binding.buttonExtract.setOnClickListener {
//            findNavController().navigate(
//                R.id.action_navigation_extract_to_navigation_show_message
//            )

//            inputPassword()
//            decode()

            CoroutineScope(Dispatchers.IO).launch {
                val text = inputPassword()
                decode()
            }
//            CoroutineScope(Dispatchers.IO).launch{
//            }
            //}

        }
        return binding.root
    }

    private fun decode() {

        Log.e("Extract", "${binding.vm?.imageUrl?.value}")

        val textChannel = Channel<String>()
        val pw = binding.vm?.password?.value
        binding.vm?.imageUrl?.value?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val jpegDecode2 = JpegDecoder()
                jpegDecode2.decode(ByteSource(requireContext(), Uri.parse(it)))
                textChannel.send(jpegDecode2.getSecretText(pw))
            }


            //Log.d("Extract", "${jpegDecode2.decryptedText}")
        }

        MainScope().launch { text_extract.text = "The message is: \n ${textChannel.receive()}" }
        //}
    }

    private suspend fun inputPassword() = withContext(Dispatchers.Main) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle("Please Enter Password: ");

        val textChannel = Channel<String>()

        val input: EditText = EditText(context);
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton(
            "OK"
        ) { dialog, which ->
            MainScope().launch {
                binding.vm?.password?.value = input.text.toString()
                textChannel.send(input.text.toString())
            }
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, which -> dialog.cancel() }

        builder.show()

        textChannel.receive()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Code.IMAGE_RESULT && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                binding.vm?.imageUrl?.value = "$it"
            }
        }
    }

//    private fun decrypt(encrypted: ByteArray) {// 1
////        val random = SecureRandom()
//        val salt = ByteArray(256)
//        salt.fill(0);
//        val password = charArrayOf('0')
//        val pbKeySpec = PBEKeySpec(password, salt, 1324, 256) // 1
//        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1") // 2
//        val keyBytes = secretKeyFactory.generateSecret(pbKeySpec).encoded // 3
//        val keySpec = SecretKeySpec(keyBytes, "AES") // 4
////        val ivRandom = SecureRandom() //not caching previous seeded instance of SecureRandom
//// 1
//        val iv = ByteArray(16)
////        ivRandom.nextBytes(iv)
//        iv.fill(1)
//        val ivSpec = IvParameterSpec(iv) // 2
//
//        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding") // 1
//
//// 2
////regenerate key from password
//        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
//        //val encrypted = ByteArray(12)
//        val decrypted = cipher.doFinal(encrypted)
//        Log.e("Decrypt", "${String(decrypted)}")
//    }
}