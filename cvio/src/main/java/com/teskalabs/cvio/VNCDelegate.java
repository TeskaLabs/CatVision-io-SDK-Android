package com.teskalabs.cvio;

interface VNCDelegate
{
	int takeImage(); // Return value: 0 - image has been pushed to VNCServer.push(), 1 - VNC server is requested to shutdown immediately

	// rfb* callbacks

	void rfbKbdAddEventProc(boolean down, long keySym, String client);
	void rfbKbdReleaseAllKeysProc(String client);

/* Indicates either pointer movement or a pointer button press or release. The pointer is
now at (x-position, y-position), and the current state of buttons 1 to 8 are represented
by bits 0 to 7 of button-mask respectively, 0 meaning up, 1 meaning down (pressed).
On a conventional mouse, buttons 1, 2 and 3 correspond to the left, middle and right
buttons on the mouse. On a wheel mouse, each step of the wheel upwards is represented
by a press and release of button 4, and each step downwards is represented by
a press and release of button 5.
From: http://www.vislab.usyd.edu.au/blogs/index.php/2009/05/22/an-headerless-indexed-protocol-for-input-1?blog=61
*/
	void rfbPtrAddEventProc(int buttonMask, int x, int y, String client);

	void rfbSetXCutTextProc(String text, String client);

	int rfbNewClientHook(String client);
}
