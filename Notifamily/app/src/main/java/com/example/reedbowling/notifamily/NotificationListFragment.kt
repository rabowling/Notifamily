package com.example.reedbowling.notifamily

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [NotificationListFragment.OnNotificationListFragmentInteractionListener] interface.
 */
class NotificationListFragment : Fragment() {

    private var columnCount = 1
    private var userId = ""

    private var listener: OnNotificationListFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
            userId = it.getString(USER_ID, "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_notification_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                val viewModel = ViewModelProviders.of(this@NotificationListFragment).get(MyViewModel::class.java)
                viewModel.getNotifications().observe(this@NotificationListFragment, Observer {
                    adapter = MyNotificationRecyclerViewAdapter(it?.filter {n ->
                        n.user == userId
                    }, listener, context)
                })
                view.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
            }
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnNotificationListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnNotificationListFragmentInteractionListener {
        fun onNotificationListFragmentInteraction(item: NotificationItem?)
    }

    companion object {

        const val ARG_COLUMN_COUNT = "column-count"

        const val USER_ID = "user-id"

        @JvmStatic
        fun newInstance(columnCount: Int, userId: String) =
                NotificationListFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_COLUMN_COUNT, columnCount)
                        putString(USER_ID, userId)
                    }
                }
    }
}
