package com.teskalabs.cvio.demo;

import android.app.Application;
import com.teskalabs.seacat.android.client.SeaCatClient;
import com.teskalabs.cvio.CatVision;
import java.io.IOException;

public class DemoApplication extends Application
{

	@Override
	public void onCreate()
	{
		super.onCreate();

		try {
			SeaCatClient.setLogMask(SeaCatClient.LogFlag.ALL_SET);
		} catch (IOException e) {
			e.printStackTrace();
		}

		CatVision.downscale = 4;
		CatVision.initialize();

		// Enable SeaCat
		SeaCatClient.initialize(getApplicationContext());
	}

}
