package com.grumpus.jam

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.grumpus.jam.*

enum class ArrowDir(val v: Vector2) {
    UP(Vector2(0f, 1f)),
    DOWN(Vector2(0f, -1f)),
    LEFT(Vector2(-1f, 0f)),
    RIGHT(Vector2(1f, 0f))
}

class Arrow(val room: Room, x: Float, y: Float, val dir: ArrowDir, val charge: Float) :
        AnimatedEntity(x, y, if (dir == ArrowDir.UP || dir == ArrowDir.DOWN) 6 else 48,
                if (dir == ArrowDir.UP || dir == ArrowDir.DOWN) 48 else 6),
        IUpdatable, IDrawable {

    val flyAnim: Animation<TextureRegion>
    val popAnim: Animation<TextureRegion>

    var dead = false

    init {
        body = Body(x, y, if (dir == ArrowDir.UP || dir == ArrowDir.DOWN) 6 else 48,
                if (dir == ArrowDir.UP || dir == ArrowDir.DOWN) 48 else 6, this)
        val atlas = JamGame.assets["img/player.atlas", TextureAtlas::class.java]
        var dirName: String
        when(dir) {
            ArrowDir.UP -> dirName = "-up"
            ArrowDir.DOWN -> dirName = "-down"
            ArrowDir.LEFT -> dirName = "-left"
            ArrowDir.RIGHT -> dirName = "-right"
        }
        flyAnim = Animation(0f, atlas.findRegions("arrow$dirName"))
        popAnim = Animation(0.06f, atlas.findRegions("arrow-pop$dirName"))
        setAnim(flyAnim)

        body.dxMax = 1700f
        body.dyMax = 1500f
        body.dx = dir.v.x * charge * body.dxMax
        body.dy = dir.v.y * charge * body.dyMax
        body.ddy = -400f

        room.addToGroup(body, Type.ARROW)

        add(UpdateComponent(this))
        add(DrawComponent(this))
        add(PhysicsComponent(room, body))
        JamGame.engine.addEntity(this)
    }

    fun die() {
        room.removeFromAnyGroup(body)
        dead = true
        setAnim(popAnim)
        body.dx = 0f
        body.dy = 0f
        body.ddx = 0f
        body.ddy = 0f
    }

    override fun update(delta: Float) {
        // handle enemy collisions in enemy class

        if (!dead) {
            // check for out of bounds, or collision with enemy or platform
            if (body.outOfBounds()) {
                add(DestroyComponent())
            } else if (room.overlaps(body, Type.SOLID)) {
                die()
            }
        }
    }

    override fun draw(delta: Float) {
        drawCurrentFrame(delta)
        if (dead && isAnimFinished()) {
            add(DestroyComponent())
        }
    }
}

