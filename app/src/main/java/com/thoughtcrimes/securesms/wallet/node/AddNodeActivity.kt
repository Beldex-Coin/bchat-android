package com.thoughtcrimes.securesms.wallet.node


import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import com.thoughtcrimes.securesms.PassphraseRequiredActionBarActivity
import com.thoughtcrimes.securesms.data.Node
import com.thoughtcrimes.securesms.data.NodeInfo
import com.thoughtcrimes.securesms.model.AsyncTaskCoroutine
import com.thoughtcrimes.securesms.util.Helper
import io.beldex.bchat.R
import io.beldex.bchat.databinding.ActivityAddNodeBinding
import org.apache.commons.lang3.ObjectUtils
import java.lang.NumberFormatException
import java.net.UnknownHostException
import java.util.concurrent.Executor


class AddNodeActivity : PassphraseRequiredActionBarActivity () {
    private lateinit var binding: ActivityAddNodeBinding
    private val nodesAdapter: NodeInfoAdapter? = null
    private val shutdown = false
    val nodeInfo: NodeInfo? = null
    val nodeBackup: NodeInfo? = null


    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivityAddNodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = resources.getString(R.string.activity_new_node_page_title)

        /*val extras = intent.extras
        var nodeInfo1 = extras!!.getCharSequence("nodeInfo") // use your key
        Log.d("Beldex","value of nodeinfo $nodeInfo1")
        nodeInfo1 = nodeInfo.toString()*/


