package comp.proj.painter.ui.embed

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EmbedViewModel : ViewModel() {

    val imageUrl = MutableLiveData<String>().apply {
        value = ""
    }

    val secretMsg = MutableLiveData<String>().apply {
        value = ""
    }

    val password = MutableLiveData<String>().apply {
        value = ""
    }
}