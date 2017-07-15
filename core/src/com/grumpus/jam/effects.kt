package com.grumpus.jam

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion

class Effect(var x: Float, var y: Float, animKey: String) : Entity(), IDrawable {
    val anim: Animation<TextureRegion>
    var stateTime = 0f
    init {
        val atlas = JamGame.assets["img/effects.atlas", TextureAtlas::class.java]
        anim = Animation(0.06f, atlas.findRegions(animKey), Animation.PlayMode.NORMAL)
        add(DrawComponent(this, Layers.EFFECTS))
        JamGame.engine.addEntity(this)
    }

    override fun draw(delta: Float) {
        stateTime += delta
        if (anim.isAnimationFinished(stateTime)) {
            add(DestroyComponent())
        } else {
            val tr = anim.getKeyFrame(stateTime)
            // center over x and y params
            val cx = x - tr.regionWidth / 2
            val cy = y - tr.regionHeight / 2

            JamGame.batch.draw(tr, cx, cy)
        }
    }
}