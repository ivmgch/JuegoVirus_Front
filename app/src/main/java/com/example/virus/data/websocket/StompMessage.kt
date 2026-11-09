package com.example.virus.data.websocket

data class StompMessage(
    val command: String,
    val headers: Map<String, String> = emptyMap(),
    val payload: String = ""
) {
    fun toFrame(): String {
        val builder = StringBuilder()
        builder.append(command).append("\n")
        headers.forEach { (k, v) ->
            builder.append(k).append(":").append(v).append("\n")
        }
        builder.append("\n")
        builder.append(payload)
        builder.append('\u0000')
        return builder.toString()
    }

    companion object {
        fun parse(raw: String): StompMessage? {
            val trimmed = raw.trimEnd('\u0000').trim()
            if (trimmed.isEmpty()) return null

            val lines = trimmed.lines()
            if (lines.isEmpty()) return null

            val command = lines[0].trim()
            val headers = mutableMapOf<String, String>()
            var payloadIndex = 1

            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isEmpty()) {
                    payloadIndex = i + 1
                    break
                }
                val split = line.indexOf(':')
                if (split != -1) {
                    val key = line.substring(0, split).trim()
                    val value = line.substring(split + 1).trim()
                    headers[key] = value
                }
            }

            val payload = if (payloadIndex < lines.size) {
                lines.subList(payloadIndex, lines.size).joinToString("\n")
            } else {
                ""
            }

            return StompMessage(command, headers, payload)
        }
    }
}
