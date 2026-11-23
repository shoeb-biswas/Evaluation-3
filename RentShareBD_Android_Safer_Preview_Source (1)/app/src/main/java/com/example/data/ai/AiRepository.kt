package com.example.data.ai

import android.util.Log
import com.example.domain.ParsedRentalIntent
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiRepository"

class AiRepository {

    private val financialCoachModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.4f
            }
        )
    }

    private val rentalSchema = Schema.obj(
        properties = mapOf(
            "area" to Schema.string(
                description = "Dhaka neighborhood name (Mirpur, Uttara, Dhanmondi, Mohammadpur, Bashundhara, Savar, Gazipur)",
                nullable = true
            ),
            "maxBudget" to Schema.integer(
                description = "Maximum monthly budget in BDT (integer)",
                nullable = true
            ),
            "roomType" to Schema.string(
                description = "Single Room, Shared Room, or Master Suite",
                nullable = true
            ),
            "nonSmoking" to Schema.boolean(
                description = "True if user requests non-smoking roommates or flat"
            ),
            "quietEnvironment" to Schema.boolean(
                description = "True if user requests quiet/study environment"
            )
        ),
        optionalProperties = listOf("area", "maxBudget", "roomType")
    )

    private val rentalParserModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = rentalSchema
                temperature = 0.1f
            }
        )
    }

    fun streamFinancialAdvice(
        groupName: String,
        currentMonthTotal: Int,
        previousMonthTotal: Int,
        predictedNextMonth: Int?,
        categoryBreakdown: Map<String, Int>,
        question: String
    ): Flow<String> = flow {
        val prompt = buildString {
            appendLine("You are the RentShare BD AI Financial Coach for shared flatmates in Dhaka, Bangladesh.")
            appendLine("Always cite currency in Bangladeshi Taka (৳ / BDT) and give practical, concise, actionable guidance.")
            appendLine()
            appendLine("Group: $groupName")
            appendLine("Current Month Total: ৳$currentMonthTotal")
            appendLine("Previous Month Total: ৳$previousMonthTotal")
            appendLine("Predicted Next Month: ${predictedNextMonth?.let { "৳$it" } ?: "Not enough history"}")
            appendLine("Category Breakdown:")
            categoryBreakdown.forEach { (cat, total) ->
                appendLine("- $cat: ৳$total")
            }
            appendLine()
            appendLine("User Question: $question")
        }

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            financialCoachModel.generateContentStream(prompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI Financial Coach returned an empty response.")
        }
    }.catch { e ->
        when (e) {
            is CancellationException -> throw e
            is TimeoutCancellationException -> {
                Log.e(TAG, "AI financial advice timed out", e)
                throw IllegalStateException("Request timed out after 60 seconds. Please try again.", e)
            }
            else -> {
                Log.e(TAG, "AI financial advice failed", e)
                throw e
            }
        }
    }

    /** General conversational helper; cannot see current listings or perform financial actions. */
    fun streamConciergeReply(
        question: String,
        recentMessages: List<Pair<String, String>>
    ): Flow<String> = flow {
        val prompt = buildString {
            appendLine("You are RentShare BD's warm, practical home-finding assistant in Bangladesh.")
            appendLine("Help with renting questions, roommate etiquette, budgeting, utility planning, safety, and app navigation.")
            appendLine("Be welcoming and concise. Answer in the language used by the user when practical.")
            appendLine("Never claim a listing is verified, available, or real unless independently confirmed by a live service.")
            appendLine("You have no live inventory access, cannot inspect private accounts, cannot transfer money or authorize wallet credit.")
            appendLine("For payment, legal, or identity-risk decisions, recommend confirmation with a regulated provider or qualified professional.")
            appendLine("Do not request banking PINs, OTPs, government ID numbers, or other secrets.")
            appendLine("Do not imply that RentShare BD has completed any payment or booking.")
            appendLine("Recent conversation:")
            recentMessages.takeLast(8).forEach { (speaker, message) ->
                appendLine("${speaker.take(12)}: ${message.take(700)}")
            }
            appendLine("User: ${question.take(1000)}")
            appendLine("Assistant:")
        }
        val buffer = StringBuilder()
        withTimeout(60_000L) {
            financialCoachModel.generateContentStream(prompt).collect { chunk ->
                chunk.text?.let { part ->
                    buffer.append(part)
                    emit(buffer.toString())
                }
            }
        }
        if (buffer.isEmpty()) throw IllegalStateException("The assistant returned an empty response.")
    }.catch { e ->
        if (e is CancellationException) throw e
        Log.e(TAG, "AI assistant request failed", e)
        throw e
    }

    suspend fun parseRentalQuery(query: String): ParsedRentalIntent {
        val prompt = """
            Extract structured rental search filters for Dhaka, Bangladesh from this user query:
            "$query"
            Valid areas: Mirpur, Uttara, Dhanmondi, Mohammadpur, Bashundhara, Savar, Gazipur.
            Valid roomTypes: Single Room, Shared Room, Master Suite.
            Convert amounts like "10k" or "10 thousand" to 10000.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                rentalParserModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("AI Rental Assistant returned an empty response.")
            val json = JSONObject(jsonText)
            val area = if (json.isNull("area")) null else json.optString("area").takeIf { it.isNotBlank() }
            val maxBudget = if (json.isNull("maxBudget")) null else json.optInt("maxBudget").takeIf { it > 0 }
            val roomType = if (json.isNull("roomType")) null else json.optString("roomType").takeIf { it.isNotBlank() }
            val nonSmoking = json.optBoolean("nonSmoking", false)
            val quietEnvironment = json.optBoolean("quietEnvironment", false)
            return ParsedRentalIntent(
                area = area,
                maxBudget = maxBudget,
                roomType = roomType,
                nonSmoking = nonSmoking,
                quietEnvironment = quietEnvironment
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI rental query timed out", e)
            throw IllegalStateException("AI Rental Assistant timed out after 60 seconds. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI rental query parsing failed", e)
            throw e
        }
    }
}
