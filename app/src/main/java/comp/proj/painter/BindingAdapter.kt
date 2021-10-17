package comp.proj.painter

import android.util.Log
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.squareup.picasso.Picasso

@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, url: String) {
    Log.d("loadImage","${url}")
    if (!url.isBlank()) {
        Log.d("loadImage","url.isNotBlank")
        Picasso.get().load(url).into(view)
    }
}