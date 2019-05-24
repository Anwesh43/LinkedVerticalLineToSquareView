package com.anwesh.uiprojects.verticallinetosquareview

/**
 * Created by anweshmishra on 24/05/19.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.app.Activity
import android.content.Context
import android.graphics.RectF

val nodes : Int = 5
val lines : Int = 2
val squares : Int = 2
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#311B92")
val backColor : Int = Color.parseColor("#BDBDBD")
val delay : Long = 20

fun Int.inverse() : Float = 1f / this
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float {
    return mirrorValue(a, b) * dir * scGap
}
fun Int.sf() : Float = 1f - 2 * this

fun Canvas.drawSquare(size : Float, sc : Float, i : Int, paint : Paint) {
    for (j in 0..(squares - 1)) {
        save()
        translate(-size + size * i, 0f)
        scale(1f, 1f - 2 * j)
        drawRect(RectF(0f, 0f, size, size * sc.divideScale(j, lines)), paint)
        restore()
    }
}

fun Canvas.drawVerticalLineToSquare(sc1 : Float, sc2 : Float, size : Float, paint : Paint) {
    for (j in 0..(lines - 1)) {
        val sc2j : Float = sc2.divideScale(j, lines)
        save()
        rotate(90f * j.sf() * sc1.divideScale(j, lines))
        drawLine(0f, 0f, 0f, -size, paint)
        restore()
        drawSquare(size, sc2j, j, paint)
    }
}

fun Canvas.drawVLTSNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    save()
    translate(w / 2, gap * (i + 1))
    drawVerticalLineToSquare(sc1, sc2, size, paint)
    restore()
}

class VerticalLineToSquareView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, lines, lines * squares)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class VLTSNode(var i : Int, val state : State = State()) {

        private var next : VLTSNode? = null
        private var prev : VLTSNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = VLTSNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawVLTSNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : VLTSNode {
            var curr : VLTSNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class VerticalLineToSquare(var i : Int) {

        private val root : VLTSNode = VLTSNode(0)
        private var curr : VLTSNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : VerticalLineToSquareView) {

        private val animator : Animator = Animator(view)
        private var vlts : VerticalLineToSquare = VerticalLineToSquare(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            vlts.draw(canvas, paint)
            animator.animate {
                vlts.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            vlts.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : VerticalLineToSquareView {
            val view : VerticalLineToSquareView = VerticalLineToSquareView(activity)
            activity.setContentView(view)
            return view
        }
    }
}