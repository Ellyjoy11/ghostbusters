#include <jni.h>
#include <string.h>

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <time.h>
#include <malloc.h>
#include <dirent.h>

#include <errno.h>

//#include <linux/delay.h>
#include <android/log.h>

#define MAX_ATTR_PATH 512
#define MAX_ATTR_LEN 32

#define  LOG_TAG    "GhostBusters"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define printf(fmt,args...)  __android_log_print(4  ,LOG_TAG, fmt, ##args)

int fd_gearsEnabled = -1;
int fd_gearCurrent = -1;
int fd_gearAuto = -1;
int fd_satCap = -1;
int fd_forceUpdate = -1;
int fd_reporting= -1;
int fd_query= -1;
int fd_powerIMl = -1;
int fd_powerIMm = -1;
int fd_coherentIMl = -1;
int fd_coherentIMm = -1;

int SatCap = 0;
int Threshold = 0;
int frameTx = 0;
int frameRx = 0;
int gearCount = 0;
jshort *fill = NULL;
char *dpath;

int sloc_interleaved = 0;
int receivers_on_x = 0;

// opens all required files internally and inticates success/error
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagInit(JNIEnv* env, jobject obj, jstring jpath)
{
	char path[256];
	char val[6];
	int fd = -1;

	printf("entering %s\n", __FUNCTION__);

	if (path != NULL) {
		dpath = (*env)->GetStringUTFChars(env, jpath, NULL);
	} else {
		dpath = "/sys/bus/i2c/devices/2-0020";
	}

	LOGE("TOUCH DEVICE path = %s\n", dpath);
	sprintf(path, "%s/f54/c95_disable", dpath);
	fd_gearsEnabled = open(path, O_RDWR);
	printf("%s: fd_gearsEnabled = %d\n", __FUNCTION__, fd_gearsEnabled);
	if (fd_gearsEnabled < 0) {
		printf("failed to open c95_disable\n");
		return -1;
	}

	sprintf(path, "%s/f54/d17_freq", dpath);
	fd_gearCurrent = open(path, O_RDWR);
	if (fd_gearCurrent < 0) {
		printf("failed to open d17_freq\n");
		return -1;
	}


	sprintf(path, "%s/f54/d17_inhibit_freq_shift", dpath);
	fd_gearAuto = open(path, O_RDWR);
	if (fd_gearAuto < 0) {
		printf("failed to open d17_inhibit_freq_shift\n");
		return -1;
	}

	sprintf(path, "%s/f54/saturation_cap", dpath);
	fd_satCap = open(path, O_RDWR);
	if (fd_satCap < 0) {
		printf("failed to open saturation_cap\n");
		return -1;
	}

	sprintf(path, "%s/f54/d6_interference_metric_lsb", dpath);
	fd_powerIMl = open(path, O_RDONLY);
	sprintf(path, "%s/f54/d6_interference_metric_msb", dpath);
	fd_powerIMm = open(path, O_RDONLY);
	if (fd_powerIMl < 0 || fd_powerIMm < 0) {
		printf("failed to open d6_interference_metric\n");
		return -1;
	}

	sprintf(path, "%s/f54/d14_cid_im_lsb", dpath);
	fd_coherentIMl = open(path, O_RDONLY);
	sprintf(path, "%s/f54/d14_cid_im_msb", dpath);
	fd_coherentIMm = open(path, O_RDONLY);
	if (fd_coherentIMl < 0 || fd_coherentIMm < 0) {
		printf("failed tp open d14_cid_im\n");
		return -1;
	}

	sprintf(path, "%s/f54/force_update", dpath);
	fd_forceUpdate = open(path, O_RDWR);
	if (fd_forceUpdate < 0) {
		printf("failed to open force_update\n");
		return -1;
	}

	sprintf(path, "%s/reporting", dpath);
	fd_reporting = open(path, O_RDWR);
	if (fd_reporting < 0) {
		printf("failed to open reporting\n");
		return -1;
	}

	sprintf(path, "%s/f54/num_of_mapped_tx", dpath);
	fd = open(path, O_RDONLY);
	if (fd < 0) {
		printf("failed to open num_of_mapped_tx\n");
		return -1;
	}
	read(fd, val, 6);
	frameTx = atoi(val);

	sprintf(path, "%s/f54/num_of_mapped_rx", dpath);
	fd = open(path, O_RDONLY);
	if (fd < 0) {
		printf("failed to open num_of_mapped_rx\n");
		return -1;
	}
	read(fd, val, 6);
	frameRx = atoi(val);

	sprintf(path, "%s/f54/q17_num_of_sense_freqs", dpath);
	fd = open(path, O_RDONLY);
	if (fd < 0) {
		printf("failed to open q17_num_of_sense_freqs\n");
		return -1;
	}
	printf("%s: fd_gearCount = %d\n", __FUNCTION__, fd);
	read(fd, val, 6);
	gearCount = atoi(val);
	printf("gearCount=%d\n", gearCount);

	fill = malloc(frameTx*frameRx*sizeof(short));
	if (fill == NULL) return -1;
	printf("%s: allocated %dx%d array of shorts @ %p\n", __FUNCTION__, frameTx, frameRx, fill);

	sprintf(path, "%s/query", dpath);
	fd_query = open(path, O_RDWR);
	if (fd_query < 0) {
		printf("failed to open query\n");
		return -1;
	}

	sprintf(path, "%s/f54/f55_q2_has_single_layer_multitouch", dpath);
	fd = open(path, O_RDONLY);
	if (fd < 0) {
		printf("failed to open f55_q2_has_single_layer_multitouch\n");
		return -1;
	}
	read(fd, val, 6);
	close(fd);
	sloc_interleaved = atoi(val);

	sprintf(path, "%s/f54/f55_c0_receivers_on_x", dpath);
	fd = open(path, O_RDONLY);
	if (fd < 0) {
		printf("failed to open f55_c0_receivers_on_x\n");
		return -1;
	}
	read(fd, val, 6);
	close(fd);
	receivers_on_x = atoi(val);

	printf("exiting %s\n", __FUNCTION__);

	return 0;
}


