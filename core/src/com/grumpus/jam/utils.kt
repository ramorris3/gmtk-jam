package com.grumpus.jam

fun approach(start: Float, end: Float, inc: Float): Float {
    if (start < end) {
        return Math.min(end, start + inc)
    } else if (start > end) {
        return Math.max(end, start - inc)
    } else {
        return end
    }
}

fun clamp(n: Float, lower: Float, upper: Float): Float {
    return Math.max(Math.min(upper, n), lower)
}