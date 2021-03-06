package gaia.cu9.ari.gaiaorbit.interfce.components;

import java.util.Date;

import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;

import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.DateDialog;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory.DateType;
import gaia.cu9.ari.gaiaorbit.util.format.IDateFormat;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnImageButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;

public class TimeComponent extends GuiComponent implements IObserver {

    /** Date format **/
    private IDateFormat df;
    /** Decimal format **/
    private INumberFormat nf, nfsci;

    protected OwnLabel date;
    protected ImageButton plus, minus;
    protected Label timeWarp;
    protected ImageButton dateEdit;
    protected DateDialog dateDialog;

    public TimeComponent(Skin skin, Stage stage) {
        super(skin, stage);

        df = DateFormatFactory.getFormatter(I18n.locale, DateType.DATE);
        nf = NumberFormatFactory.getFormatter("#########.###");
        nfsci = NumberFormatFactory.getFormatter("0.#E0");
        EventManager.instance.subscribe(this, Events.TIME_CHANGE_INFO, Events.TIME_CHANGE_CMD, Events.PACE_CHANGED_INFO);
    }

    @Override
    public void initialize() {
        // Time
        date = new OwnLabel("", skin);
        date.setName("input time");

        dateEdit = new OwnImageButton(skin, "edit");
        dateEdit.addListener(new EventListener() {

            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    // Left button click
                    if (dateDialog == null) {
                        dateDialog = new DateDialog(stage, skin);
                    }
                    dateDialog.updateTime(GaiaSky.instance.time.getTime());
                    dateDialog.display();
                }
                return false;
            }

        });
        dateEdit.addListener(new TextTooltip(txt("gui.tooltip.dateedit"), skin));

        // Pace
        Label paceLabel = new Label(txt("gui.pace"), skin);
        plus = new OwnImageButton(skin, "plus");
        plus.setName("plus");
        plus.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    // Plus pressed
                    EventManager.instance.post(Events.TIME_WARP_INCREASE_CMD);

                    return true;
                }
                return false;
            }
        });
        plus.addListener(new TextTooltip(txt("gui.tooltip.timewarpplus"), skin));

        minus = new OwnImageButton(skin, "minus");
        minus.setName("minus");
        minus.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    // Minus pressed
                    EventManager.instance.post(Events.TIME_WARP_DECREASE_CMD);
                    return true;
                }
                return false;
            }
        });
        minus.addListener(new TextTooltip(txt("gui.tooltip.timewarpminus"), skin));

        timeWarp = new OwnLabel(getFormattedTimeWrap(), skin, "warp");
        timeWarp.setName("time warp");
        Container wrapWrapper = new Container(timeWarp);
        wrapWrapper.width(60f * GlobalConf.SCALE_FACTOR);
        wrapWrapper.align(Align.center);

        VerticalGroup timeGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left).space(3 * GlobalConf.SCALE_FACTOR).padTop(3 * GlobalConf.SCALE_FACTOR);

        HorizontalGroup dateGroup = new HorizontalGroup();
        dateGroup.space(4 * GlobalConf.SCALE_FACTOR);
        dateGroup.addActor(date);
        dateGroup.addActor(dateEdit);
        timeGroup.addActor(dateGroup);

        HorizontalGroup paceGroup = new HorizontalGroup();
        paceGroup.space(1 * GlobalConf.SCALE_FACTOR);
        paceGroup.addActor(paceLabel);
        paceGroup.addActor(minus);
        paceGroup.addActor(wrapWrapper);
        paceGroup.addActor(plus);

        timeGroup.addActor(paceGroup);
        timeGroup.pack();

        component = timeGroup;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case TIME_CHANGE_INFO:
        case TIME_CHANGE_CMD:
            // Update input time
            Date time = (Date) data[0];
            date.setText(df.format(time));

            break;
        case PACE_CHANGED_INFO:
            if (data.length == 1)
                this.timeWarp.setText(getFormattedTimeWarp((double) data[0]));
            break;
        }

    }

    private String getFormattedTimeWarp(double warp) {
        if (warp > 0.9 || warp < -0.9) {
            // Remove decimals
            warp = Math.round(warp);
        } else {
            // Round to 2 decimal places
            warp = Math.round(warp * 1000.0) / 1000.0;
        }
        if (warp > 99999 || warp < -99999) {
            return "x" + nfsci.format(warp);
        } else {
            return "x" + nf.format(warp);
        }
    }

    private String getFormattedTimeWrap() {
        return getFormattedTimeWarp(GaiaSky.instance.time.getWarpFactor());
    }

}