// returns number of gears available
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagGearCount(JNIEnv* env, jobject obj)
{
        return gearCount;
}

// returns bitmask of gears enabled
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagGearsEnabled(JNIEnv* env, jobject obj)
{
	int i, n;
	unsigned gearList = 0;
	char val[64];

	lseek(fd_gearsEnabled, 0, SEEK_SET);
    	read(fd_gearsEnabled, val, 64);
	for(n=0, i=0; i<gearCount; i++, n+=2) {
		if (val[n] == '0')
			gearList |= (1 << i);
	}
	return gearList;
}

// sets enabled gears by bitmask
JNIEXPORT void JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagEnableGears(JNIEnv* env, jobject obj, int gears)
{
        int i, n;
        char val[64];

        for(n=0, i=0; i<gearCount; i++, n+=2) {
		if (gears & (1<<i))
			val[n] = '0';
		else
			val[n] = '1';
		val[n+1] = ' ';
        }
	val[gearCount*2-1] = '\0';
	lseek(fd_gearsEnabled, 0, SEEK_SET);
	write(fd_gearsEnabled, val, gearCount*2);
        return;
}

// returns currently active gear number
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagGearCurrent(JNIEnv* env, jobject obj)
{
        char val[6];
        int  gearcurrent = 0;

	lseek(fd_gearCurrent, 0, SEEK_SET);
        read(fd_gearCurrent, val, 6);
        gearcurrent = atoi(val);

        return gearcurrent;
}

// enables automatic gear selection mode
JNIEXPORT void JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagGearAuto(JNIEnv* env, jobject obj, int disable)
{
        char val[6];

        sprintf(val, "%d", disable);

	lseek(fd_gearAuto, 0, SEEK_SET);
        write(fd_gearAuto, val, 1);
	return;
}

