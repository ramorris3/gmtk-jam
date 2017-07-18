package com.grumpus.jam

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion

abstract class Enemy(val room: Room, val player: Player, x: Float, y: Float, width: Int, height: Int,
                     idleAnimName: String, dieAnimName: String)
    : AnimatedEntity(x, y, width, height), IUpdatable, IDrawable {

    val idleAnim: Animation<TextureRegion>

    init {
        val atlas = JamGame.assets["img/enemies.atlas", TextureAtlas::class.java]
        idleAnim = Animation(0.07f, atlas.findRegions(idleAnimName), Animation.PlayMode.LOOP)
        setAnim(idleAnim)

        body.dxMax = 50f
        body.dyMax = 50f

        add(UpdateComponent(this))
        add(PhysicsComponent(room, body))
        add(DrawComponent(this))
        JamGame.engine.addEntity(this)

        room.addToGroup(body, Type.ENEMY)
    }

    abstract fun spawnPlatform()

    fun die() {
        spawnPlatform()
        add(DestroyComponent())
    }

    abstract fun move(delta: Float)

    override fun update(delta: Float) {
        val arrowBody = room.getOverlapping(body, Type.ARROW)
        if (arrowBody != null) {
            die()
            if (arrowBody.entity is Arrow) {
                arrowBody.entity.die()
            }
        } else {
            move(delta)
        }
    }

    override fun draw(delta: Float) {
        drawCurrentFrame(delta)
    }
}

// class Skull
class Skull(room: Room, player: Player, x: Float, y: Float):
        Enemy(room, player, x, y, 64, 64, "skull-idle", "skull-idle") {

    val accel = 100f

    override fun move(delta: Float) {
        // chase player directly
        body.ddx = if (player.body.x < body.x) -accel else accel
        body.ddy = if (player.body.y < body.y) -accel else accel
    }

    override fun spawnPlatform() {
        Platform(room, Type.SOLID, "platform-block", 5f, body.x, body.y, 64, 64)
    }
}