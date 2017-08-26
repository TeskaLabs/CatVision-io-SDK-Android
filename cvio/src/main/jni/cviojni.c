#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <assert.h>
#include <sys/socket.h>
#include <android/log.h>
#include <sys/un.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <libgen.h>

#include "rfb/rfb.h"
#include "com_teskalabs_cvio_cviojni.h"

///

#ifdef __ANDROID__
#define JNINATIVEINTERFACEPTR const struct JNINativeInterface ***
#else
#define JNINATIVEINTERFACEPTR void **
#endif

///

static JavaVM * g_java_vm = NULL;

static jobject g_delegate_obj = NULL;
static jmethodID g_ra_JNICALLBACK_take_image_mid = 0;

static jmethodID g_ra_JNICALLBACK_rfbPtrAddEventProc_mid = 0;
static void ptrAddEvent(int buttonMask, int x, int y, rfbClientPtr cl);

static jmethodID g_ra_JNICALLBACK_rfbNewClientHook_mid = 0;
static enum rfbNewClientAction newClientHook(rfbClientPtr cl);

static void kbdAddEvent(rfbBool down, rfbKeySym keySym, struct _rfbClientRec* cl);
static jmethodID g_ra_JNICALLBACK_rfbKbdAddEventProc_mid = 0;

static void kbdReleaseAllKeys(struct _rfbClientRec* cl);
static jmethodID g_ra_JNICALLBACK_rfbKbdReleaseAllKeysProc_mid = 0;

static void setXCutText(char* str,int len, struct _rfbClientRec* cl);
static jmethodID g_ra_JNICALLBACK_rfbSetXCutTextProc_mid = 0;

static const char * TAG = "cviojni";

///

// We need a word to capture a pixel (aka 16bit color or actually 15bit)
#define BPP      (2)

///

static int logMutex_initialized = 0;
#ifdef LIBVNCSERVER_HAVE_LIBPTHREAD
static MUTEX(logMutex);
#endif

static void raLibLog(const char *format, ...)
{
	va_list args;

	// Strip newlines from format string
	char format_buf[strlen(format)+1];
	strcpy(format_buf, format);
	format_buf[strcspn(format_buf, "\r\n")] = 0;

	if (!logMutex_initialized)
	{
		INIT_MUTEX(logMutex);
		logMutex_initialized = 1;
	}

	LOCK(logMutex);
	va_start(args, format);

	__android_log_vprint(ANDROID_LOG_DEBUG, TAG, format, args);

	va_end(args);
	UNLOCK(logMutex);
}

////

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv* env;

	g_java_vm = vm;

	if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK)
	{
		return -1;
	}

	// Initialize logging
	rfbLogEnable(1);
	rfbLog=raLibLog;
	rfbErr=raLibLog;

	return JNI_VERSION_1_6;
}

///

JNIEXPORT void JNICALL Java_com_teskalabs_cvio_cviojni_set_1delegate(JNIEnv * env, jclass cls, jobject ra)
{
	assert(g_delegate_obj == NULL);

	// convert local to global reference (local will die after this method call)
	g_delegate_obj = (*env)->NewGlobalRef(env, ra);
	assert(g_delegate_obj != NULL);

	// save refs for callback
	jclass g_clazz = (*env)->GetObjectClass(env, g_delegate_obj);
	if (g_clazz == NULL)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to get object reference");
		return;
	}

	g_ra_JNICALLBACK_take_image_mid = (*env)->GetMethodID(env, g_clazz, "takeImage", "()I");
	if (g_ra_JNICALLBACK_take_image_mid == 0)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to get method id: takeImage()");
		return;
	}

	g_ra_JNICALLBACK_rfbPtrAddEventProc_mid = (*env)->GetMethodID(env, g_clazz, "rfbPtrAddEventProc", "(IIILjava/lang/String;)V");
	if (g_ra_JNICALLBACK_rfbPtrAddEventProc_mid == 0)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to get method id: rfbPtrAddEventProc()");
		return;
	}

	g_ra_JNICALLBACK_rfbNewClientHook_mid = (*env)->GetMethodID(env, g_clazz, "rfbNewClientHook", "(Ljava/lang/String;)I");
	if (g_ra_JNICALLBACK_rfbNewClientHook_mid == 0)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to get method id: rfbNewClientHook()");
		return;
	}

	g_ra_JNICALLBACK_rfbKbdAddEventProc_mid = (*env)->GetMethodID(env, g_clazz, "rfbKbdAddEventProc", "(ZJLjava/lang/String;)V");
	if (g_ra_JNICALLBACK_rfbKbdAddEventProc_mid == 0)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to get method id: rfbKbdAddEventProc()");
		return;
	}

	g_ra_JNICALLBACK_rfbKbdReleaseAllKeysProc_mid = (*env)->GetMethodID(env, g_clazz, "rfbKbdReleaseAllKeysProc", "(Ljava/lang/String;)V");
	if (g_ra_JNICALLBACK_rfbKbdReleaseAllKeysProc_mid == 0)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to get method id: rfbKbdReleaseAllKeysProc()");
		return;
	}

	g_ra_JNICALLBACK_rfbSetXCutTextProc_mid = (*env)->GetMethodID(env, g_clazz, "rfbSetXCutTextProc", "(Ljava/lang/String;Ljava/lang/String;)V");
	if (g_ra_JNICALLBACK_rfbSetXCutTextProc_mid == 0)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to get method id: rfbSetXCutTextProc()");
		return;
	}

}


