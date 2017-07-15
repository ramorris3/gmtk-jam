package com.grumpus.jam

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.FitViewport

class JamGame : Game() {

    // static things
    companion object {
        lateinit var batch: SpriteBatch
        lateinit var img: Texture

        val width = 1024
        val height = 576
    }

    val camera = OrthographicCamera()
    val viewport = FitViewport(
            width.toFloat(), height.toFloat(), camera)

    override fun create() {
        batch = SpriteBatch()
        img = Texture("img/png/test.png")
    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        batch.projectionMatrix = camera.combined

        batch.begin()
        batch.draw(img, 0f, 0f)
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
        camera.position.set(
                camera.viewportWidth / 2f,
                camera.viewportHeight / 2f,
                0f)
    }

    override fun dispose() {
        batch.dispose()
        img.dispose()
    }
}
