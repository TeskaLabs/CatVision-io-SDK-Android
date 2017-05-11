package com.teskalabs.cvio.demo;

import android.app.Application;
import com.teskalabs.cvio.CatVision;

public class DemoApplication extends Application
{

	@Override
	public void onCreate()
	{
		super.onCreate();

		CatVision.initialize(this, publicAccessKey="klskfweojfewjfweoijfwoejfiowiefjwio"));
	}

}