static int takeImage()
{
	int ret = 1;
	JNIEnv * g_env;
	assert(g_java_vm != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			__android_log_print(ANDROID_LOG_ERROR, TAG, "AttachCurrentThread failed");
			return ret;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "version not supported");
		return ret;
	}

	if (g_delegate_obj != NULL)
	{
		ret = (*g_env)->CallIntMethod(g_env, g_delegate_obj, g_ra_JNICALLBACK_take_image_mid, NULL);
	}

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);

	return ret;
}

///

static int rfbListenOnUNIXPort(const char * path)
{
    struct sockaddr_un addr;
    int sock;
    int rc;

    // Create UNIX socket in the folder that is -rwx------
    // See http://stackoverflow.com/questions/20171747/how-to-create-unix-domain-socket-with-a-specific-permissions
    char pathbuf[512];
    strcpy(pathbuf, path);
    char * dir = dirname(pathbuf);
    rc = mkdir(dir, S_IRWXU);
    if (rc != 0)
    {
    	if (errno == EEXIST) chmod(dir, S_IRWXU);
    }

    memset(&addr, 0, sizeof(struct sockaddr_un));
    addr.sun_family = AF_UNIX;
	strcpy(addr.sun_path, path);
	unlink(addr.sun_path);
	int len = strlen(addr.sun_path) + sizeof(addr.sun_family);

    if ((sock = socket(AF_UNIX, SOCK_STREAM, 0)) < 0)
    {
    	__android_log_print(ANDROID_LOG_ERROR, TAG, "socket() failed %d", errno);
		return -1;
    }

    fchmod(sock, S_IRUSR | S_IWUSR);

    if (bind(sock, (struct sockaddr *)&addr, len) < 0)
    {
    	__android_log_print(ANDROID_LOG_ERROR, TAG, "bind() failed %d", errno);
		close(sock);
		return -1;
    }

    fchmod(sock, S_IRUSR | S_IWUSR);

    if (listen(sock, 32) < 0)
    {
    	__android_log_print(ANDROID_LOG_ERROR, TAG, "listen() failed %d", errno);
		close(sock);
		return -1;
    }

    return sock;
}

////

static rfbScreenInfoPtr serverScreen = NULL;
static int serverShutdown = 3; // 1 .. ask for shutdown, 2 .. shutdown in process, 3 .. server is not started

static struct
{
	int width;
	int height;

	int line_stride; // 'Real width' including a border

	int x_offset;

	size_t fb_length;
} screenInfo;

static volatile int imageReady = 0;

