package com.roleta.app.ui.component

/** "1 item", "3 items". */
fun pluralize(count: Int, noun: String): String = "$count $noun${if (count == 1) "" else "s"}"
