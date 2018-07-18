package me.jakjak.telegramimagereceiver

import android.support.v4.content.ContextCompat
import android.util.Log
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi

class TelegramClient {

    companion object {
        val TAG = "TelegramClient"

        val eventHandlers = ArrayList<EventHandler>()

        fun bindHandler(e: EventHandler){
            eventHandlers.add(e)
        }

        fun unbindHandler(e: EventHandler){
            eventHandlers.remove(e)
        }

        val pendingImages = ArrayList<String>()

        val client = Client.create({
            Log.d(TAG, "Got update! " + it.javaClass.simpleName)
            if (it is TdApi.UpdateNewMessage) {
                if (it.message.senderUserId == BuildConfig.botUserId) {
                    Log.d(TAG, "got bot message!")
                    handleMessageFromBot(it)
                }
            } else if (it is TdApi.UpdateFile) {
                if (!it.file.local.isDownloadingActive && it.file.local.isDownloadingCompleted) {
                    onFileDownloadComplete(it)
                }
            } else if (it is TdApi.UpdateAuthorizationState) {
                if (it.authorizationState is TdApi.AuthorizationStateWaitPhoneNumber) {
                    notifyListeners(Event.NeedPhone, null)
                }
                else if (it.authorizationState is TdApi.AuthorizationStateWaitCode) {
                    notifyListeners(Event.NeedAuth, null)
                }
                else if (it.authorizationState is TdApi.AuthorizationStateReady) {
                    notifyListeners(Event.LoggedIn, null)
                }
            }
        }, {
            Log.e(TAG, "Update exception!")
        }, {
            Log.e(TAG, "Default exception!")
        })

        private fun notifyListeners(event: Event, s: String?) {
            for (handler in eventHandlers) {
                handler.handleEvent(event, s)
            }
        }

        private fun onFileDownloadComplete(it: TdApi.UpdateFile) {
            val remoteId = it.file.remote.id
            if (pendingImages.contains(remoteId)) {
                pendingImages.remove(remoteId)

                val path = it.file.local.path
                notifyListeners(Event.ImageReady, path)
            }
        }

        fun setToken(token: String) {
            client.send(TdApi.RegisterDevice(TdApi.DeviceTokenGoogleCloudMessaging(token), IntArray(0)), {
                Log.d(TAG, "Register device!")
            }, {
                Log.e(TAG, "Register device failed!")
            })
        }

        private fun handleMessageFromBot(update: TdApi.UpdateNewMessage) {
            if (update.message.content is TdApi.MessagePhoto) {
                val photo = (update.message.content as TdApi.MessagePhoto).photo as TdApi.Photo
                for (ps in photo.sizes) {
                    if (ps.type.equals("x")) {
                        // full size image!
                        if (ps.photo.local.isDownloadingCompleted) {
                            val path = ps.photo.local.path
                            notifyListeners(Event.ImageReady, path)
                        }
                        else if (ps.photo.local.isDownloadingActive) {
                            // do nothing
                        }
                        else {
                            val fileId: Int = ps.photo.id
                            pendingImages.add(ps.photo.remote.id)
                            client.send(TdApi.DownloadFile(fileId, 32), {
                                Log.d(TAG, "Sent download file!")
                            })
                        }
                        break
                    }
                }
            } else if (update.message.content is TdApi.MessageSticker) {
                // ??
            }
        }

        interface EventHandler {
            fun handleEvent(e: Event, s: String?)
        }

        enum class Event {
            NeedPhone,
            NeedAuth,
            LoggedIn,
            ImageReady
        }
    }
}