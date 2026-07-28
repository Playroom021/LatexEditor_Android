package com.latexeditor.app.editor

/**
 * Static reference data driving autocomplete. Each entry is the text
 * typed after the trigger char, plus what gets inserted (often with a
 * cursor placeholder we jump into after insertion).
 */
data class Suggestion(val trigger: String, val insertText: String, val cursorOffset: Int, val category: String)

object LatexData {

    // --- Commands (after "\") -------------------------------------------------
    val commands: List<Suggestion> = listOf(
        cmd("section", "section{}", 1),
        cmd("subsection", "subsection{}", 1),
        cmd("subsubsection", "subsubsection{}", 1),
        cmd("chapter", "chapter{}", 1),
        cmd("paragraph", "paragraph{}", 1),
        cmd("textbf", "textbf{}", 1),
        cmd("textit", "textit{}", 1),
        cmd("underline", "underline{}", 1),
        cmd("emph", "emph{}", 1),
        cmd("includegraphics", "includegraphics[width=0.8\\textwidth]{}", 1),
        cmd("label", "label{}", 1),
        cmd("ref", "ref{}", 1),
        cmd("eqref", "eqref{}", 1),
        cmd("cite", "cite{}", 1),
        cmd("footnote", "footnote{}", 1),
        cmd("caption", "caption{}", 1),
        cmd("item", "item ", 0),
        cmd("usepackage", "usepackage{}", 1),
        cmd("documentclass", "documentclass{}", 1),
        cmd("begin", "begin{}", 1),
        cmd("end", "end{}", 1),
        cmd("frac", "frac{}{}", 6),
        cmd("sqrt", "sqrt{}", 1),
        cmd("newcommand", "newcommand{\\}{}", 2),
        cmd("bibliography", "bibliography{}", 1),
        cmd("bibliographystyle", "bibliographystyle{}", 1),
        cmd("maketitle", "maketitle", 0),
        cmd("tableofcontents", "tableofcontents", 0),
        cmd("title", "title{}", 1),
        cmd("author", "author{}", 1),
        cmd("date", "date{}", 1)
    )

    // --- Math symbols (after "\" while inside math context, also merged into commands search) --
    val mathSymbols: List<Suggestion> = listOf(
        cmd("alpha", "alpha", 0), cmd("beta", "beta", 0), cmd("gamma", "gamma", 0),
        cmd("delta", "delta", 0), cmd("epsilon", "epsilon", 0), cmd("theta", "theta", 0),
        cmd("lambda", "lambda", 0), cmd("mu", "mu", 0), cmd("pi", "pi", 0),
        cmd("sigma", "sigma", 0), cmd("phi", "phi", 0), cmd("omega", "omega", 0),
        cmd("int", "int_{}^{}", 3), cmd("sum", "sum_{}^{}", 3), cmd("prod", "prod_{}^{}", 3),
        cmd("lim", "lim_{}", 1), cmd("infty", "infty", 0), cmd("partial", "partial", 0),
        cmd("nabla", "nabla", 0), cmd("cdot", "cdot", 0), cmd("times", "times", 0),
        cmd("leq", "leq", 0), cmd("geq", "geq", 0), cmd("neq", "neq", 0),
        cmd("approx", "approx", 0), cmd("rightarrow", "rightarrow", 0),
        cmd("Rightarrow", "Rightarrow", 0), cmd("in", "in", 0), cmd("subset", "subset", 0)
    )

    // --- Environments (suggested after "\begin{") -----------------------------
    val environments: List<Suggestion> = listOf(
        envSug("equation"), envSug("align"), envSug("figure"), envSug("table"),
        envSug("itemize"), envSug("enumerate"), envSug("tabular"), envSug("matrix"),
        envSug("pmatrix"), envSug("bmatrix"), envSug("center"), envSug("verbatim"),
        envSug("quote"), envSug("abstract"), envSug("thebibliography"), envSug("frame")
    )

    // --- Packages (suggested after "\usepackage{") -----------------------------
    val packages: List<Suggestion> = listOf(
        pkgSug("amsmath"), pkgSug("amssymb"), pkgSug("graphicx"), pkgSug("tikz"),
        pkgSug("geometry"), pkgSug("hyperref"), pkgSug("babel"), pkgSug("inputenc"),
        pkgSug("fontenc"), pkgSug("xcolor"), pkgSug("booktabs"), pkgSug("caption"),
        pkgSug("float"), pkgSug("listings"), pkgSug("biblatex")
    )

    private fun cmd(trigger: String, insert: String, offsetFromEnd: Int) =
        Suggestion(trigger, insert, insert.length - offsetFromEnd, "command")

    private fun envSug(name: String) = Suggestion(name, name, name.length, "environment")
    private fun pkgSug(name: String) = Suggestion(name, name, name.length, "package")

    fun allCommandLike(): List<Suggestion> = commands + mathSymbols
}
