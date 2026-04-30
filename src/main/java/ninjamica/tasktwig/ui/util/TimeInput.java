package ninjamica.tasktwig.ui.util;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.MaskChar;
import atlantafx.base.util.MaskTextFormatter;
import atlantafx.base.util.SimpleMaskChar;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;


public class TimeInput extends CustomTextField {

    public static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("h:mm a");
    private static final List<MaskChar> mask = new ArrayList<>();

    static {
        mask.add(new SimpleMaskChar(
                character -> Character.isDigit(character) || character == ' ',
                character -> character == '0' ? ' ' : character
        ));
        mask.add(new SimpleMaskChar(Character::isDigit));
        mask.add(SimpleMaskChar.fixed(':'));
        mask.add(new SimpleMaskChar(
                character -> Character.isDigit(character) && character - '0' < 6
        ));
        mask.add(new SimpleMaskChar(Character::isDigit));
        mask.add(SimpleMaskChar.fixed(' '));
        mask.add(new SimpleMaskChar(
                character -> character == 'A' || character == 'a' ||  character == 'P' ||  character == 'p',
                Character::toUpperCase
        ));
        mask.add(SimpleMaskChar.fixed('M'));
    }

    private final ObjectProperty<LocalTime> timeValue = new SimpleObjectProperty<>();
    public final boolean acceptsNull;


    public TimeInput() {
        this(null, true);
    }

    public TimeInput(LocalTime time, boolean acceptsNull) {
        this.acceptsNull = acceptsNull;

        MaskTextFormatter.create(this, mask);

        textProperty().subscribe(text -> {
            if (text != null) {
                if (text.charAt(0) != ' ' && text.charAt(0) != '1' && text.charAt(1) == '_') {
                    Platform.runLater(() -> setText(" " + text.charAt(0) + text.substring(2)));
                }
                else if (text.charAt(0) == '_' && Character.isDigit(text.charAt(1))) {
                    Platform.runLater(() -> setText(" " + text.substring(1)));
                }
                else {
                    try {
                        if (acceptsNull && text.equals("__:__ _M") || text.equals(" _:__ _M"))
                            timeValue.set(null);
                        else
                            timeValue.set(LocalTime.parse(text.trim(), timeFormat));

                        pseudoClassStateChanged(Styles.STATE_DANGER, false);
                    } catch (DateTimeParseException e) {
                        if (acceptsNull)
                            timeValue.set(null);
                        pseudoClassStateChanged(Styles.STATE_DANGER, true);
                    }
                }
            }
        });

        timeValue.subscribe((oldTime, newTime) -> {
            if (newTime != null) {
                String timeString = timeFormat.format(newTime);
                if (timeString.indexOf(':') == 1)
                    timeString = " " + timeString;
                setText(timeString);
            }
            else {
                if (this.acceptsNull) {
                    clear();
                }
                else {
                    timeValue.set(oldTime);
                    throw new NullPointerException("This TimeInput is set to not accept a null value");
                }
            }
        });
        timeValue.set(time);

        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.UP) {
                increment();
                event.consume();
            }
            else if (event.getCode() == KeyCode.DOWN) {
                decrement();
                event.consume();
            }
        });
        setOnScroll(event -> {
            if (event.getDeltaY() < 0) {
                decrement();
                event.consume();
            }
            else if (event.getDeltaY() > 0) {
                increment();
                event.consume();
            }
        });

        var clockIcon = new FontIcon(FontAwesomeSolid.CLOCK);
        clockIcon.setCursor(Cursor.HAND);
        clockIcon.setOnMouseClicked(event -> setTime(LocalTime.now()));
        setRight(clockIcon);
        setPrefWidth(110);
        setMinWidth(USE_PREF_SIZE);
    }

    public LocalTime getTime() {
        return timeValue.get();
    }

    public void setTime(LocalTime time) {
        timeValue.set(time);
    }

    public ObjectProperty<LocalTime> timeValueProperty() {
        return timeValue;
    }

    private void increment() {
        int caretPos =  getCaretPosition();
        switch (caretPos) {
            case 0,1,2 -> setTime(getTime().plusHours(1));
            case 3 -> setTime(getTime().plusMinutes(10));
            case 4,5 -> setTime(getTime().plusMinutes(1));
            case 6,7,8 -> setTime(getTime().plusHours(12));
        }
        positionCaret(caretPos);
    }

    private void decrement() {
        int caretPos =  getCaretPosition();
        switch (caretPos) {
            case 0,1,2 -> setTime(getTime().minusHours(1));
            case 3 -> setTime(getTime().minusMinutes(10));
            case 4,5 -> setTime(getTime().minusMinutes(1));
            case 6,7,8 -> setTime(getTime().minusHours(12));
        }
        positionCaret(caretPos);
    }
}
