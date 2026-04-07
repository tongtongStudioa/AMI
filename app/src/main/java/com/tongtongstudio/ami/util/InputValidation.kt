package com.tongtongstudio.ami.util

import androidx.core.text.isDigitsOnly

/**
 * Class for entries validation in edit/add fragment.
 */
class InputValidation {

    companion object {
        fun <T> isValidDecimalNum(input: T?): Boolean {
            val incompleteDecimalRegex = Regex("^\\d+\\.$")
            return input.toString() != "" && input.toString() != "null" && input.toString() != "." && !input.toString()
                .matches(incompleteDecimalRegex)
        }

        fun <T> isValidDigit(input: T?): Boolean {
            return input.toString().isNotBlank() && input.toString() != "null" && input.toString()
                .isDigitsOnly()
        }

        fun <T> isValidText(text: T?): Boolean {
            return (text?.toString() != "" && text.toString() != "null")
        }

        fun <T> isNotNull(input: T?): Boolean {
            return (input != null)
        }
    }
}