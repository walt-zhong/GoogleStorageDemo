/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhongxj.providerpagingkotlin

import android.annotation.SuppressLint
import android.app.LoaderManager
import android.content.ContentResolver
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.atomic.AtomicInteger

/**
 * Fragment that works as a client for accessing the DocumentsProvider
 * ([ImageProvider].
 */
class ImageClientFragment : Fragment() {

    private var mAdapter: ImageAdapter? = null

    private var mLayoutManager: LinearLayoutManager? = null

    private val mLoaderCallback = LoaderCallback()

    /**
     * The offset position for the ContentProvider to be used as a starting position to fetch
     * the images from.
     */
    private val mOffset = AtomicInteger(0)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_client, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = activity
        val recyclerView = activity?.findViewById<RecyclerView>(R.id.recyclerview)
        if (mLayoutManager == null) {
            mLayoutManager = LinearLayoutManager(activity)
        }
        if (recyclerView != null) {
            recyclerView.layoutManager = mLayoutManager
        }
        if (mAdapter == null) {
            mAdapter = activity?.let { ImageAdapter(it) }
        }
        if (recyclerView != null) {
            recyclerView.adapter = mAdapter
        }
        if (recyclerView != null) {
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val lastVisiblePosition = mLayoutManager!!.findLastVisibleItemPosition()
                    if (lastVisiblePosition >= mAdapter!!.fetchedItemCount) {
                        Log.d(
                            TAG,
                            "Fetch new images. LastVisiblePosition: " + lastVisiblePosition
                                    + ", NonEmptyItemCount: " + mAdapter!!.fetchedItemCount)

                        val pageId = lastVisiblePosition / LIMIT
                        // Fetch new images once the last fetched item becomes visible
                        activity.loaderManager
                            .restartLoader(pageId, null, mLoaderCallback)
                    }
                }
            })
        }

        val showButton = requireView().findViewById<Button>(R.id.button_show)
        showButton.setOnClickListener {
            activity?.loaderManager?.restartLoader(0, null, mLoaderCallback)
            showButton.visibility = View.GONE
        }
    }

    private inner class LoaderCallback : LoaderManager.LoaderCallbacks<Cursor> {

        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
            val activity = this@ImageClientFragment.activity
            return object : CursorLoader(activity) {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun loadInBackground(): Cursor? {
                    val bundle = Bundle()
                    bundle.putInt(ContentResolver.QUERY_ARG_OFFSET, mOffset.toInt())
                    bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, LIMIT)
                    if (activity != null) {
                        return activity.contentResolver
                            .query(ImageContract.CONTENT_URI, null, bundle, null)
                    }

                    return null
                }
            }
        }

        @SuppressLint("Range")
        override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
            val extras = cursor.extras
            val totalSize = extras.getInt(ContentResolver.EXTRA_TOTAL_COUNT)
            mAdapter!!.setTotalSize(totalSize)
            val beforeCount = mAdapter!!.fetchedItemCount
            while (cursor.moveToNext()) {
                val displayName = cursor.getString(cursor.getColumnIndex(
                    ImageContract.Columns.DISPLAY_NAME
                ))
                val absolutePath = cursor.getString(cursor.getColumnIndex(
                    ImageContract.Columns.ABSOLUTE_PATH
                ))

                val imageDocument = ImageAdapter.ImageDocument(absolutePath, displayName)
                mAdapter!!.add(imageDocument)
            }
            val cursorCount = cursor.count
            if (cursorCount == 0) {
                return
            }
            val activity = this@ImageClientFragment.activity
            mAdapter!!.notifyItemRangeChanged(beforeCount, cursorCount)
            val offsetSnapShot = mOffset.get()
            val message = activity?.resources
                ?.getString(R.string.fetched_images_out_of, offsetSnapShot + 1,
                    offsetSnapShot + cursorCount, totalSize)
            mOffset.addAndGet(cursorCount)
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }

        override fun onLoaderReset(loader: Loader<Cursor>) {

        }
    }

    companion object {

        private val TAG = "ImageClientFragment"

        /** The number of fetched images in a single query to the DocumentsProvider.  */
        private val LIMIT = 10

        fun newInstance(): ImageClientFragment {

            val args = Bundle()

            val fragment = ImageClientFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
