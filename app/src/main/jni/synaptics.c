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
int fd_reset = -1;
int fd_status = -1;
int fd_report_type = -1;
int fd_get_report = -1;
int fd_report_size = -1;
int fd_report_data = -1;
//int fd_rxObjThresh = -1;
int fd_txObjThresh = -1;

int SatCap = 0;
int Threshold = 0;
int frameTx = 0;
int frameRx = 0;
int gearCount = 0;
int hasCtrl113 = 0;
int enHybridOnRx = 0;
int enHybridOnTx = 0;
//int rxObjThresh;
int txObjThresh;

jshort *fill = NULL;
jint *fill_rxtx = NULL;
const char *dpath;

int sloc_panel = 0;
int receivers_on_x = 0;
int dimX = 0;
int dimY = 0;

int has_c146 = 1; /* TODO: finish DEFINE_REG_EXISTS */ 
int has_c99 = 1;
/*
#define DEFINE_REG_EXISTS(reg, field) \
int reg##field##exists() { \
	int fd; \
	if (dpath) {\
		sprintf(path, "%s/f54/" #reg  "_" #field, dpath); \
		LOGE("opening %s\n", path); \
		fd = open(path, O_RDWR); \
		if (fd < 0) { \
			LOGE("failed to open %s\n", #reg "_" #field); \
			return 0; \
		} \
		else { \
			close(fd); \
			return 1; \
		} \
	} \
	else \
		LOGE("reg %s does not exist\n", #reg "_" #field); \
		return 0; \
}


#define DEFINE_WORD_REG_EXISTS(reg, field) \
DEFINE()
int reg##field##exists() { \
*/

