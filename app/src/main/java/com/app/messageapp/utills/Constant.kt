package com.app.myapplication.utils

class Constant {
    companion object {
      val userLanguage : String = "userLanguage"
    }

    enum class MessageType {
        Archive,
        Delete,
        Pin,
        Block,
        UnArchive
    }
}
