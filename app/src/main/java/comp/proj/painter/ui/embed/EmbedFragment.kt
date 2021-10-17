package comp.proj.painter.ui.embed

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.ContextWrapper
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import comp.proj.painter.R
import comp.proj.painter.databinding.FragmentEmbedBinding
import comp.proj.painter.ui.code.JpegEncoder
import comp.proj.painter.ui.data.Code
import kotlinx.android.synthetic.main.fragment_embed.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


class EmbedFragment : Fragment() {

    private lateinit var embedViewModel: EmbedViewModel

    lateinit var binding: FragmentEmbedBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEmbedBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        embedViewModel =
            ViewModelProvider(this).get(EmbedViewModel::class.java)
        binding.vm = embedViewModel

        binding.buttonSelect.setOnClickListener {
            val mediaIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(mediaIntent, Code.IMAGE_RESULT)
        }

        binding.buttonEmbed.setOnClickListener {
            val strings = arrayOf( "embed text")

            val builder: AlertDialog.Builder = AlertDialog.Builder(context)
            builder.setSingleChoiceItems(strings, 0,
                DialogInterface.OnClickListener { dialog, which ->
                    when (which) {
                        0 -> {
//                            text_embed.visibility = View.VISIBLE;
//                            button_encode.visibility = View.VISIBLE;
                            binding.imageView.drawable.let { image ->
                                findNavController().navigate(
                                    R.id.action_navigation_embed_to_navigation_embed_text,
                                    bundleOf(Pair("coverImage", image.toBitmap()))
                                )
                                Log.e("EmbedFrag", "${image.toBitmap()}")
                            }
                            dialog.dismiss() //結束對話框
                        }

                    }

                })
            builder.show()
        }

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Code.IMAGE_RESULT && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                binding.vm?.imageUrl?.value = "$it"
                Log.e("EmbedFrag2", "${it}")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        Log.e("DB", "${PackageManager.PERMISSION_GRANTED}")
        Log.e(
            "DB", "${
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }"
        )
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
                ), 10
            )
            Log.e("EmbedFrag", "PERMISSION REQUESTING")
        } else {
            Log.e("EmbedFrag", "PERMISSION GRANTED")
        }
    }



}