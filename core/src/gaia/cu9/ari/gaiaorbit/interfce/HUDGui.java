package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GSEnumSet;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;

/**
 * Head-up display GUI which only displays information and has no options window.
 * @author Toni Sagrista
 *
 */
public class HUDGui implements IGui {
    private Skin skin;
    /**
     * The user interface stage	    
     */
    protected Stage ui;

    protected FocusInfoInterface focusInterface;
    protected NotificationsInterface notificationsInterface;
    protected MessagesInterface messagesInterface;
    protected DebugInterface debugInterface;
    protected ScriptStateInterface inputInterface;

    /**
     * Number formats
     */
    private INumberFormat format, sformat;

    /** Lock object for synchronization **/
    private Object lock;

    @Override
    public void initialize(AssetManager assetManager) {
        // User interface
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
        lock = new Object();
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        skin = GlobalResources.skin;
        format = NumberFormatFactory.getFormatter("0.0###");
        sformat = NumberFormatFactory.getFormatter("0.###E0");

        initialize();
    }

    private void initialize() {
        float pad = 5 * GlobalConf.SCALE_FACTOR;

        // FOCUS INFORMATION - BOTTOM RIGHT
        focusInterface = new FocusInfoInterface(skin);
        focusInterface.setFillParent(true);
        focusInterface.right().bottom();
        focusInterface.pad(0, 0, pad, pad);

        // DEBUG INFO - TOP RIGHT
        debugInterface = new DebugInterface(skin, lock);
        debugInterface.setFillParent(true);
        debugInterface.right().top();

        // NOTIFICATIONS INTERFACE - BOTTOM LEFT
        notificationsInterface = new NotificationsInterface(skin, lock, true);
        notificationsInterface.setFillParent(true);
        notificationsInterface.left().bottom();
        notificationsInterface.pad(0, pad, pad, 0);

        // MESSAGES INTERFACE - LOW CENTER
        messagesInterface = new MessagesInterface(skin, lock);
        messagesInterface.setFillParent(true);
        messagesInterface.left().bottom();
        messagesInterface.pad(0, 300 * GlobalConf.SCALE_FACTOR, 150 * GlobalConf.SCALE_FACTOR, 0);

        if (Constants.desktop) {
            // INPUT STATE
            inputInterface = new ScriptStateInterface(skin);
            inputInterface.setFillParent(true);
            inputInterface.right().top();
            inputInterface.pad(50 * GlobalConf.SCALE_FACTOR, 0, 0, pad);
        }

        // Add to GUI
        rebuildGui();
    }

    public void rebuildGui() {
        if (ui != null) {
            ui.clear();

            if (debugInterface != null) {
                ui.addActor(debugInterface);
            }
            if (notificationsInterface != null) {
                ui.addActor(notificationsInterface);
            }
            if (messagesInterface != null) {
                ui.addActor(messagesInterface);
            }
            if (focusInterface != null) {
                ui.addActor(focusInterface);
            }
            if (inputInterface != null) {
                ui.addActor(inputInterface);
            }
        }
    }

    @Override
    public void dispose() {
        ui.dispose();
    }

    @Override
    public void update(float dt) {
        ui.act(dt);
        notificationsInterface.update();
    }

    @Override
    public void render(int rw, int rh) {
        synchronized (lock) {
            ui.draw();
        }
    }

    @Override
    public void resize(final int width, final int height) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                resizeImmediate(width, height);
            }
        });
    }

    @Override
    public void resizeImmediate(final int width, final int height) {
        ui.getViewport().update(width, height, true);
        rebuildGui();
    }

    @Override
    public boolean cancelTouchFocus() {
        if (ui.getKeyboardFocus() != null || ui.getScrollFocus() != null) {
            ui.setScrollFocus(null);
            ui.setKeyboardFocus(null);
            return true;
        }
        return false;
    }

    @Override
    public Stage getGuiStage() {
        return ui;
    }

    @Override
    public void setSceneGraph(ISceneGraph sg) {
    }

    @Override
    public void setVisibilityToggles(ComponentType[] entities, GSEnumSet<ComponentType> visible) {
    }

    @Override
    public Actor findActor(String name) {
        return ui.getRoot().findActor(name);
    }

}
