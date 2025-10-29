// ui/screens/MrPolloViewModel.kt
package com.example.tesis.data.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

// ✅ AGREGAR happinessLevel aquí
data class PolloState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val fedToday: Boolean = false,
    val lastFedDate: Date? = null,
    val happinessLevel: Int = 50, // ✅ NUEVO
    val todayArticle: NutritionArticle = getArticleForToday()
)

data class NutritionArticle(
    val title: String,
    val content: String,
    val emoji: String
)

class MrPolloViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _polloState = MutableStateFlow(PolloState())
    val polloState: StateFlow<PolloState> = _polloState

    companion object {
        private const val TAG = "MrPolloViewModel"
    }

    fun loadPolloState() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                Log.d(TAG, "🐥 Cargando estado de Mr. Pollo para userId: $userId")

                val doc = firestore.collection("users")
                    .document(userId)
                    .collection("mrPollo")
                    .document("state")
                    .get()
                    .await()

                if (doc.exists()) {
                    val currentStreak = doc.getLong("currentStreak")?.toInt() ?: 0
                    val longestStreak = doc.getLong("longestStreak")?.toInt() ?: 0
                    val lastFedTimestamp = doc.getTimestamp("lastFedDate")
                    val lastFedDate = lastFedTimestamp?.toDate()
                    val savedHappiness = doc.getLong("happinessLevel")?.toInt() ?: 50 // ✅ LEER

                    val fedToday = lastFedDate?.let { isToday(it) } ?: false

                    // ✅ Calcular felicidad
                    val happinessLevel = calculateHappiness(lastFedDate, fedToday, savedHappiness)

                    val updatedStreak = if (!fedToday && lastFedDate != null) {
                        if (isYesterday(lastFedDate)) {
                            currentStreak
                        } else {
                            0
                        }
                    } else {
                        currentStreak
                    }

                    _polloState.value = PolloState(
                        currentStreak = updatedStreak,
                        longestStreak = longestStreak,
                        fedToday = fedToday,
                        lastFedDate = lastFedDate,
                        happinessLevel = happinessLevel, // ✅ INCLUIR
                        todayArticle = getArticleForToday()
                    )

                    // ✅ Actualizar en Firebase si cambió
                    if (happinessLevel != savedHappiness) {
                        updateHappinessInFirebase(userId, happinessLevel)
                    }

                    Log.d(TAG, "✅ Estado cargado: Racha=$updatedStreak, Felicidad=$happinessLevel")
                } else {
                    Log.d(TAG, "📝 No hay estado previo, creando nuevo")
                    _polloState.value = PolloState(
                        happinessLevel = 50,
                        todayArticle = getArticleForToday()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al cargar estado: ${e.message}", e)
            }
        }
    }

    fun feedPollo() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                Log.d(TAG, "🍽️ Alimentando a Mr. Pollo")

                val currentState = _polloState.value
                val newStreak = currentState.currentStreak + 1
                val newLongestStreak = maxOf(newStreak, currentState.longestStreak)
                val newHappiness = 100 // ✅ Felicidad al máximo

                val updatedState = currentState.copy(
                    currentStreak = newStreak,
                    longestStreak = newLongestStreak,
                    fedToday = true,
                    lastFedDate = Date(),
                    happinessLevel = newHappiness // ✅ INCLUIR
                )

                firestore.collection("users")
                    .document(userId)
                    .collection("mrPollo")
                    .document("state")
                    .set(
                        mapOf(
                            "currentStreak" to newStreak,
                            "longestStreak" to newLongestStreak,
                            "lastFedDate" to Timestamp.now(),
                            "happinessLevel" to newHappiness, // ✅ GUARDAR
                            "updatedAt" to Timestamp.now()
                        )
                    )
                    .await()

                _polloState.value = updatedState

                Log.d(TAG, "✅ Mr. Pollo alimentado! Felicidad: $newHappiness")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al alimentar: ${e.message}", e)
            }
        }
    }

    // ✅ NUEVA FUNCIÓN
    private fun calculateHappiness(lastFedDate: Date?, fedToday: Boolean, savedHappiness: Int): Int {
        if (lastFedDate == null) return 50
        if (fedToday) return savedHappiness

        val calendar = Calendar.getInstance()
        val today = calendar.time
        val daysSinceLastFed = ((today.time - lastFedDate.time) / (1000 * 60 * 60 * 24)).toInt()

        return when {
            daysSinceLastFed == 0 -> savedHappiness
            daysSinceLastFed == 1 -> 50
            daysSinceLastFed == 2 -> 25
            else -> 10
        }.coerceIn(0, 100)
    }

    // ✅ NUEVA FUNCIÓN
    private suspend fun updateHappinessInFirebase(userId: String, happiness: Int) {
        try {
            firestore.collection("users")
                .document(userId)
                .collection("mrPollo")
                .document("state")
                .update("happinessLevel", happiness)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando felicidad: ${e.message}")
        }
    }

    private fun isToday(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val todayYear = calendar.get(Calendar.YEAR)

        calendar.time = date
        val dateDay = calendar.get(Calendar.DAY_OF_YEAR)
        val dateYear = calendar.get(Calendar.YEAR)

        return dateDay == today && dateYear == todayYear
    }

    private fun isYesterday(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.get(Calendar.DAY_OF_YEAR)
        val yesterdayYear = calendar.get(Calendar.YEAR)

        calendar.time = date
        val dateDay = calendar.get(Calendar.DAY_OF_YEAR)
        val dateYear = calendar.get(Calendar.YEAR)

        return dateDay == yesterday && dateYear == yesterdayYear
    }
}

