package com.thejuki.example.activity.detail

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.thejuki.example.R
import com.thejuki.example.activity.detail.NoteDetailActivity.Tabs.*
import com.thejuki.example.activity.form.BaseFormActivity
import com.thejuki.example.activity.form.NoteFormActivity
import com.thejuki.example.api.ApiClient
import com.thejuki.example.api.AuthManager
import com.thejuki.example.extension.simple
import com.thejuki.example.fragment.ItemInfoFragment
import com.thejuki.example.fragment.list.InfoListFragment
import com.thejuki.example.json.NoteJson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_item_detail.*
import kotlinx.android.synthetic.main.sheet_contact.*

/**
 * Note Detail Activity
 *
 * Note Details
 *
 * @author **TheJuki** ([GitHub](https://github.com/TheJuki))
 * @version 1.0
 */
class NoteDetailActivity : BaseDetailActivity<NoteJson>() {
    private val logTag = "NoteDetail"
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    @SuppressLint("InflateParams")
    override fun setupBottomSheet() {
        mBottomSheetDialog = BottomSheetDialog(this)
        val sheetView = this.layoutInflater.inflate(R.layout.sheet_note, null)
        mBottomSheetDialog!!.setContentView(sheetView)

        mBottomSheetDialog!!.sheet_close.setOnClickListener({
            mBottomSheetDialog!!.dismiss()
        })

        if (AuthManager.getInstance(this).has("note_edit")) {
            mBottomSheetDialog!!.sheet_edit.setOnClickListener({
                mBottomSheetDialog!!.dismiss()
                val intent = Intent(this, NoteFormActivity::class.java).apply {
                    putExtra(BaseFormActivity.ARG_ITEM, mItem)
                }
                startActivity(intent)
            })
        } else {
            mBottomSheetDialog!!.sheet_edit.visibility = View.GONE
        }
    }

    override fun getItem(id: String) {
        disposable = ApiClient.getInstance(this).getNote(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result ->
                            if (result.id.isNullOrEmpty()) {
                                Toast.makeText(this,
                                        getString(R.string.not_found_error_description, "Note"),
                                        Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                mItem = result
                                progressbar.visibility = View.GONE
                                if (mItem!!.modifyingUser != ApiClient.getInstance(this).getUsername()) {
                                    mBottomSheetDialog!!.sheet_edit.visibility = View.GONE
                                }
                                setupTabs()
                            }
                        },
                        { error ->
                            progressbar.visibility = View.GONE
                            Log.e(logTag, error.message)
                            val simpleAlert = AlertDialog.Builder(this).create()
                            simpleAlert.simple(R.string.server_error_title, R.string.server_error_description)
                        }
                )
    }

    override fun setupTabs() {
        supportActionBar?.title = "Note: " + mItem?.id.orEmpty()

        tabs.addTab(tabs.newTab().setText(getString(R.string.info)))
        tabs.addTab(tabs.newTab().setText(getString(R.string.lbl_body)))

        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager, tabs.tabCount)

        // Set up the ViewPager with the sections adapter.
        container.adapter = mSectionsPagerAdapter

        container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))
        tabs.tabMode = TabLayout.MODE_SCROLLABLE
    }

    enum class Tabs {
        Info,
        Body
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: androidx.fragment.app.FragmentManager, private val tabCount: Int) : androidx.fragment.app.FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): androidx.fragment.app.Fragment {
            return when (values()[position]) {
                Info -> InfoListFragment.newInstance(mItem!!.getInfos())
                Body -> ItemInfoFragment.newInstance(mItem!!.body.orEmpty())
            }
        }

        override fun getCount(): Int {
            return tabCount
        }
    }
}
