package com.grumpus.jam

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion

class Quiver(val player: Player) : Entity(), IDrawable {
    var stateTime = 0f
    val anim : Animation<TextureRegion>

    init {
        val atlas = JamGame.assets["img/ui.atlas", TextureAtlas::class.java]
        anim = Animation(0.1f, atlas.findRegions("ui-arrow"), Animation.PlayMode.LOOP)

        add(DrawComponent(this, Layers.UI))
        JamGame.engine.addEntity(this)
    }

    fun pop() {
        val x = player.ammo * 16f
        val y = JamGame.height - 48f
        Effect(x, y, "ui-arrow-pop")
        player.ammo -= 1
    }

    fun reload() {
        player.ammo = player.maxAmmo
        // TODO: Sound effect here?  Animations?
    }

    override fun draw(delta: Float) {
        stateTime += delta
        val tr = anim.getKeyFrame(stateTime)
        val y = JamGame.height - tr.regionHeight - 16f

        for (i in 1..player.ammo) {
            var x = i * 16f
            x -= tr.regionWidth / 2
            JamGame.batch.draw(tr, x, y)
        }
    }
}

class AimTimer(val player: Player) : Entity(), IDrawable {
    var stateTime = 0f
    val anim : Animation<TextureRegion>

    init {
        val atlas = JamGame.assets["img/ui.atlas", TextureAtlas::class.java]
        anim = Animation(0.1f, atlas.findRegions("aim-timer"), Animation.PlayMode.LOOP)

        add(DrawComponent(this, Layers.UI))
        JamGame.engine.addEntity(this)
    }

    override fun draw(delta: Float) {
        stateTime += delta
        val scale = player.aimClock / player.aimTime
        if (player.currentState == PlayerState.AIM && scale < 0.75f) {
            val tr = anim.getKeyFrame(stateTime)
            val cx = player.body.centerX() - tr.regionWidth / 2
            val cy = player.body.centerY() - tr.regionHeight / 2
            JamGame.batch.draw(tr, cx, cy,
                    tr.regionWidth / 2f,
                    tr.regionHeight / 2f,
                    tr.regionWidth.toFloat(),
                    tr.regionHeight.toFloat(),
                    scale, scale, 0f)
        }
    }
}