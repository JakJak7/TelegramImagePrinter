package me.jakjak.telegramimagereceiver

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import kotlinx.android.synthetic.main.user_list_item.view.*
import me.jakjak.telegramimagereceiver.models.User

class MyAdapter(context: Context, val resource: Int, val data: List<User>): BaseAdapter() {

    var inflater: LayoutInflater

    init {
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: ViewHolder
        val view: View

        if (convertView == null) {
            view = inflater.inflate(resource, null)
            holder = ViewHolder(view)
            view.tag = holder
        }
        else {
            holder = convertView.tag as ViewHolder
            view = convertView
        }

        val user = data[position]
        holder.nameView.text = "${user.firstName} ${user.lastName}"

        val status: String
        if (user.isBlocked) {
            status = "blocked"
        }
        else {
            if (user.isLimited) {
                status = "limited"
            }
            else {
                status = "unlimited"
            }
        }
        holder.statusView.text = status

        return view
    }

    override fun getItemId(p0: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(position: Int): Any {
        return data[position]
    }

    class ViewHolder(view: View) {
        var nameView = view.userNameView
        var statusView = view.userLimitedView
    }
}