package com.grumpus.jam

import com.badlogic.ashley.core.Entity

class EnemySpawner(val room: Room, val player: Player) : Entity(), IUpdatable {

    val spawnTime = 3f
    var spawnClock = spawnTime

    init {
        add(UpdateComponent(this))
        JamGame.engine.addEntity(this)
    }

    override fun update(delta: Float) {
        if (spawnClock > 0) {
            spawnClock -= delta
        } else {
            spawnClock = spawnTime
            Skull(room, player, Math.random().toFloat() * JamGame.width, -64f)
        }

    }
}