JNIEXPORT jint JNICALL Java_com_teskalabs_cvio_cviojni_run_1vnc_1server(JNIEnv * env, jclass cls, jstring socketDir, jint width, jint height)
{
	int argc=1;

	if (serverScreen != NULL)
	{
		__android_log_print(ANDROID_LOG_WARN, TAG, "Server screen is already created");
		return -1;
	}	

	screenInfo.width = width;
	screenInfo.line_stride = (screenInfo.width + 7) & ~7; // Round to next 8
	screenInfo.height = height;
	screenInfo.fb_length = screenInfo.line_stride * (screenInfo.height + 1) * BPP;

	screenInfo.x_offset = (screenInfo.line_stride - screenInfo.width) / 2; // Put picture in the middle

	void * fb = (char*)malloc(screenInfo.fb_length);
	if (fb == NULL)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to allocate a frame buffer for a screen.");
		return -1;		
	}

	serverScreen = rfbGetScreen(&argc, NULL, screenInfo.line_stride, screenInfo.height, 5, 3, BPP);
	if (serverScreen == NULL)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to allocate a server screen.");
		free(fb);
		return -1;
	}

	__android_log_print(ANDROID_LOG_INFO, TAG, "VNC screen allocated: %dx%d (%dx%d) lstride:%d xoffset:%d", screenInfo.width, screenInfo.height, width, height, screenInfo.line_stride, screenInfo.x_offset);

	serverScreen->desktopName = "CatVision.io client"; //TODO: Name of the client to be passed from SeaCat
	serverScreen->frameBuffer = fb;
	serverScreen->alwaysShared = (1==1);
	
	serverScreen->inetdSock = -1;
	serverScreen->autoPort = 0;
	serverScreen->port=0;
	serverScreen->ipv6port=0;
	serverScreen->udpPort=0;
	serverScreen->httpInitDone = TRUE;

	serverScreen->newClientHook = newClientHook;
	serverScreen->ptrAddEvent = ptrAddEvent;
	serverScreen->kbdAddEvent = kbdAddEvent;
	serverScreen->kbdReleaseAllKeys = kbdReleaseAllKeys;
	serverScreen->setXCutText = setXCutText;

	rfbInitServer(serverScreen);

	const char * socketDirChar = (*env)->GetStringUTFChars(env, socketDir, 0);
	serverScreen->listenSock = rfbListenOnUNIXPort(socketDirChar);
	(*env)->ReleaseStringUTFChars(env, socketDir, socketDirChar);

	FD_SET(serverScreen->listenSock, &(serverScreen->allFds));
	serverScreen->maxFd = serverScreen->listenSock;

	serverShutdown = 0;
	imageReady = 0;

	// Preset framebuffer with black color
	uint16_t * buffer = fb;
	for(int y=0; y<screenInfo.height; y+=1)
	{
    	for(int x=0; x<screenInfo.line_stride; x+=1)
    	{
			buffer[(y*screenInfo.line_stride)+x] = 0;
   		}
	}


	while (rfbIsActive(serverScreen))
	{
		if (imageReady != 0)
		{
			imageReady = 0;
			int ret = takeImage();
			if (ret != 0)
			{
				// takeImage() requested VNC server shutdown
				serverShutdown = 1;
				rfbShutdownServer(serverScreen, (1==1));
				serverShutdown = 2;
				continue;
			}
		}

		long usec = serverScreen->deferUpdateTime*1000;
		rfbProcessEvents(serverScreen, usec);

		if (serverShutdown == 1)
		{
			rfbShutdownServer(serverScreen, (1==1));
			serverShutdown = 2;
		}
	}

	rfbScreenCleanup(serverScreen);
	free(fb);

	serverScreen = NULL;
	serverShutdown = 3;

	return 0;
}


JNIEXPORT jint JNICALL Java_com_teskalabs_cvio_cviojni_shutdown_1vnc_1server(JNIEnv * env, jclass cls)
{
	if (serverShutdown == 0) serverShutdown = 1;
	return 0;
}


JNIEXPORT void JNICALL Java_com_teskalabs_cvio_cviojni_image_1ready(JNIEnv * env, jclass cls)
{
	imageReady += 1;
}


