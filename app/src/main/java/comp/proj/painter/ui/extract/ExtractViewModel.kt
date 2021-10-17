package comp.proj.painter.ui.extract

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ExtractViewModel : ViewModel() {

    val secret_text = MutableLiveData<String>().apply {
        value = ""
    }

    val secret_imageUrl = MutableLiveData<String>().apply {
        value = ""
    }

    val imageUrl = MutableLiveData<String>().apply {
        value = ""
    }

    val password = MutableLiveData<String>().apply {
        value = ""
    }


}