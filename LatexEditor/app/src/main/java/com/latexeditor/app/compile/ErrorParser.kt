package com.latexeditor.app.compile

import com.latexeditor.app.data.LatexError

/**
 * Best-effort parser for pdflatex/xelatex console output. LaTeX logs are
 * notoriously inconsistent, so this covers the common cases:
 *   - "! Undefined control sequence."          (with "l.<n>" a couple of lines later)
 *   - "! LaTeX Error: <message>"                (with "l.<n>" following)
 *   - "Runaway argument" / unterminated environments
 *   - "! File `x.sty' not found."
 * and falls back to surfacing any line starting with "!" even if we can't
 * resolve a line number, so nothing important gets silently dropped.
 */
object ErrorParser {

    private val lineNumRegex = Regex("""^l\.(\d+)""")

    fun parse(log: String, mainFile: String): List<LatexError> {
        val lines = log.lines()
        val errors = mutableListOf<LatexError>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("!")) {
                val message = line.removePrefix("!").trim()
                // Search the next ~5 lines for a "l.<num>" marker
                var foundLine = -1
                for (j in (i + 1)..minOf(i + 6, lines.size - 1)) {
                    val m = lineNumRegex.find(lines[j].trim())
                    if (m != null) {
                        foundLine = m.groupValues[1].toIntOrNull() ?: -1
                        break
                    }
                }
                errors.add(LatexError(file = mainFile, line = if (foundLine > 0) foundLine else 0, message = message))
            }
            i++
        }

        // Common structural issue: unbalanced \begin{}/\end{} pairs, detected
        // client-side too so we can flag it even before a compile attempt.
        return errors
    }

    /** Static, pre-compile check for begin/end mismatches so users get instant feedback. */
    fun checkUnbalancedEnvironments(source: String): List<LatexError> {
        val beginRegex = Regex("""\\begin\{([a-zA-Z\*]+)\}""")
        val endRegex = Regex("""\\end\{([a-zA-Z\*]+)\}""")
        val stack = ArrayDeque<Pair<String, Int>>() // env name, line number
        val problems = mutableListOf<LatexError>()

        val lines = source.split("\n")
        for ((idx, lineText) in lines.withIndex()) {
            val lineNum = idx + 1
            for (m in beginRegex.findAll(lineText)) stack.addLast(m.groupValues[1] to lineNum)
            for (m in endRegex.findAll(lineText)) {
                val env = m.groupValues[1]
                if (stack.isEmpty()) {
                    problems.add(LatexError("", lineNum, "\\end{$env} has no matching \\begin{$env}"))
                } else {
                    val (openEnv, openLine) = stack.removeLast()
                    if (openEnv != env) {
                        problems.add(LatexError("", lineNum, "Expected \\end{$openEnv} (opened on line $openLine) but found \\end{$env}"))
                    }
                }
            }
        }
        while (stack.isNotEmpty()) {
            val (env, line) = stack.removeLast()
            problems.add(LatexError("", line, "Missing \\end{$env}"))
        }
        return problems
    }
}
