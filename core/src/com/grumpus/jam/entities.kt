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
    val groundAccel = 1200f
    val groundFx = 1300f
    var currentState = PlayerState.GROUND

    init {
        // physics constants
        body.dxMax = 110f
        body.dyMax = 180f
        body.ddy = -800f

        // animations
        val atlas = JamGame.assets["img/player.atlas", TextureAtlas::class.java]
        standAnim = Animation(0f, atlas.findRegions("player-stand"), Animation.PlayMode.LOOP)
        setAnim(standAnim)

        // add to room
        room.addToGroup(body, Type.PLAYER)

        // add to systems and core engine
        add(UpdateComponent(this))
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


        // update position based on velocity, accel, etc.
        move(delta)
    }

    private fun groundState() {
        body.fx = groundFx
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            faceLeft()
            body.ddx = -groundAccel
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            faceRight()
            body.ddx = groundAccel
        } else {
            body.ddx = 0f
        }

    }

    override fun draw(delta: Float) {
        drawCurrentFrame(delta)
    }

    private fun move(delta: Float) {
        moveX(delta)
        moveY(delta)
    }

    private fun moveX(delta: Float) {
        // update velocity
        body.dx += body.ddx * delta
        if (Math.abs(body.ddx) == 0f) {
            body.dx = approach(body.dx, 0f, body.fx * delta)
        }
        body.dx = clamp(body.dx, -body.dxMax, body.dxMax)

        // get amount to move, and direction
        var toMove = body.dx
        var sign = Math.signum(toMove)

        // step one px at a time, checking for solid collisions
        while (Math.abs(toMove) > 0) {
            if (Math.abs(toMove) < 1) {
                sign = toMove
            }

            // tentatively move
            body.x += sign
            toMove -= sign

            // check for collisions
            if (room.overlaps(body, Type.SOLID)) {
                // collision!  move back
                body.x -= sign
                body.dx = 0f
                break
            }
        }
    }

    private fun moveY(delta: Float) {
        // update velocity
        body.dy += body.ddy * delta
        body.dy = clamp(body.dy, -body.dyMax, body.dyMax)

        // get amount to move, and direction
        var toMove = body.dy
        var sign = Math.signum(toMove)

        // step one px at a time, checking for solid collisions
        while (Math.abs(toMove) > 0) {
            if (Math.abs(toMove) < 1) {
                sign = toMove
            }

            // tentatively move
            body.y += sign
            toMove -= sign

            // check for collisions
            if (room.overlaps(body, Type.SOLID)) {
                // collision!  move back
                body.y -= sign
                body.dy = 0f
                break
            }
        }
    }
}