package gaia.cu9.ari.gaiaorbit.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.badlogic.gdx.math.MathUtils;

import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.system.AbstractRenderSystem;
import gaia.cu9.ari.gaiaorbit.util.concurrent.ThreadIndexer;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory.DateType;
import gaia.cu9.ari.gaiaorbit.util.format.IDateFormat;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;

/**
 * Holds the global configuration options
 *
 * @author Toni Sagrista
 *
 */
public class GlobalConf {
    public static final String APPLICATION_NAME = "Gaia Sky";
    public static final String WEBPAGE = "http://www.zah.uni-heidelberg.de/gaia/outreach/gaiasky/";
    public static final String DOCUMENTATION = "http://gaia-sky.rtfd.io";
    public static final String ICON_URL = "http://www.zah.uni-heidelberg.de/uploads/pics/gaiasandboxlogo_02.png";

    public static final String TEXTURES_FOLDER = "data/tex/";

    public static float SCALE_FACTOR = -1.0f;

    public static void updateScaleFactor(float sf) {
        SCALE_FACTOR = sf;
        Logger.debug(GlobalConf.class.getSimpleName(), "GUI scale factor set to " + GlobalConf.SCALE_FACTOR);
    }

    public static interface IConf {
    }

    public enum ScreenshotMode {
        simple, redraw
    }

    public static class ScreenshotConf implements IConf {

        public int SCREENSHOT_WIDTH;
        public int SCREENSHOT_HEIGHT;
        public String SCREENSHOT_FOLDER;
        public ScreenshotMode SCREENSHOT_MODE;

        public void initialize(int sCREENSHOT_WIDTH, int sCREENSHOT_HEIGHT, String sCREENSHOT_FOLDER, ScreenshotMode sCREENSHOT_MODE) {
            SCREENSHOT_WIDTH = sCREENSHOT_WIDTH;
            SCREENSHOT_HEIGHT = sCREENSHOT_HEIGHT;
            SCREENSHOT_FOLDER = sCREENSHOT_FOLDER;
            SCREENSHOT_MODE = sCREENSHOT_MODE;
        }

        public boolean isSimpleMode() {
            return SCREENSHOT_MODE.equals(ScreenshotMode.simple);
        }

        public boolean isRedrawMode() {
            return SCREENSHOT_MODE.equals(ScreenshotMode.redraw);
        }

    }

    public static class PerformanceConf implements IConf {

        public boolean MULTITHREADING;
        public int NUMBER_THREADS;

        public void initialize(boolean MULTITHREADING, int NUMBER_THREADS) {
            this.MULTITHREADING = MULTITHREADING;
            this.NUMBER_THREADS = NUMBER_THREADS;
        }

        /**
         * Returns the actual number of threads. It accounts for the number of
         * threads being 0 or less, "let the program decide" option, in which
         * case the number of processors is returned.
         *
         * @return
         */
        public int NUMBER_THREADS() {
            if (NUMBER_THREADS <= 0)
                return ThreadIndexer.instance.nthreads();
            else
                return NUMBER_THREADS;
        }

    }

    public static class PostprocessConf implements IConf, IObserver {

        public int POSTPROCESS_ANTIALIAS;
        public float POSTPROCESS_BLOOM_INTENSITY;
        public float POSTPROCESS_MOTION_BLUR;
        public boolean POSTPROCESS_LENS_FLARE;
        public boolean POSTPROCESS_LIGHT_SCATTERING;
        public boolean POSTPROCESS_FISHEYE;
        /** Brightness level in [-1..1]. Default is 0. **/
        public float POSTPROCESS_BRIGHTNESS;
        /** Contrast level in [0..2]. Default is 1. **/
        public float POSTPROCESS_CONTRAST;

        public PostprocessConf() {
            EventManager.instance.subscribe(this, Events.BLOOM_CMD, Events.LENS_FLARE_CMD, Events.MOTION_BLUR_CMD, Events.LIGHT_SCATTERING_CMD, Events.FISHEYE_CMD, Events.BRIGHTNESS_CMD, Events.CONTRAST_CMD);
        }

        public void initialize(int POSTPROCESS_ANTIALIAS, float POSTPROCESS_BLOOM_INTENSITY, float POSTPROCESS_MOTION_BLUR, boolean POSTPROCESS_LENS_FLARE, boolean POSTPROCESS_LIGHT_SCATTERING, boolean POSTPROCESS_FISHEYE, float POSTPROCESS_BRIGHTNESS, float POSTPROCESS_CONTRAST) {
            this.POSTPROCESS_ANTIALIAS = POSTPROCESS_ANTIALIAS;
            this.POSTPROCESS_BLOOM_INTENSITY = POSTPROCESS_BLOOM_INTENSITY;
            this.POSTPROCESS_MOTION_BLUR = POSTPROCESS_MOTION_BLUR;
            this.POSTPROCESS_LENS_FLARE = POSTPROCESS_LENS_FLARE;
            this.POSTPROCESS_LIGHT_SCATTERING = POSTPROCESS_LIGHT_SCATTERING;
            this.POSTPROCESS_FISHEYE = POSTPROCESS_FISHEYE;
            this.POSTPROCESS_BRIGHTNESS = POSTPROCESS_BRIGHTNESS;
            this.POSTPROCESS_CONTRAST = POSTPROCESS_CONTRAST;
        }