        binding.testButton.setOnClickListener {
           /* if (nodeInfo != null) {*/

                Log.d("Beldex","nodeinfo if")
                /*nodeBackup = NodeInfo(nodeInfo)*/
                /*etNodeName.getEditText().setText(nodeInfo?.name)*/
                /*binding.nodeAddressEditTxtLayout.editText?.setText( nodeInfo?.host)
                binding.nodePortEditTxtLayout.editText?.setText(nodeInfo?.rpcPort?.toString())
                binding.nodeLoginEditTxtLayout .editText?.setText(nodeInfo?.username)
                binding.nodePasswordEditTxtLayout.editText?.setText(nodeInfo?.password)*/
                //showTestResult()
            /*} else {
                Log.d("Beldex","nodeinfo else")
                nodeInfo = NodeInfo()
                nodeBackup = null
            }*/
            test()
        }


    }


    private fun test() {
        if (applyChanges()) AsyncTestNode().execute<Boolean>()
    }


    private fun applyChanges(): Boolean {

        nodeInfo?.clear()
        showTestResult()
        val portString: String =
            binding.nodePortEditTxtLayout.editText?.text.toString().trim()
        val port: Int = if (portString.isEmpty()) {
            Node.getDefaultRpcPort()
        } else {
            try {
                portString.toInt()
            } catch (ex: NumberFormatException) {
                binding.nodePortEditText.error = getString(R.string.node_port_numeric)
                return false
            }
        }
        binding.nodePortEditTxtLayout.editText?.error = null
        if (port <= 0 || port > 65535) {
            binding.nodePortEditTxtLayout.editText?.error = getString(R.string.node_port_range)
            return false
        }
        val host: String = binding.nodeAddressEditTxtLayout.editText?.text.toString().trim()
        if (host.isEmpty()) {
            binding.nodePortEditTxtLayout.editText?.error = getString(R.string.node_host_empty)
            return false
        }
       /* val setHostSuccess: Boolean = Helper.runWithNetwork {
            try {
                nodeInfo?.host
                true
            } catch (ex: UnknownHostException) {
                binding.nodeAddressEditTxtLayout.error =
                    getString(R.string.node_host_unresolved)
                false
            }
        }
        if (!setHostSuccess) {
            binding.nodeAddressEditTxtLayout.error = getString(R.string.node_host_unresolved)
            return false
        }*/
        binding.nodeAddressEditTxtLayout.error = null
        nodeInfo?.rpcPort
        // setName() may trigger reverse DNS
        val setHostSuccess = Helper.runWithNetwork(object : Helper.Action {
            override fun run(): Boolean {
                return try {
                    nodeInfo?.host = host
                    true
                } catch (ex: UnknownHostException) {
                    binding.nodeAddressEditTxtLayout.error = getString(R.string.node_host_unresolved)
                    false
                }
            }
        })
           /* try {
                Log.d("Beldex", "node add try 1")
                Log.d("Beldex", "node add try 1 host $host")

                    Log.d("Beldex", "node add try 4")
                    nodeInfo?.host = host
                Log.d("Beldex", "node add try 5")
               return@runWithNetwork true
            } catch (ex: UnknownHostException) {
                Log.d("Beldex", "node add issue $ex")
                binding.testResult.error = (getString(R.string.node_host_unresolved))
                 return@runWithNetwork false
            }*/

        if (!setHostSuccess) {
            binding.testResult.error = (getString(R.string.node_host_unresolved))
            return false
        }
        binding.testResult.error = (null)
        nodeInfo?.rpcPort = port
        // setName() may trigger reverse DNS
        Helper.runWithNetwork(object : Helper.Action {
            override fun run(): Boolean {
                nodeInfo?.name = binding.nodeLoginEditTxtLayout.editText?.text.toString().trim()
                return true
            }
        })
        nodeInfo?.username = binding.nodeLoginEditTxtLayout.editText?.text.toString().trim()
        nodeInfo?.password = binding.nodePasswordEditTxtLayout.editText?.text.toString() // no trim for pw

        return true
    }

    private inner class AsyncTestNode :
        AsyncTaskCoroutine<Void?, Boolean?>() {
        override fun onPreExecute() {
            super.onPreExecute()
            Log.d("Beldex","node test host add ${nodeInfo?.hostAddress}")
            binding.testResult.text = (getString(R.string.node_testing, nodeInfo?.hostAddress))
        }


        override fun onPostExecute(result: Boolean?) {
           /* if (editDialog != null) {*/
                showTestResult()
           // }
            if (shutdown) {
                if (nodeBackup == null) {
                    nodesAdapter?.addNode(nodeInfo)
                } else {
                    nodesAdapter?.setNodes()
                }
            }
        }


       /* override fun doInBackground(vararg params: Executor?): Boolean? {
            nodeInfo?.testRpcService()
            return true
        }*/

        override fun doInBackground(vararg params: Void?): Boolean? {
            nodeInfo?.testRpcService()
            return true
        }
    }

 /*    class AsyncTestNode(private val addNodeActivity: AddNodeActivity) :
        AsyncTask<Void?, Void?, Boolean>() {
        override fun onPreExecute() {
            super.onPreExecute()
           // tvResult.setText(getString(R.string.node_testing, nodeInfo?.getHostAddress()))
            Log.d("Beldex","node host address ${addNodeActivity.nodeInfo?.hostAddress}")
            Log.d("Beldex","node host address ${addNodeActivity.nodeBackup?.hostAddress}")
            //addNodeActivity.binding.testResult.text = ("Testing IP: " + addNodeActivity.nodeInfo?.hostAddress)
        }


        override fun onPostExecute(result: Boolean) {
            //by hales
            *//*if (editDialog != null) {*//*
                addNodeActivity.showTestResult()
            //}
            if (addNodeActivity.shutdown) {
                if (addNodeActivity.nodeInfo == null) {
                    addNodeActivity.nodesAdapter?.addNode(addNodeActivity.nodeInfo)
                } else {
                    addNodeActivity.nodesAdapter?.setNodes()
                }
            }
        }



        override fun doInBackground(vararg p0: Void?): Boolean {
            addNodeActivity.nodeInfo?.testRpcService()
            return true
        }
    }*/

    private fun showTestResult() {
        Log.d("Beldex","node test successfull status ${nodeInfo?.isSuccessful}")
        if (nodeInfo?.isSuccessful == true) {
            binding.testResult.text = (getString(
                R.string.node_result,
                NodeFragment.FORMATTER.format(nodeInfo.height), nodeInfo.majorVersion,
                nodeInfo.responseTime, nodeInfo.hostAddress
            ))
        } else {
            //binding.testResult.text = "testing ..."
            binding.testResult.text = nodeInfo?.let { NodeInfoAdapter.getResponseErrorText(this, it.responseCode) }
        }
    }
}