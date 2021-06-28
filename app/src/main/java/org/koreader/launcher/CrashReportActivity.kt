package org.koreader.launcher

import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.koreader.launcher.databinding.CrashReportBinding
import java.io.File

class CrashReportActivity : AppCompatActivity() {
    private lateinit var binding: CrashReportBinding

    public override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.extras?.let { bundle ->
            binding = CrashReportBinding.inflate(layoutInflater)
            setContentView(binding.root)
            binding.title.text = bundle.get("title").toString()
            binding.reason.text = bundle.get("reason").toString()
            if (binding.reason.text.equals("")) {
                binding.reason.visibility = View.GONE
            }

            File(MainApp.crash_report_path).inputStream().bufferedReader().use {
                binding.logs.text = it.readText()
            }

            binding.shareReport.setOnClickListener {
                val intent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, binding.logs.text.toString())
                }
                startActivity(Intent.createChooser(intent,
                    resources.getString(R.string.common_share_rationale)))
            }
        } ?: noCrashReportAttachedError()
    }

    private fun noCrashReportAttachedError() {
        Toast.makeText(this,
            resources.getString(R.string.no_crash_attached), Toast.LENGTH_LONG).show()
        finish()
    }
}
