package com.tongtongstudio.ami.domain.usecase

sealed class ImportException : Exception() {
    object InvalidFormat : ImportException()

    object EmptyFile : ImportException()
    data class Unknown(val error: Throwable) : ImportException()
}