// disables automatic mode and selects explicit gear
JNIEXPORT void JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagGearSelect(JNIEnv* env, jobject obj, int gear)
{
        char val[6] = "1";

	lseek(fd_gearAuto, 0, SEEK_SET);
        write(fd_gearAuto, val, 1);

	sprintf(val, "%d", gear);
	lseek(fd_gearCurrent, 0, SEEK_SET);
	write(fd_gearCurrent, val, 6);

	return;
}

// returns power interference metric value
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagPowerIM(JNIEnv* env, jobject obj)
{
        char msb[4], lsb[4];
        int  powerIM = 0;
	int res = -1;
	int i;

	lseek(fd_powerIMl, 0, SEEK_SET);
	lseek(fd_powerIMm, 0, SEEK_SET);
        res = read(fd_powerIMm, msb, 4);
        res = read(fd_powerIMl, lsb, 4);

        if (res < 0) {
		printf("%s: failed to read PowerIM\n", __FUNCTION__);
		return res;
        } else {
		for(i=0; i<4; i++)
			if (!isdigit(msb[i]) || i==3)
				msb[i] = '\0';
		for(i=0; i<4; i++)
			if (!isdigit(lsb[i]) || i==3)
				lsb[i] = '\0';
        	powerIM = (atoi(msb) << 8) | atoi(lsb);
        	printf("%s: msb=%s, lsb=%s, IM=%d\n", __FUNCTION__, msb, lsb, powerIM);
	}

        return powerIM;
}

// returns coherent interference metric
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagCoherentIM(JNIEnv* env, jobject obj)
{
        char msb[4], lsb[4];
        int  coherentIM = 0;
        int res = -1;
        int i;

        lseek(fd_coherentIMl, 0, SEEK_SET);
        lseek(fd_coherentIMm, 0, SEEK_SET);
        res = read(fd_coherentIMm, msb, 4);
        res = read(fd_coherentIMl, lsb, 4);
        
        if (res < 0) {
                printf("%s: failed to read CoherentIM\n", __FUNCTION__);
                return res;
        } else {
                for(i=0; i<4; i++)
                        if (!isdigit(msb[i]) || i==3)
                                msb[i] = '\0';  
                for(i=0; i<4; i++)
                        if (!isdigit(lsb[i]) || i==3)
                                lsb[i] = '\0';
                coherentIM = (atoi(msb) << 8) | atoi(lsb);
                printf("%s: msb=%s, lsb=%s, IM=%d\n", __FUNCTION__, msb, lsb, coherentIM);
        }
        
        return coherentIM;
}

int synaptics_report2(int16_t* data)
{
	int ret = 0;
	int status;
	int fd = -1;
	int fd_status = -1;
	int i, size;
	char buf[10];
	char path[256];

	sprintf(path, "%s/f54/status", dpath);
	fd_status = open(path, O_RDONLY);
	read(fd_status, buf, 10);
	status = atoi(buf);
	if (status != 0) {
		LOGE("Touch is busy. Aborting.\n");
		return -1;
	}

	sprintf(path, "%s/f54/report_type", dpath);
	fd = open(path, O_RDWR);
	write(fd, "2", 1);
	close(fd);

	sprintf(path, "%s/f54/get_report", dpath);
	fd = open(path, O_RDWR);
	write(fd, "1", 1);
	close(fd);

	for(i=0; i<40; i++) {
		lseek(fd_status, 0, SEEK_SET);
		read(fd_status, buf, 10);
		status = atoi(buf);
		if (status == 0)
			break;
		usleep(50000);
	}
	if (status != 0) {
		LOGE("timed out waiting for report completion.\n");
		return -1;
	}
	close(fd_status);

    sprintf(path, "%s/f54/report_size", dpath);
    fd = open(path, O_RDONLY);
    read(fd, buf, 10);
    size = atoi(buf);
    close(fd);

	if (size != frameTx * frameRx * sizeof(uint16_t))
	{
		LOGE("report size mismatch %d != %d*%d*%d\n",
			size,
			frameRx,
			frameTx,
			sizeof(uint16_t));
		return -1;
	}

    sprintf(path, "%s/f54/report_data", dpath);
	fd = open(path, O_RDONLY);
	ret = read(fd, data, size);
	if (ret != size){
		LOGE("can't read report data ret=%d size=%d\n", ret, size);
		return -1;
	}
	close(fd);

	printf("%s: returning %d bytes\n", __FUNCTION__, size);

	return 0;
}


