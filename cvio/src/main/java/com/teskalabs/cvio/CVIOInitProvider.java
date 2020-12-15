package com.teskalabs.cvio;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by ateska on 31.12.17.
 *
 * A lot easier initialization of CatVision.io SDK
 * Inspired by https://firebase.googleblog.com/2016/12/how-does-firebase-initialize-on-android.html
 * and https://medium.com/@andretietz/auto-initialize-your-android-library-2349daf06920
 *
 */

final public class CVIOInitProvider extends ContentProvider {

	public CVIOInitProvider()
	{
	}

	@Override
	public boolean onCreate()
	{
		// get the context (Application context)
		Context context = getContext();

		return CatVision.init(context);
	}

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		return null;
	}

	@Nullable
	@Override
	public String getType(@NonNull Uri uri)
	{
		return null;
	}

	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values)
	{
		return null;
	}

	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs)
	{
		return 0;
	}

	@Override
	public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		return 0;
	}
}
