package com.app.messageapp.chat.dataModel

import android.os.Parcel
import android.os.Parcelable

data class SmsConversation(
    val threadId: String,
    val address: String,
    val snippet: String,
    val time: String,
    var isRead: Boolean,
    var isArchived: Boolean = false,
    var isPinned: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        threadId = parcel.readString() ?: "",
        address = parcel.readString() ?: "",
        snippet = parcel.readString() ?: "",
        time = parcel.readString() ?: "",
        isRead = parcel.readByte() != 0.toByte(),
        isArchived = parcel.readByte() != 0.toByte(),
        isPinned = parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(threadId)
        parcel.writeString(address)
        parcel.writeString(snippet)
        parcel.writeString(time)
        parcel.writeByte(if (isRead) 1 else 0)
        parcel.writeByte(if (isArchived) 1 else 0)
        parcel.writeByte(if (isPinned) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SmsConversation> {
        override fun createFromParcel(parcel: Parcel): SmsConversation {
            return SmsConversation(parcel)
        }

        override fun newArray(size: Int): Array<SmsConversation?> {
            return arrayOfNulls(size)
        }
    }
}