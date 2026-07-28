package com.latexeditor.app.ui

/**
 * Snippet catalog backing the "Symbols" tab in the editor toolbar: Greek
 * letters, operators, and pre-built structures (matrix/fraction/integral)
 * that would otherwise require memorizing exact LaTeX syntax.
 */
object SymbolPalette {

    data class Symbol(val label: String, val insert: String, val cursorOffset: Int = -1)

    val greek = listOf(
        Symbol("\u03b1", "\\alpha"), Symbol("\u03b2", "\\beta"), Symbol("\u03b3", "\\gamma"),
        Symbol("\u03b4", "\\delta"), Symbol("\u03b5", "\\epsilon"), Symbol("\u03b8", "\\theta"),
        Symbol("\u03bb", "\\lambda"), Symbol("\u03bc", "\\mu"), Symbol("\u03c0", "\\pi"),
        Symbol("\u03c3", "\\sigma"), Symbol("\u03c6", "\\phi"), Symbol("\u03c9", "\\omega")
    )

    val operators = listOf(
        Symbol("\u2211", "\\sum_{i=1}^{n}"),
        Symbol("\u222b", "\\int_{a}^{b}"),
        Symbol("\u220f", "\\prod_{i=1}^{n}"),
        Symbol("\u2264", "\\leq"), Symbol("\u2265", "\\geq"), Symbol("\u2260", "\\neq"),
        Symbol("\u00b1", "\\pm"), Symbol("\u00d7", "\\times"), Symbol("\u00f7", "\\div"),
        Symbol("\u2192", "\\rightarrow"), Symbol("\u221e", "\\infty"), Symbol("\u2202", "\\partial")
    )

    val structures = listOf(
        Symbol("Fraction", "\\frac{a}{b}", cursorOffset = 6), // lands inside first {}
        Symbol("Square root", "\\sqrt{x}", cursorOffset = 6),
        Symbol("Superscript", "x^{n}", cursorOffset = 3),
        Symbol("Subscript", "x_{n}", cursorOffset = 3),
        Symbol("2x2 Matrix", "\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}"),
        Symbol("Integral (bounds)", "\\int_{a}^{b} f(x)\\,dx"),
        Symbol("Piecewise", "f(x) = \\begin{cases} a & x > 0 \\\\ b & x \\leq 0 \\end{cases}"),
        Symbol("Limit", "\\lim_{x \\to \\infty}")
    )
}
