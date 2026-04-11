package com.lsfStudio.lsfTB.ui.component.filter

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

class FilterNumber(
    private val value: Int,
    private val minValue: Int = Int.MIN_VALUE,
    private val maxValue: Int = Int.MAX_VALUE,
) : BaseFieldFilter(value.toString()) {

    override fun onFilter(
        inputTextFieldValue: TextFieldValue,
        lastTextFieldValue: TextFieldValue
    ): TextFieldValue {
        return filterInputNumber(inputTextFieldValue, lastTextFieldValue, minValue, maxValue)
    }

    private fun filterInputNumber(
        inputTextFieldValue: TextFieldValue,
        lastInputTextFieldValue: TextFieldValue,
        minValue: Int = Int.MIN_VALUE,
        maxValue: Int = Int.MAX_VALUE,
    ): TextFieldValue {
        val inputString = inputTextFieldValue.text
        lastInputTextFieldValue.text

        val newString = StringBuilder()
        val supportNegative = minValue < 0
        var isNegative = false

        // 只允许负号在首位，并且只允许一个负�?
        if (supportNegative && inputString.isNotEmpty() && inputString.first() == '-') {
            isNegative = true
            newString.append('-')
        }

        for ((i, c) in inputString.withIndex()) {
            if (i == 0 && isNegative) continue // 首字符已经处�?
            when (c) {
                in '0'..'9' -> {
                    newString.append(c)
                    // 检查是否超出范�?
                    val tempText = newString.toString()
                    // 只在不是单独 '-' 时做判断（因�?'-' toInt 会异常）
                    if (tempText != "-" && tempText.isNotEmpty()) {
                        try {
                            val tempValue = tempText.toInt()
                            if (tempValue !in minValue..maxValue) {
                                newString.deleteCharAt(newString.lastIndex)
                            }
                        } catch (e: NumberFormatException) {
                            // 超出int范围
                            newString.deleteCharAt(newString.lastIndex)
                        }
                    }
                }
                // 忽略其他字符（包括点号）
            }
        }

        val textRange: TextRange
        if (inputTextFieldValue.selection.collapsed) { // 表示的是光标范围
            if (inputTextFieldValue.selection.end != inputTextFieldValue.text.length) { // 光标没有指向末尾
                var newPosition = inputTextFieldValue.selection.end + (newString.length - inputString.length)
                if (newPosition < 0) {
                    newPosition = inputTextFieldValue.selection.end
                }
                textRange = TextRange(newPosition)
            } else { // 光标指向了末�?
                textRange = TextRange(newString.length)
            }
        } else {
            textRange = TextRange(newString.length)
        }

        return lastInputTextFieldValue.copy(
            text = newString.toString(),
            selection = textRange
        )
    }
}
