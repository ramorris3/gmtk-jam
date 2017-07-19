package com.grumpus.jam

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion

enum class PlayerState {
    GROUND, AIR, AIM, LEDGE, DEAD
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

    val const = 1.0f
    val dxMax = 290f * const
    val dyMax = 500f * const
    var prevDx = body.dx
    val accelGround = 3000f * const
    val accelAir = 1500f * const
    val groundFx = 1800f * const
    val airFx = 900f * const
    val jumpSpeed = 390f * const
    val sensor = Body(x, y, 4, 4)

    val gravity = -625f * const
    val groundTime = 0.1f
    var groundClock = groundTime
    val shortJumpTime = 0.1f
    var shortJumpClock = shortJumpTime
    val startAimTime = 0.015f
    var startAimClock = startAimTime
    val aimTime = 1f
    var aimClock = aimTime

    var aimDir = ArrowDir.RIGHT
    val maxAmmo = 5
    var ammo = maxAmmo
    var score = 0

    val quiver = Quiver(this)
    val aimTimer = AimTimer(this)
    val counter = Counter(this)

    var currentState = PlayerState.AIR

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
                || body.dy >= 0f) {
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

    private fun stillOnRightLedge() : Boolean {
        if (!onRightWall()) return false

        sensor.x = body.x + body.width
        sensor.y = body.y + body.height
        val isClearAbove = !room.overlaps(sensor, Type.SOLID)
        sensor.y -= sensor.height
        val isBlockedBelow = room.overlaps(sensor, Type.SOLID)
        return isClearAbove && isBlockedBelow
    }

    private fun leftLedgeCheck() : Boolean {
        if (!onLeftWall()
                || body.dy >= 0f) {
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

    private fun stillOnLeftLedge() : Boolean {
        if (!onLeftWall()) return false

        sensor.x = body.x - sensor.width
        sensor.y = body.y + body.height
        val isClearAbove = !room.overlaps(sensor, Type.SOLID)
        sensor.y -= sensor.height
        val isBlockedBelow = room.overlaps(sensor, Type.SOLID)
        return isClearAbove && isBlockedBelow
    }

    private fun groundState() {
        body.fx = groundFx

        // reload
        quiver.reload()

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

    private fun ledgeState() {
        // reload
        quiver.reload()

        // just hanging, no special logic other than anim
        setAnim(ledgeAnim)

        // if ledge disappears or if player jumps, switch states
        if (!stillOnRightLedge() && !stillOnLeftLedge()) {
            currentState = PlayerState.AIR
            body.ddy = gravity
        } else if (JamGame.input.actionJustPressed()) {
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
            body.ddx = 0f
            body.dx = 0f
            currentState = PlayerState.LEDGE
            facing = if (rLedge) Facing.RIGHT else Facing.LEFT
        } else if (startAimClock <= 0 && JamGame.input.actionJustPressed() && ammo > 0) {
            body.dy = 0f
            prevDx = body.dx
            body.dx = 0f
            aimClock = aimTime
            currentState = PlayerState.AIM
        }
    }

    private fun aimState(delta: Float) {
        // aim left, right, up, or down
        if (JamGame.input.upPressed()) {
            setAnim(aimUpAnim)
            aimDir = ArrowDir.UP
        } else if (JamGame.input.downPressed()) {
            setAnim(aimDownAnim)
            aimDir = ArrowDir.DOWN
        } else if (JamGame.input.leftPressed()) {
            setAnim(aimSideAnim)
            faceLeft()
            aimDir = ArrowDir.LEFT
        } else if (JamGame.input.rightPressed()) {
            setAnim(aimSideAnim)
            faceRight()
            aimDir = ArrowDir.RIGHT
        } else if (aimDir == ArrowDir.RIGHT) {
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
            if (aimClock <= 0) {
                // TODO: play sound effect?
                Effect(body.centerX(), body.centerY(), "ui-arrow-pop")
            }
            currentState = PlayerState.AIR
            body.ddy = gravity
            Arrow(room, body.centerX(), body.centerY(), aimDir, 1f)
            quiver.pop()
            body.dx = prevDx
            body.dy = jumpSpeed / 1.5f
        }
    }

    private fun die() {
        add(DestroyComponent())
        currentState = PlayerState.DEAD
        Effect(body.centerX(), body.centerY(), "player-die")
    }

    override fun update(delta: Float) {
        // check for death conditions
        if (room.overlaps(body, Type.ENEMY) || body.centerY() <= 0) {
            if (body.centerY() < 0) body.y = -body.height / 2f
            die()
            return
        }

        // clamp to walls and ceiling
        if (body.x < 0) body.x = 0f
        if (body.x > JamGame.width - body.width) body.x = JamGame.width - body.width.toFloat()
        if (body.y > JamGame.height - body.height) body.y = JamGame.height - body.height.toFloat()

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
        }
    }

    override fun draw(delta: Float) {
        drawCurrentFrame(delta)
    }
}