        @Override
        public void notify(Events event, Object... data) {
            switch (event) {
            case BLOOM_CMD:
                POSTPROCESS_BLOOM_INTENSITY = (float) data[0];
                break;
            case LENS_FLARE_CMD:
                POSTPROCESS_LENS_FLARE = (Boolean) data[0];
                break;
            case LIGHT_SCATTERING_CMD:
                POSTPROCESS_LIGHT_SCATTERING = (Boolean) data[0];
                break;
            case MOTION_BLUR_CMD:
                POSTPROCESS_MOTION_BLUR = (float) data[0];
                break;
            case FISHEYE_CMD:
                POSTPROCESS_FISHEYE = (Boolean) data[0];
                break;
            case BRIGHTNESS_CMD:
                POSTPROCESS_BRIGHTNESS = MathUtils.clamp((float) data[0], Constants.MIN_BRIGHTNESS, Constants.MAX_BRIGHTNESS);
                break;
            case CONTRAST_CMD:
                POSTPROCESS_CONTRAST = MathUtils.clamp((float) data[0], Constants.MIN_CONTRAST, Constants.MAX_CONTRAST);
                break;
            }
        }

    }

    /**
     * Runtime configuration values, which are never persisted.
     *
     * @author Toni Sagrista
     *
     */
    public static class RuntimeConf implements IConf, IObserver {

        public boolean DISPLAY_GUI;
        public boolean UPDATE_PAUSE;
        public boolean TIME_ON;
        /** Whether we use the RealTimeClock or the GlobalClock **/
        public boolean REAL_TIME;
        public boolean INPUT_ENABLED;
        public boolean RECORD_CAMERA;
        public float LIMIT_MAG_RUNTIME;
        public int OUTPUT_FRAME_BUFFER_SIZE = 50;
        public boolean STRIPPED_FOV_MODE = false;
        /** Whether octree drawing is active or not **/
        public boolean DRAW_OCTREE;

        public RuntimeConf() {
            EventManager.instance.subscribe(this, Events.LIMIT_MAG_CMD, Events.INPUT_ENABLED_CMD, Events.DISPLAY_GUI_CMD, Events.TOGGLE_UPDATEPAUSE, Events.TOGGLE_TIME_CMD, Events.RECORD_CAMERA_CMD);
        }

        public void initialize(boolean dISPLAY_GUI, boolean uPDATE_PAUSE, boolean sTRIPPED_FOV_MODE, boolean tIME_ON, boolean iNPUT_ENABLED, boolean rECORD_CAMERA, float lIMIT_MAG_RUNTIME, boolean rEAL_TIME, boolean dRAW_OCTREE) {
            DISPLAY_GUI = dISPLAY_GUI;
            UPDATE_PAUSE = uPDATE_PAUSE;
            TIME_ON = tIME_ON;
            INPUT_ENABLED = iNPUT_ENABLED;
            RECORD_CAMERA = rECORD_CAMERA;
            LIMIT_MAG_RUNTIME = lIMIT_MAG_RUNTIME;
            STRIPPED_FOV_MODE = sTRIPPED_FOV_MODE;
            REAL_TIME = rEAL_TIME;
            DRAW_OCTREE = dRAW_OCTREE;
        }

        @Override
        public void notify(Events event, Object... data) {
            switch (event) {
            case LIMIT_MAG_CMD:
                LIMIT_MAG_RUNTIME = (float) data[0];
                AbstractRenderSystem.POINT_UPDATE_FLAG = true;
                break;

            case INPUT_ENABLED_CMD:
                INPUT_ENABLED = (boolean) data[0];
                break;

            case DISPLAY_GUI_CMD:
                if (data.length > 1) {
                    // Value
                    Boolean val = (Boolean) data[1];
                    DISPLAY_GUI = val;
                } else {
                    // Toggle
                    DISPLAY_GUI = !DISPLAY_GUI;
                }
                break;
            case TOGGLE_UPDATEPAUSE:
                UPDATE_PAUSE = !UPDATE_PAUSE;
                EventManager.instance.post(Events.UPDATEPAUSE_CHANGED, UPDATE_PAUSE);
                break;
            case TOGGLE_TIME_CMD:
                toggleTimeOn((Boolean) data[0]);
                break;
            case RECORD_CAMERA_CMD:
                toggleRecord((Boolean) data[0]);
                break;

            }

        }

        /**
         * Toggles the time
         */
        public void toggleTimeOn(Boolean timeOn) {
            if (timeOn != null) {
                TIME_ON = timeOn;
            } else {
                TIME_ON = !TIME_ON;
            }
        }

        /**
         * Toggles the record camera
         */
        public void toggleRecord(Boolean rec) {
            if (rec != null) {
                RECORD_CAMERA = rec;
            } else {
                RECORD_CAMERA = !RECORD_CAMERA;
            }
        }

    }

    /**
     * Holds the configuration for the output frame subsystem and the camera recording.
     *
     * @author Toni Sagrista
     *
     */
    public static class FrameConf implements IConf, IObserver {
        /** The width of the image frames **/
        public int RENDER_WIDTH;
        /** The height of the image frames **/
        public int RENDER_HEIGHT;
        /** The number of images per second to produce **/
        public int RENDER_TARGET_FPS;
        /** The target FPS when recording the camera **/
        public int CAMERA_REC_TARGET_FPS;
        /** Automatically activate frame output system when playing camera file **/
        public boolean AUTO_FRAME_OUTPUT_CAMERA_PLAY;
        /** The output folder **/
        public String RENDER_FOLDER;
        /** The prefix for the image files **/
        public String RENDER_FILE_NAME;
        /** Should we write the simulation time to the images? **/
        public boolean RENDER_SCREENSHOT_TIME;
        /** Whether the frame system is activated or not **/
        public boolean RENDER_OUTPUT = false;
        /** The frame output screenshot mode **/
        public ScreenshotMode FRAME_MODE;

