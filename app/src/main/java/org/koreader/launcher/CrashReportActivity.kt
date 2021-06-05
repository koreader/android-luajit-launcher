package org.koreader.launcher

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.koreader.launcher.databinding.CrashReportBinding

class CrashReportActivity : AppCompatActivity() {
    private lateinit var binding: CrashReportBinding

    public override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.extras?.let { bundle ->
            binding = CrashReportBinding.inflate(layoutInflater)
            setContentView(binding.root)

            binding.title.text = bundle.get("title").toString()
            binding.reason.text = bundle.get("reason").toString()
            binding.logs.text = bundle.get("logs").toString()

            binding.shareReport.setOnClickListener {
                val intent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, binding.logs.text.toString())
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(intent,
                    resources.getString(R.string.crash_report_share_button)))
            }
        } ?: noCrashReportAttachedError()
    }

    private fun noCrashReportAttachedError() {
        Toast.makeText(this, "No crash report", Toast.LENGTH_LONG).show()
        finish()
    }
}
