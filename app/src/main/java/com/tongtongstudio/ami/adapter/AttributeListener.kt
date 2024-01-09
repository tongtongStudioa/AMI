package com.tongtongstudio.ami.adapter

interface AttributeListener<T> {
    fun onItemClicked(attribute: T)
    fun onRemoveCrossClick(attribute: T)
}