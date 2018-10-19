package com.mhfs.voc.server

import com.mhfs.voc.VocabularyService.Companion.ILLEGAL_ARGUMENT_STATUS
import com.mhfs.voc.VocabularyService.Companion.ILLEGAL_STATE_STATUS

import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

@Provider
class IllegalStateMapper: ExceptionMapper<IllegalStateException> {
    override fun toResponse(exception: IllegalStateException): Response = Response.status(ILLEGAL_STATE_STATUS).build()
}

@Provider
class IllegalArgumentMapper: ExceptionMapper<IllegalArgumentException> {
    override fun toResponse(exception: IllegalArgumentException): Response = Response.status(ILLEGAL_ARGUMENT_STATUS).build()
}