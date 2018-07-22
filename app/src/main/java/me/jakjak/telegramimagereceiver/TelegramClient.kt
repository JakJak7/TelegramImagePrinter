package me.jakjak.telegramimagereceiver

import android.util.Log
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import me.jakjak.telegramimagereceiver.models.Job
import me.jakjak.telegramimagereceiver.models.User
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi
import java.util.*

class TelegramClient {

    companion object {
        const val TAG = "TelegramClient"
        val MAX_JOBS_PER_HOUR = 5

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
                handleMessage(it)
            } else if (it is TdApi.UpdateFile) {
                if (!it.file.local.isDownloadingActive && it.file.local.isDownloadingCompleted) {
                    onFileDownloadComplete(it)
                }
            } else if (it is TdApi.UpdateAuthorizationState) {
                if (it.authorizationState is TdApi.AuthorizationStateWaitPhoneNumber) {
                    //notifyListeners(Event.NeedPhone, null)
                    authorizeBot()
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

        private fun authorizeBot() {
            client.send(TdApi.CheckAuthenticationBotToken(BuildConfig.botToken), {
                Log.d(TAG, "Set bot token!")
            }, {
                Log.e(TAG, "Set bot token failed!")
            })
        }

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

        private fun handleMessage(update: TdApi.UpdateNewMessage) {
            if (isUserAllowed(update.message.senderUserId)) {
                sendResponse()
                return
            }
            // this else clause should be moved to after job successfully printed
            else {
                val realm = Realm.getDefaultInstance()
                val user = realm.where<User>().equalTo("userId", update.message.senderUserId).findFirst()!!
                realm.executeTransaction{
                    user.jobs.add(Job(user, Date(), ""))
                }
                Log.d(TAG, user.jobs.size.toString())
            }

            if (update.message.content is TdApi.MessageSticker) {
                val sticker = (update.message.content as TdApi.MessageSticker).sticker as TdApi.Sticker
                val file = sticker.sticker
                handleFile(file)
            }
            else if (update.message.content is TdApi.MessagePhoto) {
                val photo = (update.message.content as TdApi.MessagePhoto).photo as TdApi.Photo
                for (ps in photo.sizes) {
                    if (ps.type.equals("x")) {
                        // full size image!
                        val file = ps.photo
                        handleFile(file)
                        break
                    }
                }
            }
        }

        private fun sendResponse() {

        }

        private fun isUserAllowed(senderUserId: Int): Boolean {
            val realm = Realm.getDefaultInstance()
            try {
                val user = realm.where<User>().equalTo("userId", senderUserId).findFirst()
                if (user == null) {
                    realm.executeTransaction {
                        val newUser = realm.createObject<User>(senderUserId)
                        newUser.firstName = "john"
                        newUser.lastName = "doe"
                    }
                    return true
                }
                if (!user.isLimited) {
                    return true
                }
                val calendar = Calendar.getInstance()
                val now = calendar.time
                calendar.add(Calendar.HOUR, -1)
                val oneHourAgo = calendar.time
                val results = user.jobs.where().between("timestamp", oneHourAgo, now).findAll()
                //val results = realm.where<Job>().equalTo("jobs.user", user).between("jobs.timestamp", oneHourAgo, now).findAll()
                return results.size >= MAX_JOBS_PER_HOUR
            }
            catch (e: Exception) {
                Log.d(TAG, e.message)
            }

            return false
        }

        private fun handleFile(file: TdApi.File) {
            if (file.local.isDownloadingCompleted) {
                val path = file.local.path
                notifyListeners(Event.ImageReady, path)
            } else if (file.local.isDownloadingActive) {
                // do nothing
            } else {
                val fileId: Int = file.id
                pendingImages.add(file.remote.id)
                client.send(TdApi.DownloadFile(fileId, 32), {
                    Log.d(TAG, "Sent download file!")
                })
            }
        }

        private fun handleMessageFromBot(update: TdApi.UpdateNewMessage) {
            if (update.message.content is TdApi.MessagePhoto) {
                val photo = (update.message.content as TdApi.MessagePhoto).photo as TdApi.Photo
                for (ps in photo.sizes) {
                    if (ps.type.equals("x")) {
                        // full size image!
                        val file = ps.photo
                        handleFile(file)
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