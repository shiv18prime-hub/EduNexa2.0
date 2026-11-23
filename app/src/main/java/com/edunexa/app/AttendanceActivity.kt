package com.edunexa.app
import android.os.Bundle
import android.widget.*
import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.*
import com.edunexa.app.data.AttendanceRepository

class AttendanceActivity: EduActivity() {
    private val repo = AttendanceRepository()
    private fun dp(v:Int) = (v * resources.displayMetrics.density).toInt()
    private fun validDate(value:String):Boolean {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val input = value.trim()
        if (input.length != 10) return false
        val position = ParsePosition(0)
        val parsed = formatter.parse(input, position)
        return parsed != null && position.index == input.length
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(18),dp(18),dp(18)) }
        setContentView(ScrollView(this).apply { addView(root) })
        root.addView(TextView(this).apply { text="Attendance"; textSize=27f; setTypeface(typeface,android.graphics.Typeface.BOLD) })
        val date = EditText(this).apply { hint="Date YYYY-MM-DD"; setText(SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())); inputType=android.text.InputType.TYPE_CLASS_DATETIME }
        root.addView(date)
        val status = TextView(this).apply { text="Loading students…"; setPadding(0,dp(8),0,dp(12)) }
        root.addView(status)
        repo.schoolStudents({ students ->
            status.text = if(students.isEmpty()) "No connected students" else "${students.size} student(s) • Tap Present or Absent"
            students.forEach { (id,name) ->
                val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(dp(8),dp(8),dp(8),dp(8)) }
                row.addView(TextView(this).apply { text=name; textSize=16f },LinearLayout.LayoutParams(0,dp(52),1f))
                fun mark(present:Boolean) {
                    val day=date.text.toString().trim()
                    if(!validDate(day)){ date.error="Enter a valid date (YYYY-MM-DD)"; Toast.makeText(this,"Date is invalid. Example: 2026-11-23",Toast.LENGTH_SHORT).show(); return }
                    repo.save(id,name,day,present){ok,message->Toast.makeText(this,message,Toast.LENGTH_SHORT).show()}
                }
                row.addView(Button(this).apply { text="Present"; isAllCaps=false; setOnClickListener { mark(true) } })
                row.addView(Button(this).apply { text="Absent"; isAllCaps=false; setOnClickListener { mark(false) } })
                root.addView(row)
            }
        }, { error -> status.text=error })
    }
}