// ✅ Artículos educativos rotativos por día del mes
fun getArticleForToday(): NutritionArticle {
    val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    val articles = listOf(
        NutritionArticle(
            title = "¿Qué son las Proteínas?",
            content = "Las proteínas son nutrientes esenciales que ayudan a construir y reparar los músculos, la piel y otros tejidos del cuerpo. Se encuentran en alimentos como el pollo, pescado, huevos, frijoles y lentejas. ¡Son como los bloques de construcción de tu cuerpo!",
            emoji = "💪"
        ),
        NutritionArticle(
            title = "Carbohidratos: Tu Fuente de Energía",
            content = "Los carbohidratos son la principal fuente de energía para tu cuerpo. Existen carbohidratos simples (azúcares) y complejos (cereales integrales, avena). Los complejos te dan energía duradera y son más saludables. ¡Piensa en ellos como la gasolina de tu cuerpo!",
            emoji = "⚡"
        ),
        NutritionArticle(
            title = "Grasas Saludables",
            content = "No todas las grasas son malas. Las grasas saludables del aguacate, nueces y pescado ayudan a tu cerebro y corazón. Evita las grasas trans de alimentos procesados. ¡Tu cerebro es 60% grasa, así que aliméntalo bien!",
            emoji = "🥑"
        ),
        NutritionArticle(
            title = "Vitaminas y Minerales",
            content = "Las vitaminas y minerales son micronutrientes que tu cuerpo necesita en pequeñas cantidades. Ayudan en todo: desde tener huesos fuertes (calcio) hasta combatir enfermedades (vitamina C). ¡Come un arcoíris de frutas y verduras!",
            emoji = "🌈"
        ),
        NutritionArticle(
            title = "La Importancia del Agua",
            content = "Tu cuerpo es 60% agua. El agua ayuda a transportar nutrientes, regular temperatura y eliminar toxinas. Debes tomar al menos 8 vasos al día. Si tu orina es amarillo claro, ¡estás bien hidratado!",
            emoji = "💧"
        ),
        NutritionArticle(
            title = "Fibra: Tu Aliada Digestiva",
            content = "La fibra ayuda a tu digestión y te mantiene lleno por más tiempo. Se encuentra en frutas, verduras, cereales integrales y legumbres. ¡Una dieta alta en fibra previene muchas enfermedades!",
            emoji = "🌾"
        ),
        NutritionArticle(
            title = "¿Qué son los Antioxidantes?",
            content = "Los antioxidantes protegen tus células del daño. Los encuentras en frutas coloridas como arándanos, fresas y naranjas, así como en verduras verdes. ¡Son como el escudo protector de tu cuerpo!",
            emoji = "🛡️"
        ),
        NutritionArticle(
            title = "Azúcar: Menos es Más",
            content = "El azúcar añadido en refrescos y dulces puede causar problemas de salud. La Organización Mundial de la Salud recomienda menos de 25 gramos al día. ¡Busca el azúcar natural de las frutas!",
            emoji = "🍬"
        ),
        NutritionArticle(
            title = "Sal: Encuentra el Balance",
            content = "La sal es necesaria, pero en exceso aumenta la presión arterial. La mayoría del sodio viene de alimentos procesados, no del salero. ¡Lee las etiquetas y cocina en casa!",
            emoji = "🧂"
        ),
        NutritionArticle(
            title = "Hierro: Energía en tu Sangre",
            content = "El hierro ayuda a transportar oxígeno en tu sangre. Lo encuentras en carnes rojas, espinacas y lentejas. La vitamina C ayuda a absorberlo mejor. ¡Sin hierro, te sentirías muy cansado!",
            emoji = "🩸"
        ),
        NutritionArticle(
            title = "Calcio: Huesos Fuertes",
            content = "El calcio construye huesos y dientes fuertes. Lo encuentras en leche, yogur, queso y vegetales de hoja verde. Tu cuerpo absorbe calcio mejor con vitamina D del sol. ¡Sal a jugar!",
            emoji = "🦴"
        ),
        NutritionArticle(
            title = "Omega-3: Cerebro Brillante",
            content = "Los ácidos grasos Omega-3 son esenciales para tu cerebro y corazón. Están en pescados como salmón, sardinas, y en nueces y semillas de chía. ¡Ayudan a que pienses mejor!",
            emoji = "🐟"
        ),
        NutritionArticle(
            title = "Vitamina C: Escudo Inmune",
            content = "La vitamina C fortalece tu sistema inmune y ayuda a sanar heridas. La encuentras en naranjas, fresas, kiwi y pimientos. ¡No se almacena, así que cómela todos los días!",
            emoji = "🍊"
        ),
        NutritionArticle(
            title = "Vitamina D: El Sol en tu Cuerpo",
            content = "La vitamina D ayuda a absorber calcio y fortalece tu sistema inmune. Tu cuerpo la produce con la luz solar. También está en pescados grasos y huevos. ¡15 minutos de sol al día son suficientes!",
            emoji = "☀️"
        ),
        NutritionArticle(
            title = "Proteína Vegetal",
            content = "No solo la carne tiene proteína. Los frijoles, lentejas, garbanzos, quinoa y soya son excelentes fuentes vegetales. ¡Combinando cereales con legumbres obtienes proteína completa!",
            emoji = "🌱"
        ),
        NutritionArticle(
            title = "El Plato Balanceado",
            content = "Un plato saludable tiene: 1/2 de verduras y frutas, 1/4 de proteínas, 1/4 de cereales integrales. ¡Esta proporción te da todos los nutrientes que necesitas!",
            emoji = "🍽️"
        ),
        NutritionArticle(
            title = "Snacks Saludables",
            content = "Los mejores snacks son naturales: frutas, nueces, yogur, vegetales con hummus. Evita snacks procesados con mucha sal y azúcar. ¡Prepara tus snacks con anticipación!",
            emoji = "🥜"
        ),
        NutritionArticle(
            title = "Leer Etiquetas Nutricionales",
            content = "La información nutricional te ayuda a elegir mejor. Fíjate en azúcares añadidos, sodio y grasas trans. Los ingredientes se listan por cantidad: ¡si el azúcar es primero, hay mucho!",
            emoji = "🏷️"
        ),
        NutritionArticle(
            title = "Masticar Bien tu Comida",
            content = "Masticar bien ayuda a la digestión y te hace comer más despacio, lo que ayuda a sentirte satisfecho. Intenta masticar cada bocado al menos 20 veces. ¡Tu estómago te lo agradecerá!",
            emoji = "😋"
        ),
        NutritionArticle(
            title = "Desayuno: La Comida Importante",
            content = "El desayuno enciende tu metabolismo después de dormir. Incluye proteína, carbohidratos complejos y fruta. ¡Un buen desayuno mejora tu concentración en la escuela!",
            emoji = "🍳"
        ),
        NutritionArticle(
            title = "Porciones Adecuadas",
            content = "El tamaño de las porciones importa tanto como lo que comes. Una porción de proteína es del tamaño de tu palma, de arroz del tamaño de tu puño. ¡Usa tu mano como guía!",
            emoji = "✋"
        ),
        NutritionArticle(
            title = "Comer Despacio",
            content = "Tu cerebro tarda 20 minutos en recibir la señal de que estás lleno. Comer despacio te ayuda a disfrutar más y evitar comer de más. ¡Deja el tenedor entre bocado y bocado!",
            emoji = "🐌"
        ),
        NutritionArticle(
            title = "Variedad en tu Dieta",
            content = "Ningún alimento tiene todos los nutrientes. Comer variado asegura que obtengas todo lo necesario. Intenta probar un alimento nuevo cada semana. ¡La variedad es la clave!",
            emoji = "🎨"
        ),
        NutritionArticle(
            title = "Alimentos Procesados vs Naturales",
            content = "Los alimentos procesados a menudo tienen más azúcar, sal y grasas malas. Los alimentos naturales como frutas, verduras y granos integrales son más nutritivos. ¡Entre menos etiqueta, mejor!",
            emoji = "🥗"
        ),
        NutritionArticle(
            title = "La Importancia del Sueño",
            content = "Dormir bien está conectado con la nutrición. La falta de sueño aumenta el hambre y los antojos de comida chatarra. ¡Duerme 8-10 horas para controlar tu apetito!",
            emoji = "😴"
        ),
        NutritionArticle(
            title = "Actividad Física y Nutrición",
            content = "La buena nutrición y el ejercicio van de la mano. Los alimentos te dan la energía para moverte, y el ejercicio ayuda a tu cuerpo a usar los nutrientes mejor. ¡Son el equipo perfecto!",
            emoji = "🏃"
        ),
        NutritionArticle(
            title = "Prebióticos y Probióticos",
            content = "Los probióticos son bacterias buenas en yogur y alimentos fermentados. Los prebióticos (en plátanos, ajo, cebolla) alimentan esas bacterias. ¡Cuida tu microbiota intestinal!",
            emoji = "🦠"
        ),
        NutritionArticle(
            title = "Escucha a tu Cuerpo",
            content = "Tu cuerpo sabe lo que necesita. Come cuando tengas hambre, para cuando estés satisfecho. Aprende a diferenciar hambre real de antojos emocionales. ¡Confía en tus señales internas!",
            emoji = "🧘"
        ),
        NutritionArticle(
            title = "Cocinar en Casa",
            content = "Cocinar en casa te da control sobre ingredientes y porciones. Es más saludable y económico que comer fuera. ¡Aprende recetas simples y diviértete cocinando!",
            emoji = "👨‍🍳"
        ),
        NutritionArticle(
            title = "Súper Alimentos",
            content = "Aunque no existe un alimento mágico, algunos destacan: arándanos (antioxidantes), salmón (omega-3), espinacas (hierro y vitaminas), avena (fibra). ¡Inclúyelos regularmente!",
            emoji = "⭐"
        ),
        NutritionArticle(
            title = "Hidratación en Deportes",
            content = "Cuando haces ejercicio pierdes agua por el sudor. Toma agua antes, durante y después. Si el ejercicio dura más de una hora, considera bebidas deportivas. ¡Mantente hidratado!",
            emoji = "⚽"
        )
    )

    // Rotar artículos según el día del mes (1-31)
    val index = (dayOfMonth - 1) % articles.size
    return articles[index]
}