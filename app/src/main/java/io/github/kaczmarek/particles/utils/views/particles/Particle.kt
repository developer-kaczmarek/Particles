package io.github.kaczmarek.particles.utils.views.particles

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Int = 0,
    var vy: Int = 0,
    var radius: Float = 10F,
    var alpha: Int = 255
)