package com.example.sopadeletras

data class Word(
    val text: String,
    val altText: String,
    var isCrossedOut: Boolean = false
)
