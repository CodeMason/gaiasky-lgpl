package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.TwoWayHashmap;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Frustumd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public class CameraManager implements ICamera, IObserver {
    /**
     * Convenience enum to describe the camera mode
     * 
     * @author Toni Sagrista
     *
     */
    public enum CameraMode {
        /** Free navigation **/
        Free_Camera,
        /** Focus **/
        Focus,
        /** Gaia Scene **/
        Gaia_Scene,
        /** Spacecraft **/
        Spacecraft,
        /** FOV1 **/
        Gaia_FOV1,
        /** FOV2 **/
        Gaia_FOV2,
        /** Both fields of view **/
        Gaia_FOV1and2;

        static TwoWayHashmap<String, CameraMode> equivalences;

        static {
            String fc = "Free camera";
            String foc = "Focus object";
            String gs = "Gaia scene";
            String sc = "Spacecraft";
            String f1 = "Gaia FoV 1";
            String f2 = "Gaia FoV 2";
            String f12 = "Gaia FoV1 and FoV2";

            equivalences = new TwoWayHashmap<String, CameraMode>();
            equivalences.add(fc, Free_Camera);
            equivalences.add(foc, Focus);
            equivalences.add(gs, Gaia_Scene);
            equivalences.add(sc, Spacecraft);
            equivalences.add(f1, Gaia_FOV1);
            equivalences.add(f2, Gaia_FOV2);
            equivalences.add(f12, Gaia_FOV1and2);

        }

        public static CameraMode getMode(int idx) {
            if (idx >= 0 && idx < CameraMode.values().length) {
                return CameraMode.values()[idx];
            } else {
                return null;
            }
        }

        @Override
        public String toString() {
            return equivalences.getBackward(this);
        }

        public static CameraMode fromString(String str) {
            return equivalences.getForward(str);
        }

        public boolean isGaiaFov() {
            return this.equals(CameraMode.Gaia_FOV1) || this.equals(CameraMode.Gaia_FOV2) || this.equals(CameraMode.Gaia_FOV1and2);
        }
    }

    public CameraMode mode;

    public ICamera current;

    public NaturalCamera naturalCamera;
    public FovCamera fovCamera;
    public SpacecraftCamera spacecraftCamera;

    private ICamera[] cameras;

    /** Last position, for working out velocity **/
    private Vector3d lastPos, out, in;
    private Vector3 vec, v0, v1;

    /** Current velocity in km/h **/
    private double speed;
    /** Velocity vector **/
    private Vector3d velocity;

    public CameraManager(AssetManager manager, CameraMode mode) {
        // Initialize
        // Initialize Cameras
        CameraMode previousMode = this.mode;
        naturalCamera = new NaturalCamera(manager, this);
        fovCamera = new FovCamera(manager, this);
        spacecraftCamera = new SpacecraftCamera(manager, this);

        cameras = new ICamera[] { naturalCamera, fovCamera, spacecraftCamera };

        this.mode = mode;
        lastPos = new Vector3d();
        in = new Vector3d();
        out = new Vector3d();
        vec = new Vector3();
        v0 = new Vector3();
        v1 = new Vector3();
        velocity = new Vector3d();

        updateCurrentCamera(previousMode);

        EventManager.instance.subscribe(this, Events.CAMERA_MODE_CMD, Events.FOV_CHANGE_NOTIFICATION);
    }

    public void updateCurrentCamera(CameraMode previousMode) {

        // Update
        switch (mode) {
        case Free_Camera:
        case Focus:
        case Gaia_Scene:
            current = naturalCamera;
            // Copy
            if (previousMode == CameraMode.Spacecraft)
                naturalCamera.copyParamsFrom(spacecraftCamera);
            break;
        case Spacecraft:
            current = spacecraftCamera;
            // Copy
            if (previousMode == CameraMode.Free_Camera || previousMode == CameraMode.Focus || previousMode == CameraMode.Gaia_Scene)
                spacecraftCamera.copyParamsFrom(naturalCamera);
            break;
        case Gaia_FOV1:
        case Gaia_FOV2:
        case Gaia_FOV1and2:
            current = fovCamera;
            break;
        }

    }

    public boolean isNatural() {
        return current == naturalCamera;
    }

    @Override
    public PerspectiveCamera getCamera() {
        return current.getCamera();
    }

    @Override
    public float getFovFactor() {
        return current.getFovFactor();
    }

    @Override
    public Vector3d getPos() {
        return current.getPos();
    }

    @Override
    public void setPos(Vector3d pos) {
        current.setPos(pos);
    }

    @Override
    public Vector3d getInversePos() {
        return current.getInversePos();
    }

    @Override
    public Vector3d getDirection() {
        return current.getDirection();
    }

    @Override
    public void setDirection(Vector3d dir) {
        current.setDirection(dir);
    }

    @Override
    public Vector3d getUp() {
        return current.getUp();
    }

    /**
     * Update method.
     * 
     * @param dt
     *            Delta time in seconds.
     * @param time
     *            The time frame provider.
     */
    public void update(float dt, ITimeFrameProvider time) {
        current.update(dt, time);
        if (current != fovCamera && GlobalConf.scene.COMPUTE_GAIA_SCAN) {
            fovCamera.updateDirections(time);
        }

        // Speed = dx/dt
        velocity.set(lastPos).sub(current.getPos());
        speed = (velocity.len() * Constants.U_TO_KM) / (dt * Constants.S_TO_H);

        // Post event with camera motion parameters
        EventManager.instance.post(Events.CAMERA_MOTION_UPDATED, current.getPos(), speed, velocity.nor(), current.getCamera());

        // Update last pos
        lastPos.set(current.getPos());

        int screenX = Gdx.input.getX();
        int screenY = Gdx.input.getY();

        // Update Pointer Alpha/Delta
        updatePointerRADEC(screenX, screenY);
        // Update Pointer LAT/LON
        updateFocusLatLon(screenX, screenY);
    }

    private void updatePointerRADEC(int screenX, int screenY) {
        vec.set(screenX, screenY, 0.5f);
        ICamera camera = current;
        camera.getCamera().unproject(vec);

        in.set(vec);

        Coordinates.cartesianToSpherical(in, out);

        double alpha = out.x * AstroUtils.TO_DEG;
        double delta = out.y * AstroUtils.TO_DEG;

        EventManager.instance.post(Events.RA_DEC_UPDATED, alpha, delta, screenX, screenY);
    }

    private void updateFocusLatLon(int screenX, int screenY) {
        if (isNatural()) {
            // Hover over planets gets us lat/lon
            if (current.getFocus() != null && current.getFocus() instanceof Planet) {
                Planet p = (Planet) current.getFocus();
                p.transform.getTranslationf(vec);
                //pcenter.set((float) p.pos.x, (float) p.pos.y, (float) p.pos.z);
                ICamera camera = current;
                v0.set(screenX, screenY, 0f);
                v1.set(screenX, screenY, 0.5f);
                camera.getCamera().unproject(v0);
                camera.getCamera().unproject(v1);

                Ray ray = new Ray(v0, v1.sub(v0));
                Vector3 intersection = new Vector3();
                boolean inter = Intersector.intersectRaySphere(ray, vec, p.getRadius(), intersection);

                if (inter) {
                    // We found an intersection point
                    Matrix4 localTransformInv = new Matrix4();
                    p.setToLocalTransform(1, localTransformInv, false);
                    localTransformInv.inv();
                    intersection.mul(localTransformInv);

                    in.set(intersection);
                    Coordinates.cartesianToSpherical(in, out);

                    double lon = (Math.toDegrees(out.x) + 90) % 360;
                    double lat = Math.toDegrees(out.y);

                    EventManager.instance.post(Events.LON_LAT_UPDATED, lon, lat, screenX, screenY);

                }

            }

        }
    }

    int pxRendererBackup = -1;

    /**
     * Sets the new camera mode and updates the frustum
     * 
     * @param mode
     */
    public void updateMode(CameraMode mode, boolean postEvent) {
        CameraMode previousMode = this.mode;
        this.mode = mode;
        updateCurrentCamera(previousMode);
        for (ICamera cam : cameras) {
            cam.updateMode(mode, postEvent);
        }

        if (postEvent) {
            EventManager.instance.post(Events.FOV_CHANGE_NOTIFICATION, this.getCamera().fieldOfView, getFovFactor());
        }
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case CAMERA_MODE_CMD:
            CameraMode cm = (CameraMode) data[0];
            updateMode(cm, true);
            break;
        case FOV_CHANGE_NOTIFICATION:
            updateAngleEdge(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            break;
        default:
            break;
        }

    }

    @Override
    public Vector3d[] getDirections() {
        return current.getDirections();
    }

    @Override
    public int getNCameras() {
        return current.getNCameras();
    }

    @Override
    public PerspectiveCamera[] getFrontCameras() {
        return current.getFrontCameras();
    }

    @Override
    public CameraMode getMode() {
        return mode;
    }

    @Override
    public void updateAngleEdge(int width, int height) {
        for (ICamera cam : cameras)
            cam.updateAngleEdge(width, height);
    }

    @Override
    public float getAngleEdge() {
        return current.getAngleEdge();
    }

    @Override
    public CameraManager getManager() {
        return this;
    }

    @Override
    public void render(int rw, int rh) {
        current.render(rw, rh);
    }

    @Override
    public ICamera getCurrent() {
        return current;
    }

    @Override
    public double getVelocity() {
        return speed;
    }

    @Override
    public boolean isFocus(CelestialBody cb) {
        return current.isFocus(cb);
    }

    @Override
    public void checkClosest(CelestialBody cb) {
        current.checkClosest(cb);
    }

    @Override
    public CelestialBody getFocus() {
        return current.getFocus();
    }

    public void computeGaiaScan(ITimeFrameProvider time, CelestialBody cb) {
        current.computeGaiaScan(time, cb);
    }

    @Override
    public boolean isVisible(ITimeFrameProvider time, CelestialBody cb) {
        return current.isVisible(time, cb);
    }

    @Override
    public double getDistance() {
        return current.getDistance();
    }

    @Override
    public void setCamera(PerspectiveCamera cam) {
        current.setCamera(cam);
    }

    @Override
    public void setCameraStereoLeft(PerspectiveCamera cam) {
        current.setCameraStereoLeft(cam);
    }

    @Override
    public void setCameraStereoRight(PerspectiveCamera cam) {
        current.setCameraStereoRight(cam);
    }

    @Override
    public PerspectiveCamera getCameraStereoLeft() {
        return current.getCameraStereoLeft();
    }

    @Override
    public PerspectiveCamera getCameraStereoRight() {
        return current.getCameraStereoRight();
    }

    @Override
    public CelestialBody getClosest() {
        return current.getClosest();
    }

    @Override
    public void resize(int width, int height) {
        for (ICamera cam : cameras)
            cam.resize(width, height);
    }

    @Override
    public void setShift(Vector3d shift) {
        current.setShift(shift);
    }

    @Override
    public Vector3d getShift() {
        return current.getShift();
    }

    @Override
    public Frustumd getFrustum() {
        return current.getFrustum();
    }

}
