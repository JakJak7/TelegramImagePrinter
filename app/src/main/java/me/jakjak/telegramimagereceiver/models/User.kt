package me.jakjak.telegramimagereceiver.models

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class User(
        @PrimaryKey var userId: Int = 0,
        var firstName: String = "",
        var lastName: String = "",
        var jobs : RealmList<Job> = RealmList()
): RealmObject() {
}