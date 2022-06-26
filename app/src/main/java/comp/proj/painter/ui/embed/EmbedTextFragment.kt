package comp.proj.painter.ui.embed

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import comp.proj.painter.databinding.FragmentEmbedTextBinding
import comp.proj.painter.ui.code.JpegEncoder
import kotlinx.android.synthetic.main.fragment_embed.*
import kotlinx.android.synthetic.main.fragment_embed_text.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.security.spec.InvalidKeySpecException
import java.time.Duration
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private var OK = false

/**
 * A simple [Fragment] subclass.
 * Use the [EmbedTextFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EmbedTextFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: ByteArray? = null
    private var param2: String? = null

    private lateinit var embedViewModel: EmbedViewModel
    lateinit var binding: FragmentEmbedTextBinding
    lateinit var shareUri: Uri

    val waitChannel = Channel<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // param1 = it.getByteArray("coverImage")
            Log.e("EmbedTextFrag", "onCreate: ${param1}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentEmbedTextBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        embedViewModel =
            ViewModelProvider(this).get(EmbedViewModel::class.java)
        binding.vm = embedViewModel

        binding.buttonSubmit.setOnClickListener {
//            CoroutineScope(Dispatchers.Main).launch { encode() }
            val inputManager: InputMethodManager =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)  //auto hide the keyboard
                        as InputMethodManager

            inputManager.hideSoftInputFromWindow(
                requireActivity().currentFocus?.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )//auto hide the keyboard

            if (binding.vm?.password?.value == null) {
                Log.e("EmbedTextFrag","${binding.vm?.password?.value}")
                Snackbar.make(button_encode,"not ok", Snackbar.LENGTH_LONG).show()
            } else {
                encode()
            }

            llProgressBar.visibility = View.VISIBLE
//            button_share.visibility = View.VISIBLE;

        }

        binding.buttonShare.visibility = View.GONE
        binding.buttonShare.setOnClickListener {
            share()
        }

        MainScope().launch {
            waitChannel.receive()
            binding.buttonShare.visibility = View.VISIBLE
            llProgressBar.visibility = View.GONE
            //Log.d("EmbedTextFrag", "toast")
            Snackbar.make(binding.buttonSubmit, "DONE", Snackbar.LENGTH_LONG).show()
            //Toast.makeText(requireContext(), "DONE", Toast.LENGTH_LONG).show()
        }

        return binding.root
        //return inflater.inflate(R.layout.fragment_embed_text, container, false)
    }

    private fun encode() {
        val uri = uri()

        binding.vm?.secretMsg?.value.let { msg ->

            //val fos = FileOutputStream(storageDir);
            Log.e("EmbedTextFrag", "fos ${msg}")

            //val out = BufferedOutputStream(fos);
            val out = requireContext().contentResolver.openOutputStream(uri)
            Log.e("EmbedTextFrag", "out")

            arguments?.let { value ->
                val bitmap = value.get("coverImage") as Bitmap
                Log.e("EmbedTextFrag", "imgURL = ${bitmap}")
                Log.e("EmbedTextFrag", "${binding.coverImage.drawable}")

                CoroutineScope(Dispatchers.IO).launch {
                    val jpegEncoder = JpegEncoder(
                        bitmap, 90, out, msg?.let { encryptedString(it) }
                    )

                    Log.e("EmbedTextFrag", "call encoder")
                    jpegEncoder.Compress();

                    Log.d("EmbedTextFrag", "encoded")

                    out?.close()

                    waitChannel.send(true)

                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "image/jpg"
                    }

//                        val shareIntent = Intent.createChooser(sendIntent, null)
//                        startActivity(Intent.createChooser(shareIntent, "Share Image"))

                }

            }


        }

        shareUri = uri
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment EmbedTextFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            EmbedTextFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 0
            )
        } else {
            Log.e("DB", "PERMISSION GRANTED")
        }
    }

    fun checkPermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                )
    }

    private fun encryptedString(msgToEncrypt: String): String {
        binding.vm?.password?.value.let { pw ->
            try {
                val salt = ByteArray(256)
                salt.fill(0);

                val password = pw?.toCharArray()

                val pbKeySpec = PBEKeySpec(password, salt, 1324, 256) // 1
                val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1") // 2
                val keyBytes = secretKeyFactory.generateSecret(pbKeySpec).encoded // 3
                val keySpec = SecretKeySpec(keyBytes, "AES") // 4

                val iv = ByteArray(16)
                iv.fill(1)
                val ivSpec = IvParameterSpec(iv) // 2

                val data: ByteArray = msgToEncrypt.toByteArray(StandardCharsets.UTF_8)

                val cipher = Cipher.getInstance("AES/CTR/NoPadding") // 1
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
                val encrypted = cipher.doFinal(data) // 2

                val base64: String = Base64.encodeToString(encrypted, Base64.DEFAULT)

                Log.e("Encrypt", "${base64}")
                return base64
            } catch (e: InvalidKeySpecException) {

                requireActivity().runOnUiThread {
                    Toast.makeText(
                        activity,
                        "Please select cover image first",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                val builder = AlertDialog.Builder(context)

                val alertDialog = builder.create()
                alertDialog.show()
            }
            return "";
        }
    }


    private fun uri(): Uri {
        val contentValue = ContentValues()

        contentValue.put(
            MediaStore.MediaColumns.DISPLAY_NAME,
            "${Date().time}.jpg"
        );       //file name
        contentValue.put(
            MediaStore.MediaColumns.MIME_TYPE,
            "image/jpeg"
        );        //file extension, will automatically add to file
        contentValue.put(
            MediaStore.MediaColumns.RELATIVE_PATH,
            Environment.DIRECTORY_DOWNLOADS
        );     //end "/" is not mandatory

        val uri = requireContext().contentResolver.insert(
            MediaStore.Files.getContentUri("external"),
            contentValue
        ) //important!

        return uri!!
    }

    private fun share() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, shareUri)
            type = "image/jpg"
        }
        Log.e("Share", "${shareUri}")
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(Intent.createChooser(shareIntent, "Share Image"));
    }

}