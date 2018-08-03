package me.jakjak.telegramimagereceiver.models

import io.realm.RealmObject
import java.util.*

open class Job(
        var user: User? = null,
        var timestamp: Date? = null,
        var imageId: String? = null,
        var imagePath: String? = null,
        var text: String? = null,
        var completed: Boolean = false
) : RealmObject() {
}