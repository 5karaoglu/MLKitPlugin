package com.example.mlkitlib.ocr.ui

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mlkitlib.ocr.TextItem
import com.google.mlkit.vision.text.Text

class CameraViewModel: ViewModel() {

    private val _selectedTextList = MutableLiveData(mutableListOf<TextItem>())
    val selectedTextList get() = _selectedTextList

    fun addToSelectedTextList(textItem: TextItem){
        val list = selectedTextList.value ?: mutableListOf<TextItem>()
        list.add(textItem)
        _selectedTextList.value = list
    }

    fun setList(list: MutableList<TextItem>){
        _selectedTextList.value = list
    }

    fun removeFromSelectedTextList(textItem: TextItem){
        _selectedTextList.value?.remove(textItem)
    }

    fun clearList(){
        _selectedTextList.value = mutableListOf<TextItem>()
    }

    fun handleClickedText(item: TextItem) {
        val list = selectedTextList.value
        if (!list.isNullOrEmpty()){
            val text = list.find {
                Log.d("TESTING", "handleClickedText: ${it.text} while ${item.text}")
                it.text== item.text
            }
            val index = list.indexOf(text)
            text!!.isSelected = !text.isSelected
            list[index] = text
            _selectedTextList.value = list
        }


        /*selectedTextList.value?.forEachIndexed { index, textItem ->

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
        }*/
    }
}