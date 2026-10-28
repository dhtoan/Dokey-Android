package com.dhtoan.dokey.ime

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

/** Simple line icons drawn in code, centred on (cx, cy) and fitting a square of side [s]. */
object Icons {
    private val path = Path()

    fun shift(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint, filled: Boolean, locked: Boolean) {
        path.reset()
        path.moveTo(cx, cy - s * 0.45f)
        path.lineTo(cx + s * 0.42f, cy)
        path.lineTo(cx + s * 0.2f, cy)
        path.lineTo(cx + s * 0.2f, cy + s * 0.3f)
        path.lineTo(cx - s * 0.2f, cy + s * 0.3f)
        path.lineTo(cx - s * 0.2f, cy)
        path.lineTo(cx - s * 0.42f, cy)
        path.close()
        val style = p.style
        p.style = if (filled) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
        c.drawPath(path, p)
        p.style = style
        if (locked) c.drawLine(cx - s * 0.2f, cy + s * 0.48f, cx + s * 0.2f, cy + s * 0.48f, p)
    }

    fun backspace(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        path.reset()
        path.moveTo(cx - s * 0.5f, cy)
        path.lineTo(cx - s * 0.22f, cy - s * 0.32f)
        path.lineTo(cx + s * 0.5f, cy - s * 0.32f)
        path.lineTo(cx + s * 0.5f, cy + s * 0.32f)
        path.lineTo(cx - s * 0.22f, cy + s * 0.32f)
        path.close()
        c.drawPath(path, p)
        val x = cx + s * 0.12f
        val d = s * 0.13f
        c.drawLine(x - d, cy - d, x + d, cy + d, p)
        c.drawLine(x - d, cy + d, x + d, cy - d, p)
    }

    fun enter(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        path.reset()
        path.moveTo(cx + s * 0.4f, cy - s * 0.35f)
        path.lineTo(cx + s * 0.4f, cy + s * 0.1f)
        path.lineTo(cx - s * 0.4f, cy + s * 0.1f)
        c.drawPath(path, p)
        c.drawLine(cx - s * 0.4f, cy + s * 0.1f, cx - s * 0.15f, cy - s * 0.15f, p)
        c.drawLine(cx - s * 0.4f, cy + s * 0.1f, cx - s * 0.15f, cy + s * 0.35f, p)
    }

    fun search(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        c.drawCircle(cx - s * 0.1f, cy - s * 0.1f, s * 0.27f, p)
        c.drawLine(cx + s * 0.1f, cy + s * 0.1f, cx + s * 0.38f, cy + s * 0.38f, p)
    }

    fun send(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        path.reset()
        path.moveTo(cx - s * 0.4f, cy - s * 0.36f)
        path.lineTo(cx + s * 0.44f, cy)
        path.lineTo(cx - s * 0.4f, cy + s * 0.36f)
        path.lineTo(cx - s * 0.26f, cy)
        path.close()
        c.drawPath(path, p)
    }

    fun arrowRight(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        c.drawLine(cx - s * 0.38f, cy, cx + s * 0.38f, cy, p)
        c.drawLine(cx + s * 0.38f, cy, cx + s * 0.1f, cy - s * 0.28f, p)
        c.drawLine(cx + s * 0.38f, cy, cx + s * 0.1f, cy + s * 0.28f, p)
    }

    fun check(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        path.reset()
        path.moveTo(cx - s * 0.36f, cy)
        path.lineTo(cx - s * 0.1f, cy + s * 0.26f)
        path.lineTo(cx + s * 0.38f, cy - s * 0.28f)
        c.drawPath(path, p)
    }

    fun gear(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        c.drawCircle(cx, cy, s * 0.14f, p)
        c.drawCircle(cx, cy, s * 0.29f, p)
        for (i in 0 until 8) {
            val a = Math.PI / 4 * i
            val ca = cos(a).toFloat()
            val sa = sin(a).toFloat()
            c.drawLine(cx + ca * s * 0.29f, cy + sa * s * 0.29f, cx + ca * s * 0.42f, cy + sa * s * 0.42f, p)
        }
    }

    fun chevronDown(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        path.reset()
        path.moveTo(cx - s * 0.32f, cy - s * 0.14f)
        path.lineTo(cx, cy + s * 0.18f)
        path.lineTo(cx + s * 0.32f, cy - s * 0.14f)
        c.drawPath(path, p)
    }

    /** A small keyboard pushed to one side. */
    fun oneHand(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        c.drawRoundRect(cx - s * 0.42f, cy - s * 0.3f, cx + s * 0.42f, cy + s * 0.3f, s * 0.08f, s * 0.08f, p)
        val style = p.style
        p.style = Paint.Style.FILL
        c.drawRoundRect(cx - s * 0.02f, cy - s * 0.16f, cx + s * 0.3f, cy + s * 0.16f, s * 0.05f, s * 0.05f, p)
        p.style = style
    }

    fun swapSides(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        c.drawLine(cx - s * 0.38f, cy, cx + s * 0.38f, cy, p)
        c.drawLine(cx - s * 0.38f, cy, cx - s * 0.18f, cy - s * 0.2f, p)
        c.drawLine(cx - s * 0.38f, cy, cx - s * 0.18f, cy + s * 0.2f, p)
        c.drawLine(cx + s * 0.38f, cy, cx + s * 0.18f, cy - s * 0.2f, p)
        c.drawLine(cx + s * 0.38f, cy, cx + s * 0.18f, cy + s * 0.2f, p)
    }

    fun expand(c: Canvas, cx: Float, cy: Float, s: Float, p: Paint) {
        val d = s * 0.34f
        c.drawLine(cx - d, cy + d, cx + d, cy - d, p)
        c.drawLine(cx + d, cy - d, cx + d * 0.25f, cy - d, p)
        c.drawLine(cx + d, cy - d, cx + d, cy - d * 0.25f, p)
        c.drawLine(cx - d, cy + d, cx - d * 0.25f, cy + d, p)
        c.drawLine(cx - d, cy + d, cx - d, cy + d * 0.25f, p)
    }
}