        public FrameConf() {
            EventManager.instance.subscribe(this, Events.CONFIG_PIXEL_RENDERER, Events.FRAME_OUTPUT_CMD);
        }

        public void initialize(int rENDER_WIDTH, int rENDER_HEIGHT, int rENDER_TARGET_FPS, int cAMERA_REC_TARGET_FPS, boolean aUTO_FRAME_OUTPUT_CAMERA_PLAY, String rENDER_FOLDER, String rENDER_FILE_NAME, boolean rENDER_SCREENSHOT_TIME, boolean rENDER_OUTPUT, ScreenshotMode fRAME_MODE) {
            RENDER_WIDTH = rENDER_WIDTH;
            RENDER_HEIGHT = rENDER_HEIGHT;
            RENDER_TARGET_FPS = rENDER_TARGET_FPS;
            CAMERA_REC_TARGET_FPS = cAMERA_REC_TARGET_FPS;
            AUTO_FRAME_OUTPUT_CAMERA_PLAY = aUTO_FRAME_OUTPUT_CAMERA_PLAY;
            RENDER_FOLDER = rENDER_FOLDER;
            RENDER_FILE_NAME = rENDER_FILE_NAME;
            RENDER_SCREENSHOT_TIME = rENDER_SCREENSHOT_TIME;
            RENDER_OUTPUT = rENDER_OUTPUT;
            FRAME_MODE = fRAME_MODE;
        }

        @Override
        public void notify(Events event, Object... data) {
            switch (event) {
            case CONFIG_PIXEL_RENDERER:
                RENDER_WIDTH = (int) data[0];
                RENDER_HEIGHT = (int) data[1];
                RENDER_TARGET_FPS = (int) data[2];
                RENDER_FOLDER = (String) data[3];
                RENDER_FILE_NAME = (String) data[4];
                break;
            case FRAME_OUTPUT_CMD:
                if (data.length > 0) {
                    RENDER_OUTPUT = (Boolean) data[0];
                } else {
                    RENDER_OUTPUT = !RENDER_OUTPUT;
                }
                // Flush buffer if needed
                if (!RENDER_OUTPUT && GaiaSky.instance != null) {
                    EventManager.instance.post(Events.FLUSH_FRAMES);
                }
            }
        }
    }

    /**
     * Holds all configuration values related to data.
     *
     * @author Toni Sagrista
     *
     */
    public static class DataConf implements IConf {

        /** The json data file in case of local data source **/
        public String OBJECTS_JSON_FILE;
        /** String with different files for different qualities **/
        public String[] OBJECTS_JSON_FILE_GQ;

        /** The json file with the catalogue(s) to load **/
        public String CATALOG_JSON_FILE;
        /** HYG JSON file **/
        public String HYG_JSON_FILE;
        /** TGAS JSON file **/
        public String TGAS_JSON_FILE;

        /** Whether we are connected to the Object Server in this session **/
        public boolean OBJECT_SERVER_CONNECTION = false;
        /** If we use the ObjectServer, this contains the visualization id **/
        public String VISUALIZATION_ID;
        /** Object server IP address/hostname **/
        public String OBJECT_SERVER_HOSTNAME = "localhost";
        /** Object server port **/
        public int OBJECT_SERVER_PORT = 5555;
        /** Object server user name **/
        public String OBJECT_SERVER_USERNAME;
        /** Object Server pass **/
        public String OBJECT_SERVER_PASSWORD;
        /**
         * Limit magnitude used for loading stars. All stars above this
         * magnitude will not even be loaded by the sandbox.
         **/
        public float LIMIT_MAG_LOAD;
        /**
         * Whether to use the real attitude of Gaia or the NSL approximation
         **/
        public boolean REAL_GAIA_ATTITUDE;

        public void initialize(boolean oBJECT_SERVER_CONNECTION, String cATALOG_JSON_FILE, String hYG_JSON_FILE, String tGAS_JSON_FILE, String oBJECTS_JSON_FILE, String[] oBJECTS_JSON_FILE_GQ, String oBJECT_SERVER_HOSTNAME, int oBJECT_SERVER_PORT, String vISUALIZATION_ID, float lIMIT_MAG_LOAD, boolean rEAL_GAIA_ATTITUDE) {
            OBJECT_SERVER_CONNECTION = oBJECT_SERVER_CONNECTION;

            CATALOG_JSON_FILE = cATALOG_JSON_FILE;
            HYG_JSON_FILE = hYG_JSON_FILE;
            TGAS_JSON_FILE = tGAS_JSON_FILE;

            OBJECTS_JSON_FILE = oBJECTS_JSON_FILE;
            OBJECTS_JSON_FILE_GQ = oBJECTS_JSON_FILE_GQ;

            OBJECT_SERVER_HOSTNAME = oBJECT_SERVER_HOSTNAME;
            OBJECT_SERVER_PORT = oBJECT_SERVER_PORT;
            VISUALIZATION_ID = vISUALIZATION_ID;
            LIMIT_MAG_LOAD = lIMIT_MAG_LOAD;
            REAL_GAIA_ATTITUDE = rEAL_GAIA_ATTITUDE;
        }

