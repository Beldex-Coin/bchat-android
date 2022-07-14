package com.thoughtcrimes.securesms.changelog

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ExpandableListView
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityChangeLogBinding
import org.json.JSONException
import org.json.JSONObject
import com.thoughtcrimes.securesms.BaseActionBarActivity
import com.thoughtcrimes.securesms.util.setUpActionBarBchatLogo
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset

class ChangeLogActivity : BaseActionBarActivity() {
    private lateinit var binding:ActivityChangeLogBinding

    lateinit var listAdapter: ExpandableListAdapter
    lateinit var listDataHeader: ArrayList<String>
    lateinit var listDataChild: HashMap<String, List<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpActionBarBchatLogo("Changelog")

            // preparing list data
            prepareListData()
        
            listAdapter =
                ExpandableListAdapter(
                    this@ChangeLogActivity,
                    listDataHeader,
                    listDataChild
                )

            // setting list adapter
            binding.expandableListView!!.setAdapter(listAdapter)

    }

    private fun loadJSONFromAsset(): String? {
        var json: String? = null
        json = try {
            val `is`: InputStream = this.assets.open("changeLog.json")
            val size: Int = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }
    
    /*
     * Preparing the list data
     */
    private fun prepareListData() {
        listDataHeader = ArrayList<String>()
        listDataChild = HashMap()

        try {
            val obj = JSONObject(loadJSONFromAsset())
            val changeLogArray = obj.getJSONArray("change_log")
            for (i in 0 until changeLogArray.length()) {
                val comingSoon: MutableList<String> = ArrayList()
                val changeLogObject = changeLogArray.getJSONObject(i)
                val changeLogTitleValue = changeLogObject.getString("title")

                listDataHeader.add(changeLogTitleValue)
                val descriptionsArray = changeLogObject.getJSONArray("descriptions")
                for (j in 0 until descriptionsArray.length()) {
                    val descriptionsObject = descriptionsArray.getJSONObject(j)
                    val descriptionsObjectValue = descriptionsObject.getString("description")
                    comingSoon.add(descriptionsObjectValue)
                }
                listDataChild[listDataHeader[i]] = comingSoon

                //comingSoon.clear()
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
}