JNIEXPORT jint JNICALL Java_com_teskalabs_cvio_cviojni_push_1pixels_1rgba_18888(JNIEnv * env, jclass cls, jobject pixels, jint s_stride)
{
	if (serverShutdown != 0) return 0;
	if (serverScreen == NULL) return 0;

	jbyte * fbptr;
	fbptr = (*env)->GetDirectBufferAddress(env, pixels); 
	if (fbptr == 0)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to get direct buffer address.");
		return -1;
	}

	int len = (*env)->GetDirectBufferCapacity(env, pixels); 
	if (len <= 0)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "Invalid direct buffer len: %d", len);
		return -1;
	}

	// This needs to be super-optimized!
	// TODO: See NEON/SIMD optimisations in libyuv https://chromium.googlesource.com/libyuv/libyuv/+/master/source/row_neon64.cc 
	uint8_t * s = (uint8_t *)fbptr;
	uint16_t * t = (uint16_t *)serverScreen->frameBuffer;

	int max_x=-1,max_y=-1, min_x=99999, min_y=99999;

	for (int y=0; y < screenInfo.height; y+=1)
	{
		int tpos = (y * screenInfo.line_stride) + screenInfo.x_offset;
		const int spos = y * s_stride;

		for (int x=0; x < screenInfo.width; x+=1, tpos+=1)
		{
			const int si = spos + (x << 2); //2 is for 32bit
			if ((si+2) >= len)
			{
				__android_log_print(ANDROID_LOG_ERROR, TAG, "Direct buffer overflow: %d vs %d %dx%d spos:%d", si+2, len, x, y, spos);
				return -1;
			}

			const uint16_t r = s[si+0] >> 3;
			const uint16_t g = s[si+1] >> 3;
			const uint16_t b = s[si+2] >> 3;

			const uint16_t p = (b << 10) | (g << 5) | r;
			
			if (t[tpos] == p) continue; // No update needed
			t[tpos] = p;

			if (x > max_x) max_x = x;
			if (x < min_x) min_x = x;
			if (y > max_y) max_y = y;
			if (y < min_y) min_y = y;
		}
	}

	if (max_x == -1) return 0; // No update needed

	rfbMarkRectAsModified(serverScreen,
		screenInfo.x_offset + min_x    , min_y,
		screenInfo.x_offset + max_x + 1, max_y + 1
	);

	return 0;
}


JNIEXPORT jint JNICALL Java_com_teskalabs_cvio_cviojni_push_1pixels_1rgba_1565(JNIEnv * env, jclass cls, jobject pixels, jint s_stride)
{
	if (serverShutdown != 0) return 0;
	if (serverScreen == NULL) return 0;

	jbyte * fbptr;
	fbptr = (*env)->GetDirectBufferAddress(env, pixels); 
	if (fbptr == 0)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to get direct buffer address.");
		return -1;
	}

	int len = (*env)->GetDirectBufferCapacity(env, pixels); 
	if (len <= 0)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "Invalid direct buffer len: %d", len);
		return -1;
	}

	// This needs to be super-optimized!
	// TODO: See NEON/SIMD optimisations in libyuv https://chromium.googlesource.com/libyuv/libyuv/+/master/source/row_neon64.cc 
	uint16_t * s = (uint16_t *)fbptr; // RGB_565
	uint16_t * t = (uint16_t *)serverScreen->frameBuffer; // RGB_555

	int max_x=-1,max_y=-1, min_x=99999, min_y=99999;

	for (int y=0; y < screenInfo.height; y+=1)
	{
		int tpos = (y * screenInfo.line_stride) + screenInfo.x_offset;
		const int spos = y * (s_stride / 2);

		for (int x=0; x < screenInfo.width; x+=1, tpos+=1)
		{
			const int si = spos + x;
			if ((si*2) > len)
			{
				__android_log_print(ANDROID_LOG_ERROR, TAG, "Direct buffer overflow: %d vs %d %dx%d spos:%d sceen:%dx%d s_stride:%d", si, len, x, y, spos, screenInfo.width, screenInfo.height, s_stride);
				return -1;
			}

			const uint16_t r = (s[si] >> 11) & 0x001F;
			const uint16_t g = (s[si] >>  6) & 0x001F;
			const uint16_t b = (s[si]      ) & 0x001F;

    		const uint16_t p = (b << 10) | (g << 5) | r;

			if (t[tpos] == p) continue; // No update needed
			t[tpos] = p;

			if (x > max_x) max_x = x;
			if (x < min_x) min_x = x;
			if (y > max_y) max_y = y;
			if (y < min_y) min_y = y;
		}
	}

	if (max_x == -1) return 0; // No update needed

	rfbMarkRectAsModified(serverScreen,
		screenInfo.x_offset + min_x    , min_y,
		screenInfo.x_offset + max_x + 1, max_y + 1
	);

	return 0;
}



// Callbacks section

