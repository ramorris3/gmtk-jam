package com.grumpus.jam

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
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
    GROUND, JUMP, AIM, LEDGE
}

class Player(val room: Room, x: Float, y: Float) : AnimatedEntity(x, y, 16, 54), IUpdatable, IDrawable {
    val standAnim: Animation<TextureRegion>
    val accelGround = 1800f
    val accelAir = 1300f
    val groundFx = 1300f
    val airFx = 900f
    val jumpSpeed = 365f
    val gravity = -600f
    var currentState = PlayerState.GROUND
    val sensor = Body(x, y, 16, 16)
    val sensorText = Texture("img/sensor.png")

    init {
        // physics constants
        body.solid = true
        body.dxMax = 290f
        body.dyMax = 500f
        body.ddy = gravity

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
            PlayerState.JUMP -> jumpState()
            PlayerState.LEDGE -> ledgeState()
            else -> {
                println("State was not recognized.")
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

    private fun onGround() : Boolean {
        body.y -= 1
        val onGround = room.overlaps(body, Type.SOLID)
        body.y += 1
        return onGround
    }

    private fun onRightWall() : Boolean {
        body.x += 1
        val onWall = room.overlaps(body, Type.SOLID)
        body.x -= 1
        return onWall
    }

    private fun onLeftWall() : Boolean {
        body.x -= 1
        val onWall = room.overlaps(body, Type.SOLID)
        body.x += 1
        return onWall
    }

    private fun rightLedgeCheck() : Boolean {
        // check if against wall
        if (!onRightWall()
            || body.dy >= 0f
            || !Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            return false
        }

        sensor.x = body.x + body.width
        sensor.y = body.prevY + body.height
        val wasFree = !room.overlaps(sensor, Type.SOLID)
        sensor.y = body.y + body.height
        val isNotFree = room.overlaps(sensor, Type.SOLID)

        if (wasFree && isNotFree) {
            // set body height to ledge edge and return true
            while (room.overlaps(sensor, Type.SOLID)) {
                sensor.y += 1
                body.y += 1
            }
            return true
        }

        return false
    }

    private fun leftLedgeCheck() : Boolean {
        if (!onLeftWall()
            || body.dy >= 0f
            || !Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            return false
        }

        sensor.x = body.x - sensor.width
        sensor.y = body.prevY + body.height
        val wasFree = !room.overlaps(sensor, Type.SOLID)
        sensor.y = body.y + body.height
        val isNotFree = room.overlaps(sensor, Type.SOLID)

        if (wasFree && isNotFree) {
            while (room.overlaps(sensor, Type.SOLID)) {
                sensor.y += 1
                body.y += 1
            }
            return true
        }

        return false
    }

    private fun groundState() {
        body.fx = groundFx

        // run
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            faceLeft()
            body.ddx = -accelGround
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            faceRight()
            body.ddx = accelGround
        } else {
            body.ddx = 0f
            setAnim(standAnim)
        }

        // jump
        if (actionJustPressed()) {
            body.dy = jumpSpeed
            currentState = PlayerState.JUMP
        } else if (!onGround()) {
            currentState = PlayerState.JUMP
        }

    }

    private fun jumpState() {
        body.fx = airFx

        // move left and right in air
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            faceLeft()
            body.ddx = -accelAir
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            faceRight()
            body.ddx = accelAir
        } else {
            body.ddx = 0f
        }

        val onGround = onGround()
        val rLedge = rightLedgeCheck()
        val lLedge = leftLedgeCheck()

        // landed on ground
        if (onGround) {
            body.dy = 0f
            currentState = PlayerState.GROUND
        } else if (rLedge || lLedge) {
            body.ddy = 0f
            body.dy = 0f
            currentState = PlayerState.LEDGE
            facing = if (rLedge) Facing.RIGHT else Facing.LEFT
        }
    }

    private fun ledgeState() {
        // just hanging

        // TODO: set player ledge animation without loop

        // jumping off
        if (actionJustPressed()) {
            currentState = PlayerState.JUMP
            body.ddy = gravity

            // letting self down
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                body.y -= 1
            } else {
                body.y += 1
                body.dy = jumpSpeed
            }
        }


    }

    override fun draw(delta: Float) {
        drawCurrentFrame(delta)
        JamGame.batch.draw(sensorText, sensor.x, sensor.y)
    }
}