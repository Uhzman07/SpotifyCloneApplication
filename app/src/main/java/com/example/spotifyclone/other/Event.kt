package com.example.spotifyclone.other

/*
This class is made an open class because we want it to be inherited from
 */
open class Event<out T>(private val data:T) {
    var hasBeenHandled = false
        private set // Remember the function

    fun getContentIfNotHandled(): T?{
        return if(hasBeenHandled){
            null
        } else{
            hasBeenHandled = true
            data // what is to be returned
        }
    }

    fun peekContent() = data // just in case we want to check our data

}