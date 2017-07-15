package com.grumpus.jam

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion

/**
 * Use this to create classes for staging specific components and features
 */

class MovementStaging(room: Room, x: Float, y: Float): Entity(), IDrawable {
    val body = Body(x, y, 64, 64)
    val anim: Animation<TextureRegion>
    var stateTime = 0f

    init {
        val atlas = JamGame.assets["img/platforms.atlas", TextureAtlas::class.java]
        anim = Animation(0.1f, atlas.findRegions("platform-block"), Animation.PlayMode.LOOP)

        room.addToGroup(body, Type.SOLID)

        add(DrawComponent(this, Layers.SOLIDS))
        JamGame.engine.addEntity(this)
    }

    override fun draw(delta: Float) {
        stateTime += delta
        val tr = anim.getKeyFrame(stateTime)
        JamGame.batch.draw(tr, body.x, body.y)
    }



}
