package me.jakjak.telegramimagereceiver

import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main.*
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi


class LoginActivity : AppCompatActivity() {

    val TAG: String = "LoginActivity"

    lateinit var client: Client

    var remoteId: String? = null

    var ready = false
    lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
            val deviceToken = instanceIdResult.token
            token = instanceIdResult.token
            Log.d("Firebase", "token "+ deviceToken)
        }

        startClient()
    }

    private fun startClient() {
        client = Client.create({
            Log.d(TAG, "Got update!")
            if (it is TdApi.UpdateNewMessage) {
                if (it.message.senderUserId == Constants.botId) {
                    Log.d(TAG, "got bot message!")
                    handleMessageFromBot(it)
                }
            } else if (it is TdApi.UpdateFile) {
                if (!it.file.local.isDownloadingActive && it.file.local.isDownloadingCompleted) {
                    if (it.file.remote.id.equals(remoteId)) {
                        remoteId = null
                        val path = it.file.local.path

                        displayImage(path)
                        // send intent to print app!
                    }
                }
            } else if (it is TdApi.UpdateConnectionState && it.state is TdApi.ConnectionStateReady) {
                client.send(TdApi.RegisterDevice(TdApi.DeviceTokenGoogleCloudMessaging(token), IntArray(0)), {
                    Log.d(TAG, "Register device!")
                }, {
                    Log.e(TAG, "Register device failed!")
                })
            }
        }, {
            Log.e(TAG, "Update exception!")
        }, {
            Log.e(TAG, "Default exception!")
        })
    }

    private fun displayImage(path: String?) {
        this.runOnUiThread({
            var bmp = BitmapFactory.decodeFile(path)
            val factor = 1080.0 / bmp.width
            bmp = android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * factor).toInt(), (bmp.height * factor).toInt(), false)
            try {
                image.setImageBitmap(bmp)
            } catch (e: Exception) {
                Log.e(TAG, e.message)
            }
        })
    }

    private fun handleMessageFromBot(update: TdApi.UpdateNewMessage) {
        if (update.message.content is TdApi.MessagePhoto) {
            val photo = (update.message.content as TdApi.MessagePhoto).photo as TdApi.Photo
            for (ps in photo.sizes) {
                if (ps.type.equals("x")) {
                    // full size image!
                    if (ps.photo.local.isDownloadingCompleted) {
                        displayImage(ps.photo.local.path)
                    }
                    else if (ps.photo.local.isDownloadingActive) {
                        // do nothing
                    }
                    else {
                        val fileId: Int = ps.photo.id
                        remoteId = ps.photo.remote.id
                        client.send(TdApi.DownloadFile(fileId, 32), {
                            Log.d(TAG, "Sent download file!")
                        })
                    }
                    break
                }
            }
            photo.sizes
        } else if (update.message.content is TdApi.MessageSticker) {
            // ??
        }
    }

    override fun onStart() {
        super.onStart()

        client.send(TdApi.SetTdlibParameters(TdApi.TdlibParameters(
                false,
                getApplicationContext().getFilesDir().getAbsolutePath() + "/",
                getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + "/",
                true,
                true,
                true,
                false,
                Constants.appId,
                Constants.appHash,
                "EN-en",
                Constants.device,
                Constants.androidVersion,
                "1.0",
                true, // turn off storage optimizer if weird behavior
                false
        )), {
            Log.d(TAG, "Set TDLib parameters")
        }, {
            Log.e(TAG, "Set TDLib parameters failed")
        })

        client.send(TdApi.CheckDatabaseEncryptionKey(Constants.secretEncryptionKey.toByteArray()), {
            Log.d(TAG, "Set DatabaseEncryptionKey")
        }, {
            Log.e(TAG, "Set DatabaseEncryptionKey failed")
        })

        client.send(TdApi.SetAuthenticationPhoneNumber("***REMOVED***", false, false), {
            Log.d(TAG, "Set PhoneNumber")
        }, {
            Log.e(TAG, "Set PhoneNumber failed")
        })
    }

    fun submitCode(view: View) {
        val code: String = codeField?.text.toString()
        client.send(TdApi.CheckAuthenticationCode(code,"",""), {
            Log.d(TAG, "Set auth code")
        }, {
            Log.e(TAG, "Set auth code failed")
        })
    }
}
