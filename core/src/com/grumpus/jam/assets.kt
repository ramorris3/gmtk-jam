package com.grumpus.jam

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.TextureAtlas

class Assets : AssetManager() {
    fun loadAll() {
        load("img/player.atlas", TextureAtlas::class.java)
        load("img/enemies.atlas", TextureAtlas::class.java)
        load("img/platforms.atlas", TextureAtlas::class.java)
        load("img/effects.atlas", TextureAtlas::class.java)
        load("img/ui.atlas", TextureAtlas::class.java)
        finishLoading()
    }
}