        public void initialize(String cATALOG_JSON_FILE, String oBJECTS_JSON_FILE, boolean dATA_SOURCE_LOCAL, float lIMIT_MAG_LOAD) {
            this.CATALOG_JSON_FILE = cATALOG_JSON_FILE;
            this.OBJECTS_JSON_FILE = oBJECTS_JSON_FILE;
            this.OBJECT_SERVER_CONNECTION = dATA_SOURCE_LOCAL;
            this.LIMIT_MAG_LOAD = lIMIT_MAG_LOAD;
        }

        public void initialize(String cATALOG_JSON_FILE, String dATA_JSON_FILE, boolean dATA_SOURCE_LOCAL, float lIMIT_MAG_LOAD, boolean rEAL_GAIA_ATTITUDE) {
            this.CATALOG_JSON_FILE = cATALOG_JSON_FILE;
            this.OBJECTS_JSON_FILE = dATA_JSON_FILE;
            this.OBJECT_SERVER_CONNECTION = dATA_SOURCE_LOCAL;
            this.LIMIT_MAG_LOAD = lIMIT_MAG_LOAD;
            this.REAL_GAIA_ATTITUDE = rEAL_GAIA_ATTITUDE;
        }
    }

    public static class ScreenConf implements IConf {

        public int SCREEN_WIDTH;
        public int SCREEN_HEIGHT;
        public int FULLSCREEN_WIDTH;
        public int FULLSCREEN_HEIGHT;
        public boolean FULLSCREEN;
        public boolean RESIZABLE;
        public boolean VSYNC;
        public boolean SCREEN_OUTPUT = true;

        public void initialize(int sCREEN_WIDTH, int sCREEN_HEIGHT, int fULLSCREEN_WIDTH, int fULLSCREEN_HEIGHT, boolean fULLSCREEN, boolean rESIZABLE, boolean vSYNC, boolean sCREEN_OUTPUT) {
            SCREEN_WIDTH = sCREEN_WIDTH;
            SCREEN_HEIGHT = sCREEN_HEIGHT;
            FULLSCREEN_WIDTH = fULLSCREEN_WIDTH;
            FULLSCREEN_HEIGHT = fULLSCREEN_HEIGHT;
            FULLSCREEN = fULLSCREEN;
            RESIZABLE = rESIZABLE;
            VSYNC = vSYNC;
            SCREEN_OUTPUT = sCREEN_OUTPUT;
        }

        public int getScreenWidth() {
            return FULLSCREEN ? FULLSCREEN_WIDTH : SCREEN_WIDTH;
        }

        public int getScreenHeight() {
            return FULLSCREEN ? FULLSCREEN_HEIGHT : SCREEN_HEIGHT;
        }

    }

    public static class ProgramConf implements IConf, IObserver {

        public static enum StereoProfile {
            /** Left image -> left eye, distortion **/
            VR_HEADSET,
            /** Left image -> left eye, distortion **/
            HD_3DTV,
            /** Left image -> right eye, no distortion **/
            CROSSEYE,
            /** Left image -> left eye, no distortion **/
            PARALLEL_VIEW,
            /** Red-cyan anaglyphic 3D mode **/
            ANAGLYPHIC
        }

        public boolean DISPLAY_TUTORIAL;
        public String TUTORIAL_POINTER_SCRIPT_LOCATION;
        public String TUTORIAL_SCRIPT_LOCATION;
        public boolean SHOW_CONFIG_DIALOG;
        public boolean SHOW_DEBUG_INFO;
        public Date LAST_CHECKED;
        public String LAST_VERSION;
        public String VERSION_CHECK_URL;
        public String UI_THEME;
        public String SCRIPT_LOCATION;
        public String LOCALE;
        public boolean CUBEMAP360_MODE;
        public boolean STEREOSCOPIC_MODE;
        /** Eye separation in stereoscopic mode in meters **/
        public float STEREOSCOPIC_EYE_SEPARATION_M = 1;
        /** This controls the side of the images in the stereoscopic mode **/
        public StereoProfile STEREO_PROFILE = StereoProfile.VR_HEADSET;

        private Boolean lensglowBackup = null;

        private IDateFormat df = DateFormatFactory.getFormatter("dd/MM/yyyy HH:mm:ss");

        public ProgramConf() {
            EventManager.instance.subscribe(this, Events.TOGGLE_STEREOSCOPIC_CMD, Events.TOGGLE_STEREO_PROFILE_CMD, Events.CUBEMAP360_CMD);
        }

        public void initialize(boolean dISPLAY_TUTORIAL, String tUTORIAL_POINTER_SCRIPT_LOCATION, String tUTORIAL_SCRIPT_LOCATION, boolean sHOW_CONFIG_DIALOG, boolean sHOW_DEBUG_INFO, Date lAST_CHECKED, String lAST_VERSION, String vERSION_CHECK_URL, String uI_THEME, String sCRIPT_LOCATION, String lOCALE, boolean sTEREOSCOPIC_MODE, StereoProfile sTEREO_PROFILE, boolean cUBEMAP360_MODE) {
            DISPLAY_TUTORIAL = dISPLAY_TUTORIAL;
            TUTORIAL_POINTER_SCRIPT_LOCATION = tUTORIAL_POINTER_SCRIPT_LOCATION;
            TUTORIAL_SCRIPT_LOCATION = tUTORIAL_SCRIPT_LOCATION;
            SHOW_CONFIG_DIALOG = sHOW_CONFIG_DIALOG;
            SHOW_DEBUG_INFO = sHOW_DEBUG_INFO;
            LAST_CHECKED = lAST_CHECKED;
            LAST_VERSION = lAST_VERSION;
            VERSION_CHECK_URL = vERSION_CHECK_URL;
            UI_THEME = uI_THEME;
            SCRIPT_LOCATION = sCRIPT_LOCATION;
            LOCALE = lOCALE;
            STEREOSCOPIC_MODE = sTEREOSCOPIC_MODE;
            STEREO_PROFILE = sTEREO_PROFILE;
            CUBEMAP360_MODE = cUBEMAP360_MODE;
        }

