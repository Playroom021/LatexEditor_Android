package com.latexeditor.app.ui

import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView

/**
 * Lets the user pick rows/columns/alignment with simple controls and
 * generates the corresponding \begin{tabular} block, instead of hand
 * writing the column spec and & / \\ separators.
 */
object TableGeneratorDialog {

    fun show(context: Context, onGenerate: (String) -> Unit) {
        val padding = (16 * context.resources.displayMetrics.density).toInt()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val rowsInput = EditText(context).apply { hint = "Rows (e.g. 3)"; setText("3") }
        val colsInput = EditText(context).apply { hint = "Columns (e.g. 3)"; setText("3") }

        val alignGroup = RadioGroup(context).apply { orientation = LinearLayout.HORIZONTAL }
        val leftBtn = RadioButton(context).apply { text = "Left"; id = 1 }
        val centerBtn = RadioButton(context).apply { text = "Center"; id = 2; isChecked = true }
        val rightBtn = RadioButton(context).apply { text = "Right"; id = 3 }
        alignGroup.addView(leftBtn); alignGroup.addView(centerBtn); alignGroup.addView(rightBtn)

        layout.addView(TextView(context).apply { text = "Table generator" ; textSize = 16f })
        layout.addView(rowsInput)
        layout.addView(colsInput)
        layout.addView(TextView(context).apply { text = "Alignment"; gravity = Gravity.START })
        layout.addView(alignGroup)

        AlertDialog.Builder(context)
            .setTitle("Insert table")
            .setView(layout)
            .setPositiveButton("Insert") { _, _ ->
                val rows = rowsInput.text.toString().toIntOrNull()?.coerceIn(1, 30) ?: 3
                val cols = colsInput.text.toString().toIntOrNull()?.coerceIn(1, 15) ?: 3
                val align = when (alignGroup.checkedRadioButtonId) { 1 -> "l"; 3 -> "r"; else -> "c" }
                onGenerate(generate(rows, cols, align))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generate(rows: Int, cols: Int, align: String): String {
        val colSpec = (1..cols).joinToString(" | ") { align }
        val sb = StringBuilder()
        sb.append("\\begin{table}[h]\n")
        sb.append("\\centering\n")
        sb.append("\\begin{tabular}{$colSpec}\n")
        sb.append("\\hline\n")
        for (r in 1..rows) {
            val cells = (1..cols).joinToString(" & ") { c -> if (r == 1) "Header $c" else "Cell $r,$c" }
            sb.append("$cells \\\\\n")
            if (r == 1) sb.append("\\hline\n")
        }
        sb.append("\\hline\n")
        sb.append("\\end{tabular}\n")
        sb.append("\\caption{Caption}\n")
        sb.append("\\label{tab:my-table}\n")
        sb.append("\\end{table}\n")
        return sb.toString()
    }
}
