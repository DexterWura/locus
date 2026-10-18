package com.whereami.locus.auth

data class SecurityQuestion(
    val id: String,
    val prompt: String
)

object SecurityQuestions {
    val all: List<SecurityQuestion> = listOf(
        SecurityQuestion("pet", "What was the name of your first pet?"),
        SecurityQuestion("school", "What was the name of your elementary school?"),
        SecurityQuestion("city", "In what city were you born?"),
        SecurityQuestion("street", "What street did you grow up on?"),
        SecurityQuestion("food", "What is your favorite childhood food?"),
        SecurityQuestion("friend", "What was your childhood best friend’s first name?"),
        SecurityQuestion("teacher", "What was your favorite teacher’s last name?"),
        SecurityQuestion("book", "What was the first book you loved?")
    )

    fun byId(id: String): SecurityQuestion? = all.find { it.id == id }
}
