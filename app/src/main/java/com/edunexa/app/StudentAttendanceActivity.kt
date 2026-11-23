package com.edunexa.app
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import com.edunexa.app.data.AttendanceRepository
import com.google.android.material.card.MaterialCardView

class StudentAttendanceActivity:EduActivity(){
    private val repo=AttendanceRepository()
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(18),dp(18),dp(18))}
        setContentView(ScrollView(this).apply{addView(root)})
        root.addView(TextView(this).apply{text="My Attendance";textSize=27f;setTypeface(typeface,android.graphics.Typeface.BOLD)})
        val summary=TextView(this).apply{textSize=16f;setPadding(0,dp(8),0,dp(12))}
        root.addView(summary)
        val refresh=Button(this).apply{text="Refresh Attendance";isAllCaps=false}
        root.addView(refresh)
        val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        root.addView(list)
        fun load(){
            summary.text="Loading attendance…"
            list.removeAllViews()
            repo.studentAttendance({rows->
                list.removeAllViews()
                val present=rows.count{it["present"]==true}
                val absent=rows.size-present
                val pct=if(rows.isNotEmpty())present*100.0/rows.size else 0.0
                summary.text=if(rows.isEmpty())"No attendance records yet." else "Total: ${rows.size}  •  Present: ${present}  •  Absent: ${absent}\nAttendance: %.1f%%".format(pct)
                if(rows.isEmpty()){
                    list.addView(TextView(this).apply{text="Your attendance will appear here once your teacher records it.";textSize=15f;setPadding(0,dp(16),0,dp(16))})
                }else rows.forEach{r->
                    val isPresent=r["present"]==true
                    val card=MaterialCardView(this).apply{
                        radius=dp(14).toFloat();cardElevation=dp(2).toFloat()
                        setCardBackgroundColor(if(isPresent)Color.rgb(232,247,233) else Color.rgb(255,235,235))
                        setContentPadding(dp(14),dp(14),dp(14),dp(14))
                        addView(TextView(this@StudentAttendanceActivity).apply{
                            text="${r["date"]}   •   ${if(isPresent)"Present ✓" else "Absent ✗"}"
                            textSize=16f
                            setTextColor(if(isPresent)Color.rgb(28,120,55) else Color.rgb(170,35,35))
                        })
                    }
                    val lp=LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));list.addView(card,lp)
                }
            },{error->summary.text=error})
        }
        refresh.setOnClickListener{load()}
        load()
    }
}
