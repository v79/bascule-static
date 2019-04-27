package org.liamjd.bascule.flexmark.hyde

import com.vladsch.flexmark.ast.util.Parsing
import java.util.regex.Pattern

class HydeTagParsing {
	val myParsing: Parsing
	val OPEN_MACROTAG: String
	val MACRO_OPEN: Pattern
	val MACRO_TAG: Pattern

	constructor(parsing: Parsing) {
		this.myParsing = parsing
		this.OPEN_MACROTAG = "\\{%\\s+(" + myParsing.TAGNAME + ")(?:\\s+.+)?\\s+%\\}"
		this.MACRO_OPEN = Pattern.compile("^$OPEN_MACROTAG\\s*$", Pattern.CASE_INSENSITIVE)
		this.MACRO_TAG = Pattern.compile(OPEN_MACROTAG)
	}
}
