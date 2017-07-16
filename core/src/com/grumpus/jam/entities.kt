package com.grumpus.jam

import com.badlogic.ashley.core.Entity
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
    GROUND, AIR, AIM, LEDGE
}

class Player(val room: Room, x: Float, y: Float) : AnimatedEntity(x, y, 16, 54), IUpdatable, IDrawable {
    val standAnim: Animation<TextureRegion>
    val aimSideAnim: Animation<TextureRegion>
    val aimUpAnim: Animation<TextureRegion>
    val aimDownAnim: Animation<TextureRegion>
    val jumpAnim: Animation<TextureRegion>
    val runAnim: Animation<TextureRegion>
    val slideAnim: Animation<TextureRegion>
    val ledgeAnim: Animation<TextureRegion>

    val const = 1.5f
    val dxMax = 290f * const
    val dyMax = 500f * const
    val accelGround = 2200f * const
    val accelAir = 1300f * const
    val groundFx = 1800f * const
    val airFx = 900f * const
    val jumpSpeed = 365f * const
    val gravity = -625f * const

    val groundTime = 0.225f
    var groundClock = groundTime
    val shortJumpTime = 0.1f
    var shortJumpClock = shortJumpTime
    val startAimTime = 0.225f
    var startAimClock = startAimTime
    val aimTime = 1.8f
    var aimClock = aimTime

    var currentState = PlayerState.GROUND

    val sensor = Body(x, y, 16, 16)
    val sensorText = Texture("img/sensor.png")

    init {
        // physics constants
        body.solid = true
        body.dxMax = dxMax
        body.dyMax = dyMax
        body.ddy = gravity

        // animations
        val atlas = JamGame.assets["img/player.atlas", TextureAtlas::class.java]
        standAnim = Animation(0f, atlas.findRegions("player-stand"), Animation.PlayMode.LOOP)
        aimSideAnim = Animation(0f, atlas.findRegions("player-aim-side"))
        aimUpAnim = Animation(0f, atlas.findRegions("player-aim-up"))
        aimDownAnim = Animation(0f, atlas.findRegions("player-aim-down"))
        jumpAnim = Animation(0.04f, atlas.findRegions("player-jump"), Animation.PlayMode.LOOP)
        runAnim = Animation(0.05f, atlas.findRegions("player-run"), Animation.PlayMode.LOOP)
        slideAnim = Animation(0.04f, atlas.findRegions("player-slide"))
        ledgeAnim = Animation(0f, atlas.findRegions("player-ledge"))
        setAnim(standAnim)

        // add to room
        room.addToGroup(body, Type.PLAYER)

        // add to systems and core engine
        add(UpdateComponent(this))
        add(PhysicsComponent(room, body))
        add(DrawComponent(this, Layers.ENTITIES))
        JamGame.engine.addEntity(this)
    }

    private fun onGround() : Boolean {
        body.y -= 1
        val onGround = room.overlaps(body, Type.SOLID)
        body.y += 1
        return onGround
    }

    private fun wasOnGround() : Boolean {
        return groundClock > 0
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
            || !JamGame.input.rightPressed()) {
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
            || !JamGame.input.leftPressed()) {
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
        if (JamGame.input.leftPressed()) {
            if (body.dx > 0) {
                faceRight()
                setAnim(slideAnim)
            } else {
                faceLeft()
                setAnim(runAnim)
            }
            body.ddx = -accelGround
        } else if (JamGame.input.rightPressed()) {
            if (body.dx < 0) {
                faceLeft()
                setAnim(slideAnim)
            } else {
                faceRight()
                setAnim(runAnim)
            }
            body.ddx = accelGround
        } else {
            body.ddx = 0f
            if (Math.abs(body.dx) > 0) {
                setAnim(slideAnim)
            } else {
                setAnim(standAnim)
            }
        }

        // jump
        if (JamGame.input.actionJustPressed()) {
            body.dy = jumpSpeed
            shortJumpClock = shortJumpTime
            startAimClock = startAimTime
            currentState = PlayerState.AIR
        } else if (!wasOnGround()) {
            currentState = PlayerState.AIR
            shortJumpClock = 0f
            startAimClock = startAimTime
        }

    }

    private fun jumpState() {
        body.fx = airFx
        setAnim(jumpAnim)

        // TODO: recharge aim time??

        // allow short hopping
        if (shortJumpClock > 0 && JamGame.input.actionReleased()) {
            shortJumpClock = 0f
            body.dy /= 2
        }

        // move left and right in air
        if (JamGame.input.leftPressed()) {
            faceLeft()
            body.ddx = -accelAir
        } else if (JamGame.input.rightPressed()) {
            faceRight()
            body.ddx = accelAir
        } else {
            body.ddx = 0f
        }

        val onGround = onGround()
        val rLedge = rightLedgeCheck()
        val lLedge = leftLedgeCheck()

        // landed on ground or ledge, or started aiming
        if (onGround) {
            body.dy = 0f
            currentState = PlayerState.GROUND
        } else if (rLedge || lLedge) {
            body.ddy = 0f
            body.dy = 0f
            currentState = PlayerState.LEDGE
            facing = if (rLedge) Facing.RIGHT else Facing.LEFT
        } else if (startAimClock <= 0 && JamGame.input.actionJustPressed()) {
            body.dy = 0f
//            body.dx = 300f * -facing.dir
            body.dx = 0f
            aimClock = aimTime
            currentState = PlayerState.AIM
        }
    }

    private fun ledgeState() {
        // just hanging, no special logic other than anim
        setAnim(ledgeAnim)

        // jumping off
        if (JamGame.input.actionJustPressed()) {
            currentState = PlayerState.AIR
            startAimClock = startAimTime
            body.ddy = gravity

            // letting self down
            if (JamGame.input.downPressed()) {
                body.y -= 1
            } else {
                body.y += 1
                body.dy = jumpSpeed
                shortJumpClock = shortJumpTime
            }
        }
    }

    private fun aimState(delta: Float) {
        // aim left, right, up, or down
        if (JamGame.input.upPressed()) {
            setAnim(aimUpAnim)
        } else if (JamGame.input.downPressed()) {
            setAnim(aimDownAnim)
        } else if (JamGame.input.leftPressed()) {
            setAnim(aimSideAnim)
            faceLeft()
        } else if (JamGame.input.rightPressed()) {
            setAnim(aimSideAnim)
            faceRight()
        } else {
            setAnim(aimSideAnim)
        }

        // slow falling, stop moving horizontally
        body.ddx = 0f
        body.ddy = -10f

        // decrement aim timer
        aimClock -= delta

        // if out of charge or on ground, go back to air state
        if (onGround()) {
            body.ddy = gravity
            aimClock = aimTime
            currentState = PlayerState.GROUND
        } else if (aimClock <= 0 || JamGame.input.actionReleased()) {
            currentState = PlayerState.AIR
            body.ddy = gravity
        }
    }

    override fun update(delta: Float) {
        // update timers
        if (onGround()) groundClock = groundTime
        if (groundClock > 0) groundClock -= delta
        if (shortJumpClock > 0) shortJumpClock -= delta
        if (startAimClock > 0) startAimClock -= delta

        // state logic
        when (currentState) {
            PlayerState.GROUND -> groundState()
            PlayerState.AIR -> jumpState()
            PlayerState.LEDGE -> ledgeState()
            PlayerState.AIM -> aimState(delta)
            else -> {
                println("State was not recognized.")
            }
        }
    }

    override fun draw(delta: Float) {
        drawCurrentFrame(delta)
        // DEBUG
//        JamGame.batch.draw(sensorText, sensor.x, sensor.y)
    }
}