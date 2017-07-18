package com.grumpus.jam

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2

abstract class AnimatedEntity(x: Float, y: Float, width: Int, height: Int) : Entity() {
    var body = Body(x, y, width, height)
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
            var y = body.centerY() - tr.regionHeight / 2

            // take into account "facing" offset
            if (facing == Facing.LEFT) {
                x += tr.regionWidth
            }

            // draw the frame
            JamGame.batch.draw(tr, x, y, 0f, 0f, tr.regionWidth.toFloat(),
                    tr.regionHeight.toFloat(), facing.dir, 1f, rotation) // TODO: Rotation??
        }
    }

    fun isAnimFinished() : Boolean {
        return currentAnim?.isAnimationFinished(stateTime) ?: false
    }
}

abstract class Enemy(val room: Room, val player: Player, x: Float, y: Float, width: Int, height: Int,
                     idleAnimName: String, dieAnimName: String)
    : AnimatedEntity(x, y, width, height), IUpdatable, IDrawable {

    val idleAnim: Animation<TextureRegion>

    init {
        val atlas = JamGame.assets["img/enemies.atlas", TextureAtlas::class.java]
        idleAnim = Animation(0.07f, atlas.findRegions(idleAnimName), Animation.PlayMode.LOOP)
        setAnim(idleAnim)

        body.dxMax = 50f
        body.dyMax = 50f

        add(UpdateComponent(this))
        add(PhysicsComponent(room, body))
        add(DrawComponent(this))
        JamGame.engine.addEntity(this)

        room.addToGroup(body, Type.ENEMY)
    }

    abstract fun spawnPlatform()

    fun die() {
        spawnPlatform()
        add(DestroyComponent())
    }

    abstract fun move(delta: Float)

    override fun update(delta: Float) {
        val arrowBody = room.getOverlapping(body, Type.ARROW)
        if (arrowBody != null) {
            die()
            if (arrowBody.entity is Arrow) {
                arrowBody.entity.die()
            }
        } else {
            move(delta)
        }
    }

    override fun draw(delta: Float) {
        drawCurrentFrame(delta)
    }
}

// class Skull
class Skull(room: Room, player: Player, x: Float, y: Float):
    Enemy(room, player, x, y, 64, 64, "skull-idle", "skull-idle") {

    val accel = 100f

    override fun move(delta: Float) {
        // chase player directly
        body.ddx = if (player.body.x < body.x) -accel else accel
        body.ddy = if (player.body.y < body.y) -accel else accel
    }

    override fun spawnPlatform() {
        Platform(room, Type.SOLID, "platform-block", 5f, body.x, body.y, 64, 64)
    }
}

enum class ArrowDir(val v: Vector2) {
    UP(Vector2(0f, 1f)),
    DOWN(Vector2(0f, -1f)),
    LEFT(Vector2(-1f, 0f)),
    RIGHT(Vector2(1f, 0f))
}

