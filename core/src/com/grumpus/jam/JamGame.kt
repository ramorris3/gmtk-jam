package com.grumpus.jam

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch

class JamGame : Game() {

    // static things
    companion object {
        lateinit var batch: SpriteBatch
        lateinit var assets: Assets
        lateinit var engine: Engine

        val width = 1024
        val height = 576

        val bgColor = Color(11f/255, 0f/255, 28f/255, 1f)
    }

    override fun create() {
        batch = SpriteBatch()
        engine = Engine()
        assets = Assets()

        // TODO: go to loading screen for loading assets
        assets.loadAll()

        setScreen(PlayScreen())
    }

    override fun dispose() {
        batch.dispose()
    }
}