// returns logical OR of min and max peaks in a delta frame (2 MSBs are negative peak, 2 LSBs are positive)
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagDeltaPeaks(JNIEnv* env, jobject obj, int loops)
{
//	short *array;
	int rx, tx, delta, d_min, d_max;
	int g_min = 0, g_max = 0, l;

//	array = malloc(frameRx*frameTx*sizeof(short));
//	synaptics_report2(array);
//int indexS;
//	for (indexS=0;;indexS++) if (indexS<100000) break;
	for (l=0; l<loops; l++) {
		synaptics_report2(fill);
		d_min = d_max = 0;
		for(tx=0; tx<frameTx; tx++)
			for(rx=0; rx<frameRx; rx++) {
				//delta = *(array+tx*frameRx+rx);
				delta = *(fill+tx*frameRx+rx);
				//printf("%3d ", delta);
				if (delta > d_max)
					d_max = delta;
				if (delta < d_min)
					d_min = delta;
			}
		if (d_min < g_min)
			g_min = d_min;
		if (d_max > g_max)
			g_max = d_max;
	}
//	free(array);
	return (g_max << 16) | abs(g_min);
}

// returns number of Tx lines
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagFrameTx(JNIEnv* env, jobject obj)
{
        return frameTx;
}

// returns number of Rx lines
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagFrameRx(JNIEnv* env, jobject obj)
{
        return frameRx;
}

static void compose_sloc_frame(int frameTx, int frameRx, short *array) {
	int tx, tx_dest;
	uint16_t *dest = malloc(frameTx * frameRx * sizeof(short));
	for(tx = 0, tx_dest = 0; tx < frameTx; tx += 2, ++tx_dest)
		memcpy(dest + tx_dest * frameRx, array + tx * frameRx, frameRx * sizeof(short));

	for(tx = 1; tx < frameTx; tx += 2, ++tx_dest)
		memcpy(dest + tx_dest * frameRx, array + tx * frameRx, frameRx * sizeof(short));

	memcpy(array, dest, frameTx * frameRx * sizeof(short));
	free(dest);
}

// flip XY to match geometry of the touch IC
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagDeltaFrameFlipXY(JNIEnv* env, jobject obj)
{
	return receivers_on_x;
}

JNIEXPORT jshortArray JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagDeltaFrame(JNIEnv *env, jobject obj)
{
	jshortArray result;
	//short *array = malloc(frameTx * frameRx * 2);
	result = (*env)->NewShortArray(env, frameTx*frameRx);
	if (result == NULL) {
     		return NULL; /* out of memory error thrown */
 	}

 	//synaptics_report2(array);
 	synaptics_report2(fill);

	if (sloc_interleaved)
		compose_sloc_frame(frameTx, frameRx, fill);


	// move from the temp structure to the java structure
	// (*env)->SetShortArrayRegion(env, result, 0, frameTx*frameRx, array);
	(*env)->SetShortArrayRegion(env, result, 0, frameTx*frameRx, fill);
	return result;
}

// blocks reporting of touch events to framework
JNIEXPORT void JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagDisableTouch(JNIEnv* env, jobject obj)
{
        char val[6] = "0";

	lseek(fd_reporting, 0, SEEK_SET);
        write(fd_reporting, val, 1);
        return;
}

