package com.teskalabs.cvio;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

class VNCServer extends ContextWrapper {

    private static final String TAG = VNCServer.class.getName();

    private Thread mVNCThread = null;

	private final String socketFileName;

    public VNCServer(Context base) {
		super(base);

		// There has to be one directory (/s/) - it is used to ensure correct access level
		socketFileName = getDir("cvio", Context.MODE_PRIVATE).getAbsolutePath() + "/s/vnc";
    }


    public boolean start(final int screenWidth, final int screenHeight)
    {
        if ((screenWidth < 0) || (screenHeight < 0))
        {
            Log.e(TAG, "Screen width/height is not specified");
            return false;
        }

        // Check if VNC Thread is alive
        if (this.mVNCThread != null)
        {
            try {
                this.mVNCThread.join(1);
            } catch (InterruptedException e) {
                // No-op
            }
            if (!this.mVNCThread.isAlive())
            {
                this.mVNCThread = null;
            }
        }

        // Prepare VNC thread and launch
        if (this.mVNCThread == null)
        {
            this.mVNCThread = new Thread(new Runnable() { public void run() {
                int rc;

                Log.d(TAG, "VNC Server started");

                rc = cviojni.run_vnc_server(socketFileName, screenWidth, screenHeight);
                if (rc != 0) Log.w(TAG, "VNC Server thread exited with rc: " + rc);

                Log.i(TAG, "VNC Server terminated");
            }});
            this.mVNCThread.setName("cvioVNCThread");
            this.mVNCThread.setDaemon(true);
            this.mVNCThread.start();
        }

        return true;
    }


    public void stop() {

		if (this.mVNCThread == null) return;

        int rc;
        rc = cviojni.shutdown_vnc_server();
        if (rc != 0) Log.w(TAG, "shutdown_vnc_server returned: " + rc);

        if (this.mVNCThread != null)
        {
            try {
                this.mVNCThread.join(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.mVNCThread = null;
        }
    }

	public String getSocketFileName() {
		return socketFileName;
	}

}
