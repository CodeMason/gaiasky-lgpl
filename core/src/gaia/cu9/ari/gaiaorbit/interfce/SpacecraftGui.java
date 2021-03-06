package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.scenegraph.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.scenegraph.SpacecraftCamera;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GSEnumSet;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Pair;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.g3d.ModelBuilder2;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnImageButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnSlider;

public class SpacecraftGui implements IGui, IObserver {
    private Skin skin;
    protected Stage ui;

    private Container<HorizontalGroup> buttonContainer;
    private Container<Label> thrustContainer;
    private HorizontalGroup buttonRow, engineGroup;
    private VerticalGroup motionGroup, nearestGroup, thrustGroup;
    private OwnImageButton stabilise, stop, exit, enginePlus, engineMinus;
    private Slider enginePower;
    private Slider thrustv, thrusty, thrustp, thrustr;
    private Slider thrustvm, thrustym, thrustpm, thrustrm;
    private OwnLabel mainvel, yawvel, pitchvel, rollvel, closestname, closestdist, thrustfactor;

    /** The spacecraft camera **/
    private SpacecraftCamera camera;

    /** Number format **/
    private INumberFormat nf, sf;

    /** Camera to render the attitude indicator system **/
    private PerspectiveCamera aiCam;

    /** Attitude indicator **/
    private ModelBatch mb;
    private DecalBatch db;
    private SpriteBatch sb;
    private Model aiModel;
    private ModelInstance aiModelInstance;
    private Texture aiTexture, aiPointerTexture, controlPadTexture, aiVelTex, aiAntivelTex;
    private Decal aiVelDec, aiAntivelDec;
    private Environment env;
    private Matrix4 aiTransform;
    private Viewport aiViewport;
    private DirectionalLight dlight;
    /** Reference to spacecraft camera rotation quaternion **/
    private Quaternion qf;
    /** Reference to spacecraft camera velocity vector **/
    private Vector3d vel;

    private float indicatorw, indicatorh, indicatorx, indicatory;

    /** Aux vectors **/
    private Vector3 aux3f1, aux3f2;

    private boolean thrustEvents = true;

    public SpacecraftGui(SpacecraftCamera camera) {
        super();
        this.camera = camera;
        this.qf = camera.getRotationQuaternion();
        this.vel = camera.vel;
        aux3f1 = new Vector3();
        aux3f2 = new Vector3();

        nf = NumberFormatFactory.getFormatter("##0.##");
        sf = NumberFormatFactory.getFormatter("#0.###E0");
    }

