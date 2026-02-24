package com.example.qrscanfinalv2

import kotlin.text.contains
import kotlin.text.equals
import kotlin.text.isEmpty
import kotlin.text.lowercase
import kotlin.text.startsWith
import kotlin.text.trim

object HeritageAIEngine {

    // ---- Types of questions (simple NLP) ----
    private enum class QType { WHAT, WHY, WHEN, WHO, HOW, FUNFACT, OTHER }

    private fun detectType(q: String): QType {
        val s = q.lowercase()

        return when {
            s.startsWith("why") || s.contains(" why ") -> QType.WHY
            s.startsWith("what") || s.contains(" what ") -> QType.WHAT
            s.startsWith("when") || s.contains(" when ") -> QType.WHEN
            s.startsWith("who") || s.contains(" who ") -> QType.WHO
            s.startsWith("how") || s.contains(" how ") -> QType.HOW
            s.contains("fun") || s.contains("fact") || s.contains("interesting") -> QType.FUNFACT
            else -> QType.OTHER
        }
    }

    // ---- Public API: generate answer using context ----
    fun answer(zone: String, place: String, question: String): String {
        val q = question.trim()
        if (q.isEmpty()) return "Type a question first."

        val type = detectType(q)
        val z = zone.trim()
        val p = place.trim()

        // 1) Sigiriya knowledge
        if (z.equals("Sigiriya", true)) {
            return answerSigiriya(p, type, q)
        }

        // 2) Default fallback for other zones
        return "You are at $p in $z. Ask about history, purpose, architecture, or fun facts. (Offline Heritage AI Engine)"
    }

    // ---- Sigiriya answers by place ----
    private fun answerSigiriya(place: String, type: QType, q: String): String {
        val p = place.lowercase()

        return when {
            p.contains("mirror") -> mirrorWall(type)
            p.contains("lion") || p.contains("gate") -> lionGate(type)
            p.contains("water") -> waterGardens(type)
            p.contains("museum") -> museum(type)
            p.contains("summit") -> summit(type)
            p.contains("boulder") -> boulderGardens(type)
            p.contains("cobra") || p.contains("cave") -> cobraHoodCave(type)
            else -> "This place is in Sigiriya. Ask: why it is important, what it is, how it was built, or a fun fact."
        }
    }

    // ---- Place content ----
    private fun mirrorWall(type: QType): String = when (type) {
        QType.WHAT -> "The Mirror Wall is a polished wall at Sigiriya. Long ago it was so smooth it reflected the king and visitors like a mirror."
        QType.WHY -> "The Mirror Wall is important because it contains ancient visitor poems and graffiti. These writings are valuable evidence of language and culture from that period."
        QType.HOW -> "It was finished with fine plaster and polished to create a reflective surface. The high-quality finish shows advanced craftsmanship."
        QType.FUNFACT -> "Some of the old poems on the Mirror Wall are among Sri Lanka’s most famous historic writings."
        else -> "The Mirror Wall is a polished historic wall with ancient visitor writings. Ask 'Why is it important?' or 'What is Mirror Wall?'"
    }

    private fun lionGate(type: QType): String = when (type) {
        QType.WHAT -> "The Lion Gate is the main entrance area leading to the summit of Sigiriya. It originally had a large lion structure."
        QType.WHY -> "It symbolizes royal power and served as a dramatic entrance to the fortress palace. Today, only the lion paws remain."
        QType.WHEN -> "It is linked to the construction period of King Kashyapa’s fortress at Sigiriya (commonly placed in the 5th century CE)."
        QType.FUNFACT -> "Visitors climb between the giant lion paws to reach the upper levels—one of Sigiriya’s most iconic sights."
        else -> "The Lion Gate was a grand entrance with a lion structure. Ask 'What is Lion Gate?' or 'Why is it important?'"
    }

    private fun waterGardens(type: QType): String = when (type) {
        QType.WHAT -> "The Water Gardens are landscaped gardens with pools, fountains, and symmetrical layouts at the base of Sigiriya."
        QType.HOW -> "They used clever hydraulic engineering, including channels and pressure, to feed fountains and pools."
        QType.WHY -> "They show advanced planning and engineering, and supported the palace environment with beauty and water management."
        QType.FUNFACT -> "Some fountains can still work during the rainy season because the ancient water system is still functional."
        else -> "The Water Gardens show ancient landscaping and water engineering. Ask 'How did it work?' or 'Why is it important?'"
    }

    private fun museum(type: QType): String = when (type) {
        QType.WHAT -> "The Sigiriya Museum presents artifacts, models, and information about the history, archaeology, and engineering of Sigiriya."
        QType.WHY -> "It helps visitors understand the site with exhibits and reconstructions, making the heritage easier to learn."
        QType.FUNFACT -> "Museums often include scaled models of Sigiriya to explain the full layout clearly."
        else -> "The museum gives context about Sigiriya’s history and archaeology. Ask 'What can I learn here?'"
    }

    private fun summit(type: QType): String = when (type) {
        QType.WHAT -> "The Summit is the top of Sigiriya rock where the palace and fortress areas were located."
        QType.WHY -> "It offered security, visibility, and status—an ideal location for a royal fortress."
        QType.FUNFACT -> "From the summit you can see far across the surrounding plains—one reason the location was strategically powerful."
        else -> "The summit is the top fortress area. Ask 'Why build a palace on top?'"
    }

    private fun boulderGardens(type: QType): String = when (type) {
        QType.WHAT -> "The Boulder Gardens are areas with large natural rocks, pathways, and structures integrated into the landscape."
        QType.WHY -> "They combine nature with design—some boulders were used for shelters and defensive advantage."
        QType.FUNFACT -> "Some boulders have drip-ledges and ancient modifications, showing how people adapted the natural rock."
        else -> "The Boulder Gardens integrate huge rocks into the site design. Ask for a fun fact!"
    }

    private fun cobraHoodCave(type: QType): String = when (type) {
        QType.WHAT -> "Cobra Hood Cave is a rock shelter named for its shape, used historically as a shelter/monastic space."
        QType.WHY -> "Rock shelters like this show early habitation and religious activity around the Sigiriya area."
        QType.FUNFACT -> "Many caves in Sri Lanka have drip-ledges carved to stop rainwater entering."
        else -> "Cobra Hood Cave is a named rock shelter. Ask 'What is it?' or 'Why is it important?'"
    }
}