static void ptrAddEvent(int buttonMask, int x, int y, rfbClientPtr cl)
{	
	JNIEnv * g_env;
	assert(g_java_vm != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			__android_log_print(ANDROID_LOG_ERROR, TAG, "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "version not supported");
		return;
	}

	if (g_delegate_obj != NULL)
	{
		jstring jClient = (*g_env)->NewStringUTF(g_env, cl->host);
		(*g_env)->CallVoidMethod(g_env, g_delegate_obj, g_ra_JNICALLBACK_rfbPtrAddEventProc_mid, buttonMask, x-screenInfo.x_offset, y, jClient, NULL);
		if (jClient != NULL) (*g_env)->DeleteLocalRef(g_env, jClient);
	}

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);
}


enum rfbNewClientAction newClientHook(rfbClientPtr cl)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);

	enum rfbNewClientAction ret = RFB_CLIENT_ACCEPT;

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			__android_log_print(ANDROID_LOG_ERROR, TAG, "AttachCurrentThread failed");
			return ret;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "version not supported");
		return ret;
	}

	if (g_delegate_obj != NULL)
	{
		jstring jClient = (*g_env)->NewStringUTF(g_env, cl->host);
		jint ret = (*g_env)->CallIntMethod(g_env, g_delegate_obj, g_ra_JNICALLBACK_rfbNewClientHook_mid, jClient, NULL);
		if (jClient != NULL) (*g_env)->DeleteLocalRef(g_env, jClient);
	}

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);


	switch (ret)
	{
		case 0:
			ret = RFB_CLIENT_ACCEPT;
			break;

		case 1:
			ret = RFB_CLIENT_ON_HOLD;
			break;

		case 2:
			ret = RFB_CLIENT_REFUSE;
			break;
	}

	return ret;	
}


void kbdAddEvent(rfbBool down, rfbKeySym keySym, struct _rfbClientRec* cl)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			__android_log_print(ANDROID_LOG_ERROR, TAG, "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "version not supported");
		return;
	}

	jlong jkeysym = keySym;

	if (g_delegate_obj != NULL)
	{
		jstring jClient = (*g_env)->NewStringUTF(g_env, cl->host);
		(*g_env)->CallVoidMethod(g_env, g_delegate_obj, g_ra_JNICALLBACK_rfbKbdAddEventProc_mid, down != 0, jkeysym, jClient, NULL);
		if (jClient != NULL) (*g_env)->DeleteLocalRef(g_env, jClient);
	}

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);
}

void kbdReleaseAllKeys(struct _rfbClientRec* cl)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			__android_log_print(ANDROID_LOG_ERROR, TAG, "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "version not supported");
		return;
	}

	if (g_delegate_obj != NULL)
	{
		jstring jClient = (*g_env)->NewStringUTF(g_env, cl->host);
		(*g_env)->CallVoidMethod(g_env, g_delegate_obj, g_ra_JNICALLBACK_rfbKbdReleaseAllKeysProc_mid, jClient, NULL);
		if (jClient != NULL) (*g_env)->DeleteLocalRef(g_env, jClient);
	}

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);
}


void setXCutText(char* str,int len, struct _rfbClientRec* cl)
{
	JNIEnv * g_env;
	assert(g_java_vm != NULL);

	int getEnvStat = (*g_java_vm)->GetEnv(g_java_vm, (void **)&g_env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if ((*g_java_vm)->AttachCurrentThread(g_java_vm, (JNINATIVEINTERFACEPTR) &g_env, NULL) != 0)
		{
			__android_log_print(ANDROID_LOG_ERROR, TAG, "AttachCurrentThread failed");
			return;
		}
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		__android_log_print(ANDROID_LOG_ERROR, TAG, "version not supported");
		return;
	}

	char text_buf[len + 1];
	memcpy(text_buf, str, len);
	text_buf[len] = '\0';

	if (g_delegate_obj != NULL)
	{
		jstring jClient = (*g_env)->NewStringUTF(g_env, cl->host);
		jstring jText = (*g_env)->NewStringUTF(g_env, text_buf);
		(*g_env)->CallVoidMethod(g_env, g_delegate_obj, g_ra_JNICALLBACK_rfbSetXCutTextProc_mid, jText, jClient, NULL);
		if (jText != NULL) (*g_env)->DeleteLocalRef(g_env, jText);
		if (jClient != NULL) (*g_env)->DeleteLocalRef(g_env, jClient);
	}

	if (getEnvStat == JNI_EDETACHED)
		(*g_java_vm)->DetachCurrentThread(g_java_vm);

}