        public void initialize(boolean dISPLAY_TUTORIAL, boolean sHOW_DEBUG_INFO, String uI_THEME, String lOCALE, boolean sTEREOSCOPIC_MODE, StereoProfile sTEREO_PROFILE) {
            DISPLAY_TUTORIAL = dISPLAY_TUTORIAL;
            SHOW_DEBUG_INFO = sHOW_DEBUG_INFO;
            UI_THEME = uI_THEME;
            LOCALE = lOCALE;
            STEREOSCOPIC_MODE = sTEREOSCOPIC_MODE;
            STEREO_PROFILE = sTEREO_PROFILE;
        }

        public String getLastCheckedString() {
            IDateFormat df = DateFormatFactory.getFormatter(I18n.locale, DateType.DATE);
            return df.format(LAST_CHECKED);
        }

        public boolean isStereoHalfWidth() {
            return STEREOSCOPIC_MODE && (STEREO_PROFILE != StereoProfile.HD_3DTV && STEREO_PROFILE != StereoProfile.ANAGLYPHIC);
        }

        public boolean isStereoFullWidth() {
            return !isStereoHalfWidth();
        }

        public boolean isStereoHalfViewport() {
            return STEREOSCOPIC_MODE && STEREO_PROFILE != StereoProfile.ANAGLYPHIC;
        }

        @Override
        public void notify(Events event, Object... data) {
            switch (event) {
            case TOGGLE_STEREOSCOPIC_CMD:
                if (!GaiaSky.instance.cam.mode.isGaiaFov()) {
                    boolean stereomode = !STEREOSCOPIC_MODE;
                    if (data.length > 1)
                        stereomode = (boolean) data[1];

                    STEREOSCOPIC_MODE = stereomode;
                    // Enable/disable gui
                    //EventManager.instance.post(Events.DISPLAY_GUI_CMD, I18n.bundle.get("notif.cleanmode"), !STEREOSCOPIC_MODE);
                    EventManager.instance.post(Events.TOGGLE_STEREOSCOPIC_INFO, STEREOSCOPIC_MODE);

                    EventManager.instance.post(Events.POST_NOTIFICATION, "You have entered 3D mode. Go back to normal mode using <CTRL+S>");
                    EventManager.instance.post(Events.POST_NOTIFICATION, "Switch between stereoscopic modes using <CTRL+SHIFT+S>");
                }
                break;
            case TOGGLE_STEREO_PROFILE_CMD:
                int idx = STEREO_PROFILE.ordinal();
                StereoProfile[] vals = StereoProfile.values();
                idx = (idx + 1) % vals.length;
                STEREO_PROFILE = vals[idx];

                EventManager.instance.post(Events.TOGGLE_STEREO_PROFILE_INFO, STEREO_PROFILE);
                break;
            case CUBEMAP360_CMD:
                CUBEMAP360_MODE = (Boolean) data[0];
                EventManager.instance.post(Events.DISPLAY_GUI_CMD, I18n.bundle.get("notif.cleanmode"), !CUBEMAP360_MODE);

                //                if (CUBEMAP360_MODE) {
                //                    // Entering 360 mode
                //                    lensglowBackup = GlobalConf.postprocess.POSTPROCESS_LIGHT_SCATTERING;
                //                    EventManager.instance.post(Events.LIGHT_SCATTERING_CMD, false);
                //                } else {
                //                    // Exiting 360 mode
                //                    if (lensglowBackup != null)
                //                        EventManager.instance.post(Events.LIGHT_SCATTERING_CMD, lensglowBackup);
                //                }
                EventManager.instance.post(Events.POST_NOTIFICATION, "You have entered the 360 mode.  Go back to normal mode using <CTRL+3>");
                break;
            }
        }

    }

    public static class VersionConf implements IConf {
        public String version;
        public String buildtime;
        public String builder;
        public String system;
        public String build;

        // Version: major.minor.rev

        public int major;
        public int minor;
        public int rev;

        public void initialize(String version, String buildtime, String builder, String system, String build, int major, int minor, int rev) {
            this.version = version;
            this.buildtime = buildtime;
            this.builder = builder;
            this.system = system;
            this.build = build;
            this.major = major;
            this.minor = minor;
            this.rev = rev;
        }

        public static int[] getMajorMinorRevFromString(String version) {
            String majorS = version.substring(0, version.indexOf("."));
            String minorS, revS;
            int dots = GlobalResources.countOccurrences(version, '.');
            int idx0 = GlobalResources.nthIndexOf(version, '.', 1);
            if (dots == 1) {
                minorS = version.substring(version.indexOf(".", idx0) + 1, version.length());
                revS = null;
            } else if (dots > 1) {
                int idx1 = GlobalResources.nthIndexOf(version, '.', 2);
                minorS = version.substring(version.indexOf(".", idx0) + 1, version.indexOf(".", idx1));
                revS = version.substring(version.indexOf(".", idx1) + 1, version.length());
            } else {
                return null;
            }
            if (majorS.matches("^\\D{1}\\d+$")) {
                majorS = majorS.substring(1, majorS.length());
            }
            if (minorS.matches("^\\d+\\D{1}$")) {
                minorS = minorS.substring(0, minorS.length() - 1);
            }
            if (revS != null && revS.matches("^\\d+\\D{1}$")) {
                revS = revS.substring(0, revS.length() - 1);
            }
            return new int[] { Integer.parseInt(majorS), Integer.parseInt(minorS), revS != null ? Integer.parseInt(revS) : 0 };
        }

