package com.grillpoet.animy.domain.model

import java.util.concurrent.atomic.AtomicLong

private val idCounter = AtomicLong(System.currentTimeMillis() * 1000)

/** 单调递增的唯一 ID。线程安全。 */
fun newId(): Long = idCounter.incrementAndGet()
