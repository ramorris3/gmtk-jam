package com.grumpus.jam

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.TextureAtlas

class Assets : AssetManager() {
    fun loadAll() {
        load("img/enemies.atlas", TextureAtlas::class.java)
        finishLoading()
    }
}