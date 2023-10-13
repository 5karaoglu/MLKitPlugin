package com.example.mlkitlib.ocr.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mlkitlib.ocr.TextItem
import com.google.mlkit.vision.text.Text

class CameraViewModel: ViewModel() {

    private val _selectedTextList = MutableLiveData(mutableListOf<TextItem>())
    val selectedTextList get() = _selectedTextList

    fun addToSelectedTextList(textItem: TextItem){
        _selectedTextList.value?.add(textItem)
    }

    fun removeFromSelectedTextList(textItem: TextItem){
        _selectedTextList.value?.remove(textItem)
    }

    fun clearList(){
        _selectedTextList.value?.clear()
    }

    fun handleClickedText(item: TextItem) {
        selectedTextList.value?.forEachIndexed { index, textItem ->
            textItem.takeIf {
                it.rect == item.rect
            }?.let {
                val list = _selectedTextList.value
                val text = list?.get(index)?.copy(isSelected = !it.isSelected)
                if (text != null) {
                    list[index] = text
                }
                _selectedTextList.value = list
            }
        }
    }
}