package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.graphics.PerspectiveCamera;

import gaia.cu9.ari.gaiaorbit.scenegraph.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.util.math.Frustumd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public interface ICamera {

    /**
     * Returns the perspective camera.
     * 
     * @return The perspective camera.
     */
    public PerspectiveCamera getCamera();

    /**
     * Sets the active camera
     * 
     * @param cam
     */
    public void setCamera(PerspectiveCamera cam);

    public PerspectiveCamera getCameraStereoLeft();

    public PerspectiveCamera getCameraStereoRight();

    public void setCameraStereoLeft(PerspectiveCamera cam);

    public void setCameraStereoRight(PerspectiveCamera cam);

    public Frustumd getFrustum();

    public PerspectiveCamera[] getFrontCameras();

    public ICamera getCurrent();

    public float getFovFactor();

    public Vector3d getPos();

    public void setPos(Vector3d pos);

    public void setDirection(Vector3d dir);

    public Vector3d getInversePos();

    public Vector3d getDirection();

    public Vector3d getUp();

    public Vector3d[] getDirections();

    public int getNCameras();

    public void setShift(Vector3d shift);

    public Vector3d getShift();

    /**
     * Updates the camera.
     * 
     * @param dt
     *            The time since the las frame in seconds.
     * @param time
     *            The frame time provider (simulation time).
     */
    public void update(float dt, ITimeFrameProvider time);

    public void updateMode(CameraMode mode, boolean postEvent);

    public CameraMode getMode();

    public void updateAngleEdge(int width, int height);

    /**
     * Gets the angle of the edge of the screen, diagonally. It assumes the
     * vertical angle is the field of view and corrects the horizontal using the
     * aspect ratio. It depends on the viewport size and the field of view
     * itself.
     * 
     * @return The angle in radians.
     */
    public float getAngleEdge();

    public CameraManager getManager();

    public void render(int rw, int rh);

    /**
     * Gets the current velocity of the camera in km/h.
     * 
     * @return The velocity in km/h.
     */
    public double getVelocity();

    /**
     * Gets the distance from the camera to the centre of our reference frame
     * (Sun)
     * 
     * @return The distance
     */
    public double getDistance();

    /**
     * Returns the foucs if any.
     * 
     * @return The foucs object if it is in focus mode. Null otherwise.
     */
    public CelestialBody getFocus();

    /**
     * Checks if this body is the current focus.
     * 
     * @param cb
     * @return
     */
    public boolean isFocus(CelestialBody cb);

    /**
     * Called after updating the body's distance to the cam, it updates the
     * closest body in the camera to figure out the camera near.
     * 
     * @param cb
     *            The body to check
     */
    public void checkClosest(CelestialBody cb);

    public CelestialBody getClosest();

    public boolean isVisible(ITimeFrameProvider time, CelestialBody cb);

    public void computeGaiaScan(ITimeFrameProvider time, CelestialBody cb);

    public void resize(int width, int height);
}
