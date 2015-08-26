package gaia.cu9.ari.gaiaorbit.desktop;

import gaia.cu9.ari.gaiaorbit.GaiaSandbox;
import gaia.cu9.ari.gaiaorbit.data.FileLocator;
import gaia.cu9.ari.gaiaorbit.data.SceneGraphJsonLoader;
import gaia.cu9.ari.gaiaorbit.desktop.gui.swing.Gui;
import gaia.cu9.ari.gaiaorbit.desktop.gui.swing.IconManager;
import gaia.cu9.ari.gaiaorbit.desktop.gui.swing.Observer;
import gaia.cu9.ari.gaiaorbit.desktop.gui.swing.jsplash.JSplash;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.script.JythonFactory;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.SysUtils;
import gaia.cu9.ari.gaiaorbit.util.time.GlobalClock;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglCanvas;

public class GaiaSandboxGuiSwing extends JFrame {

    private static final long serialVersionUID = 1L;
    private GaiaSandbox program;
    private static ISceneGraph sg;
    private static LwjglCanvas canvas;

    public GaiaSandboxGuiSwing() {

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        String appName = GlobalConf.getFullApplicationName();
        this.setTitle(appName);

        // Init configuration
        LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
        cfg.title = appName;
        cfg.resizable = true;
        cfg.samples = GlobalConf.postprocess.POSTPROCESS_ANTIALIAS;
        cfg.vSyncEnabled = GlobalConf.screen.VSYNC;
        cfg.foregroundFPS = 400;
        cfg.useGL30 = false;
        cfg.addIcon("icon/ic_launcher.png", Files.FileType.Internal);

        // Init scripting
        JythonFactory.initialize();
        // Init app
        program = new GaiaSandbox(false);
        program.setSceneGraph(sg);
        canvas = new LwjglCanvas(program, cfg);
        //	canvas = new LwjglAWTCanvas(program);
        //	canvas.getGraphics().setVSync(GlobalConf.instance.VSYNC);

        Gui gui = new Gui(this, canvas);
        gui.initialize(sg);

        pack();
        setVisible(true);
        setSize(new Dimension(1024, 600));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    @Override
    public void dispose() {
        Gdx.app.exit();
        canvas.exit();
    }

    public static void main(String[] args) {
        try {
            // Set look and feel
            //UIManager.setLookAndFeel("com.alee.laf.WebLookAndFeel");
            //WebLookAndFeel.setAllowLinuxTransparency(false);

            File confFile = new File(System.getProperty("properties.file"));
            FileInputStream fis = new FileInputStream(confFile);
            GlobalConf.initialize(fis, GaiaSandboxDesktop.class.getResourceAsStream("/version"));

            fis.close();

            JFrame frame = null;
            if (SysUtils.checkUnity()) {
                // A frame containing the console
                frame = new JFrame();
                frame.add(new JLabel(""));
                frame.setUndecorated(true);
                frame.pack();
                frame.toBack();
                frame.setVisible(true);
            }

            // The splash window
            URL url = GaiaSandboxGuiSwing.class.getResource("/img/splash/splash1.png");
            JSplash splash = new JSplash(url, true, true, false, GlobalConf.version.version, null, Color.WHITE, Color.BLACK);
            splash.splashOn();
            splash.toFront();

            if (SysUtils.checkUnity()) {
                frame.setVisible(false);
            }

            // This captures events from the loading system and passes them to the splash and console
            new Observer(splash);

            GlobalClock.initialize(0.01f);
            FileLocator.initialize();
            sg = SceneGraphJsonLoader.loadSceneGraph(new FileInputStream(FileLocator.getFile(GlobalConf.data.DATA_JSON_FILE)), GlobalClock.clock, GlobalConf.performance.MULTITHREADING, GlobalConf.performance.NUMBER_THREADS());
            IconManager.initialise(new File("data/ui/"));
            splash.splashOff();
            splash.dispose();

            if (SysUtils.checkUnity()) {
                frame.dispose();
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new GaiaSandboxGuiSwing();
            }
        });

    }

}