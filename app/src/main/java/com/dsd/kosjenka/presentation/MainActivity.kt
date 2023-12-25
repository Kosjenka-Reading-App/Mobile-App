package com.dsd.kosjenka.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.ActivityMainBinding
import com.dsd.kosjenka.presentation.home.camera.Camera2Fragment
import com.dsd.kosjenka.presentation.home.camera.VisageWrapper
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var visageWrapper: VisageWrapper

    private val MY_PERMISSIONS_REQUEST_STORAGE = 2

    init {
        System.loadLibrary("VisageWrapper")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        visageWrapper = VisageWrapper.get(this)
        visageWrapper.AllocateResources()

        copyAssets(filesDir.absolutePath)

    }

    override fun onSupportNavigateUp(): Boolean = navController.navigateUp()

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
             MY_PERMISSIONS_REQUEST_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    copyAssets(filesDir.absolutePath)
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the feature requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    /**
     * Utility method called to copy required file to trackerdata folder.
     *
     * @param rootDir  absolute path to directory where files should be copied.
     * @param filename name of file that will be copied.
     */
    fun copyFile(rootDir: String, filename: String) {
        val assetManager = this.assets
        var inp: InputStream? = null
        var out: OutputStream? = null
        try {
            val newFileName = rootDir + File.separator + filename
            val file = File(newFileName)
            if (!file.exists()) {
                inp = assetManager.open("trackerdata/$filename")
                out = FileOutputStream(newFileName)
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inp.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                out.flush()
                out.close()
                out = null
                inp.close()
                inp = null
            }
        } catch (e: java.lang.Exception) {
            Log.e("VisageTrackerDemo", e.message!!)
        }
    }

    /**
     * Utility method called to create required directories and initiate copy of all assets required for tracking.
     *
     * @param rootDir absolute path to root directory used for storing assets required for tracking.
     */
    fun copyAssets(rootDir: String) {
        val assetManager = this.assets
        var assets: Array<String>? = null
        try {
            assets = assetManager.list("trackerdata")
            for (asset in assets!!) {
                Log.i("VisageTrackerDemo", rootDir + File.separator + asset)
                try {
                    if (!asset.contains("vfa") && !asset.contains("vfr")
                        && !asset.contains("vft")
                    ) copyFile(rootDir, asset)
                } catch (e: Exception) {
                    Log.e("VisageTrackerDemo", e.message!!)
                }
            }
        } catch (e: Exception) {
            Log.e("VisageTrackerDemo", e.message!!)
        }

        // create dirs
        val dirs = arrayOf(
            "vft",
            "vft/er",
            "vft/fa",
            "vft/ff",
            "vft/fm",
            "vft/pr"
        )
        for (dirname in dirs) {
            try {
                val dir = File(rootDir + File.separator + dirname)
                if (!dir.exists()) dir.mkdir()
            } catch (e: Exception) {
                Log.e("VisageTrackerDemo", e.message!!)
            }
        }

        // copy files
        val files = arrayOf(
            "vft/er/efa.lbf",
            "vft/er/efc.lbf",
            "vft/fa/aux_file.bin",
            "vft/fa/d1qy.tflite",
            "vft/fa/d2.tflite",
            "vft/ff/ff.tflite",
            "vft/fm/candide3.fdp",
            "vft/fm/candide3.wfm",
            "vft/fm/jk_300.fdp",
            "vft/fm/jk_300.wfm",
            "vft/fm/jk_300.cfg",
            "vft/fm/jk_300_wEars.fdp",
            "vft/fm/jk_300_wEars.wfm",
            "vft/fm/jk_300_wEars.cfg",
            "vft/pr/pr.tflite"
        )
        for (filename in files) {
            try {
                Log.i("VisageTrackerDemo", rootDir + File.separator + filename)
                copyFile(rootDir, filename)
            } catch (e: Exception) {
                Log.e("VisageTrackerDemo", e.message!!)
            }
        }
    }

}