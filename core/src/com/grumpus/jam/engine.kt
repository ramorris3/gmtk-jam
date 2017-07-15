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