        @Override
        public String toString() {
            return version;
        }
    }

    public static class SceneConf implements IConf, IObserver {
        public long OBJECT_FADE_MS;
        public float STAR_BRIGHTNESS;
        public float AMBIENT_LIGHT;
        public int CAMERA_FOV;
        public float CAMERA_SPEED;
        public float TURNING_SPEED;
        public float ROTATION_SPEED;
        public int CAMERA_SPEED_LIMIT_IDX;
        public double CAMERA_SPEED_LIMIT;
        public boolean FOCUS_LOCK;
        public boolean FOCUS_LOCK_ORIENTATION;
        public float LABEL_NUMBER_FACTOR;
        /** Visibility of component types **/
        public boolean[] VISIBILITY;

        /** Display galaxy as 3D object or as a 2D texture **/
        public boolean GALAXY_3D;

        /** Whether to display proper motion vectors **/
        public boolean PROPER_MOTION_VECTORS;
        /** Factor to apply to the length of the proper motion vectors **/
        public float PM_LEN_FACTOR;
        /** This governs the number of proper motion vectors to display **/
        public float PM_NUM_FACTOR;

        public boolean STAR_COLOR_TRANSIT;
        public boolean ONLY_OBSERVED_STARS;
        public boolean COMPUTE_GAIA_SCAN;
        /** The pixel render system: 0 - normal, 1 - bloom, 2 - fuzzy **/
        public int PIXEL_RENDERER;
        /** The line render system: 0 - normal, 1 - shader **/
        public int LINE_RENDERER;
        /** The graphics quality mode: 0 - high, 1 - normal, 2 - low **/
        public int GRAPHICS_QUALITY;

        /** Whether to show crosshair in focus mode **/
        public boolean CROSSHAIR;

        /** Resolution of each of the faces in the cubemap which will be mapped to a equirectangular projection for
         * the 360 mode.
         */
        public int CUBEMAP_FACE_RESOLUTION;

        public double STAR_THRESHOLD_NONE;
        public double STAR_THRESHOLD_POINT;
        public double STAR_THRESHOLD_QUAD;

        public float POINT_ALPHA_MIN;
        public float POINT_ALPHA_MAX;

        /** Size of stars rendered as point primitives **/
        public float STAR_POINT_SIZE;
        /** Fallback value **/
        public float STAR_POINT_SIZE_BAK;

        /**
         * Particle fade in/out flag for octree-backed catalogs. WARNING: This
         * implies particles are sent to GPU at each cycle
         **/
        public boolean OCTREE_PARTICLE_FADE;

        /**
         * Angle [rad] above which we start painting stars in octant with fade
         * in
         **/
        public float OCTANT_THRESHOLD_0;
        /**
         * Angle [rad] below which we paint stars in octant with fade out. Above
         * this angle, inner stars are painted with full brightness
         **/
        public float OCTANT_THRESHOLD_1;

        public SceneConf() {
            EventManager.instance.subscribe(this, Events.FOCUS_LOCK_CMD, Events.ORIENTATION_LOCK_CMD, Events.PROPER_MOTIONS_CMD, Events.STAR_BRIGHTNESS_CMD, Events.PM_LEN_FACTOR_CMD, Events.PM_NUM_FACTOR_CMD, Events.FOV_CHANGED_CMD, Events.CAMERA_SPEED_CMD, Events.ROTATION_SPEED_CMD, Events.TURNING_SPEED_CMD, Events.SPEED_LIMIT_CMD, Events.TRANSIT_COLOUR_CMD, Events.ONLY_OBSERVED_STARS_CMD, Events.COMPUTE_GAIA_SCAN_CMD, Events.PIXEL_RENDERER_CMD, Events.OCTREE_PARTICLE_FADE_CMD, Events.STAR_POINT_SIZE_CMD, Events.STAR_POINT_SIZE_INCREASE_CMD, Events.STAR_POINT_SIZE_DECREASE_CMD, Events.STAR_POINT_SIZE_RESET_CMD, Events.STAR_MIN_OPACITY_CMD, Events.AMBIENT_LIGHT_CMD, Events.GALAXY_3D_CMD, Events.CROSSHAIR_CMD);
        }

