package com.teskalabs.cvio;

import com.teskalabs.seacat.android.client.SeaCatPlugin;

import java.util.Properties;

final public class CVIOSeaCatPlugin extends SeaCatPlugin {

	final private int port;

	CVIOSeaCatPlugin(int port)
	{
		this.port = port;
	}

	@Override
	public Properties getCharacteristics(){
		Properties p = new Properties();
		p.setProperty("RA", "vnc:"+port);
		return p;
	}

}
