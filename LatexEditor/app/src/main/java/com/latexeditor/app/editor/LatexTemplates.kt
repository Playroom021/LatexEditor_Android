package com.latexeditor.app.editor

object LatexTemplates {

    val blank = """
        |\documentclass{article}
        |\usepackage[utf8]{inputenc}
        |
        |\begin{document}
        |
        |\end{document}
        |""".trimMargin()

    val article = """
        |\documentclass[11pt]{article}
        |\usepackage[utf8]{inputenc}
        |\usepackage{amsmath, amssymb}
        |\usepackage{graphicx}
        |
        |\title{Untitled Document}
        |\author{Your Name}
        |\date{\today}
        |
        |\begin{document}
        |\maketitle
        |
        |\section{Introduction}
        |Start writing here.
        |
        |\end{document}
        |""".trimMargin()

    val report = """
        |\documentclass[12pt]{report}
        |\usepackage[utf8]{inputenc}
        |\usepackage{amsmath}
        |\usepackage{graphicx}
        |
        |\title{Report Title}
        |\author{Your Name}
        |\date{\today}
        |
        |\begin{document}
        |\maketitle
        |\tableofcontents
        |
        |\chapter{Introduction}
        |
        |\end{document}
        |""".trimMargin()

    val resume = """
        |\documentclass[11pt]{article}
        |\usepackage[utf8]{inputenc}
        |\usepackage[margin=0.8in]{geometry}
        |\pagestyle{empty}
        |
        |\begin{document}
        |\begin{center}
        |    {\LARGE \textbf{Your Name}} \\
        |    email@example.com \ $\cdot$ \ (555) 555-5555 \ $\cdot$ \ City, Country
        |\end{center}
        |
        |\section*{Education}
        |University Name \hfill City, Country \\
        |Degree, Major \hfill Month Year -- Month Year
        |
        |\section*{Experience}
        |\textbf{Job Title} -- Company \hfill Month Year -- Month Year
        |\begin{itemize}
        |    \item Accomplishment or responsibility
        |\end{itemize}
        |
        |\section*{Skills}
        |List your skills here.
        |
        |\end{document}
        |""".trimMargin()

    val researchPaper = """
        |\documentclass[11pt]{article}
        |\usepackage[utf8]{inputenc}
        |\usepackage{amsmath, amssymb}
        |\usepackage{graphicx}
        |\usepackage[backend=biber]{biblatex}
        |\addbibresource{references.bib}
        |
        |\title{Paper Title}
        |\author{Author Name}
        |\date{\today}
        |
        |\begin{document}
        |\maketitle
        |
        |\begin{abstract}
        |Write your abstract here.
        |\end{abstract}
        |
        |\section{Introduction}
        |
        |\section{Related Work}
        |
        |\section{Methodology}
        |
        |\section{Results}
        |
        |\section{Conclusion}
        |
        |\printbibliography
        |\end{document}
        |""".trimMargin()

    val letter = """
        |\documentclass{letter}
        |\usepackage[utf8]{inputenc}
        |\signature{Your Name}
        |\address{Your Address \\ City, Country}
        |
        |\begin{document}
        |\begin{letter}{Recipient Name \\ Recipient Address}
        |\opening{Dear Sir or Madam,}
        |
        |Write your letter content here.
        |
        |\closing{Sincerely,}
        |\end{letter}
        |\end{document}
        |""".trimMargin()

    val beamer = """
        |\documentclass{beamer}
        |\usetheme{Madrid}
        |
        |\title{Presentation Title}
        |\author{Your Name}
        |\date{\today}
        |
        |\begin{document}
        |
        |\frame{\titlepage}
        |
        |\begin{frame}{Outline}
        |    \tableofcontents
        |\end{frame}
        |
        |\begin{frame}{First Slide}
        |    Content goes here.
        |\end{frame}
        |
        |\end{document}
        |""".trimMargin()

    val assignment = """
        |\documentclass[12pt]{article}
        |\usepackage[utf8]{inputenc}
        |\usepackage{amsmath, amssymb}
        |
        |\title{Assignment Title}
        |\author{Your Name}
        |\date{\today}
        |
        |\begin{document}
        |\maketitle
        |
        |\section*{Problem 1}
        |
        |\section*{Problem 2}
        |
        |\end{document}
        |""".trimMargin()

    val book = """
        |\documentclass[11pt]{book}
        |\usepackage[utf8]{inputenc}
        |
        |\title{Book Title}
        |\author{Your Name}
        |\date{\today}
        |
        |\begin{document}
        |\frontmatter
        |\maketitle
        |\tableofcontents
        |
        |\mainmatter
        |\chapter{First Chapter}
        |
        |\end{document}
        |""".trimMargin()

    fun byId(id: String): String = when (id) {
        "article" -> article
        "report" -> report
        "resume" -> resume
        "research_paper" -> researchPaper
        "letter" -> letter
        "beamer" -> beamer
        "assignment" -> assignment
        "book" -> book
        else -> blank
    }

    val catalog = listOf(
        "blank" to "Blank Document",
        "article" to "Article",
        "report" to "Report",
        "resume" to "Resume / CV",
        "research_paper" to "Research Paper",
        "assignment" to "Assignment",
        "book" to "Book",
        "letter" to "Letter",
        "beamer" to "Presentation (Beamer)"
    )
}