    public void initialize(AssetManager assetManager) {
        // User interface
        sb = GlobalResources.spriteBatch;
        ui = new Stage(new ScreenViewport(), sb);

        indicatorw = 300;
        indicatorh = 300;
        indicatorx = -20;
        indicatory = -25;

        // init gui camera
        aiCam = new PerspectiveCamera(30, indicatorw, indicatorh);
        aiCam.near = (float) (1e5 * Constants.KM_TO_U);
        aiCam.far = (float) (1e8 * Constants.KM_TO_U);
        aiCam.up.set(0, 1, 0);
        aiCam.direction.set(0, 0, 1);
        aiCam.position.set(0, 0, 0);

        // Init AI
        dlight = new DirectionalLight();
        dlight.color.set(1f, 1f, 1f, 1f);
        dlight.setDirection(-1f, .05f, .5f);
        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f), new ColorAttribute(ColorAttribute.Specular, .5f, .5f, .5f, 1f));
        env.add(dlight);
        db = new DecalBatch(new CameraGroupStrategy(aiCam));
        mb = new ModelBatch();

        assetManager.load("data/tex/attitudeindicator-2.png", Texture.class);
        assetManager.load("img/ai-pointer.png", Texture.class);
        assetManager.load("img/controlpad.png", Texture.class);
        assetManager.load("img/ai-vel.png", Texture.class);
        assetManager.load("img/ai-antivel.png", Texture.class);

    }

    /**
     * Constructs the interface
     */
    public void doneLoading(AssetManager assetManager) {
        skin = GlobalResources.skin;

        aiTexture = assetManager.get("data/tex/attitudeindicator-2.png", Texture.class);
        aiTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        aiPointerTexture = assetManager.get("img/ai-pointer.png", Texture.class);
        aiPointerTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        controlPadTexture = assetManager.get("img/controlpad.png", Texture.class);
        controlPadTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        aiVelTex = assetManager.get("img/ai-vel.png", Texture.class);
        aiVelTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        aiAntivelTex = assetManager.get("img/ai-antivel.png", Texture.class);
        aiAntivelTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        aiVelDec = Decal.newDecal(new TextureRegion(aiVelTex));
        aiAntivelDec = Decal.newDecal(new TextureRegion(aiAntivelTex));

        Material mat = new Material(new TextureAttribute(TextureAttribute.Diffuse, aiTexture), new ColorAttribute(ColorAttribute.Specular, 0.3f, 0.3f, 0.3f, 1f));
        aiModel = new ModelBuilder2().createSphere(1, 30, 30, mat, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
        aiTransform = new Matrix4();
        aiModelInstance = new ModelInstance(aiModel, aiTransform);
        aiViewport = new ExtendViewport(indicatorw, indicatorh, aiCam);

        buildGui();

        EventManager.instance.subscribe(this, Events.SPACECRAFT_STABILISE_CMD, Events.SPACECRAFT_STOP_CMD, Events.SPACECRAFT_INFO, Events.SPACECRAFT_THRUST_INFO);

    }

    private void buildGui() {
        /** BUTTONS **/
        buttonContainer = new Container<HorizontalGroup>();
        buttonRow = new HorizontalGroup();
        buttonRow.pad(0, 70, 5, 0);
        buttonRow.space(3);
        buttonRow.setFillParent(true);
        buttonRow.align(Align.bottomLeft);

        stabilise = new OwnImageButton(skin, "sc-stabilise");
        stabilise.setProgrammaticChangeEvents(false);
        stabilise.setName("stabilise");
        stabilise.setChecked(camera.isStabilising());
        stabilise.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    EventManager.instance.post(Events.SPACECRAFT_STABILISE_CMD, stabilise.isChecked());
                    return true;
                }
                return false;
            }
        });
        stabilise.addListener(new TextTooltip(txt("gui.tooltip.sc.stabilise"), skin));

        stop = new OwnImageButton(skin, "sc-stop");
        stop.setProgrammaticChangeEvents(false);
        stop.setName("stop spacecraft");
        stop.setChecked(camera.isStopping());
        stop.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    EventManager.instance.post(Events.SPACECRAFT_STOP_CMD, stop.isChecked());
                    return true;
                }
                return false;
            }
        });
        stop.addListener(new TextTooltip(txt("gui.tooltip.sc.stop"), skin));

        exit = new OwnImageButton(skin, "sc-exit");
        exit.setProgrammaticChangeEvents(false);
        exit.setName("exit spacecraft");
        exit.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.Focus);
                    return true;
                }
                return false;
            }
        });
        exit.addListener(new TextTooltip(txt("gui.tooltip.sc.exit"), skin));

        buttonRow.addActor(stabilise);
        buttonRow.addActor(stop);
        buttonRow.addActor(exit);

        buttonContainer.setActor(buttonRow);

        buttonContainer.pack();

        /** ENGINE GROUP **/
        engineGroup = new HorizontalGroup();
        engineGroup.pad(0, 10, 5, 0);
        engineGroup.space(0.5f);
        engineGroup.align(Align.bottomLeft);

        // Engine controls
        float enginePowerH = indicatory + indicatorh / 1.14f;
        Table engineControls = new Table();
        engineControls.pad(0f);

        enginePlus = new OwnImageButton(skin, "sc-engine-power-up");
        enginePlus.addListener(new TextTooltip(txt("gui.tooltip.sc.powerup"), skin));
        enginePlus.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    EventManager.instance.post(Events.SPACECRAFT_THRUST_INCREASE_CMD);
                    return true;
                }
                return false;
            }

        });
        engineMinus = new OwnImageButton(skin, "sc-engine-power-down");
        enginePlus.addListener(new TextTooltip(txt("gui.tooltip.sc.powerdown"), skin));
        engineMinus.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    EventManager.instance.post(Events.SPACECRAFT_THRUST_DECREASE_CMD);
                    return true;
                }
                return false;
            }

        });

        Group engineLabelRotated = new Group();
        Label engineLabel = new OwnLabel("ENGINE POWER", skin);
        engineLabelRotated.addActor(engineLabel);
        float engineLabelH = enginePowerH - enginePlus.getHeight() - engineMinus.getHeight() - 2;
        engineLabelRotated.addAction(Actions.rotateBy(-90));
        engineLabelRotated.addAction(Actions.moveBy(-5, ((engineLabelH - enginePlus.getHeight()) - engineLabel.getWidth()) / 2f + engineLabel.getWidth()));
        engineLabelRotated.setHeight(engineLabelH);

        engineControls.add(enginePlus);
        engineControls.row();
        engineControls.add(engineLabelRotated);
        engineControls.row();
        engineControls.add(engineMinus);

        // Power slider - The value of the slider is the index of the thrustFactor array 
        enginePower = new OwnSlider(0, SpacecraftCamera.thrustFactor.length - 1, 1, true, skin, "sc-engine");
        enginePower.setName("engine power slider");
        enginePower.setValue(1);
        enginePower.setHeight(enginePowerH);
        enginePower.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (thrustEvents)
                    if (event instanceof ChangeEvent) {
                        EventManager.instance.post(Events.SPACECRAFT_THRUST_SET_CMD, Math.round(enginePower.getValue()));
                        return true;
                    }
                return false;
            }
        });

        engineGroup.addActor(enginePower);
        engineGroup.addActor(engineControls);

        engineGroup.pack();

        /** INFORMATION **/
        int groupspacing = 2;
        thrustfactor = new OwnLabel("", skin);
        thrustContainer = new Container<Label>(thrustfactor);
        thrustContainer.pad(0, 40, enginePowerH * 2 + 25, 0);

        mainvel = new OwnLabel("", skin);
        HorizontalGroup mvg = new HorizontalGroup();
        mvg.space(groupspacing);
        mvg.addActor(new OwnLabel(txt("gui.sc.velocity") + ":", skin, "sc-header"));
        mvg.addActor(mainvel);
        yawvel = new OwnLabel("", skin);
        HorizontalGroup yvg = new HorizontalGroup();
        yvg.space(groupspacing);
        yvg.addActor(new OwnLabel(txt("gui.sc.yaw") + ":", skin, "sc-header"));
        yvg.addActor(yawvel);
        pitchvel = new OwnLabel("", skin);
        HorizontalGroup pvg = new HorizontalGroup();
        pvg.space(groupspacing);
        pvg.addActor(new OwnLabel(txt("gui.sc.pitch") + ":", skin, "sc-header"));
        pvg.addActor(pitchvel);
        rollvel = new OwnLabel("", skin);
        HorizontalGroup rvg = new HorizontalGroup();
        rvg.space(groupspacing);
        rvg.addActor(new OwnLabel(txt("gui.sc.roll") + ":", skin, "sc-header"));
        rvg.addActor(rollvel);

        motionGroup = new VerticalGroup();
        motionGroup.pad(0, 80, 200, 0);
        motionGroup.space(1);
        motionGroup.align(Align.topLeft);

        HorizontalGroup ypGroup = new HorizontalGroup();
        ypGroup.space(4);
        ypGroup.addActor(yvg);
        ypGroup.addActor(pvg);

        motionGroup.addActor(mvg);
        motionGroup.addActor(ypGroup);
        motionGroup.addActor(rvg);

        motionGroup.pack();

        /** NEAREST **/
        nearestGroup = new VerticalGroup();
        nearestGroup.pad(0, 160, 5, 0);
        nearestGroup.space(1);
        nearestGroup.align(Align.topLeft);

        closestname = new OwnLabel("", skin);
        closestdist = new OwnLabel("", skin);
        HorizontalGroup cng = new HorizontalGroup();
        cng.space(groupspacing);
        cng.addActor(new OwnLabel(txt("gui.sc.nearest") + ":", skin, "sc-header"));
        cng.addActor(closestname);

        HorizontalGroup cdg = new HorizontalGroup();
        cdg.space(groupspacing);
        cdg.addActor(new OwnLabel(txt("gui.sc.distance") + ":", skin, "sc-header"));
        cdg.addActor(closestdist);

        nearestGroup.addActor(cng);
        nearestGroup.addActor(cdg);

        nearestGroup.pack();

        /** THRUST INDICATORS for VEL, YAW, PITCH, ROLL **/
        int thrustHeight = 40;
        thrustGroup = new VerticalGroup();
        thrustGroup.space(1f);
        thrustGroup.pad(0, 215, 85, 0);

        HorizontalGroup thrustPlus = new HorizontalGroup().space(1f);
        HorizontalGroup thrustMinus = new HorizontalGroup().space(1f);

        thrustv = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust");
        thrustv.setHeight(thrustHeight);
        thrustv.setDisabled(true);
        thrustvm = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust-minus");
        thrustvm.setHeight(thrustHeight);
        thrustvm.setDisabled(true);

        thrusty = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust");
        thrusty.setHeight(thrustHeight);
        thrusty.setDisabled(true);
        thrustym = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust-minus");
        thrustym.setHeight(thrustHeight);
        thrustym.setDisabled(true);

        thrustp = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust");
        thrustp.setHeight(thrustHeight);
        thrustp.setDisabled(true);
        thrustpm = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust-minus");
        thrustpm.setHeight(thrustHeight);
        thrustpm.setDisabled(true);

        thrustr = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust");
        thrustr.setHeight(thrustHeight);
        thrustr.setDisabled(true);
        thrustrm = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust-minus");
        thrustrm.setHeight(thrustHeight);
        thrustrm.setDisabled(true);

        thrustPlus.addActor(thrustv);
        thrustMinus.addActor(thrustvm);

        thrustPlus.addActor(thrusty);
        thrustMinus.addActor(thrustym);

        thrustPlus.addActor(thrustp);
        thrustMinus.addActor(thrustpm);

        thrustPlus.addActor(thrustr);
        thrustMinus.addActor(thrustrm);

        thrustGroup.addActor(thrustPlus);
        thrustGroup.addActor(thrustMinus);

        thrustGroup.pack();

        rebuildGui();
    }

    private void rebuildGui() {
        if (ui != null) {
            ui.clear();
            ui.addActor(buttonContainer);
            ui.addActor(engineGroup);
            ui.addActor(motionGroup);
            ui.addActor(nearestGroup);
            ui.addActor(thrustContainer);
            ui.addActor(thrustGroup);
        }

        /** CAPTURE SCROLL FOCUS **/
        ui.addListener(new EventListener() {

            @Override
            public boolean handle(Event event) {
                if (event instanceof InputEvent) {
                    InputEvent ie = (InputEvent) event;

                    if (ie.getType() == Type.mouseMoved) {
                        Actor scrollPanelAncestor = getScrollPanelAncestor(ie.getTarget());
                        ui.setScrollFocus(scrollPanelAncestor);
                    } else if (ie.getType() == Type.touchDown) {
                        if (ie.getTarget() instanceof TextField)
                            ui.setKeyboardFocus(ie.getTarget());
                    }
                }
                return false;
            }

            private Actor getScrollPanelAncestor(Actor actor) {
                if (actor == null) {
                    return null;
                } else if (actor instanceof ScrollPane) {
                    return actor;
                } else {
                    return getScrollPanelAncestor(actor.getParent());
                }
            }

        });
    }

    @Override
    public void dispose() {
        ui.dispose();
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void update(float dt) {
        ui.act(dt);
    }

    @Override
    public void render(int rw, int rh) {
        /** ATTITUDE INDICATOR **/
        aiViewport.setCamera(aiCam);
        aiViewport.setWorldSize(indicatorw, indicatorh);
        aiViewport.setScreenBounds((int) indicatorx, (int) indicatory, (int) indicatorw, (int) indicatorh);
        aiViewport.apply();

        mb.begin(aiCam);

        aiTransform.idt();

        aiTransform.translate(0, 0, 4);
        aiTransform.rotate(qf);
        aiTransform.rotate(0, 1, 0, 90);

        mb.render(aiModelInstance, env);

        mb.end();

        // VELOCITY INDICATORS IN NAVBALL
        // velocity
        if (!vel.isZero()) {
            aux3f1.set(vel.valuesf()).nor().scl(0.54f);
            aux3f1.mul(qf);
            aux3f1.add(0, 0, 4);

            // antivelocity
            aux3f2.set(vel.valuesf()).nor().scl(-0.54f);
            aux3f2.mul(qf);
            aux3f2.add(0, 0, 4);

            aiVelDec.setPosition(aux3f1);
            aiVelDec.setScale(0.003f);
            aiVelDec.lookAt(aiCam.position, aiCam.up);

            aiAntivelDec.setPosition(aux3f2);
            aiAntivelDec.setScale(0.003f);
            aiAntivelDec.lookAt(aiCam.position, aiCam.up);

            Gdx.gl.glEnable(GL20.GL_BLEND);
            db.add(aiVelDec);
            db.add(aiAntivelDec);
            db.flush();
        }

        aiViewport.setWorldSize(rw, rh);
        aiViewport.setScreenBounds(0, 0, rw, rh);
        aiViewport.apply();

        // ai pointer
        sb.begin();
        sb.draw(aiPointerTexture, indicatorx + indicatorw / 2 - 16, indicatory + indicatorh / 2 - 16);
        sb.end();

        /** REST OF GUI **/
        ui.draw();

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
        sb.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        rebuildGui();
    }

    /**
     * Removes the focus from this Gui and returns true if the focus was in the GUI, false otherwise.
     * @return true if the focus was in the GUI, false otherwise.
     */
    public boolean cancelTouchFocus() {
        if (ui.getScrollFocus() != null) {
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

    private String txt(String key) {
        return I18n.bundle.get(key);
    }

    private String txt(String key, Object... params) {
        return I18n.bundle.format(key, params);
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case SPACECRAFT_STABILISE_CMD:
            Boolean state = (Boolean) data[0];
            stabilise.setChecked(state);
            break;
        case SPACECRAFT_STOP_CMD:
            state = (Boolean) data[0];
            stop.setChecked(state);
            break;
        case SPACECRAFT_INFO:
            double y = -(Double) data[0];
            double p = -(Double) data[1];
            double r = (Double) data[2];
            double v = (Double) data[3];
            double thf = (Double) data[6];
            double epow = (Double) data[7];
            double ypow = (Double) data[8];
            double ppow = (Double) data[9];
            double rpow = (Double) data[10];

            yawvel.setText(nf.format(y) + "°");
            pitchvel.setText(nf.format(p) + "°");
            rollvel.setText(nf.format(r) + "°");

            Pair<Double, String> velstr = GlobalResources.doubleToVelocityString(v);
            mainvel.setText(sf.format(velstr.getFirst()) + " " + velstr.getSecond());

            if (data[4] != null) {
                closestname.setText((String) data[4]);
                Pair<Float, String> cldist = GlobalResources.floatToDistanceString((Float) data[5]);
                closestdist.setText(sf.format(cldist.getFirst()) + " " + cldist.getSecond());
            } else {
                closestname.setText("");
                closestdist.setText("");
            }

            thrustfactor.setText("x" + (thf > 1000 ? sf.format(thf) : nf.format(thf)));

            setPowerValuesSlider(thrustv, thrustvm, epow);
            setPowerValuesSlider(thrusty, thrustym, ypow);
            setPowerValuesSlider(thrustp, thrustpm, ppow);
            setPowerValuesSlider(thrustr, thrustrm, rpow);

            break;
        case SPACECRAFT_THRUST_INFO:
            thrustEvents = false;

            enginePower.setValue((Integer) data[0]);

            thrustEvents = true;
            break;
        }

    }

    private void setPowerValuesSlider(Slider plus, Slider minus, double value) {
        plus.setValue((float) value);
        minus.setValue(1f + (float) value);
    }
}
