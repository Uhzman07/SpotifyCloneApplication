package com.example.spotifyclone.other

import android.os.Message

/**
 * Note that the event class in Android is used to keep track of errors mostly
 * It is mostly utilised just in case we want to display errors using Snackbars and other utilities like that
 * When the device is rotated, the snackbar that has been assumed gone could show up again if we do not make use of the event class
 */

/**
 * the "out" modifier in a generic type parameter allows the generic type to be covariant,
 * enabling more flexibility in using subtypes. It is commonly used in scenarios where the generic
 * type is used as a return type.
 * Using this, even if the parameter data type is not so equal to that of the generic given that they are equal,
 * we will not get an error because of the out keyword that had been used to justify covariance
 */
data class Resource<out T>(val status: Status, val data: T?, val message: String?) {

    companion object{
        fun <T> success(data:T?) = Resource(Status.SUCCESS,data,null)

        // Case of error but with message to send
        fun <T> error(message: String, data:T?) = Resource(Status.ERROR,data,message)

        // This is when we are loading; that is when we are making use of the loading mechanism
        fun <T> loading(data:T?) = Resource(Status.LOADING,data,null)

    }

}

enum class Status{
    SUCCESS,
    ERROR,
    LOADING
}