class Arrow(val room: Room, x: Float, y: Float, val dir: ArrowDir, val charge: Float) :
    AnimatedEntity(x, y, if (dir == ArrowDir.UP || dir == ArrowDir.DOWN) 6 else 48,
            if (dir == ArrowDir.UP || dir == ArrowDir.DOWN) 48 else 6),
    IUpdatable, IDrawable {

    val flyAnim: Animation<TextureRegion>
    val popAnim: Animation<TextureRegion>

    var dead = false

    init {
        body = Body(x, y, if (dir == ArrowDir.UP || dir == ArrowDir.DOWN) 6 else 48,
                if (dir == ArrowDir.UP || dir == ArrowDir.DOWN) 48 else 6, this)
        val atlas = JamGame.assets["img/player.atlas", TextureAtlas::class.java]
        var dirName: String
        when(dir) {
            ArrowDir.UP -> dirName = "-up"
            ArrowDir.DOWN -> dirName = "-down"
            ArrowDir.LEFT -> dirName = "-left"
            ArrowDir.RIGHT -> dirName = "-right"
        }
        flyAnim = Animation(0f, atlas.findRegions("arrow$dirName"))
        popAnim = Animation(0.06f, atlas.findRegions("arrow-pop$dirName"))
        setAnim(flyAnim)

        body.dxMax = 1700f
        body.dyMax = 1500f
        body.dx = dir.v.x * charge * body.dxMax
        body.dy = dir.v.y * charge * body.dyMax
        body.ddy = -400f

        room.addToGroup(body, Type.ARROW)

        add(UpdateComponent(this))
        add(DrawComponent(this))
        add(PhysicsComponent(room, body))
        JamGame.engine.addEntity(this)
    }

    fun die() {
        room.removeFromAnyGroup(body)
        dead = true
        setAnim(popAnim)
        body.dx = 0f
        body.dy = 0f
        body.ddx = 0f
        body.ddy = 0f
    }

    override fun update(delta: Float) {
        // handle enemy collisions in enemy class

        if (!dead) {
            // check for out of bounds, or collision with enemy or platform
            if (body.outOfBounds()) {
                add(DestroyComponent())
            } else if (room.overlaps(body, Type.SOLID)) {
                die()
            }
        }
    }

    override fun draw(delta: Float) {
        drawCurrentFrame(delta)
        if (dead && isAnimFinished()) {
            add(DestroyComponent())
        }
    }
}

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

    val const = 1.0f
    val dxMax = 290f * const
    val dyMax = 500f * const
    var prevDx = body.dx
    val accelGround = 3000f * const
    val accelAir = 1500f * const
    val groundFx = 1800f * const
    val airFx = 900f * const
    val jumpSpeed = 390f * const
    val gravity = -625f * const

    val groundTime = 0.1f
    var groundClock = groundTime
    val shortJumpTime = 0.1f
    var shortJumpClock = shortJumpTime
    val startAimTime = 0.015f
    var startAimClock = startAimTime
    val aimTime = 1f
    var aimClock = aimTime
    val maxAmmo = 5
    var ammo = maxAmmo

    var currentState = PlayerState.AIR

    val sensor = Body(x, y, 4, 4)
    val sensorText = Texture("img/sensor.png")
    val ammoAnim = Texture("img/raw/arrow-up_0.png")

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
//            || !JamGame.input.rightPressed()) {
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
//            || !JamGame.input.leftPressed()) {
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
        ammo = maxAmmo

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
        ammo = maxAmmo

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
        var dir: ArrowDir
        if (JamGame.input.upPressed()) {
            setAnim(aimUpAnim)
            dir = ArrowDir.UP
        } else if (JamGame.input.downPressed()) {
            setAnim(aimDownAnim)
            dir = ArrowDir.DOWN
        } else if (JamGame.input.leftPressed()) {
            setAnim(aimSideAnim)
            faceLeft()
            dir = ArrowDir.LEFT
        } else if (JamGame.input.rightPressed()) {
            setAnim(aimSideAnim)
            faceRight()
            dir = ArrowDir.RIGHT
        } else {
            setAnim(aimSideAnim)
            dir = if (facing == Facing.LEFT) ArrowDir.LEFT else ArrowDir.RIGHT
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
            Arrow(room, body.centerX(), body.centerY(), dir, 1f)
            ammo -= 1
            body.dx = prevDx
            body.dy = jumpSpeed / 1.5f
        }
    }

    private fun die() {
        add(DestroyComponent())
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
            else -> {
                println("State was not recognized.")
            }
        }
    }

    override fun draw(delta: Float) {
        drawCurrentFrame(delta)

        for (i in 1..ammo) {
            var x = 50.0 + 5.0 * i
            var y = 500.0
            JamGame.batch.draw(ammoAnim, x.toFloat(), y.toFloat())
        }
        // DEBUG
//        JamGame.batch.draw(sensorText, sensor.x, sensor.y)
    }
}