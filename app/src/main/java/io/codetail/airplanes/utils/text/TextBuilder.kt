package io.codetail.airplanes.utils.text

import android.text.SpannableStringBuilder

/**
 * created at 5/21/17
 *
 * @author Ozodrukh
 * @version 1.0
 */
class TextBuilder(text: CharSequence = "") : SpannableStringBuilder(text) {
    var start = 0

    override fun append(text: Char): TextBuilder {
        start += 1
        return apply { super.append(text) }
    }

    override fun append(text: CharSequence?): TextBuilder {
        start = length
        return apply { super.append(text) }
    }

    fun appendSpan(text: CharSequence, span: Any, flag: Int = 0): TextBuilder {
        start = length
        return append(text).span(span, start, length, flag)
    }

    fun span(span: Any, start: Int, length: Int = this.length, flag: Int = 0): TextBuilder {
        return apply { setSpan(span, start, length, flag) }
    }
}