package com.grumpus.jam

import com.badlogic.ashley.core.*
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.ashley.systems.SortedIteratingSystem

enum class Layers {
    SOLIDS, EFFECTS, ENTITIES, UI
}

interface IUpdatable {
    fun update(delta: Float)
}

interface IDrawable {
    fun draw(delta: Float)
}

class UpdateComponent(val updatable: IUpdatable) : Component
class PhysicsComponent(val room: Room, val body: Body) : Component
class DrawComponent(val drawable: IDrawable,
                    var layer: Layers = Layers.ENTITIES) : Component
class DestroyComponent : Component

class UpdateSystem : IteratingSystem(Family.all(UpdateComponent::class.java).get()) {
    private val ucm = ComponentMapper.getFor(UpdateComponent::class.java)

    override fun processEntity(entity: Entity?, deltaTime: Float) {
        if (entity != null) {
            val uc = ucm[entity]
            uc.updatable.update(deltaTime)
        }
    }
}

class PhysicsSystem : IteratingSystem(Family.all(UpdateComponent::class.java).get()) {
    private val pcm = ComponentMapper.getFor(PhysicsComponent::class.java)

    override fun processEntity(entity: Entity?, deltaTime: Float) {
        if (entity != null) {
            val pc = pcm[entity]
            updateVelocity(pc.body, deltaTime)

            if (pc.body.solid) {
                // move to solid
                moveToSolid(pc.body, pc.room, deltaTime)
            } else {
                move(pc.body, deltaTime)
            }
        }
    }

    private fun updateVelocity(body: Body, delta: Float) {
        body.dx += body.ddx * delta
        body.dy += body.ddy * delta

        if (Math.abs(body.ddx) == 0f) {
            body.dx = approach(body.dx, 0f, body.fx * delta)
        }
        if (Math.abs(body.ddy) == 0f) {
            body.dy = approach(body.dy, 0f, body.fy * delta)
        }
        body.dx = clamp(body.dx, -body.dxMax, body.dxMax)
        body.dy = clamp(body.dy, -body.dyMax, body.dyMax)
    }

    private fun move(body: Body, delta: Float) {
        body.x += body.dx * delta
        body.y += body.dy * delta
        println("moving!")
    }

    private fun moveToSolid(body: Body, room: Room, delta: Float) {
        // try to move by body's velocity, one px at a time
        moveToSolidX(body, room, delta)
        moveToSolidY(body, room, delta)
    }

    private fun moveToSolidX(body: Body, room: Room, delta: Float) {
        // get direction to move in
        var toMove = body.dx * delta
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

    private fun moveToSolidY(body: Body, room: Room, delta: Float) {
        var toMove = body.dy * delta
        var sign = Math.signum(toMove)

        while(Math.abs(toMove) > 0) {
            if (Math.abs(toMove) < 1) {
                sign = toMove
            }

            body.y += sign
            toMove -= sign

            if (room.overlaps(body, Type.SOLID)) {
                body.y -= sign
                body.dy = 0f
                break
            }
        }
    }
}


class DrawSystem: SortedIteratingSystem(Family.all(DrawComponent::class.java).get(), DrawComparator()) {
    private val dcm = ComponentMapper.getFor(DrawComponent::class.java)

    override fun processEntity(entity: Entity?, deltaTime: Float) {
        if (entity != null) {
            val dc = dcm[entity]
            dc.drawable.draw(deltaTime)
        }
    }

    private class DrawComparator : Comparator<Entity> {
        private val dcm = ComponentMapper.getFor(DrawComponent::class.java)

        override fun compare(o1: Entity?, o2: Entity?): Int {
            if (o1 != null && o2 != null) {
                val e1 = dcm.get(o1)
                val e2 = dcm.get(o2)
                return e1.layer.compareTo(e2.layer)
            }
            return 0
        }
    }
}

class DestroySystem: IteratingSystem(Family.all(DestroyComponent::class.java).get()) {
    override fun processEntity(entity: Entity?, deltaTime: Float) {
        if (entity != null) {
            engine.removeEntity(entity)
            println("Removed from engine")
        }
    }
}