        public void initialize(int gRAPHICS_QUALITY, long oBJECT_FADE_MS, float sTAR_BRIGHTNESS, float aMBIENT_LIGHT, int cAMERA_FOV, float cAMERA_SPEED, float tURNING_SPEED, float rOTATION_SPEED, int cAMERA_SPEED_LIMIT_IDX, boolean fOCUS_LOCK, boolean fOCUS_LOCK_ORIENTATION, float lABEL_NUMBER_FACTOR, boolean[] vISIBILITY, int pIXEL_RENDERER, int lINE_RENDERER, double sTAR_TH_ANGLE_NONE, double sTAR_TH_ANGLE_POINT, double sTAR_TH_ANGLE_QUAD, float pOINT_ALPHA_MIN, float pOINT_ALPHA_MAX,
                boolean oCTREE_PARTICLE_FADE, float oCTANT_TH_ANGLE_0, float oCTANT_TH_ANGLE_1, boolean pROPER_MOTION_VECTORS, float pM_NUM_FACTOR, float pM_LEN_FACTOR, float sTAR_POINT_SIZE, boolean gALAXY_3D, int cUBEMAP_FACE_RESOLUTION, boolean cROSSHAIR) {
            GRAPHICS_QUALITY = gRAPHICS_QUALITY;
            OBJECT_FADE_MS = oBJECT_FADE_MS;
            STAR_BRIGHTNESS = sTAR_BRIGHTNESS;
            AMBIENT_LIGHT = aMBIENT_LIGHT;
            CAMERA_FOV = cAMERA_FOV;
            CAMERA_SPEED = cAMERA_SPEED;
            TURNING_SPEED = tURNING_SPEED;
            ROTATION_SPEED = rOTATION_SPEED;
            CAMERA_SPEED_LIMIT_IDX = cAMERA_SPEED_LIMIT_IDX;
            this.updateSpeedLimit();
            FOCUS_LOCK = fOCUS_LOCK;
            FOCUS_LOCK_ORIENTATION = fOCUS_LOCK_ORIENTATION;
            LABEL_NUMBER_FACTOR = lABEL_NUMBER_FACTOR;
            VISIBILITY = vISIBILITY;
            PIXEL_RENDERER = pIXEL_RENDERER;
            LINE_RENDERER = lINE_RENDERER;
            STAR_THRESHOLD_NONE = sTAR_TH_ANGLE_NONE;
            STAR_THRESHOLD_POINT = sTAR_TH_ANGLE_POINT;
            STAR_THRESHOLD_QUAD = sTAR_TH_ANGLE_QUAD;
            POINT_ALPHA_MIN = pOINT_ALPHA_MIN;
            POINT_ALPHA_MAX = pOINT_ALPHA_MAX;
            OCTREE_PARTICLE_FADE = oCTREE_PARTICLE_FADE;
            OCTANT_THRESHOLD_0 = oCTANT_TH_ANGLE_0;
            OCTANT_THRESHOLD_1 = oCTANT_TH_ANGLE_1;
            PROPER_MOTION_VECTORS = pROPER_MOTION_VECTORS;
            PM_NUM_FACTOR = pM_NUM_FACTOR;
            PM_LEN_FACTOR = pM_LEN_FACTOR;
            STAR_POINT_SIZE = sTAR_POINT_SIZE;
            STAR_POINT_SIZE_BAK = STAR_POINT_SIZE;
            GALAXY_3D = gALAXY_3D;
            CUBEMAP_FACE_RESOLUTION = cUBEMAP_FACE_RESOLUTION;
            CROSSHAIR = cROSSHAIR;
        }

        public void updateSpeedLimit() {
            switch (CAMERA_SPEED_LIMIT_IDX) {
            case 0:
                // 100 km/h is 0.027 km/s
                CAMERA_SPEED_LIMIT = 0.0277777778 * Constants.KM_TO_U;
                break;
            case 1:
            case 2:
                // 1 c and 2 c
                CAMERA_SPEED_LIMIT = CAMERA_SPEED_LIMIT_IDX * 3e8 * Constants.M_TO_U;
                break;
            case 3:
                // 10 c
                CAMERA_SPEED_LIMIT = 10 * 3e8 * Constants.M_TO_U;
                break;
            case 4:
                //1000 c
                CAMERA_SPEED_LIMIT = 1000 * 3e8 * Constants.M_TO_U;
                break;
            case 5:
                CAMERA_SPEED_LIMIT = 1 * Constants.AU_TO_U;
                break;
            case 6:
                CAMERA_SPEED_LIMIT = 10 * Constants.AU_TO_U;
                break;
            case 7:
                CAMERA_SPEED_LIMIT = 1000 * Constants.AU_TO_U;
                break;
            case 8:
                CAMERA_SPEED_LIMIT = 10000 * Constants.AU_TO_U;
                break;
            case 9:
            case 10:
                // 1 pc/s and 2 pc/s
                CAMERA_SPEED_LIMIT = (CAMERA_SPEED_LIMIT_IDX - 8) * Constants.PC_TO_U;
                break;
            case 11:
                // 10 pc/s
                CAMERA_SPEED_LIMIT = 10 * Constants.PC_TO_U;
                break;
            case 12:
                // 1000 pc/s
                CAMERA_SPEED_LIMIT = 1000 * Constants.PC_TO_U;
                break;
            case 13:
                // No limit
                CAMERA_SPEED_LIMIT = -1;
                break;

            }
        }

