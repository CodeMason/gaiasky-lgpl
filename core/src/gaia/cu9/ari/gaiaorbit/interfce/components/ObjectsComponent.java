package gaia.cu9.ari.gaiaorbit.interfce.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.ui.Tree.Node;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.NaturalInputController;
import gaia.cu9.ari.gaiaorbit.scenegraph.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.TwoWayHashmap;
import gaia.cu9.ari.gaiaorbit.util.comp.CelestialBodyComparator;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnScrollPane;

public class ObjectsComponent extends GuiComponent implements IObserver {
    boolean tree = false;
    boolean list = true;

    protected ISceneGraph sg;

    protected Actor objectsList;
    protected TextField searchBox;
    protected OwnScrollPane focusListScrollPane;

    private Timer focusTimer;

    /**
     * Tree to model equivalences
     */
    private TwoWayHashmap<SceneGraphNode, Node> treeToModel;

    public ObjectsComponent(Skin skin, Stage stage) {
        super(skin, stage);
        EventManager.instance.subscribe(this, Events.FOCUS_CHANGED);
        focusTimer = new Timer();
    }

    @Override
    public void initialize() {

        searchBox = new TextField("", skin);
        searchBox.setName("search box");
        searchBox.setMessageText(txt("gui.objects.search"));
        searchBox.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof InputEvent) {
                    InputEvent ie = (InputEvent) event;
                    if (ie.getType() == Type.keyUp) {
                        String text = searchBox.getText();
                        if (sg.containsNode(text.toLowerCase())) {
                            final SceneGraphNode node = sg.getNode(text.toLowerCase());
                            if (node instanceof CelestialBody) {
                                Gdx.app.postRunnable(new Runnable() {
                                    @Override
                                    public void run() {
                                        EventManager.instance.post(Events.FOCUS_CHANGE_CMD, node, true);
                                    }
                                });

                            }
                        }
                        NaturalInputController.pressedKeys.remove(ie.getKeyCode());

                        if (ie.getKeyCode() == Keys.ESCAPE) {
                            // Lose focus
                            stage.setKeyboardFocus(null);
                        }
                    } else if (ie.getType() == Type.keyDown) {
                        if (ie.getKeyCode() == Keys.CONTROL_LEFT || ie.getKeyCode() == Keys.CONTROL_RIGHT || ie.getKeyCode() == Keys.SHIFT_LEFT || ie.getKeyCode() == Keys.SHIFT_RIGHT) {
                            // Lose focus
                            stage.setKeyboardFocus(null);
                        }
                    }
                    return true;
                }
                return false;
            }
        });

        treeToModel = new TwoWayHashmap<SceneGraphNode, Node>();

        Logger.info(txt("notif.sgtree.init"));

        if (tree) {
            final Tree objectsTree = new Tree(skin, "bright");
            objectsTree.setName("objects list");
            objectsTree.setPadding(1 * GlobalConf.SCALE_FACTOR);
            objectsTree.setIconSpacing(1 * GlobalConf.SCALE_FACTOR, 1 * GlobalConf.SCALE_FACTOR);
            objectsTree.setYSpacing(0);
            Array<Node> nodes = createTree(sg.getRoot().children);
            for (Node node : nodes) {
                objectsTree.add(node);
            }
            objectsTree.expandAll();
            objectsTree.addListener(new EventListener() {

                @Override
                public boolean handle(Event event) {
                    if (event instanceof ChangeEvent) {
                        if (objectsTree.getSelection().hasItems()) {
                            if (objectsTree.getSelection().hasItems()) {
                                Node n = objectsTree.getSelection().first();
                                final SceneGraphNode sgn = treeToModel.getBackward(n);
                                Gdx.app.postRunnable(new Runnable() {
                                    @Override
                                    public void run() {
                                        EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.Focus);
                                        EventManager.instance.post(Events.FOCUS_CHANGE_CMD, sgn, false);
                                    }
                                });

                            }

                        }
                        return true;
                    }
                    return false;
                }

            });
            objectsList = objectsTree;
        } else if (list) {
            final com.badlogic.gdx.scenes.scene2d.ui.List<String> focusList = new com.badlogic.gdx.scenes.scene2d.ui.List<String>(skin, "light");
            focusList.setName("objects list");
            Array<CelestialBody> focusableObjects = sg.getFocusableObjects();
            Array<String> names = new Array<String>(focusableObjects.size);

            for (CelestialBody cb : focusableObjects) {
                // Omit stars with no proper names
                if (!GlobalResources.isNumeric(cb.name)) {
                    names.add(cb.name);
                }
            }
            names.sort();

            SceneGraphNode sol = sg.getNode("Sol");
            if (sol != null) {
                Array<CelestialBody> solChildren = new Array<CelestialBody>();
                sol.addFocusableObjects(solChildren);
                solChildren.sort(new CelestialBodyComparator());
                for (CelestialBody cb : solChildren)
                    names.insert(0, cb.name);
            }

            focusList.setItems(names);
            focusList.pack();//
            focusList.addListener(new EventListener() {

                @Override
                public boolean handle(Event event) {
                    if (event instanceof ChangeEvent) {
                        ChangeEvent ce = (ChangeEvent) event;
                        Actor actor = ce.getTarget();
                        final String name = ((com.badlogic.gdx.scenes.scene2d.ui.List<String>) actor).getSelected();
                        if (name != null) {
                            Gdx.app.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    // Change focus
                                    EventManager.instance.post(Events.FOCUS_CHANGE_CMD, sg.getNode(name), false);
                                    // Change camera mode to focus
                                    EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.Focus);
                                }
                            });
                        }
                        return true;
                    }
                    return false;
                }
            });
            objectsList = focusList;
        }
        Logger.info(txt("notif.sgtree.initialised"));

        if (tree || list) {
            focusListScrollPane = new OwnScrollPane(objectsList, skin, "minimalist");
            focusListScrollPane.setName("objects list scroll");
            focusListScrollPane.setFadeScrollBars(false);
            focusListScrollPane.setScrollingDisabled(true, false);

            focusListScrollPane.setHeight(tree ? 200 * GlobalConf.SCALE_FACTOR : 100 * GlobalConf.SCALE_FACTOR);
            focusListScrollPane.setWidth(160);
        }

        VerticalGroup objectsGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left).space(3 * GlobalConf.SCALE_FACTOR);
        objectsGroup.addActor(searchBox);
        if (focusListScrollPane != null) {
            objectsGroup.addActor(focusListScrollPane);
        }

        component = objectsGroup;

    }

    private Array<Node> createTree(Array<SceneGraphNode> nodes) {
        Array<Node> treeNodes = new Array<Node>(nodes.size);
        for (SceneGraphNode node : nodes) {
            Label l = new Label(node.name, skin, "ui-10");
            l.setColor(Color.BLACK);
            Node treeNode = new Node(l);

            if (node.children != null && node.children.size != 0) {
                treeNode.addAll(createTree(node.children));
            }

            treeNodes.add(treeNode);
            treeToModel.add(node, treeNode);
        }

        return treeNodes;
    }

    public void setSceneGraph(ISceneGraph sg) {
        this.sg = sg;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case FOCUS_CHANGED:
            // Update focus selection in focus list
            SceneGraphNode sgn = null;
            if (data[0] instanceof String) {
                sgn = sg.getNode((String) data[0]);
            } else {
                sgn = (SceneGraphNode) data[0];
            }
            // Select only if data[1] is true
            if (sgn != null) {
                if (tree) {
                    Tree objList = ((Tree) objectsList);
                    Node node = treeToModel.getForward(sgn);
                    objList.getSelection().set(node);
                    node.expandTo();

                    focusListScrollPane.setScrollY(focusListScrollPane.getMaxY() - node.getActor().getY());
                } else if (list) {
                    // Update focus selection in focus list
                    com.badlogic.gdx.scenes.scene2d.ui.List<String> objList = (com.badlogic.gdx.scenes.scene2d.ui.List<String>) objectsList;
                    Array<String> items = objList.getItems();
                    SceneGraphNode node = (SceneGraphNode) data[0];

                    // Select without firing events, do not use set()
                    objList.getSelection().items().clear();
                    objList.getSelection().items().add(node.name);

                    int itemIdx = items.indexOf(node.name, false);
                    if (itemIdx >= 0) {
                        objList.getSelection().setProgrammaticChangeEvents(false);
                        objList.setSelectedIndex(itemIdx);
                        objList.getSelection().setProgrammaticChangeEvents(true);
                        float itemHeight = objList.getItemHeight();
                        focusListScrollPane.setScrollY(itemIdx * itemHeight);
                    }
                }
            }
            break;
        }

    }

}
