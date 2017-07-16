package com.grumpus.jam

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

class Input : Entity(), IUpdatable {
    private var shift = false
    private var prevShift = shift

    init {
        add(UpdateComponent(this))
        JamGame.engine.addEntity(this)
    }

    fun upPressed() : Boolean {
        return Gdx.input.isKeyPressed(Input.Keys.UP)
    }

    fun downPressed() : Boolean {
        return Gdx.input.isKeyPressed(Input.Keys.DOWN)
    }

    fun leftPressed() : Boolean {
        return Gdx.input.isKeyPressed(Input.Keys.LEFT)
    }

    fun rightPressed() : Boolean {
        return Gdx.input.isKeyPressed(Input.Keys.RIGHT)
    }

    fun actionPressed() : Boolean {
        return (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))
    }

    fun actionJustPressed() : Boolean {
        return (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_RIGHT))
    }

    fun actionReleased() : Boolean {
        return prevShift && !shift
    }

    override fun update(delta: Float) {
        prevShift = shift
        shift = actionPressed()
    }
}