        @Override
        public void notify(Events event, Object... data) {
            switch (event) {
            case TRANSIT_COLOUR_CMD:
                STAR_COLOR_TRANSIT = (boolean) data[1];
                break;
            case ONLY_OBSERVED_STARS_CMD:
                ONLY_OBSERVED_STARS = (boolean) data[1];
                break;
            case COMPUTE_GAIA_SCAN_CMD:
                COMPUTE_GAIA_SCAN = (boolean) data[1];
                break;
            case FOCUS_LOCK_CMD:
                FOCUS_LOCK = (boolean) data[1];
                break;
            case ORIENTATION_LOCK_CMD:
                FOCUS_LOCK_ORIENTATION = (boolean) data[1];
                break;
            case AMBIENT_LIGHT_CMD:
                AMBIENT_LIGHT = (float) data[0];
                break;

            case STAR_BRIGHTNESS_CMD:
                STAR_BRIGHTNESS = Math.max(0.01f, (float) data[0]);
                break;
            case FOV_CHANGED_CMD:
                CAMERA_FOV = MathUtilsd.clamp(((Float) data[0]).intValue(), Constants.MIN_FOV, Constants.MAX_FOV);
                break;
            case PM_NUM_FACTOR_CMD:
                PM_NUM_FACTOR = Math.min(Constants.MAX_PM_NUM_FACTOR, Math.max(Constants.MIN_PM_NUM_FACTOR, (float) data[0]));
                break;
            case PM_LEN_FACTOR_CMD:
                PM_LEN_FACTOR = Math.min(Constants.MAX_PM_LEN_FACTOR, Math.max(Constants.MIN_PM_LEN_FACTOR, (float) data[0]));
                break;

            case CAMERA_SPEED_CMD:
                CAMERA_SPEED = (float) data[0];
                break;
            case ROTATION_SPEED_CMD:
                ROTATION_SPEED = (float) data[0];
                break;
            case TURNING_SPEED_CMD:
                TURNING_SPEED = (float) data[0];
                break;
            case SPEED_LIMIT_CMD:
                CAMERA_SPEED_LIMIT_IDX = (Integer) data[0];
                updateSpeedLimit();
                break;
            case PIXEL_RENDERER_CMD:
                PIXEL_RENDERER = (Integer) data[0];
                break;
            case OCTREE_PARTICLE_FADE_CMD:
                OCTREE_PARTICLE_FADE = (boolean) data[1];
                break;
            case PROPER_MOTIONS_CMD:
                PROPER_MOTION_VECTORS = (boolean) data[1];
                break;
            case STAR_POINT_SIZE_CMD:
                STAR_POINT_SIZE = (float) data[0];
                break;
            case STAR_POINT_SIZE_INCREASE_CMD:
                STAR_POINT_SIZE = Math.min(STAR_POINT_SIZE + Constants.STEP_STAR_POINT_SIZE, Constants.MAX_STAR_POINT_SIZE);
                EventManager.instance.post(Events.STAR_POINT_SIZE_INFO, STAR_POINT_SIZE);
                break;
            case STAR_POINT_SIZE_DECREASE_CMD:
                STAR_POINT_SIZE = Math.max(STAR_POINT_SIZE - Constants.STEP_STAR_POINT_SIZE, Constants.MIN_STAR_POINT_SIZE);
                EventManager.instance.post(Events.STAR_POINT_SIZE_INFO, STAR_POINT_SIZE);
                break;
            case STAR_POINT_SIZE_RESET_CMD:
                STAR_POINT_SIZE = STAR_POINT_SIZE_BAK;
                break;
            case STAR_MIN_OPACITY_CMD:
                POINT_ALPHA_MIN = (float) data[0];
                break;
            case GALAXY_3D_CMD:
                GALAXY_3D = (boolean) data[0];
                break;
            case CROSSHAIR_CMD:
                CROSSHAIR = (boolean) data[0];
                break;
            }

        }

        public boolean isNormalPixelRenderer() {
            return PIXEL_RENDERER == 0;
        }

        public boolean isBloomPixelRenderer() {
            return PIXEL_RENDERER == 1;
        }

        public boolean isFuzzyPixelRenderer() {
            return PIXEL_RENDERER == 2;
        }

        public boolean isNormalLineRenderer() {
            return LINE_RENDERER == 0;
        }

        public boolean isQuadLineRenderer() {
            return LINE_RENDERER == 1;
        }

        public boolean isHighQuality() {
            return GRAPHICS_QUALITY == 0;
        }

        public boolean isNormalQuality() {
            return GRAPHICS_QUALITY == 1;
        }

        public boolean isLowQuality() {
            return GRAPHICS_QUALITY == 2;
        }
    }

    public static List<IConf> configurations;

    public static FrameConf frame;
    public static ScreenConf screen;
    public static ProgramConf program;
    public static DataConf data;
    public static SceneConf scene;
    public static RuntimeConf runtime;
    public static ScreenshotConf screenshot;
    public static PerformanceConf performance;
    public static PostprocessConf postprocess;
    public static VersionConf version;

    static boolean initialized = false;

    public GlobalConf() {
        super();
    }

    public static boolean initialized() {
        return initialized;
    }

    /**
     * Initialises the properties
     */
    public static void initialize(VersionConf vc, ProgramConf pc, SceneConf sc, DataConf dc, RuntimeConf rc, PostprocessConf ppc, PerformanceConf pfc, FrameConf fc, ScreenConf scrc, ScreenshotConf shc) throws Exception {
        if (!initialized) {
            if (configurations == null) {
                configurations = new ArrayList<IConf>();
            }

            version = vc;
            program = pc;
            scene = sc;
            data = dc;
            runtime = rc;
            postprocess = ppc;
            performance = pfc;
            frame = fc;
            screenshot = shc;
            screen = scrc;

            configurations.add(program);
            configurations.add(scene);
            configurations.add(data);
            configurations.add(runtime);
            configurations.add(postprocess);
            configurations.add(performance);
            configurations.add(frame);
            configurations.add(screenshot);
            configurations.add(screen);

            initialized = true;
        }

    }

    public static String getFullApplicationName() {
        return APPLICATION_NAME + " - " + version.version;
    }

}
