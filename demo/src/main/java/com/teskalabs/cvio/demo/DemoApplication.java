package com.teskalabs.cvio.demo;

import android.app.Application;
import android.util.Log;

import com.teskalabs.cvio.CatVision;
import com.teskalabs.cvio.exceptions.CatVisionException;

public class DemoApplication extends Application
{

	private static final String TAG = DemoApplication.class.getName();

	@Override
	public void onCreate()
	{
		super.onCreate();

		CatVision.initialize(this);
	}

}
