package com.grumpus.jam

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion

class Solid(val room: Room, val type: Type, val animKey: String,
            var time: Float, x: Float, y: Float, width: Int, height: Int) :
        Entity(),
        IUpdatable,
        IDrawable {

    val body = Body(x, y, width, height)
    val anim: Animation<TextureRegion>
    var stateTime = 0f

    init {
        // set up anim
        val atlas = JamGame.assets["img/platforms.atlas", TextureAtlas::class.java]
        anim = Animation(0.1f, atlas.findRegions(animKey), Animation.PlayMode.LOOP)

        // clamp to integer position
        body.x = Math.round(body.x).toFloat()
        body.y = Math.round(body.y).toFloat()

        // add to collision group
        room.addToGroup(body, type)

        // register with systems and engine
        add(UpdateComponent(this))
        add(DrawComponent(this, Layers.SOLIDS))
        JamGame.engine.addEntity(this)
    }

    override fun update(delta: Float) {
        time -= delta
        if (time <= 0) {
            destroy()
        }
    }

    override fun draw(delta: Float) {
        stateTime += delta

        // get key frame
        val tr = anim.getKeyFrame(stateTime)

        // center animation over body
        val x = body.centerX() - tr.regionWidth / 2
        val y = body.centerX() - tr.regionHeight / 2

        JamGame.batch.draw(tr, x, y)
    }

    fun destroy() {
        // flag for destroy, remove from room
        add(DestroyComponent())
        room.removeFromGroup(body, type)

        // TODO: create an animation effect and a sound?
    }
}
