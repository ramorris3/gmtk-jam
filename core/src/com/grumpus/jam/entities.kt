package com.grumpus.jam

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion

abstract class AnimatedEntity(x: Float, y: Float, width: Int, height: Int) : Entity() {
    val body = Body(x, y, width, height)
    private var currentAnim: Animation<TextureRegion>? = null
    private var stateTime = 0f
    var facing = Facing.RIGHT
    var rotation = 0f

    fun faceLeft() {
        facing = Facing.LEFT
    }

    fun faceRight() {
        facing = Facing.RIGHT
    }

    fun setAnim(anim: Animation<TextureRegion>) {
        if (currentAnim == null || anim != currentAnim) {
            stateTime = 0f
            currentAnim = anim
        }
    }

    fun getCurrentFrame(delta: Float) : TextureRegion? {
        stateTime += delta
        return currentAnim?.getKeyFrame(stateTime)
    }

    fun drawCurrentFrame(delta: Float) {
        val tr = getCurrentFrame(delta)
        if (tr != null) {
            // center current frame horizontally over body hitbox
            var x = body.centerX() - tr.regionWidth / 2

            // take into account "facing" offset
            if (facing == Facing.LEFT) {
                x += tr.regionWidth
            }

            // draw the frame
            JamGame.batch.draw(tr, x, body.y, 0f, 0f, tr.regionWidth.toFloat(),
                    tr.regionHeight.toFloat(), facing.dir, 1f, rotation) // TODO: Rotation??
        }
    }
}

//class Enemy(x: Float)

enum class PlayerState {
    GROUND, JUMP, AIM
}

class Player(val room: Room, x: Float, y: Float) : AnimatedEntity(x, y, 16, 54), IUpdatable, IDrawable {
    val standAnim: Animation<TextureRegion>
    val xAccel = 1200f
    val groundFx = 1300f
    val airFx = 900f
    val jumpSpeed = 500f
    var currentState = PlayerState.GROUND

    init {
        // physics constants
        body.solid = true
        body.dxMax = 210f
        body.dyMax = 180f
        body.ddy = -100f

        // animations
        val atlas = JamGame.assets["img/player.atlas", TextureAtlas::class.java]
        standAnim = Animation(0f, atlas.findRegions("player-stand"), Animation.PlayMode.LOOP)
        setAnim(standAnim)

        // add to room
        room.addToGroup(body, Type.PLAYER)

        // add to systems and core engine
        add(UpdateComponent(this))
        add(PhysicsComponent(room, body))
        add(DrawComponent(this, Layers.ENTITIES))
        JamGame.engine.addEntity(this)
    }

    override fun update(delta: Float) {
        when (currentState) {
            PlayerState.GROUND -> groundState()
            else -> {
                Gdx.app.debug("Player", "State was not recognized.")
            }
        }
    }

    private fun actionPressed() : Boolean {
        return (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))
    }

    private fun actionJustPressed() : Boolean {
        return (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_RIGHT))
    }

    private fun groundState() {
        body.fx = groundFx

        // run
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            faceLeft()
            body.ddx = -xAccel
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            faceRight()
            body.ddx = xAccel
        } else {
            body.ddx = 0f
            setAnim(standAnim)
        }

        // jump
        if (actionJustPressed()) {
            body.dy = jumpSpeed
        }

    }

    private fun jumpState() {
        body.fx = airFx
    }

    override fun draw(delta: Float) {
        drawCurrentFrame(delta)
    }
}