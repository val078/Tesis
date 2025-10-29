package com.example.tesis.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EmailRepository private constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Función original (para el usuario actual)
    // ⭐ Enviar reporte semanal del usuario actual
    suspend fun sendWeeklyReportEmail(context: Context): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Simplemente redirigimos a la versión general
            sendWeeklyReportForUser(context, userId)

        } catch (e: Exception) {
            Log.e("EmailRepository", "❌ Error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ⭐ NUEVA FUNCIÓN: Enviar reporte de un usuario específico (para admin)
    suspend fun sendWeeklyReportForUser(context: Context, userId: String): Result<String> {
        return try {
            Log.d("EmailRepository", "📊 Generando reporte para userId: $userId")

            // 1️⃣ Obtener datos del usuario
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val userName = userDoc.getString("name") ?: "Usuario"
            val parentEmailString = userDoc.getString("parentEmail")
                ?: return Result.failure(Exception("Este usuario no tiene correo parental registrado"))

            Log.d("EmailRepository", "👤 Usuario: $userName")
            Log.d("EmailRepository", "📧 Email parental: $parentEmailString")

            val parentEmails = parentEmailString
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches() }
                .toTypedArray()

            if (parentEmails.isEmpty()) {
                return Result.failure(Exception("No hay correos parentales válidos"))
            }

            // 2️⃣ Obtener estadísticas
            val sevenDaysAgo = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -7)
            }.time

            val diarySnapshot = firestore.collection("diaryEntries")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("timestamp", Timestamp(sevenDaysAgo))
                .get()
                .await()

            val totalEntries = diarySnapshot.size()
            val entriesByDate = diarySnapshot.documents
                .groupBy { it.getString("date") ?: "Sin fecha" }
                .mapValues { it.value.size }
                .toSortedMap()

            val gamesSnapshot = firestore.collection("users")
                .document(userId)
                .collection("gameResults")
                .get()
                .await()

            val totalGames = gamesSnapshot.size()
            var totalScore = 0
            gamesSnapshot.documents.forEach { doc ->
                totalScore += (doc.getLong("score")?.toInt() ?: 0)
            }
            val averageScore = if (totalGames > 0) totalScore / totalGames else 0

            val aiLogsSnapshot = firestore.collection("aiLogs")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val aiRecommendations = aiLogsSnapshot.documents
                .mapNotNull { doc ->
                    val response = doc.getString("aiResponse") ?: return@mapNotNull null
                    val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: return@mapNotNull null
                    val dateFormat = SimpleDateFormat("dd/MM", Locale("es", "ES"))
                    val date = dateFormat.format(timestamp)
                    Triple(timestamp.time, date, response)
                }
                .sortedByDescending { it.first }
                .take(5)
                .map { Pair(it.second, it.third) }

            Log.d("EmailRepository", "📊 Stats: Entradas=$totalEntries, Juegos=$totalGames, IA=${aiRecommendations.size}")

            // 3️⃣ Crear HTML bonito
            val dateFormat = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES"))
            val today = dateFormat.format(Date())

            val reportHTML = createReportHTML(
                userName = userName,
                today = today,
                totalEntries = totalEntries,
                entriesByDate = entriesByDate,
                totalGames = totalGames,
                averageScore = averageScore,
                aiRecommendations = aiRecommendations
            )

            // 4️⃣ ✅ CONVERTIR HTML A PDF
            val pdfFile = createPDFFromHTML(
                context = context,
                userName = userName,
                htmlContent = reportHTML
            )

            Log.d("EmailRepository", "📄 PDF creado: ${pdfFile.absolutePath}")
            Log.d("EmailRepository", "📏 Tamaño: ${pdfFile.length()} bytes")

            // 5️⃣ Obtener URI del PDF
            val pdfUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            Log.d("EmailRepository", "🔗 URI: $pdfUri")

            // 6️⃣ ✅ CREAR INTENT CON PDF
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"  // ✅ Cambiar a PDF
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                putExtra(Intent.EXTRA_EMAIL, parentEmails)
                putExtra(Intent.EXTRA_SUBJECT, "📊 Reporte Semanal de $userName - $today")
                putExtra(
                    Intent.EXTRA_TEXT,
                    """
                Estimado/a padre/madre/tutor:
                
                Adjunto encontrará el reporte semanal de $userName en formato PDF.
                
                📊 Resumen:
                • Total de comidas registradas: $totalEntries
                • Juegos educativos completados: $totalGames
                • Puntuación promedio: $averageScore puntos
                
                El PDF adjunto contiene estadísticas detalladas con gráficos 
                y análisis del progreso de $userName.
                
                Saludos cordiales,
                Equipo NutriPro 🍎
                """.trimIndent()
                )
                putExtra(Intent.EXTRA_STREAM, pdfUri)

                // ✅ ClipData para permisos
                clipData = android.content.ClipData.newRawUri("", pdfUri)
            }

            Log.d("EmailRepository", "✅ Intent con PDF creado")

            // 7️⃣ Crear chooser
            val chooserIntent = Intent.createChooser(emailIntent, "Enviar reporte con:").apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            }

            try {
                context.startActivity(chooserIntent)
                Log.d("EmailRepository", "✅ Chooser lanzado exitosamente")
            } catch (e: Exception) {
                Log.e("EmailRepository", "❌ Error al abrir chooser: ${e.message}")
                return Result.failure(Exception("No se pudo abrir la aplicación de email"))
            }

            Result.success("Reporte PDF generado exitosamente para $userName")

        } catch (e: Exception) {
            Log.e("EmailRepository", "❌ Error: ${e.message}", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // Función createReportHTML (la que ya tienes)
    private fun createReportHTML(
        userName: String,
        today: String,
        totalEntries: Int,
        entriesByDate: Map<String, Int>,
        totalGames: Int,
        averageScore: Int,
        aiRecommendations: List<Pair<String, String>>
    ): String {
        // ... tu código HTML existente (déjalo como está) ...

        val daysListHTML = if (entriesByDate.isNotEmpty()) {
            entriesByDate.entries.joinToString("") { (date, count) ->
                """
                <div style="padding: 8px 0; border-bottom: 1px solid #eee;">
                    <span style="color: #666;">$date</span>
                    <span style="float: right; font-weight: bold; color: #4CAF50;">$count comidas</span>
                </div>
                """.trimIndent()
            }
        } else {
            """
            <div style="padding: 15px; text-align: center; color: #999;">
                No hay entradas registradas esta semana
            </div>
            """.trimIndent()
        }

        val aiRecommendationsHTML = if (aiRecommendations.isNotEmpty()) {
            val recommendationsList = aiRecommendations.joinToString("") { (date, response) ->
                """
                <div style="background: #fff; padding: 15px; margin: 10px 0; border-radius: 8px; border-left: 4px solid #2196F3; box-shadow: 0 1px 3px rgba(0,0,0,0.1);">
                    <div style="color: #2196F3; font-size: 12px; font-weight: bold; margin-bottom: 5px;">
                        📅 $date
                    </div>
                    <div style="color: #333; line-height: 1.5;">
                        $response
                    </div>
                </div>
                """.trimIndent()
            }

            """
            <div style="margin-bottom: 30px;">
                <h2 style="color: #2196F3; font-size: 20px; margin: 0 0 15px 0; padding-bottom: 10px; border-bottom: 3px solid #2196F3;">
                    🤖 Recomendaciones de la IA
                </h2>
                <div style="background: #f5f5f5; padding: 15px; border-radius: 10px;">
                    <p style="margin: 0 0 15px 0; color: #666; font-size: 14px;">
                        Estas son las últimas recomendaciones personalizadas que recibió $userName:
                    </p>
                    $recommendationsList
                </div>
            </div>
            """.trimIndent()
        } else {
            """
            <div style="margin-bottom: 30px;">
                <h2 style="color: #2196F3; font-size: 20px; margin: 0 0 15px 0; padding-bottom: 10px; border-bottom: 3px solid #2196F3;">
                    🤖 Recomendaciones de la IA
                </h2>
                <div style="background: #f5f5f5; padding: 20px; border-radius: 10px; text-align: center;">
                    <p style="margin: 0; color: #999;">
                        No hay recomendaciones de la IA esta semana
                    </p>
                </div>
            </div>
            """.trimIndent()
        }

        val evaluationMessage = when {
            totalEntries >= 15 -> "¡Excelente! $userName está muy comprometido/a con su alimentación. 🌟"
            totalEntries >= 7 -> "Buen trabajo. $userName está registrando sus comidas regularmente. 👍"
            totalEntries > 0 -> "$userName puede mejorar registrando más comidas durante la semana. 💪"
            else -> "$userName no ha registrado comidas esta semana. Anímal@ a usar la app. 📱"
        }

        return """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reporte Semanal</title>
</head>
<body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f5f5f5;">
    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff;">
        
        <!-- Header -->
        <div style="background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%); color: white; padding: 30px 20px; text-align: center;">
            <h1 style="margin: 0; font-size: 28px; font-weight: bold;">📊 Reporte Semanal</h1>
            <p style="margin: 10px 0 0 0; font-size: 18px; opacity: 0.9;">$userName</p>
        </div>

        <!-- Fecha -->
        <div style="background-color: #f9f9f9; padding: 15px 20px; border-bottom: 2px solid #eee;">
            <p style="margin: 0; color: #666; font-size: 14px;">
                📅 <strong>Fecha del reporte:</strong> $today
            </p>
            <p style="margin: 5px 0 0 0; color: #666; font-size: 14px;">
                📆 <strong>Periodo:</strong> Últimos 7 días
            </p>
        </div>

        <!-- Contenido Principal -->
        <div style="padding: 20px;">
            
            <!-- Sección: Diario Alimenticio -->
            <div style="margin-bottom: 30px;">
                <h2 style="color: #4CAF50; font-size: 20px; margin: 0 0 15px 0; padding-bottom: 10px; border-bottom: 3px solid #4CAF50;">
                    📝 Diario Alimenticio
                </h2>
                
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                    <div style="background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%); padding: 20px; border-radius: 10px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                        <div style="font-size: 32px; font-weight: bold; color: #2e7d32;">$totalEntries</div>
                        <div style="color: #558b2f; margin-top: 5px; font-size: 14px;">Entradas totales</div>
                    </div>
                    <div style="background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%); padding: 20px; border-radius: 10px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                        <div style="font-size: 32px; font-weight: bold; color: #1565c0;">${entriesByDate.size}</div>
                        <div style="color: #1976d2; margin-top: 5px; font-size: 14px;">Días registrados</div>
                    </div>
                </div>

                <div style="background: #fafafa; padding: 15px; border-radius: 10px; border: 1px solid #e0e0e0;">
                    <h3 style="margin: 0 0 15px 0; color: #333; font-size: 16px;">📊 Detalle por día:</h3>
                    $daysListHTML
                </div>
            </div>

            <!-- Sección: Juegos Educativos -->
            <div style="margin-bottom: 30px;">
                <h2 style="color: #FF9800; font-size: 20px; margin: 0 0 15px 0; padding-bottom: 10px; border-bottom: 3px solid #FF9800;">
                    🎮 Juegos Educativos
                </h2>
                
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px;">
                    <div style="background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%); padding: 20px; border-radius: 10px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                        <div style="font-size: 32px; font-weight: bold; color: #e65100;">$totalGames</div>
                        <div style="color: #ef6c00; margin-top: 5px; font-size: 14px;">Juegos completados</div>
                    </div>
                    <div style="background: linear-gradient(135deg, #f3e5f5 0%, #e1bee7 100%); padding: 20px; border-radius: 10px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                        <div style="font-size: 32px; font-weight: bold; color: #6a1b9a;">$averageScore</div>
                        <div style="color: #7b1fa2; margin-top: 5px; font-size: 14px;">Puntos promedio</div>
                    </div>
                </div>
            </div>
            
            $aiRecommendationsHTML

            <!-- Evaluación -->
            <div style="background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%); padding: 20px; border-radius: 10px; border-left: 5px solid #2196F3;">
                <h3 style="margin: 0 0 10px 0; color: #1565c0; font-size: 16px;">💬 Evaluación:</h3>
                <p style="margin: 0; color: #333; line-height: 1.6;">$evaluationMessage</p>
            </div>

        </div>

        <!-- Footer -->
        <div style="background-color: #f5f5f5; padding: 20px; text-align: center; border-top: 2px solid #e0e0e0;">
            <p style="margin: 0; color: #999; font-size: 12px;">
                Este reporte fue generado automáticamente por <strong>NutriKids</strong>
            </p>
            <p style="margin: 10px 0 0 0; color: #999; font-size: 12px;">
                📱 App de seguimiento nutricional para niños y adolescentes
            </p>
        </div>

    </div>
</body>
</html>
        """.trimIndent()
    }

    companion object {
        @Volatile
        private var INSTANCE: EmailRepository? = null

        fun getInstance(): EmailRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmailRepository().also { INSTANCE = it }
            }
        }
    }

    private fun createPDFFromHTML(
        context: Context,
        userName: String,
        htmlContent: String
    ): File {
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "reporte_${userName.replace(" ", "_")}_$timestamp.pdf"
        val pdfFile = File(reportsDir, fileName)

        try {
            // Convertir HTML a PDF usando iText html2pdf
            val converterProperties = com.itextpdf.html2pdf.ConverterProperties()

            // Configurar para soportar UTF-8 y CSS
            converterProperties.setCharset("UTF-8")

            com.itextpdf.html2pdf.HtmlConverter.convertToPdf(
                htmlContent,
                java.io.FileOutputStream(pdfFile),
                converterProperties
            )

            Log.d("EmailRepository", "✅ HTML convertido a PDF: ${pdfFile.length()} bytes")

        } catch (e: Exception) {
            Log.e("EmailRepository", "❌ Error convirtiendo HTML a PDF: ${e.message}", e)
            throw e
        }

        return pdfFile
    }
}