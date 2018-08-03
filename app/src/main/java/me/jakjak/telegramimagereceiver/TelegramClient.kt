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

        val eventHandlers = ArrayList<EventHandler>()
        var jobHandler: ((Job) -> Unit)? = null

        fun bindHandler(e: EventHandler){
            eventHandlers.add(e)
        }

        fun unbindHandler(e: EventHandler){
            eventHandlers.remove(e)
        }

        val pendingImages = ArrayList<String>()

        val client = Client.create({
            Log.d(TAG, "Got update! " + it.javaClass.simpleName)
            if (it is TdApi.UpdateNewMessage && it.message.senderUserId != BuildConfig.botUserId) {
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

        //TODO store pending in db instead of memory
        private fun onFileDownloadComplete(it: TdApi.UpdateFile) {
            val remoteId = it.file.remote.id
            if (pendingImages.contains(remoteId)) {
                pendingImages.remove(remoteId)

                val path = it.file.local.path
                val realm = Realm.getDefaultInstance()
                val job = realm.where<Job>().equalTo("imageId", remoteId).isNull("imagePath").sort("timestamp").findFirst()!!

                realm.executeTransaction{
                    job.imagePath = path
                }

                jobHandler?.invoke(job)
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
            if (!UpdateService.isRunning) {
                return
            }
            else if (UpdateService.isRunning && !UpdateService.isConnected) {
                sendResponse(update, "Printer offline")
                return
            }

            var file: TdApi.File? = null
            if (update.message.content is TdApi.MessageSticker) {
                val sticker = (update.message.content as TdApi.MessageSticker).sticker as TdApi.Sticker
                file = sticker.sticker
            }
            else if (update.message.content is TdApi.MessagePhoto) {
                val photo = (update.message.content as TdApi.MessagePhoto).photo as TdApi.Photo
                for (ps in photo.sizes) {
                    if (ps.type.equals("x")) {
                        // full size image!
                        file = ps.photo
                        break
                    }
                }
            }
            else if (update.message.content is TdApi.MessageText) {
                //TODO
            }

            if (file != null) {
                if (!isUserAllowed(update.message.senderUserId)) {
                    sendResponse(update, "You've sent too many prints, try again in an hour")
                    return
                }
                else {
                    val realm = Realm.getDefaultInstance()
                    val user = realm.where<User>().equalTo("userId", update.message.senderUserId).findFirst()!!
                    val job = Job(user, Date(), file.remote.id)

                    handleFile(job, file)
                    sendResponse(update, "Printing!")

                    realm.executeTransaction{
                        user.jobs.add(job)
                    }
                }
            }
        }

        private fun sendResponse(update: TdApi.UpdateNewMessage, message: String) {
            val inputMessageText = TdApi.InputMessageText(TdApi.FormattedText(message, null), true, true)
            client.send(TdApi.SendMessage(update.message.chatId, update.message.replyToMessageId, false, true, null, inputMessageText), {

            }, {

            })
        }

        private fun isUserAllowed(senderUserId: Int): Boolean {
            try {
                val realm = Realm.getDefaultInstance()
                val user = realm.where<User>().equalTo("userId", senderUserId).findFirst()
                if (user == null) {
                    createUser(realm, senderUserId)
                    return true
                }
                if (user.isBlocked) {
                    return false
                }
                if (!user.isLimited) {
                    return true
                }
                val calendar = Calendar.getInstance()
                val now = calendar.time
                calendar.add(Calendar.HOUR, -1)
                val oneHourAgo = calendar.time
                val results = user.jobs.where().between("timestamp", oneHourAgo, now).findAll()

                return results.size < Constants.MAX_JOBS_PER_HOUR
            }
            catch (e: Exception) {
                Log.d(TAG, e.message)
            }

            return false
        }

        private fun createUser(realm: Realm, senderUserId: Int) {
            realm.executeTransaction {
                realm.createObject<User>(senderUserId)
            }
            client.send(TdApi.GetUser(senderUserId), {
                Log.d(TAG, "get user info!")
                val remoteUser = it as TdApi.User
                realm.executeTransaction {
                    val localUser = realm.where<User>().equalTo("userId", remoteUser.id).findFirst()!!
                    localUser.firstName = remoteUser.firstName
                    localUser.lastName = remoteUser.lastName
                }
            }, {
                Log.d(TAG, "get user info failed!")
            })
        }

        private fun handleFile(job: Job, file: TdApi.File) {
            if (file.local.isDownloadingCompleted) {
                val path = file.local.path
                job.imagePath = path
                jobHandler?.invoke(job)
                //notifyListeners(Event.ImageReady, path)
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

        interface EventHandler {
            fun handleEvent(e: Event, s: String?)
        }

        enum class Event {
            NeedPhone,
            NeedAuth,
            LoggedIn
        }
    }
}