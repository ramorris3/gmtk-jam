package com.grumpus.jam

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.viewport.FitViewport

class PlayScreen : ScreenAdapter() {
    val camera = OrthographicCamera()
    val viewport = FitViewport(
            JamGame.width.toFloat(), JamGame.height.toFloat(), camera)

    init {
        // TODO: add systems to rooms so that rooms and their entities are self-contained
        JamGame.engine.addSystem(UpdateSystem())
        JamGame.engine.addSystem(DrawSystem())
        JamGame.engine.addSystem(DestroySystem())

        // TODO: load rooms dynamically
        val room = Room()

        Solid(room, Type.SOLID, "platform-block", 10f, 32f, 32f, 64, 64)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(
                JamGame.bgColor.r,
                JamGame.bgColor.g,
                JamGame.bgColor.b,
                JamGame.bgColor.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        JamGame.batch.projectionMatrix = camera.combined

        JamGame.batch.begin()
        JamGame.engine.update(delta)
        JamGame.batch.end()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
        camera.position.set(
                camera.viewportWidth / 2f,
                camera.viewportHeight / 2f,
                0f)
    }
}