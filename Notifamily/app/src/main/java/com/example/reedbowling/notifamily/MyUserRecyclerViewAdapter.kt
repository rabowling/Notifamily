package com.example.reedbowling.notifamily

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.example.reedbowling.notifamily.ChildListFragment.OnChildListFragmentInteractionListener
import kotlinx.android.synthetic.main.fragment_child_item.view.*

/**
 * [RecyclerView.Adapter] that can display a [User] and makes a call to the
 * specified [OnChildListFragmentInteractionListener].
 */
class MyUserRecyclerViewAdapter(
        private val mValues: List<User>?,
        private val mListener: OnChildListFragmentInteractionListener?,
        private val mContext: Context)
    : RecyclerView.Adapter<MyUserRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as User
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onChildListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_child_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues?.get(position)
        holder.mName.text = item?.name
        holder.mCompleted.text = mContext.getString(R.string.user_completed, item?.completed)
        holder.mNotification.text = mContext.getString(R.string.user_total, item?.num)

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues!!.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mName : TextView = mView.childNameView
        val mCompleted : TextView = mView.childCompletedView
        val mNotification : TextView = mView.childNotifView

        override fun toString(): String {
            return super.toString() + " '" + mName.text + "'"
        }
    }
}
