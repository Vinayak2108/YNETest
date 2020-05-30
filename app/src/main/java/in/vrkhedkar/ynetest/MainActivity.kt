package `in`.vrkhedkar.ynetest

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*

const val TASK_SCAN = 0
const val TASK_PUSH = 1

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startButton.setOnClickListener {
            RxPermissions(this)
                .requestEach(Manifest.permission.CAMERA)
                .subscribe { permission ->
                    when{
                        permission.granted ->{startActivityForResult(Intent(this,Scanner::class.java), TASK_SCAN)}
                        permission.shouldShowRequestPermissionRationale ->{
                            AlertDialog.Builder(this)
                                .setTitle("Attention!")
                                .setMessage("Allow us to use camera to perform this task")
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
        sendButton.setOnClickListener {
            startActivityForResult(Intent(this,PushData::class.java).also {
                it.putExtra(DATA,scannedData.text)
            }, TASK_PUSH)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            TASK_SCAN->{
                if(resultCode == Activity.RESULT_OK){
                    data?.extras?.let {
                        if(!it.getString(DATA).isNullOrBlank()){
                            setScannedData(it.getString(DATA))
                        }
                    }
                }else{
                    resetScannedData()
                }
            }
            TASK_PUSH->{
                if(resultCode == Activity.RESULT_OK){
                    resetScannedData()
                }
            }
        }
    }

    private fun resetScannedData(){
        scannedData.text = getString(R.string.initial_message)
        sendButton.visibility = View.GONE
    }

    private fun setScannedData(string: String?) {
        scannedData.text = string
        sendButton.visibility = View.VISIBLE
    }

}
