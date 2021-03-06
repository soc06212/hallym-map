package xyz.ridsoft.hal.more

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import xyz.ridsoft.hal.databinding.ActivityAppInfoBinding
import xyz.ridsoft.hal.developer.FacilityJsonBuilderActivity
import xyz.ridsoft.hal.developer.JsonBuilderActivity
import xyz.ridsoft.hal.etc.ApplicationInfo
import xyz.ridsoft.hal.value.SharedPreferencesKeys

class AppInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppInfoBinding

    private var clicked = 0

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtAppName.setOnClickListener {
            if (clicked >= 7) {
                val pref = getSharedPreferences(SharedPreferencesKeys.DEVELOPER_PREF, 0)
                val edit = pref.edit()
                val alert = AlertDialog.Builder(this)
                alert.setTitle("개발자 메뉴")
                    .setMessage("개발자 모드를 활성화하시겠습니까?")
                    .setNeutralButton("닫기") { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton("활성화") { dialog, _ ->
                        edit.putBoolean(SharedPreferencesKeys.BOOL_DEV_MENU_ENABLE, true)
                        edit.apply()
                        dialog.dismiss()
                    }
                    .setNegativeButton("비활성화") { dialog, _ ->
                        edit.putBoolean(SharedPreferencesKeys.BOOL_DEV_MENU_ENABLE, false)
                        edit.apply()
                        dialog.dismiss()
                    }
                    .show()
                clicked = 0
            } else {
                clicked++
            }
        }

        val appInfo = ApplicationInfo(this)
        binding.txtAppInfoVersion.text = appInfo.getVersionName()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.txtAppInfoBuilid.text = appInfo.getLongVersionCode().toString()
        } else {
            binding.txtAppInfoBuilid.text = appInfo.getVersionCode().toString()
        }
    }

}