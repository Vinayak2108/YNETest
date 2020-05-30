package `in`.vrkhedkar.ynetest

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.budiyev.android.codescanner.*
import com.google.zxing.Result
import kotlinx.android.synthetic.main.activity_scanner.*

class Scanner : AppCompatActivity() {

    private lateinit var codeScanner: CodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        setUpScanner()
    }

    private fun setUpScanner() {
        codeScanner = CodeScanner(this, scannerView)
        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = CodeScanner.TWO_DIMENSIONAL_FORMATS
        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.scanMode = ScanMode.SINGLE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false
        codeScanner.decodeCallback = DecodeCallback {
            runOnUiThread {
                returnResult(it)
            }
        }
        codeScanner.errorCallback = ErrorCallback {
            runOnUiThread {
                returnResult(null)
            }
        }
    }

    private fun returnResult(result: Result?) {
        setResult(Activity.RESULT_OK, Intent().also {
            it.putExtra(DATA,result?.text)
        })
        finish()
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }
}
