package app.qrcode.sharer.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight

/**
 * 简单的 Markdown 文本组件
 * 支持: **粗体**, ### 标题, - 列表项
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = parseSimpleMarkdown(text),
        modifier = modifier,
        style = style,
        color = color
    )
}

private fun parseSimpleMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.lines()

        lines.forEachIndexed { index, line ->
            when {
                line.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(line.removePrefix("### "))
                    }
                }

                line.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(line.removePrefix("## "))
                    }
                }

                line.trimStart().startsWith("- ") -> {
                    val indent = line.takeWhile { it == ' ' || it == '\t' }
                    append(indent)
                    append("• ")
                    appendInlineFormatted(line.trimStart().removePrefix("- "))
                }

                else -> {
                    appendInlineFormatted(line)
                }
            }
            if (index < lines.lastIndex) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.appendInlineFormatted(text: String) {
    var remaining = text
    val boldRegex = """\*\*(.+?)\*\*""".toRegex()

    while (remaining.isNotEmpty()) {
        val match = boldRegex.find(remaining)
        if (match != null) {
            append(remaining.substring(0, match.range.first))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            remaining = remaining.substring(match.range.last + 1)
        } else {
            append(remaining)
            break
        }
    }
}
