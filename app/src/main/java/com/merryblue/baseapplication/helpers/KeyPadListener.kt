package com.merryblue.baseapplication.helpers

interface KeyPadListener {
    fun onKeyClicked(code: Int)

    fun onKeyDelete()
}