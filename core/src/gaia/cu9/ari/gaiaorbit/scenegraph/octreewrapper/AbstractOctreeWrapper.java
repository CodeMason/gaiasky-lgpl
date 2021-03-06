package gaia.cu9.ari.gaiaorbit.scenegraph.octreewrapper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.system.AbstractRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.Particle;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.scenegraph.Transform;
import gaia.cu9.ari.gaiaorbit.util.GSEnumSet;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.MyPools;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;

/**
 * Abstract Octree wrapper with the common parts of the regular Octree wrapper and
 * the concurrent one.
 * @author Toni Sagrista
 *
 */
public abstract class AbstractOctreeWrapper extends SceneGraphNode implements Iterable<OctreeNode<Particle>> {

    public OctreeNode<Particle> root;
    /** Roulette list with the objects to process **/
    protected Array<SceneGraphNode> roulette;
    public Map<Particle, OctreeNode<Particle>> parenthood;
    /** The number of objects added to render in the last frame **/
    protected int lastNumberObjects = 0;
    /**
     * Is this just a copy?
     */
    protected boolean copy = false;

    protected AbstractOctreeWrapper() {
        super("Octree", null);
    }

    protected AbstractOctreeWrapper(String parentName, OctreeNode<Particle> root) {
        this();
        this.ct = GSEnumSet.of(ComponentType.Others);
        this.root = root;
        this.parentName = parentName;
        this.parenthood = new HashMap<Particle, OctreeNode<Particle>>();

    }

    /** 
     * An octree wrapper has as 'scene graph children' all the elements contained in the
     * octree, even though it acts as a hub that decides which are processed and which are not.
     */
    @Override
    public void initialize() {
        super.initialize();
    }

    /**
     * Adds all the objects of the octree (recursively) to the root list.
     * @param octant
     * @param root
     */
    private void addObjectsDeep(OctreeNode<Particle> octant, SceneGraphNode root) {
        if (octant.objects != null) {
            root.add(octant.objects.items);
            for (Particle sgn : octant.objects) {
                parenthood.put(sgn, octant);
            }
        }

        for (int i = 0; i < 8; i++) {
            OctreeNode<Particle> child = octant.children[i];
            if (child != null) {
                addObjectsDeep(child, root);
            }
        }
    }

    public void add(List<Particle> children, OctreeNode<Particle> octant) {
        super.add(children);
        for (Particle sgn : children) {
            parenthood.put(sgn, octant);
        }
    }

    public void add(Particle child, OctreeNode<Particle> octant) {
        super.add(child);
        parenthood.put(child, octant);
    }

    public void removeParenthood(SceneGraphNode child) {
        parenthood.remove(child);
    }

    public void update(ITimeFrameProvider time, final Transform parentTransform, ICamera camera) {
        update(time, parentTransform, camera, 1f);
    }

    public void update(ITimeFrameProvider time, final Transform parentTransform, ICamera camera, float opacity) {
        this.opacity = opacity;
        transform.set(parentTransform);

        // Update octants
        if (!copy) {
            // Compute observed octants and fill roulette list
            root.update(transform, camera, roulette, 1f);

            if (roulette.size != lastNumberObjects) {
                // Need to update the points in renderer
                AbstractRenderSystem.POINT_UPDATE_FLAG = true;
                lastNumberObjects = roulette.size;
            }

            updateLocal(time, camera);

            // Call the update method of all entities in the roulette list. This is implemented in the subclass.
            EventManager.instance.post(Events.DEBUG3, "Octree threads: " + getRouletteDebug());
            updateOctreeObjects(time, transform, camera);

            // Reset mask
            roulette.clear();

            // Update focus, just in case
            CelestialBody focus = camera.getFocus();
            if (focus != null) {
                SceneGraphNode star = focus.getFirstStarAncestor();
                OctreeNode<Particle> parent = parenthood.get(star);
                if (parent != null && !parent.isObserved()) {
                    star.update(time, star.parent.transform, camera);
                }
            }
        } else {
            // Just update children
            for (SceneGraphNode node : children) {
                node.update(time, transform, camera);
            }
        }

    }

    /**
     * Runs the update on all the observed and selected octree objects.
     * @param time
     * @param parentTransform
     * @param camera
     */
    protected abstract void updateOctreeObjects(ITimeFrameProvider time, final Transform parentTransform, ICamera camera);

    /**
     * Adds the octants to the render lists.
     * @param time
     */
    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        if (!copy) {
            addToRenderLists(camera, root);
        }
    }

    public void addToRenderLists(ICamera camera, OctreeNode<Particle> octant) {
        if (GlobalConf.runtime.DRAW_OCTREE && octant.observed && addToRender(octant, RenderGroup.LINE)) {
            for (int i = 0; i < 8; i++) {
                OctreeNode<Particle> child = octant.children[i];
                if (child != null) {
                    addToRenderLists(camera, child);
                }
            }
        }
    }

    @Override
    /** Not implemented **/
    public Iterator<OctreeNode<Particle>> iterator() {
        return null;
    }

    @Override
    public int getStarCount() {
        return root.nObjects;
    }

    @Override
    public Object getStars() {
        return children;
    }

    /**
     * Gets a copy of this object but does not copy its parent or children.
     * @return
     */
    @Override
    public <T extends SceneGraphNode> T getSimpleCopy() {
        Class<? extends AbstractOctreeWrapper> clazz = this.getClass();
        Pool<? extends AbstractOctreeWrapper> pool = MyPools.get(clazz);
        try {
            AbstractOctreeWrapper instance = pool.obtain();
            instance.copy = true;
            instance.name = this.name;
            instance.transform.set(this.transform);
            instance.ct = this.ct;
            if (this.localTransform != null)
                instance.localTransform.set(this.localTransform);

            return (T) instance;
        } catch (Exception e) {
            Logger.error(e);
        }
        return null;
    }

    protected abstract String getRouletteDebug();
}
