package com.grumpus.jam

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion

/**
 * Use this to create classes for staging specific components and features
 */

class Staging : IUpdatable, IDrawable, Entity() {
    private val atlas = JamGame.assets["img/enemies.atlas", TextureAtlas::class.java]
    private val idleAnim = Animation<TextureRegion>(
            0.1f,
            atlas.findRegions("skull-idle"),
            Animation.PlayMode.LOOP)
    private val dieAnim = Animation<TextureRegion>(
            0.04f,
            atlas.findRegions("enemy-die"))
    private var stateTime = 0f
    private var dead = false

    private var x = JamGame.width / 2 - 16f
    private var y = JamGame.height / 2 - 16f
    private var spd = 200f

    private var drawComp = DrawComponent(this)

    init {
        add(UpdateComponent(this))
        add(drawComp)
        JamGame.engine.addEntity(this)
    }

    override fun update(delta: Float) {
        if (dead) {
            if (dieAnim.isAnimationFinished(stateTime)) {
                add(DestroyComponent())
            }
        } else {
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                x -= spd * delta
            } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                x += spd * delta
            }

            if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                y += spd * delta
            } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                y -= spd * delta
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
                die()
                println("Just died")
            }
        }

        println("Is updating")

    }

    override fun draw(delta: Float) {
        stateTime += delta
        if (dead) {
            JamGame.batch.draw(dieAnim.getKeyFrame(stateTime), x, y)
        } else {
            JamGame.batch.draw(idleAnim.getKeyFrame(stateTime), x, y)
        }
    }

    private fun die() {
        stateTime = 0f
        dead = true
    }
}
