package com.app.messageapp.chat.dataModel

import android.os.Parcel
import android.os.Parcelable

data class SmsConversation(
    val threadId: String,
    val address: String,
    val snippet: String,
    val time: String,
    var isRead: Boolean,
    var isPinned: Boolean = false
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "", // threadId
        parcel.readString() ?: "", // address
        parcel.readString() ?: "", // snippet
        parcel.readString() ?: "", // time
        parcel.readByte() != 0.toByte() // isRead
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(threadId)
        parcel.writeString(address)
        parcel.writeString(snippet)
        parcel.writeString(time)
        parcel.writeByte(if (isRead) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<SmsConversation> {
        override fun createFromParcel(parcel: Parcel): SmsConversation {
            return SmsConversation(parcel)
        }

        override fun newArray(size: Int): Array<SmsConversation?> {
            return arrayOfNulls(size)
        }
    }

    override fun describeContents(): Int {
        return 0
    }
}
