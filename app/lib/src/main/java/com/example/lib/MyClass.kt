package com.example.lib

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun main() {
    CoroutineScope(Dispatchers.IO).launch{
        copyFile()
    }
}

suspend fun copyFile(){

}