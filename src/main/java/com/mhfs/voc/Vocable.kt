package com.mhfs.voc

import java.util.*

class Vocable private constructor(val associatedWords: List<Word>, val nextDue: Date?, val active: Boolean) {
    val associatedData: Map<String, String> = HashMap()
}