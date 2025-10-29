// ui/components/VerificationCodeInput.kt
package com.example.tesis.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tesis.ui.theme.PrimaryOrange
import com.example.tesis.ui.theme.PinkOrange
import androidx.compose.ui.draw.alpha

@Composable
fun VerificationCodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    length: Int = 7,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Campo de texto invisible que maneja la entrada
        BasicTextField(
            value = code,
            onValueChange = { newValue ->
                if (newValue.length <= length && newValue.all { it.isDigit() }) {
                    onCodeChange(newValue)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { innerTextField ->
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    val codeList = code.toList()

                    repeat(length) { index ->
                        val char = if (index < codeList.size) codeList[index].toString() else ""
                        val isCurrentFieldFocused = isFocused && (index == code.length || (code.length == length && index == length - 1))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .border(
                                    width = if (isCurrentFieldFocused) 2.5.dp else 2.dp,
                                    color = if (enabled) {
                                        when {
                                            isCurrentFieldFocused -> PrimaryOrange
                                            code.length > index -> PrimaryOrange
                                            else -> PinkOrange.copy(alpha = 0.7f)
                                        }
                                    } else {
                                        Color.Gray.copy(alpha = 0.5f)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                style = TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontSize = 18.sp,
                                    lineHeight = 24.sp
                                ),
                                color = if (enabled) MaterialTheme.colorScheme.onBackground else Color.Gray
                            )
                        }

                        // Espacio entre campos (más pequeño)
                        if (index < length - 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }

                // Campo invisible que maneja la entrada real
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .alpha(0f) // Invisible pero funcional
                ) {
                    innerTextField()
                }
            }
        )
    }
}