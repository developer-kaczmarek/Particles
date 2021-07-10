package io.github.kaczmarek.particles.utils.views.particles

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import io.github.kaczmarek.particles.R
import io.github.kaczmarek.particles.utils.views.common.viewScope
import kotlinx.coroutines.*
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class ParticlesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {
    private var shouldDrawWhenTouching = false
    private var touchX = 0F
    private var touchY = 0F
    private val particles = arrayListOf<Particle>()
    private var drawingJob: Job? = null
    private val path = Path()

    private val particlesPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        strokeWidth = 2F
        color = ContextCompat.getColor(context, R.color.colorParticle)
    }

    private val linesPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 2F
        color = ContextCompat.getColor(context, R.color.colorParticle)
    }

    init {
        holder?.addCallback(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y
            }
            MotionEvent.ACTION_DOWN -> shouldDrawWhenTouching = true
            else -> shouldDrawWhenTouching = false
        }
        return true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        drawingJob = viewScope.launch {
            initParticles()
            while (isActive) {
                val canvas = holder.lockCanvas() ?: continue

                try {
                    synchronized(holder) {
                        canvas.drawColor(
                            ContextCompat.getColor(context, R.color.colorBackground),
                            PorterDuff.Mode.SRC
                        )

                        particles.forEachIndexed { index, particle ->
                            if (index != 60) {
                                particle.x += particle.vx
                                particle.y += particle.vy
                                when {
                                    particle.x < 0 -> particle.x = width.toFloat()
                                    particle.x > width -> particle.x = 0F
                                }
                                when {
                                    particle.y < 0 -> particle.y = height.toFloat()
                                    particle.y > height -> particle.y = 0F
                                }
                            }

                            for (j in 0 until PARTICLES_COUNT) {
                                if (index != j) {
                                    canvas.drawLinesBetweenParticles(
                                        particles[index],
                                        particles[j],
                                        false
                                    )
                                }
                            }

                            particlesPaint.alpha = particle.alpha

                            canvas.drawCircle(
                                particle.x,
                                particle.y,
                                particle.radius,
                                particlesPaint
                            )
                        }

                        if (shouldDrawWhenTouching) {
                            canvas.drawParticlesWhenTouching()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, e.message.toString())
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        runBlocking {
            drawingJob?.cancelAndJoin()
        }
    }

    private fun initParticles() {
        particles.clear()
        for (i in 0 until PARTICLES_COUNT) {
            particles.add(
                Particle(
                    x = Random.nextInt(0, width).toFloat(),
                    y = Random.nextInt(0, height).toFloat(),
                    vx = Random.nextInt(-2, 2),
                    vy = Random.nextInt(-2, 2),
                    radius = Random.nextInt(MIN_RADIUS, MAX_RADIUS).toFloat(),
                    alpha = Random.nextInt(150, 255)
                )
            )
        }
    }

    private fun Canvas.drawLinesBetweenParticles(
        p1: Particle,
        p2: Particle,
        isManualParticle: Boolean
    ) {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        val dist = sqrt(dx * dx + dy * dy)
        val maxDist = if (isManualParticle) 440 else 220
        if (dist < maxDist) {
            path.moveTo(p1.x, p1.y)
            path.lineTo(p2.x, p2.y)
            val distRatio = (maxDist - dist) / maxDist

            linesPaint.alpha = (min(p1.alpha, p2.alpha) * distRatio / 2).toInt()
            this.drawPath(path, linesPaint)

            path.reset()
        }
    }

    private fun Canvas.drawParticlesWhenTouching() {
        val manualParticle = Particle(x = touchX, y = touchY)
        with(manualParticle) {
            particlesPaint.alpha = alpha
            drawCircle(x, y, radius, particlesPaint)
        }

        for (j in 0 until PARTICLES_COUNT) {
            drawLinesBetweenParticles(manualParticle, particles[j], true)
        }
    }

    companion object {
        const val PARTICLES_COUNT = 60
        const val MIN_RADIUS = 3
        const val MAX_RADIUS = 10
        const val TAG = "ParticleView"
    }
}