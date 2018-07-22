package me.jakjak.telegramimagereceiver

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_user_list.*
import me.jakjak.telegramimagereceiver.models.User

class UserListActivity : AppCompatActivity() {

    lateinit var adapter: MyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        val realm = Realm.getDefaultInstance()
        val users = realm.where<User>().findAll()

        userListView.setOnItemClickListener{ _, _, position, _ ->
            val user = users[position]!!
            realm.executeTransaction{
                user.isLimited = !user.isLimited
            }
            adapter.notifyDataSetChanged()
        }

        userListView.setOnItemLongClickListener{ _, _, position, _ ->
            val user = users[position]!!
            realm.executeTransaction{
                user.isBlocked = !user.isBlocked
            }
            adapter.notifyDataSetChanged()
            return@setOnItemLongClickListener true
        }

        adapter = MyAdapter(this, R.layout.user_list_item, users)
        userListView.adapter = adapter
    }
}
