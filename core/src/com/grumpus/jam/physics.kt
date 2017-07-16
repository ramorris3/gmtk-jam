package com.grumpus.jam

enum class Facing(val dir: Float) {
    LEFT(-1f),
    RIGHT(1f)
}

enum class Type {
    PLAYER, ENEMY, SOLID, SEMISOLID
}

class Body(var x: Float, var y: Float, val width: Int, val height: Int) {
    var prevX = x
    var prevY = y
    var solid = false
    var dx = 0f
    var dy = 0f
    var dxMax = 0f
    var dyMax = 0f
    var ddx = 0f
    var ddy = 0f
    var fx = 0f
    var fy = 0f

    fun overlaps(other: Body) : Boolean {
        if (equals(other)) return false
        return (x < other.x + other.width
                && x + width > other.x
                && y < other.y + other.height
                && y + height > other.y)
    }

    fun centerX() : Float {
        return width / 2 + x
    }

    fun centerY() : Float {
        return height / 2 + y
    }

    fun outOfBounds() : Boolean {
        return (x < 0 || x > JamGame.width
            || y < 0 || y > JamGame.width)
    }
}

class Group: ArrayList<Body>() {

    fun getOverlapping(body: Body): Body? {
        for (b in this) {
            if (b.overlaps(body)) {
                return b
            }
        }
        return null
    }

    fun overlaps(body: Body): Boolean {
        return getOverlapping(body) != null
    }

}

// A collection of collision groups
class Room {
    private val groups = HashMap<Type, Group>()

    init {
        reset()
    }

    fun addToGroup(body: Body, type: Type) {
        groups[type]?.add(body)
    }

    fun removeFromGroup(body: Body, type: Type) {
        groups[type]?.remove(body)
    }

    fun removeFromAnyGroup(body: Body) {
        for (type in Type.values()) {
            groups[type]?.remove(body)
        }
    }

    fun getOverlapping(body: Body, type: Type) : Body? {
        return groups[type]?.getOverlapping(body)
    }

    fun overlaps(body: Body, type: Type) : Boolean {
        return getOverlapping(body, type) != null
    }

    fun reset() {
        // clear/init groups
        for (type in Type.values()) {
            groups[type] = Group()
        }
        // clear out all entities
//        JamGame.engine.removeAllEntities()
    }
}