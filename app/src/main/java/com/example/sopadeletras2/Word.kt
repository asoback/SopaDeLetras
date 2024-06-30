package com.example.sopadeletras2

data class Word(
    val text: String,
    val altText: String,
    var isCrossedOut: Boolean = false,
    var wasPlaced: Boolean = false
)
