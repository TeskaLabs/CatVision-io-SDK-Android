package com.teskalabs.cvio;

import android.content.Intent;
import com.teskalabs.seacat.android.client.SeaCatClient;

class CVIOInternals {

	public static Intent createIntent(String action)
	{
		Intent Intent = new Intent(action);
		Intent.addCategory(CatVision.CATEGORY_CVIO);
		Intent.addFlags(android.content.Intent.FLAG_FROM_BACKGROUND);
		return Intent;
	}

}
