package com.grumpus.jam.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.grumpus.jam.JamGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

		config.width = 1024;
		config.height = 576;
//        config.fullscreen = true;

		new LwjglApplication(new JamGame(), config);
	}
}
