package com.grumpus.jam

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.viewport.FitViewport

class PlayScreen : ScreenAdapter() {
    val camera = OrthographicCamera()
    val viewport = FitViewport(
            JamGame.width.toFloat(), JamGame.height.toFloat(), camera)

    init {
        val room = Room()
        JamGame.engine.addSystem(UpdateSystem())
        JamGame.engine.addSystem(PhysicsSystem())
        JamGame.engine.addSystem(DrawSystem())
        JamGame.engine.addSystem(DestroySystem(room))

        // player and starting platform
        val player = Player(room, JamGame.width / 2f, JamGame.height / 2f)
        StartPlatform(room, player.body.x - 32, player.body.y - 128)

        EnemySpawner(room, player)

//        // TODO: remove this, enemy controller instead
//        for (x in 0..JamGame.width - 64 step 64) {
//            MovementStaging(room, x.toFloat(), 0f)
//        }
//        Skull(room, player,128f, 128f)
//        Skull(room, player, 956f, 256f)
//        Skull(room, player, 1000f, 400f)
//        Skull(room, player, 1f, 480f)
    }

    override fun render(delta: Float) {
        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
        }

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