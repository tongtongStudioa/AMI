package com.tongtongstudio.ami.adapter.simple

interface AttributeListener<T> {
    fun onItemClicked(attribute: T)
    fun onRemoveCrossClick(attribute: T)
}