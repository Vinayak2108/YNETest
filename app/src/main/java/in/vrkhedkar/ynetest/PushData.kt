package `in`.vrkhedkar.ynetest

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.net.wifi.SupplicantState
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_push_data.*
import kotlinx.coroutines.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.*


const val DATA = "data"

class PushData : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_push_data)
        connectAndSend.setOnClickListener {
            RxPermissions(this)
                .requestEach(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe { permission ->
                    when{
                        permission.granted ->{
                            if(validate()){
                                connect(ssid.text.toString(),password.text.toString())
                            }
                        }
                        permission.shouldShowRequestPermissionRationale ->{
                            AlertDialog.Builder(this)
                                .setTitle("Attention!")
                                .setMessage("Allow us to use Location to perform this task")
                                .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                                    dialog.dismiss()
                                }.show()
                        }
                        else->{
                            AlertDialog.Builder(this)
                                .setTitle("Attention!")
                                .setMessage("You need to Allow permission from Settings to perform this task")
                                .setNegativeButton(getString(R.string.ok)) { dialog, _ ->
                                    dialog.dismiss()
                                    startActivity(Intent().also {
                                        it.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                        it.addCategory(Intent.CATEGORY_DEFAULT)
                                        it.data = Uri.parse("package:$packageName")
                                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        it.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                                        it.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                    })
                                }
                                .setPositiveButton(getString(R.string.cancel)) { dialog, _ ->
                                    dialog.dismiss()
                                }.show()
                        }
                    }
                }
        }

        send.setOnClickListener {
            pushData(scannedData.text.toString())
        }
    }

    private fun pushData(data: String) {
        GlobalScope.launch {
            withContext(Dispatchers.IO){
                try {
                    val socket = Socket("192.168.1.29",4096)
                    val input = DataInputStream(socket.getInputStream())
                    val out = DataOutputStream(socket.getOutputStream())
                    out.writeUTF(data)
                    val response = input.readUTF()
                    if(response == "OK"){
                        withContext(Dispatchers.Main){
                            Toast.makeText(applicationContext,"Successfully pushed",Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }else{
                        withContext(Dispatchers.Main){
                            showError("Error from server")
                        }
                    }

                }catch (e:Exception){
                    withContext(Dispatchers.Main){
                        showError("Connectivity Error")
                    }
                }
            }
        }
    }

    private fun validate(): Boolean {
        error.text = ""
        if(ssid.text.toString().isNullOrBlank()){
            showError("Invalid SSID")
            return false
        }
        if(password.text.toString().isNullOrBlank()){
            showError("Invalid Password")
            return false
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        intent?.let {
            setData(it.getStringExtra(DATA))
        }
    }

    private fun setData(data: String?) {
        scannedData.text = data
    }

    private fun showError(message:String){
        error.text = message
    }

    private fun connect(ssid: String, password: String){

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val state = wifiManager.connectionInfo.supplicantState
        if (state == SupplicantState.COMPLETED && wifiManager.connectionInfo.ssid == "\"$ssid\"") {
            pushData(scannedData.text.toString())
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val suggestion = WifiNetworkSuggestion.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(password)
                    .setIsAppInteractionRequired(true)
                    .build()

                val suggestionsList: MutableList<WifiNetworkSuggestion> =
                    ArrayList()
                suggestionsList.add(suggestion)

                val status = wifiManager.addNetworkSuggestions(suggestionsList)

                if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                    Log.i("XXX", "Connection Error")
                }

                val intentFilter = IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)

                val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        applicationContext.unregisterReceiver(this)
                        if (intent.action != WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION) {
                            return
                        }
                        pushData(scannedData.text.toString())
                    }
                }
                applicationContext.registerReceiver(broadcastReceiver,intentFilter)
            }else{

                val wifiConfig = WifiConfiguration()
                wifiConfig.SSID = "\"$ssid\"";
                wifiConfig.status = WifiConfiguration.Status.ENABLED;
                wifiConfig.priority = 40;

                wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wifiConfig.preSharedKey = "\"$password\"";
                wifiConfig.hiddenSSID = true

                val id = wifiManager.addNetwork(wifiConfig)
                wifiManager.disconnect()
                wifiManager.enableNetwork(id,true)
                wifiManager.reconnect()
                GlobalScope.launch {
                    withContext(Dispatchers.Main){
                        var counter = 5
                        while (counter>=0){
                            val state = wifiManager.connectionInfo.supplicantState
                            if(state == SupplicantState.COMPLETED && wifiManager.connectionInfo.ssid == "\"$ssid\""){
                                pushData(scannedData.text.toString())
                                break
                            }
                            delay(1000)
                            counter--
                        }
                    }
                }
            }
        }

    }

}