#define DEFINE_REG(reg, field, cond) \
int fd_##reg##field = -1; \
int set##reg##field(char val) { \
	if (cond && dpath) { \
    		char val_str[6]; \
		char path[256]; \
		if (fd_##reg##field == -1) { \
			sprintf(path, "%s/f54/" #reg  "_" #field, dpath); \
			LOGE("opening %s\n", path); \
			fd_##reg##field = open(path, O_RDWR); \
			if (fd_##reg##field < 0) { \
				LOGE("failed to open fd_%s\n", #reg "_" #field); \
				return -1; \
			} \
		} \
		if (fd_##reg##field > 0) { \
			LOGE("setting %s to %d\n", #reg "_" #field, val); \
			sprintf(val_str, "%d", val); \
			lseek(fd_##reg##field, 0, SEEK_SET); \
			write(fd_##reg##field, val_str, strlen(val_str)); \
		} \
		else { \
			LOGE( "error writing %s\n",  #reg "_" #field); \
			return -1; \
		} \
		return 0; \
	} \
	else \
		return -1; \
} \
char get##reg##field() { \
	if (cond && dpath) { \
    		char val_str[6] = {0}; \
		char path[256]; \
		char val = 0; \
		if (fd_##reg##field == -1) { \
			sprintf(path, "%s/f54/" #reg  "_" #field, dpath); \
			LOGE("opening %s\n", path); \
			fd_##reg##field = open(path, O_RDWR); \
			if (fd_##reg##field < 0) { \
				printf("failed to open fd_%s\n", #reg "_" #field); \
				return -1; \
			} \
		} \
		if (fd_##reg##field > 0) { \
			lseek(fd_##reg##field, 0, SEEK_SET); \
			read(fd_##reg##field, val_str, sizeof(val_str)-1); \
			val = atoi(val_str); \
			LOGE( "%s is %s\n",  #reg "_" #field, val_str); \
			return val; \
		} \
		else { \
			LOGE( "error reading %s\n",  #reg "_" #field); \
			return -1; \
		} \
	} \
	else \
		return -1; \
}

#define EXPORT_REG(type, reg, field, javaname) \
JNIEXPORT int JNICALL \
Java_com_motorola_ghostbusters_TouchDevice_diagSet##javaname(JNIEnv* env, jobject obj, type val) \
{ \
	return set##reg##field(val); \
} \
JNIEXPORT type JNICALL \
Java_com_motorola_ghostbusters_TouchDevice_diag##javaname(JNIEnv* env, jobject obj) \
{ \
	return (type)get##reg##field(); \
}

#define DEFINE_WORD_REG(reg, field, cond) \
DEFINE_REG(reg, field##_lsb, cond) \
DEFINE_REG(reg, field##_msb, cond) \
int set##reg##field(unsigned short val) { \
	if (set##reg##field##_lsb(val & 0xFF) || set##reg##field##_msb(val >> 8)) \
		return -1; \
	else { \
		return 0; \
	} \
} \
int get##reg##field() { \
	char lsb = get##reg##field##_lsb(); \
	char msb = get##reg##field##_msb(); \
	int val; \
	LOGE( "lsb is %d\n",  lsb); \
	LOGE( "msb is %d\n",  msb); \
	val = (msb << 8) | lsb; \
	LOGE( "%s is %d\n",  #reg "_" #field, val); \
	return val; \
}

DEFINE_WORD_REG(c99, int_dur, has_c99)
EXPORT_REG(int, c99, int_dur, TranscapIntDur)

DEFINE_WORD_REG(c146, int_dur, has_c146)
EXPORT_REG(int, c146, int_dur, HybridIntDur)

DEFINE_REG(c113, rx_obj_thresh, hasCtrl113)
EXPORT_REG(int, c113, rx_obj_thresh, RxObjThresh)

DEFINE_REG(c146, stretch_dur, has_c146)
EXPORT_REG(int, c146, stretch_dur, HybridStretchDur)

static void reset_touch()
{
    char val[6] = "1";
    LOGE("resetting touch\n");
	lseek(fd_reset, 0, SEEK_SET);
    write(fd_reset, val, 1);
    return;
}

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

	sprintf(path, "%s/f54/reset", dpath);
    	fd_reset = open(path, O_RDWR);
    	if (fd_reset < 0) {
    		printf("failed to open reset\n");
    		return -1;
    	}

//    reset_touch();

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

	fill = malloc(frameTx*frameRx*sizeof(short));
	if (fill == NULL) return -1;
	printf("%s: allocated %dx%d array of shorts @ %p\n", __FUNCTION__, frameTx, frameRx, fill);

	fill_rxtx = malloc((frameTx + frameRx) * sizeof(jint));
    	if (fill_rxtx == NULL) return -1;
    printf("%s: allocated %d + %d array of jint @ %p\n", __FUNCTION__, frameTx, frameRx, fill_rxtx);


	sprintf(path, "%s/f54/f55_q2_has_single_layer_multitouch", dpath);
	fd = open(path, O_RDONLY);
	if (fd < 0) {
		printf("failed to open f55_q2_has_single_layer_multitouch\n");
		return -1;
	}
	read(fd, val, 6);
	close(fd);
	sloc_panel = atoi(val);
	printf("sloc panel = %d\n", sloc_panel);

	sprintf(path, "%s/f54/f55_c0_receivers_on_x", dpath);
	fd = open(path, O_RDONLY);
	if (fd < 0) {
		printf("failed to open f55_c0_receivers_on_x\n");
		return -1;
	}
	read(fd, val, 6);
	close(fd);
	receivers_on_x = atoi(val);
	printf("receivers on x = %d\n", receivers_on_x);

	if (sloc_panel) {
	    dimX = frameTx * 2;
	    dimY = frameRx / 2;
	 }
	 else if (receivers_on_x) {
	    dimX = frameTx;
	    dimY = frameRx;
	 }
	 else {
	    dimX = frameRx;
	    dimY = frameTx;
	 }



	sprintf(path, "%s/f54/status", dpath);
    fd_status = open(path, O_RDONLY);
    if (fd_status < 0) {
    	printf("failed to open status\n");
    	return -1;
    }

    sprintf(path, "%s/f54/report_type", dpath);
    fd_report_type = open(path, O_RDWR);
    if (fd_report_type < 0) {
      	printf("failed to open report_type\n");
      	return -1;
    }

	sprintf(path, "%s/f54/get_report", dpath);
    fd_get_report = open(path, O_RDWR);
    if (fd_get_report < 0) {
    	printf("failed to open get_report\n");
        return -1;
    }

    sprintf(path, "%s/f54/report_size", dpath);
    fd_report_size = open(path, O_RDONLY);
    if (fd_report_size < 0) {
    	printf("failed to open report_size\n");
    	return -1;
    }

    sprintf(path, "%s/f54/report_data", dpath);
    fd_report_data = open(path, O_RDONLY);
    if (fd_report_data < 0) {
    	printf("failed to open report_data\n");
    	return -1;
    }

	sprintf(path, "%s/f54/saturation_cap", dpath);
	fd_satCap = open(path, O_RDWR);
	if (fd_satCap < 0) {
		printf("failed to open saturation_cap\n");
		return -1;
	}

	hasCtrl113 = 0;
	sprintf(path, "%s/f54/has_ctrl113", dpath);
	fd = open(path, O_RDONLY);
	if (fd < 0) {
		printf("failed to open has_ctrl113\n");
	}
	else {
		read(fd, val, 6);
		close(fd);
		hasCtrl113 = atoi(val);
		printf("control 113 is %s\n", hasCtrl113 ? "present" : "not present");
    }

	if (hasCtrl113) {
		sprintf(path, "%s/f54/c113_en_hybrid_on_tx", dpath);
		fd = open(path, O_RDONLY);
		if (fd < 0) {
			printf("failed to open c113_en_hybrid_on_tx\n");
			return -1;
		}
		read(fd, val, 6);
		close(fd);
		enHybridOnTx = atoi(val);
		printf("hybrid on tx is %s\n", enHybridOnTx ? "enabled" : "not enabled");

		sprintf(path, "%s/f54/c113_en_hybrid_on_rx", dpath);
		fd = open(path, O_RDONLY);
		if (fd < 0) {
			printf("failed to open c113_en_hybrid_on_rx\n");
			return -1;
		}
		read(fd, val, 6);
		close(fd);
		enHybridOnRx = atoi(val);
		printf("hybrid on rx is %s\n", enHybridOnRx ? "enabled" : "not enabled");
/*
		sprintf(path, "%s/f54/c113_rx_obj_thresh", dpath);
		fd_rxObjThresh = open(path, O_RDWR);
		if (fd_rxObjThresh < 0) {
			printf("failed to open c113_rx_obj_thresh\n");
			return -1;
		}

		read(fd_rxObjThresh, val, 6);
        	rxObjThresh = atoi(val);
*/
		sprintf(path, "%s/f54/c113_tx_obj_thresh", dpath);
		fd_txObjThresh = open(path, O_RDWR);
		if (fd_txObjThresh < 0) {
			printf("failed to open c113_tx_obj_thresh\n");
			return -1;
		}

		read(fd_txObjThresh, val, 6);
		txObjThresh = atoi(val);
	}

	sprintf(path, "%s/f54/force_update", dpath);
	fd_forceUpdate = open(path, O_RDWR);
	if (fd_forceUpdate < 0) {
		printf("failed to open force_update\n");
		return -1;
	}

	sprintf(path, "%s/query", dpath);
	fd_query = open(path, O_RDWR);
	if (fd_query < 0) {
		printf("failed to open query\n");
		return -1;
	}

	sprintf(path, "%s/reporting", dpath);
	fd_reporting = open(path, O_RDWR);
	if (fd_reporting < 0) {
		printf("failed to open reporting\n");
		return -1;
	}

	sprintf(path, "%s/f54/c95_disable", dpath);
	fd_gearsEnabled = open(path, O_RDWR);
	printf("%s: fd_gearsEnabled = %d\n", __FUNCTION__, fd_gearsEnabled);
	if (fd_gearsEnabled < 0) {
		printf("failed to open c95_disable (%d: %s)\n", errno, strerror(errno));
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
	
	printf("exiting %s\n", __FUNCTION__);

	return 0;
}

// returns flag indication presence of hybrid baseline control
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagHasHybridBaselineControl(JNIEnv* env, jobject obj)
{
	if (hasCtrl113)
		return 1;
	else
		return 0;
}


// returns Tx Object Threshold
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagTxObjThresh(JNIEnv* env, jobject obj)
{
	if (hasCtrl113)
		return txObjThresh;
	else
		return -1;
}
/*
// returns Rx Object Threshold
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagRxObjThresh(JNIEnv* env, jobject obj)
{
	if (hasCtrl113)
		return rxObjThresh;
	else
		return -1;
}
*/
// sets Tx Object Threshold
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagSetTxObjThresh(JNIEnv* env, jobject obj, int thresh)
{
	LOGE("function called %s\n", __FUNCTION__);
	if (hasCtrl113) {
    	char val[12];
		LOGE("setting TX threshold to %d\n", thresh);
		sprintf(val, "%d", thresh);
		lseek(fd_txObjThresh, 0, SEEK_SET);
        write(fd_txObjThresh, val, strlen(val));
        txObjThresh = thresh;
		return 0;
	} else
		return -1;
}
/*
// sets Rx Object Threshold
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagSetRxObjThresh(JNIEnv* env, jobject obj, int thresh)
{
	LOGE("function called %s\n", __FUNCTION__);
	if (hasCtrl113) {
    	char val[12];
		LOGE("setting RX threshold to %d\n", thresh);
		sprintf(val, "%d", thresh);
		lseek(fd_rxObjThresh, 0, SEEK_SET);
        write(fd_rxObjThresh, val, strlen(val));
        rxObjThresh = thresh;
		return 0;
	} else
		return -1;
}
*/
// returns true if hybrid is using rx
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagEnHybridOnRx(JNIEnv* env, jobject obj)
{
	if (hasCtrl113)
		return enHybridOnRx;
	else
		return 0;
}

// returns true if hybrid is using tx
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagEnHybridOnTx(JNIEnv* env, jobject obj)
{
	if (hasCtrl113)
		return enHybridOnTx;
	else
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

	LOGE("function called %s\n", __FUNCTION__);

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

	LOGE("function called %s %d\n", __FUNCTION__, gears);

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

	LOGE("function called %s\n", __FUNCTION__);

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

	LOGE("function called %s %d\n", __FUNCTION__, disable);

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

	LOGE("function called %s %d\n", __FUNCTION__, gear);

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

	LOGE("function called %s\n", __FUNCTION__);

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

	LOGE("function called %s\n", __FUNCTION__);

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
	int i, size;
	char buf[10];
	char path[256];
	static int count = 0;

	LOGE("function called %s\n", __FUNCTION__);

	lseek(fd_status, 0, SEEK_SET);
    read(fd_status, buf, 10);
    status = atoi(buf);
	if (status != 0) {
		LOGE("%d successful reports processed\n", count);
		count = 0;
		/* if touch report is in progress wait until it completes or times out */
		for (i=0; status == 1 & i < 25; i++) {
    		LOGE("Touch is busy. Waiting...\n");
    		usleep(50000);
    		lseek(fd_status, 0, SEEK_SET);
            read(fd_status, buf, 10);
    	}
    	/* if not idle or timedout touch has to be reset */
    	if (!(status == 0 || status == -110)) {
    		LOGE("Touch is still busy. Aborting.\n");
    		return -1;
    	}
    }

	lseek(fd_report_type, 0, SEEK_SET);
	write(fd_report_type, "2", 1);

	lseek(fd_get_report, 0, SEEK_SET);
	write(fd_get_report, "1", 1);

	for(i=0; i<40; i++) {
		lseek(fd_status, 0, SEEK_SET);
		read(fd_status, buf, 10);
		status = atoi(buf);
		if (status == 0)
			break;
		usleep(50000);
	}

	if (status == -110) {
		LOGE("Timed out waiting for report completion.\n");
		return -1;
	} else if (status == 1) {
		LOGE("Touch is still busy.\n");
		return -1;
	} else if (status != 0) {
      		LOGE("Unknown error (%d).\n", status);
      		return -1;
    }

	lseek(fd_report_size, 0, SEEK_SET);
    read(fd_report_size, buf, 10);
    size = atoi(buf);

	if (size != frameTx * frameRx * sizeof(uint16_t))
	{
		LOGE("report size mismatch %d != %d*%d*%u\n",
			size,
			frameRx,
			frameTx,
			(unsigned int)sizeof(uint16_t));
		return -1;
	}

	lseek(fd_report_data, 0, SEEK_SET);
	ret = read(fd_report_data, data, size);
	if (ret != size){
		LOGE("can't read report data ret=%d size=%d\n", ret, size);
		return -1;
	}

	++count;
	if (!(count % 100)) {
		LOGE("Success counter = %d\n", count);
	}

	return 0;
}

int synaptics_rx_tx_report(char report_type, int32_t* data)
{
	int ret = 0;
	int status;
	int i, size;
	char buf[10];
	char path[256];
	static int count = 0;
	char report_type_str[4] = {0};

	LOGE("function called %s\n", __FUNCTION__);

	lseek(fd_status, 0, SEEK_SET);
    read(fd_status, buf, 10);
    status = atoi(buf);
	if (status != 0) {
		LOGE("%d successful reports processed\n", count);
		count = 0;
		/* if touch report is in progress wait until it completes or times out */
		for (i=0; status == 1 & i < 25; i++) {
    		LOGE("Touch is busy. Waiting...\n");
    		usleep(50000);
    		lseek(fd_status, 0, SEEK_SET);
            read(fd_status, buf, 10);
    	}
    	/* if not idle or timedout touch has to be reset */
    	if (!(status == 0 || status == -110)) {
    		LOGE("Touch is still busy. Aborting.\n");
    		return -1;
    	}
    }

	sprintf(report_type_str, "%u", (uint16_t)report_type);
	lseek(fd_report_type, 0, SEEK_SET);
	write(fd_report_type, report_type_str, strlen(report_type_str));

	lseek(fd_get_report, 0, SEEK_SET);
	write(fd_get_report, "1", 1);

	for(i=0; i<40; i++) {
		lseek(fd_status, 0, SEEK_SET);
		read(fd_status, buf, 10);
		status = atoi(buf);
		if (status == 0)
			break;
		usleep(50000);
	}

	if (status == -110) {
		LOGE("Timed out waiting for report completion.\n");
		return -1;
	} else if (status == 1) {
		LOGE("Touch is still busy.\n");
		return -1;
	} else if (status != 0) {
      		LOGE("Unknown error (%d).\n", status);
      		return -1;
    }

	lseek(fd_report_size, 0, SEEK_SET);
    read(fd_report_size, buf, 10);
    size = atoi(buf);

	if (size != (frameTx + frameRx) * sizeof(int32_t))
	{
		LOGE("report size mismatch %d != (%d+%d)*%u\n",
			size,
			frameRx,
			frameTx,
			(unsigned int)sizeof(jint));
		return -1;
	}

	lseek(fd_report_data, 0, SEEK_SET);
	ret = read(fd_report_data, data, size);
	if (ret != size){
		LOGE("can't read report data ret=%d size=%d\n", ret, size);
		return -1;
	}

	++count;
	if (!(count % 100)) {
		LOGE("Success counter = %d\n", count);
	}

	return 0;
}

// returns logical OR of min and max peaks in a delta frame (2 MSBs are negative peak, 2 LSBs are positive)
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagDeltaPeaks(JNIEnv* env, jobject obj, int loops)
{
//	short *array;
	int rx, tx, delta, d_min, d_max;
	int g_min = 0, g_max = 0, l;

	LOGE("function called %s\n", __FUNCTION__);

//	array = malloc(frameRx*frameTx*sizeof(short));
//	synaptics_report2(array);
//int indexS;
//	for (indexS=0;;indexS++) if (indexS<100000) break;
	for (l=0; l<loops; l++) {
		int status = synaptics_report2(fill);

		if (status != 0)
			return 0;

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

// returns logical OR of min and max peaks in a Hybrid Abs RX Delta (2 MSBs are negative peak, 2 LSBs are positive)
// this is for report type 59
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagRxDeltaPeaks(JNIEnv* env, jobject obj, int loops)
{
//	short *array;
	int rx, tx, delta, d_min, d_max;
	int g_min = 0, g_max = 0, l;

	LOGE("function called %s\n", __FUNCTION__);

	if (!hasCtrl113) {
		LOGE("%s called while F54.c113 is not present\n", __FUNCTION__);
		return -1;
	}

//	array = malloc(frameRx*frameTx*sizeof(short));
//	synaptics_report2(array);
//int indexS;
//	for (indexS=0;;indexS++) if (indexS<100000) break;
	for (l=0; l<loops; l++) {
		int status = synaptics_rx_tx_report(59, fill_rxtx);

		if (status != 0)
			return 0;

		d_min = d_max = 0;
		for(rx=0; rx<frameRx; rx++) {
							//delta = *(array+tx*frameRx+rx);
				delta = *(fill_rxtx + rx);
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

// returns array of min/max peaks for Hybrid Abs TX RX Delta reports 59 or 63
// [0] = Tx min
// [1] = Tx max
// [2] = Rx min
// [3] = Rx max
JNIEXPORT jintArray JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagRxTxDeltaPeaks(JNIEnv* env, jobject obj, int loops, int report_type)
{
	int rx, tx, delta, d_min, d_max, l;
	int peaks[4] = {0};
	int *g_minTx = &peaks[0];
	int *g_maxTx = &peaks[1];
	int *g_minRx = &peaks[2];
	int *g_maxRx = &peaks[3];
	jintArray result;

	LOGE("function called %s\n", __FUNCTION__);

	if (!hasCtrl113) {
		LOGE("%s called while F54.c113 is not present\n", __FUNCTION__);
		return NULL;
	}

	result = (*env)->NewIntArray(env, 4);
	if (result == NULL) {
     		return NULL; /* out of memory error thrown */
 	}

	for (l=0; l<loops; l++) {
		int status = synaptics_rx_tx_report(report_type, fill_rxtx);

		if (status != 0)
			return NULL;

		d_min = d_max = 0;
		for(rx=0; rx<frameRx; rx++) {
			delta = *(fill_rxtx + rx);
			if (delta > d_max)
				d_max = delta;
			if (delta < d_min)
				d_min = delta;
		}
		if (d_min < *g_minRx)
			*g_minRx = abs(d_min);
		if (d_max > *g_maxRx)
			*g_maxRx = d_max;

		d_min = d_max = 0;
		for(tx = 0; tx < frameTx; tx++) {
			delta = *(fill_rxtx + frameRx + tx);
			if (delta > d_max)
				d_max = delta;
			if (delta < d_min)
				d_min = delta;
		}
		if (d_min < *g_minTx)
			*g_minTx = abs(d_min);
		if (d_max > *g_maxTx)
			*g_maxTx = d_max;
	}
	
	(*env)->SetIntArrayRegion(env, result, 0, 4, peaks);
	return result;
}


// returns number of Tx lines
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagFrameY(JNIEnv* env, jobject obj)
{
        return dimY;
}

// returns number of Rx lines
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagFrameX(JNIEnv* env, jobject obj)
{
        return dimX;
}

static void compose_sloc_frame(int frameTx, int frameRx, short *array) {
	int tx, rx;
	int x, y;
	static uint16_t *dest = NULL;

	if (!dest)
		dest = malloc(dimX * dimY * sizeof(short));

	x = 0;
	y = 0;
	for(tx = 0; tx < frameTx; ++tx) {
		for(rx = 0; rx < frameRx; ++rx) {
			dest[y * dimX + x] = array[tx * frameRx + rx];
			if (y == (dimY - 1)) {
            	y = 0;
            	++x;
            }
            else
				++y;
		}
	}

	memcpy(array, dest, frameTx * frameRx * sizeof(short));
}

static void invert_frame(int frameTx, int frameRx, short *array)
{
	int tx, rx;
	static uint16_t *dest = NULL;

	if (!dest)
		dest = malloc(frameTx * frameRx * sizeof(short));

	for (tx = 0; tx < frameTx; ++tx)
		for (rx = 0; rx < frameRx; ++rx) {
			dest[frameTx * rx + tx] = array[frameRx * tx + rx];
		}

	memcpy(array, dest, frameTx * frameRx * sizeof(short));
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

	if (sloc_panel)
	{
		compose_sloc_frame(frameTx, frameRx, fill);
	}
	else if (receivers_on_x)
	{
		invert_frame(frameTx, frameRx, fill);
	}

	// move from the temp structure to the java structure
	// (*env)->SetShortArrayRegion(env, result, 0, frameTx*frameRx, array);
	(*env)->SetShortArrayRegion(env, result, 0, frameTx*frameRx, fill);
	return result;
}

JNIEXPORT jintArray JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagRxTxDelta(JNIEnv *env, jobject obj)
{
	jshortArray result;

	result = (*env)->NewIntArray(env, frameTx + frameRx);
	if (result == NULL) {
     		return NULL; /* out of memory error thrown */
 	}

 	//synaptics_report2(array);
 	synaptics_rx_tx_report(59, fill_rxtx);

	// move from the temp structure to the java structure
	// (*env)->SetShortArrayRegion(env, result, 0, frameTx*frameRx, array);
	(*env)->SetIntArrayRegion(env, result, 0, frameTx + frameRx, fill_rxtx);
	return result;
}



// blocks reporting of touch events to framework
JNIEXPORT void JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagDisableTouch(JNIEnv* env, jobject obj)
{
        char val[6] = "0";

	LOGE("function called %s\n", __FUNCTION__);

	lseek(fd_reporting, 0, SEEK_SET);
        write(fd_reporting, val, 1);
        return;
}

// restores reporting of touch events to framework
JNIEXPORT void JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagEnableTouch(JNIEnv* env, jobject obj)
{
        char val[6] = "1";

	LOGE("function called %s\n", __FUNCTION__);

	lseek(fd_reporting, 0, SEEK_SET);
        write(fd_reporting, val, 1);
        return;
}

int touch_event_count()
{
        char val[20] = {0};
	const char prefix[] = "STOPPED(";
	int touch_count = 0; 

	LOGE("function called %s\n", __FUNCTION__);

	lseek(fd_reporting, 0, SEEK_SET);
        read(fd_reporting, val, sizeof(val) - 1);
	if (!strncmp(val, prefix, sizeof(prefix) - 1)) {
		touch_count = atoi(val + sizeof(prefix) - 1);
		return touch_count;
	}
	else
        	return -1;
}

JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagTouchEventCount(JNIEnv* env, jobject obj)
{
	return touch_event_count();
}

JNIEXPORT void JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagForceUpdate(JNIEnv* env, jobject obj)
{
        char val[6] = "1";

	LOGE("function called %s\n", __FUNCTION__);

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

	LOGE("function called %s\n", __FUNCTION__);

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

	LOGE("function called %s\n", __FUNCTION__);

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

	LOGE("function called %s\n", __FUNCTION__);

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

JNIEXPORT void JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagResetTouch(JNIEnv* env, jobject obj)
{
    char val[6] = "1";
    LOGE("function called %s\n", __FUNCTION__);

	lseek(fd_reset, 0, SEEK_SET);
    write(fd_reset, val, 1);
    return;
}

// returns touch report status
JNIEXPORT int JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagStatus(JNIEnv* env, jobject obj)
{
	char buf[6];
	int status = 0;

	LOGE("function called %s\n", __FUNCTION__);

	lseek(fd_status, 0, SEEK_SET);
    read(fd_status, buf, 10);
    status = atoi(buf);

	return status;
}

// closes all opened files
JNIEXPORT void JNICALL
Java_com_motorola_ghostbusters_TouchDevice_diagClose(JNIEnv* env, jobject obj /*, ???*/)
{
	LOGE("function called %s\n", __FUNCTION__);

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
	free(fill_rxtx);

	return;
}