// restores reporting of touch events to framework
JNIEXPORT void JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagEnableTouch(JNIEnv* env, jobject obj)
{
        char val[6] = "1";

	lseek(fd_reporting, 0, SEEK_SET);
        write(fd_reporting, val, 1);
        return;
}

// restores reporting of touch events to framework
JNIEXPORT void JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagForceUpdate(JNIEnv* env, jobject obj)
{
        char val[6] = "1";

	lseek(fd_forceUpdate, 0, SEEK_SET);
        write(fd_forceUpdate, val, 1);
        return;
}


// returns finger threshold value (absolute)
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagFingerThreshold(JNIEnv* env, jobject obj)
{

	char data[64];
	char *val;
	char *query = "F12@15:0=";

	lseek(fd_query, 0, SEEK_SET);
	write(fd_query, query, strlen(query));
	lseek(fd_query, 0, SEEK_SET);
	read(fd_query, data, 64);
	val = strstr(data, "=");
	if (val != NULL && strlen(val) >= 2) {
		val++; 
		val[2] = '\0';
		Threshold = strtol(val, NULL, 16) * 10 / 25 * SatCap / 100;
		printf("%s: val=%s, Threshold=%d\n", __FUNCTION__, val, Threshold);
	}

//	Threshold = SatCap*20/100;
	return Threshold;
}

// returns finger hysteresis value (absolute)
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagFingerHysteresis(JNIEnv* env, jobject obj)
{
	return Threshold/2;
}

// returns sensor saturation level
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagSaturationLevel(JNIEnv* env, jobject obj)
{
	char val[6];
	//int SatCap = 0;

	lseek(fd_satCap, 0, SEEK_SET);
	read(fd_satCap, val, 6);
	SatCap = atoi(val);

	return SatCap;
}

JNIEXPORT jobjectArray JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagStats(JNIEnv* env, jobject obj)
{
        jobjectArray ret;
        int i;

		FILE * fp;
		char * line = NULL;
		size_t len = 0;
		ssize_t read;
		int maxcount = 15;
		int count = 0;
		char **newstats;
		char **stats = malloc(maxcount*sizeof(char*));
		char path[MAX_ATTR_PATH];


        sprintf(path, "%s/stats", dpath);

		fp = fopen(path, "rw");
		if (fp == NULL)
           LOGE("Failed to open %s\n", path);

        if (!fp)
        	goto out;

		count = 0;
		while ((read = getline(&line, &len, fp)) != -1) {
        	stats[count++] = strdup(line);
        	if (count == maxcount) {
        		maxcount +=5;
        		newstats = realloc(stats,maxcount*sizeof(char*));

        		if (!newstats) {
        			LOGE("Failed to realloc stats - out of memory\n");
        			break;
        		}
        		else
        			stats = newstats;
        	}
        }

		fclose(fp);
		if (line)
        	free(line);

out:

		ret= (*env)->NewObjectArray(env, count,
				(*env)->FindClass(env, "java/lang/String"),
				(*env)->NewStringUTF(env, ""));

		for(i=0;i<count;i++) {
				(*env)->SetObjectArrayElement(env, ret,i,(*env)->NewStringUTF(env, stats[i]));
				free(stats[i]);
		}

        free(stats);
        return(ret);
}

// closes all opened files
JNIEXPORT void JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagClose(JNIEnv* env, jobject obj /*, ???*/)
{
	close(fd_gearsEnabled);
	close(fd_gearCurrent);
	close(fd_gearAuto);
	close(fd_satCap);
	close(fd_forceUpdate);
	close(fd_powerIMm);
	close(fd_powerIMl);
	close(fd_coherentIMm);
	close(fd_coherentIMl);
	close(fd_reporting);
	close(fd_query);

	free(fill);

	return;
}