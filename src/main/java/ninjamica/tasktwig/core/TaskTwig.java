package ninjamica.tasktwig.core;

import com.dropbox.core.*;
import com.dropbox.core.json.JsonReader;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import tools.jackson.core.*;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings({"OctalInteger", "ResultOfMethodCallIgnored"})
public class TaskTwig implements Serializable {

    static class TwigJsonAssertException extends RuntimeException {
        public TwigJsonAssertException(String message) {
            super(message);
        }
    }
    static class TwigJsonVersionException extends TwigJsonAssertException {
        public TwigJsonVersionException(String message) {
            super(message);
        }
    }

    private static final String DBX_API_KEY = "ul8ujplgavm586q";
    private static final int CONFIG_VERSION = 5;

    private static final File DATA_DIR = new File("data");
    private static final File DBX_DIR = new File(DATA_DIR.getPath() + "/dbx");
    private static final File DBX_CRED_FILE = new File(DBX_DIR.getPath() + "/credential.app");
    private static final File COMMIT_FILE = new File(DATA_DIR.getPath()+"/commit.json");
    private static final File LAST_SYNCED_COMMIT_FILE = new File(DATA_DIR.getPath()+"/last_synced_commit.json");
    private static final File CONFIG_FILE = new File(DATA_DIR.getPath()+"/config.json");
    public enum DataFile {
        SLEEP (new File(DATA_DIR.getPath()+"/sleep.json")),
        WORKOUT (new File(DATA_DIR.getPath()+"/workout.json")),
        TASK (new File(DATA_DIR.getPath()+"/task.json")),
        ROUTINE (new File(DATA_DIR.getPath()+"/routine.json")),
        LIST (new File(DATA_DIR.getPath()+"/list.json")),
        JOURNAL (new File(DATA_DIR.getPath()+"/journal.json"));

        public final File file;
        DataFile(File file) {
            this.file = file;
        }
    }

    private static TaskTwig instance;
    private static boolean notFXThread = false;
    private static final ReadOnlyObjectWrapper<LocalDate> today = new ReadOnlyObjectWrapper<>(null);

    private final ObjectMapper mapper = new ObjectMapper();

    private ObservableMap<LocalDate, Sleep> sleepRecords;
    private ObservableList<Workout> workoutRecords;
    private ObservableList<Exercise> exerciseList;
    private ObservableList<Task> taskList;
    private ObservableList<TwigList> twigLists;
    private ObservableList<Routine> routineList;
    private ObservableMap<LocalDate, Journal> journalMap;
    private final ObjectProperty<LocalDateTime> sleepStart = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> workoutStart = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalTime> dayStart = new SimpleObjectProperty<>(LocalTime.of(5,00));
    private final ObjectProperty<LocalTime> nightStart = new SimpleObjectProperty<>(LocalTime.of(18,00));
    private final BooleanProperty autoSync = new SimpleBooleanProperty(true);
    private final IntegerProperty syncInterval = new SimpleIntegerProperty(15);
    private final StringProperty visualTheme = new SimpleStringProperty();

    private DbxCredential dbxCredential;
    private final ObjectProperty<DbxClientV2> dbxClient = new SimpleObjectProperty<>();
    private DbxPKCEWebAuth currentDbxAuthAttempt;
    private CommitData lastSyncedCommit;


    public TaskTwig() {
        TaskTwig.instance = this;

        if (!DATA_DIR.exists())
            DATA_DIR.mkdirs();

        if (!DBX_DIR.exists())
            DBX_DIR.mkdirs();

        readConfigFile();
        readDataFiles();
        lastSyncedCommit = readLastSyncedCommitData();

        dayStart.subscribe((oldStart, newStart) -> {
            System.out.println(oldStart + " -> " + newStart);
            updateToday();
            if (newStart != oldStart)
                writeConfigFile();
        });
        nightStart.subscribe((oldStart, newStart) -> {
            System.out.println(oldStart + " -> " + newStart);
            if (newStart != oldStart)
                writeConfigFile();
        });
        autoSync.subscribe((oldVal, newVal) -> {
            System.out.println(oldVal + " -> " + newVal);
            if (newVal != oldVal)
                writeConfigFile();
        });
        syncInterval.subscribe((oldVal, newVal) -> {
            System.out.println(oldVal + " -> " + newVal);
            if (!Objects.equals(newVal, oldVal))
                writeConfigFile();
        });
        visualTheme.subscribe((oldVal, newVal) -> {
            System.out.println(oldVal + " -> " + newVal);
            if (!Objects.equals(newVal, oldVal))
                writeConfigFile();
        });
    }

    static TaskTwig instance() {
        return TaskTwig.instance;
    }

    public ObservableValue<LocalTime> getDayStart() {
        return dayStart;
    }

    public void setDayStart(LocalTime dayStart) {
        this.dayStart.set(dayStart);
    }

    public ObservableValue<LocalTime> getNightStart() {
        return nightStart;
    }

    public void setNightStart(LocalTime nightStart) {
        this.nightStart.set(nightStart);
    }


    public void updateToday() {
        LocalDate date;
        if (LocalTime.now().isBefore(dayStart.get()))
            date = LocalDate.now().minusDays(1);
        else
            date = LocalDate.now();

        if (!date.equals(today.get()))
            today.setValue(date);
    }

    public static ObjectExpression<LocalDate> todayValue() {
        instance.updateToday();
        return today.getReadOnlyProperty();
    }

    public static LocalDate today() {
        instance.updateToday();
        return today.getValue();
    }

    public static LocalDate effectiveDate(LocalDateTime date) {
        if (date.toLocalTime().isBefore(instance.dayStart.get()))
            return date.toLocalDate().minusDays(1);
        
        else
            return date.toLocalDate();
    }

    public static boolean isNight(LocalTime time) {
        if (time.isBefore(instance.dayStart.get()))
            return true;

        else
            return time.isAfter(instance.nightStart.get());
    }

    public static boolean isNight() {
        return TaskTwig.isNight(LocalTime.now());
    }

    static boolean notFxThread() {
        return TaskTwig.notFXThread;
    }

    static <U> U callWithFXSafety(Supplier<U> supplier) {
        if (!Platform.isFxApplicationThread() && TaskTwig.notFXThread)
            return CompletableFuture.supplyAsync(supplier, Platform::runLater).join();
        else
            return supplier.get();
    }

    public BooleanProperty autoSyncProperty() {
        return autoSync;
    }

    public IntegerProperty syncIntervalProperty() {
        return syncInterval;
    }

    public String getVisualTheme() {
        return visualTheme.get();
    }

    public void setVisualTheme(String visualTheme) {
        this.visualTheme.set(visualTheme);
    }

    public void startSleep(LocalDateTime sleepStart) {
        this.sleepStart.setValue(sleepStart);
    }

    public void finishSleep(LocalDateTime finishTime) {
        sleepRecords.put(finishTime.toLocalDate().minusDays(1), new Sleep(sleepStart.getValue(), finishTime));
        sleepStart.setValue(null);
    }

    public boolean isSleeping() {
        return this.sleepStart.getValue() != null;
    }

    public ReadOnlyObjectProperty<LocalDateTime> sleepStart() {
        return this.sleepStart;
    }

    public ObservableMap<LocalDate, Sleep> sleepRecords() {
        return sleepRecords;
    }

    public void startWorkout() {
        this.startWorkout(LocalDateTime.now());
    }

    public void startWorkout(LocalDateTime startTime) {
        workoutStart.setValue(startTime);
    }

    public void finishWorkout(SortedMap<Exercise, Integer> exercises, LocalDateTime finishTime) {
        workoutRecords.add(new Workout(workoutStart.getValue(), finishTime, exercises));
        workoutStart.setValue(null);
    }

    public boolean isWorkingOut() {
        return workoutStart.getValue() != null;
    }

    public ReadOnlyObjectProperty<LocalDateTime> workoutStart() {
        return workoutStart;
    }

    public ObservableList<Workout> workoutRecords() {
        return workoutRecords;
    }

    public List<Exercise> getExerciseList() {
        return new ArrayList<>(exerciseList);
    }

    public ObservableList<Exercise> exerciseList() {
        return exerciseList;
    }

    public ObservableList<Task> taskList() {
        return taskList;
    }

    public ObservableList<TwigList> twigLists() {
        return twigLists;
    }

    public ObservableList<Routine> routineList() {
        return routineList;
    }

    public ObservableMap<LocalDate, Journal> journalMap() {
        return journalMap;
    }

    public ReadOnlyObjectProperty<DbxClientV2> dbxClient() {
        return dbxClient;
    }

    public String getDbxAccountName() throws DbxException, NullPointerException {
        return dbxClient.get().users().getCurrentAccount().getName().getDisplayName();
    }

    public Journal todaysJournal() {
        journalMap.putIfAbsent(today(), new Journal());
        return journalMap.get(today());
    }

    private void readConfigFile() {
        try (JsonParser parser = mapper.createParser(CONFIG_FILE)) {
            parser.nextToken();
            assertEqual(parser.nextName(), "version");
            int version = parser.nextIntValue(0);

            switch (version) {
                case 5 -> {
                    assertEqual(parser.nextName(), "dayStart");
                    dayStart.set(LocalTime.parse(parser.nextStringValue()));

                    assertEqual(parser.nextName(), "nightStart");
                    nightStart.set(LocalTime.parse(parser.nextStringValue()));

                    assertEqual(parser.nextName(), "autoSync");
                    autoSync.set(parser.nextBooleanValue());

                    assertEqual(parser.nextName(), "syncInterval");
                    syncInterval.set(parser.nextIntValue(15));

                    assertEqual(parser.nextName(), "theme");
                    visualTheme.set(parser.nextStringValue());
                }
                case 4 -> {
                    assertEqual(parser.nextName(), "dayStart");
                    dayStart.set(LocalTime.parse(parser.nextStringValue()));

                    assertEqual(parser.nextName(), "nightStart");
                    nightStart.set(LocalTime.parse(parser.nextStringValue()));

                    assertEqual(parser.nextName(), "autoSync");
                    autoSync.set(parser.nextBooleanValue());

                    assertEqual(parser.nextName(), "syncInterval");
                    syncInterval.set(parser.nextIntValue(15));

                    assertEqual(parser.nextName(), "theme");
                    visualTheme.set(parser.nextStringValue());

                    assertEqual(parser.nextName(), "lastSyncedHash");
                    if (parser.nextValue() == JsonToken.VALUE_STRING)
                        lastSyncedCommit = new CommitData(Instant.MIN, Base64.getDecoder().decode(parser.getValueAsString()), Collections.emptyMap());
                    else
                        lastSyncedCommit = CommitData.NONE;
                }
                case 3 -> {
                    assertEqual(parser.nextName(), "dayStart");
                    dayStart.set(LocalTime.parse(parser.nextStringValue()));

                    assertEqual(parser.nextName(), "nightStart");
                    nightStart.set(LocalTime.parse(parser.nextStringValue()));

                    assertEqual(parser.nextName(), "autoSync");
                    autoSync.set(parser.nextBooleanValue());

                    assertEqual(parser.nextName(), "syncInterval");
                    syncInterval.set(parser.nextIntValue(15));

                    assertEqual(parser.nextName(), "lastSyncedHash");
                    if (parser.nextValue() == JsonToken.VALUE_STRING)
                        lastSyncedCommit = new CommitData(Instant.MIN, Base64.getDecoder().decode(parser.getValueAsString()), Collections.emptyMap());
                    else
                        lastSyncedCommit = CommitData.NONE;
                }
                case 2 -> {
                    assertEqual(parser.nextName(), "dayStart");
                    dayStart.set(LocalTime.parse(parser.nextStringValue()));

                    assertEqual(parser.nextName(), "nightStart");
                    nightStart.set(LocalTime.parse(parser.nextStringValue()));

                    assertEqual(parser.nextName(), "autoSync");
                    autoSync.set(parser.nextBooleanValue());

                    assertEqual(parser.nextName(), "lastSyncedHash");
                    if (parser.nextValue() == JsonToken.VALUE_STRING)
                        lastSyncedCommit = new CommitData(Instant.MIN, Base64.getDecoder().decode(parser.getValueAsString()), Collections.emptyMap());
                    else
                        lastSyncedCommit = CommitData.NONE;
                }
                case 1 -> {
                    assertEqual(parser.nextName(), "dayStart");
                    dayStart.set(LocalTime.parse(parser.nextStringValue()));

                    assertEqual(parser.nextName(), "nightStart");
                    nightStart.set(LocalTime.parse(parser.nextStringValue()));

                    autoSync.set(true);

                    assertEqual(parser.nextName(), "lastSyncedHash");
                    if (parser.nextValue() == JsonToken.VALUE_STRING)
                        lastSyncedCommit = new CommitData(Instant.MIN, Base64.getDecoder().decode(parser.getValueAsString()), Collections.emptyMap());
                    else
                        lastSyncedCommit = CommitData.NONE;
                }
                default -> throw new TwigJsonVersionException("Unsupported config file version: " + version);
            }
        }
        catch (JacksonIOException e) {
            System.out.println("Error reading config file: " + e.getMessage());
        }
    }

    private void writeConfigFile() {
        System.out.println("Writing config file");
        try (JsonGenerator generator = mapper.createGenerator(CONFIG_FILE, JsonEncoding.UTF8)) {
            generator.writeStartObject();

            generator.writeNumberProperty("version", CONFIG_VERSION);
            generator.writePOJOProperty("dayStart", dayStart.get());
            generator.writePOJOProperty("nightStart", nightStart.get());
            generator.writeBooleanProperty("autoSync", autoSync.get());
            generator.writeNumberProperty("syncInterval", syncInterval.get());
            generator.writeStringProperty("theme", visualTheme.get());

            generator.writeEndObject();
        }
    }

    private void readDataFiles() {
        // Parse sleep records
        SortedMap<LocalDate, Sleep> sleepMap = new TreeMap<>();
        try (JsonParser parser = mapper.createParser(DataFile.SLEEP.file)) {
            parser.nextToken();
            assertEqual(parser.nextName(), "version");
            int version = parser.nextIntValue(0);

            assertEqual(parser.nextName(), "sleepProgressStart");
            if (parser.nextToken() == JsonToken.VALUE_STRING)
                this.sleepStart.setValue(LocalDateTime.parse(parser.getString()));
            else {
                this.sleepStart.setValue(null);
                assertEqual(parser.currentToken(), JsonToken.VALUE_NULL);
            }

            assertEqual(parser.nextName(), "sleepRecords");
            parser.nextToken();
            parseJsonMap(sleepMap, parser, LocalDate::parse, node -> new Sleep(node, version));
        }
        catch (TwigJsonAssertException | JacksonIOException e) {
            this.sleepStart.setValue(null);
            sleepMap.clear();
        }
        finally {
            this.sleepRecords = FXCollections.observableMap(sleepMap);
        }

        // Parse workout records
        this.exerciseList = FXCollections.observableArrayList();
        this.workoutRecords = FXCollections.observableArrayList();
        try (JsonParser parser = mapper.createParser(DataFile.WORKOUT.file)) {
            parser.nextToken();
            assertEqual(parser.nextName(), "version");
            int version = parser.nextIntValue(0);

            assertEqual(parser.nextName(), "workoutProgressStart");
            if (parser.nextToken() == JsonToken.VALUE_STRING)
                this.workoutStart.setValue(LocalDateTime.parse(parser.getString()));
            else {
                this.workoutStart.setValue(null);
                assertEqual(parser.currentToken(), JsonToken.VALUE_NULL);
            }

            assertEqual(parser.nextName(), "exercises");
            parser.nextToken();
            parseJsonList(this.exerciseList, parser, node -> new Exercise(node, version));

            assertEqual(parser.nextName(), "workoutRecords");
            parser.nextToken();
            parseJsonList(this.workoutRecords, parser, node -> new Workout(node, version));
        } catch (TwigJsonAssertException | JacksonIOException e) {
            this.exerciseList.clear();
            this.workoutStart.setValue(null);
            this.workoutRecords.clear();
        }

        // Parse tasks
        this.taskList = FXCollections.observableArrayList(task -> new Observable[] {task.intervalProperty(), task.inProgressObservable()});
        try (JsonParser parser = mapper.createParser(DataFile.TASK.file)) {
            parser.nextToken();
            assertEqual(parser.nextName(), "version");
            int version =  parser.nextIntValue(0);

            assertEqual(parser.nextName(), "tasks");
            parser.nextToken();
            parseJsonList(this.taskList, parser, node -> new Task(node, version));
        } catch (TwigJsonAssertException | JacksonIOException e) {
            this.taskList.clear();
        }

        // Parse lists
        this.twigLists = FXCollections.observableArrayList();
        try (JsonParser parser = mapper.createParser(DataFile.LIST.file)) {
            parser.nextToken();
            assertEqual(parser.nextName(), "version");
            int version =  parser.nextIntValue(0);

            assertEqual(parser.nextName(), "lists");
            parser.nextToken();
            parseJsonList(this.twigLists, parser, node -> new TwigList(node, version));
        } catch (TwigJsonAssertException | JacksonIOException e) {
            this.twigLists.clear();
        }

        // Parse routines
        this.routineList = FXCollections.observableArrayList(routine -> new Observable[] {routine.interval()});
        try (JsonParser parser = mapper.createParser(DataFile.ROUTINE.file)) {
            parser.nextToken();
            assertEqual(parser.nextName(), "version");
            int version =  parser.nextIntValue(0);

            assertEqual(parser.nextName(), "routines");
            parser.nextToken();
            parseJsonList(this.routineList, parser, node -> new Routine(node, version));
        } catch (TwigJsonAssertException | JacksonIOException e) {
            this.routineList.clear();
        }

        // Parse journals
        SortedMap<LocalDate, Journal> journals = new TreeMap<>();
        try (JsonParser parser = mapper.createParser(DataFile.JOURNAL.file)) {
            parser.nextToken();
            assertEqual(parser.nextName(), "version");
            int version =  parser.nextIntValue(0);

            assertEqual(parser.nextName(), "journals");
            parser.nextToken();
            parseJsonMap(journals, parser, LocalDate::parse, node -> new Journal(node, version));
        } catch (TwigJsonAssertException | JacksonIOException e) {
            journals.clear();
        }
        this.journalMap = FXCollections.observableMap(journals);
    }

    public void saveToFileFX() {
        TaskTwig.notFXThread = true;
        saveToDataFiles();
        TaskTwig.notFXThread = false;
    }

    public void saveToDataFiles() {
        CommitData liveCommit = genLiveCommitData();
        List<DataFile> saveFiles = findOutOfDateFiles(liveCommit);
        System.out.println("Saving files: " + saveFiles);

        for (DataFile file : saveFiles) {
            switch (file) {
                case SLEEP -> {
                    try (JsonGenerator generator = mapper.createGenerator(DataFile.SLEEP.file, JsonEncoding.UTF8)) {
                        generator.writeStartObject();

                        generator.writeNumberProperty("version", Sleep.VERSION);
                        generator.writePOJOProperty("sleepProgressStart", this.sleepStart.getValue());
                        generator.writePOJOProperty("sleepRecords", this.sleepRecords);
                        generator.writeEndObject();
                    }
                }
                case WORKOUT -> {
                    try (JsonGenerator generator = mapper.createGenerator(DataFile.WORKOUT.file, JsonEncoding.UTF8)) {
                        generator.writeStartObject();

                        generator.writeNumberProperty("version", Workout.VERSION);
                        generator.writePOJOProperty("workoutProgressStart", this.workoutStart.getValue());
                        generator.writePOJOProperty("exercises", this.exerciseList);
                        generator.writePOJOProperty("workoutRecords", this.workoutRecords);

                        generator.writeEndObject();
                    }
                }
                case TASK -> {
                    try (JsonGenerator generator = mapper.createGenerator(DataFile.TASK.file, JsonEncoding.UTF8)) {
                        generator.writeStartObject();

                        generator.writeNumberProperty("version", Task.VERSION);

                        generator.writeArrayPropertyStart("tasks");
                        for (Task task : taskList) {
                            generator.writePOJO(task);
                        }
                        generator.writeEndArray();

                        generator.writeEndObject();
                    }
                }
                case LIST -> {
                    try (JsonGenerator generator = mapper.createGenerator(DataFile.LIST.file, JsonEncoding.UTF8)) {
                        generator.writeStartObject();

                        generator.writeNumberProperty("version", TwigList.VERSION);

                        generator.writeArrayPropertyStart("lists");
                        for (TwigList list : twigLists) {
                            generator.writePOJO(list);
                        }
                        generator.writeEndArray();

                        generator.writeEndObject();
                    }
                }
                case ROUTINE -> {
                    try (JsonGenerator generator = mapper.createGenerator(DataFile.ROUTINE.file, JsonEncoding.UTF8)) {
                        generator.writeStartObject();

                        generator.writeNumberProperty("version", Routine.VERSION);

                        generator.writeArrayPropertyStart("routines");
                        for (Routine routine : routineList) {
                            generator.writePOJO(routine);
                        }
                        generator.writeEndArray();

                        generator.writeEndObject();
                    }
                }
                case JOURNAL -> {
                    try (JsonGenerator generator = mapper.createGenerator(DataFile.JOURNAL.file, JsonEncoding.UTF8)) {
                        generator.writeStartObject();

                        generator.writeNumberProperty("version", Journal.VERSION);

                        generator.writeObjectPropertyStart("journals");
                        for (Map.Entry<LocalDate, Journal> journalEntry : journalMap.entrySet()) {
                            if (!journalEntry.getValue().isEmpty()) {
                                generator.writePOJOProperty(journalEntry.getKey().toString(), journalEntry.getValue());
                            }
                        }
                        generator.writeEndObject();

                        generator.writeEndObject();
                    }
                }
            }
        }

        if (!saveFiles.isEmpty()) {
            writeCommitData(liveCommit, COMMIT_FILE);
        }
    }

    private void assertEqual(Object actual, Object expected) throws TwigJsonAssertException {
        if (!expected.equals(actual))
            throw new TwigJsonAssertException("JSON parse encountered unexpected value. Expected: " + expected + ", actual: " + actual);
    }

    private <T> void parseJsonList(List<T> list, JsonParser parser, Function<JsonNode, T> valueConstructor) {
        parser.nextToken();
        while (parser.currentToken() != JsonToken.END_ARRAY) {
            JsonNode node = parser.readValueAsTree();
            T value = valueConstructor.apply(node);
            list.add(value);
            parser.nextToken();
        }
    }

    private <K, V> void parseJsonMap(Map<K, V> map, JsonParser parser,
                                     Function<String, K> keyParser, Function<JsonNode, V> valueConstructor) {
        parser.nextToken();
        while (parser.currentToken() != JsonToken.END_OBJECT) {
            K key = keyParser.apply(parser.currentName());
            parser.nextToken();
            JsonNode node = parser.readValueAsTree();
            V value = valueConstructor.apply(node);
            map.put(key, value);
            parser.nextToken();
        }
    }


// ----------------------------------------------- Hashing and Commits -------------------------------------------------

    public record CommitData(Instant timestamp, byte[] commitHash, Map<DataFile, byte[]> fileHashes) {

        public static final CommitData NONE = new CommitData(null, null, Collections.emptyMap());
    }

    private byte[] hashLiveDataFile(DataFile file) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            switch (file) {
                case SLEEP -> {
                    digest.update((byte) Sleep.VERSION);

                    if (callWithFXSafety(this::isSleeping))
                        digest.update(callWithFXSafety(sleepStart::get).toString().getBytes(StandardCharsets.UTF_8));

                    SortedMap<LocalDate, Sleep> sleepMap = callWithFXSafety(() -> new TreeMap<>(sleepRecords));
                    for (Map.Entry<LocalDate, Sleep> sleepEntry : sleepMap.entrySet()) {
                        digest.update(sleepEntry.getKey().toString().getBytes(StandardCharsets.UTF_8));
                        sleepEntry.getValue().hashContents(digest);
                    }
                }
                case WORKOUT -> {
                    digest.update((byte) Workout.VERSION);

                    if (callWithFXSafety(this::isWorkingOut))
                        digest.update(callWithFXSafety(workoutStart::get).toString().getBytes(StandardCharsets.UTF_8));

                    for (Exercise exercise : callWithFXSafety(() -> new ArrayList<>(exerciseList))) {
                        exercise.hashContents(digest);
                    }

                    for (Workout workout : callWithFXSafety(() -> new ArrayList<>(workoutRecords))) {
                        workout.hashContents(digest);
                    }
                }
                case TASK ->  {
                    digest.update((byte) Task.VERSION);

                    for (Task task : callWithFXSafety(() -> new ArrayList<>(taskList))) {
                        task.hashContents(digest);
                    }
                }
                case ROUTINE -> {
                    digest.update((byte) Routine.VERSION);

                    for (Routine routine : callWithFXSafety(() -> new ArrayList<>(routineList))) {
                        routine.hashContents(digest);
                    }
                }
                case LIST -> {
                    digest.update((byte) TwigList.VERSION);

                    for (TwigList twigList : callWithFXSafety(() -> new ArrayList<>(twigLists))) {
                        twigList.hashContents(digest);
                    }
                }
                case JOURNAL -> {
                    digest.update((byte) Journal.VERSION);

                    SortedMap<LocalDate, Journal> journals = callWithFXSafety(() -> new TreeMap<>(journalMap));
                    for (Map.Entry<LocalDate, Journal> journalEntry : journals.entrySet()) {
                        digest.update(journalEntry.getKey().toString().getBytes(StandardCharsets.UTF_8));
                        journalEntry.getValue().hashContents(digest);
                    }
                }
            }

            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private CommitData genLiveCommitData() {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");

            Map<DataFile, byte[]> fileHashes = new TreeMap<>();
            for (DataFile dataFile : DataFile.values()) {
                byte[] hash = hashLiveDataFile(dataFile);
                fileHashes.put(dataFile, hash);
                digest.update(hash);
            }

            return new CommitData(Instant.now(), digest.digest(), fileHashes);
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private CommitData readCommitData(JsonParser parser) {
        var base64Decoder = Base64.getDecoder();

        parser.nextToken();
        assertEqual(parser.nextName(), "timestamp");
        Instant timestamp = Instant.parse(parser.nextStringValue());

        assertEqual(parser.nextName(), "commitHash");
        byte[] commitHash = base64Decoder.decode(parser.nextStringValue());

        Map<DataFile, byte[]> fileHashes = new TreeMap<>();
        assertEqual(parser.nextName(), "fileHashes");
        parser.nextToken();
        parseJsonMap(fileHashes, parser, DataFile::valueOf, node -> base64Decoder.decode(node.asString()));

        return new CommitData(timestamp, commitHash, fileHashes);
    }

    private void writeCommitData(CommitData commitData, File file) {
        try (JsonGenerator generator = mapper.createGenerator(file, JsonEncoding.UTF8)) {
            var base64Encoder = Base64.getEncoder();

            generator.writeStartObject();

            generator.writePOJOProperty("timestamp", commitData.timestamp());
            generator.writeStringProperty("commitHash", base64Encoder.encodeToString(commitData.commitHash()));

            generator.writeObjectPropertyStart("fileHashes");

            for (Map.Entry<DataFile, byte[]> hash : commitData.fileHashes().entrySet()) {
                generator.writeStringProperty(hash.getKey().toString(), base64Encoder.encodeToString(hash.getValue()));
            }

            generator.writeEndObject();
            generator.writeEndObject();
        }
    }

    private List<DataFile> findOutOfDateFiles(CommitData liveCommit) {

        try (JsonParser parser = mapper.createParser(COMMIT_FILE)) {
            List<DataFile> files = new ArrayList<>();
            CommitData diskCommit = readCommitData(parser);

            for (Map.Entry<DataFile, byte[]> liveHash : liveCommit.fileHashes().entrySet()) {

                if (!diskCommit.fileHashes.containsKey(liveHash.getKey())
                        || !Arrays.equals(liveHash.getValue(), diskCommit.fileHashes().get(liveHash.getKey()))) {
                    files.add(liveHash.getKey());
                }
            }

            return files;
        }
        catch (TwigJsonAssertException | JacksonException e) {
            System.out.println("Failed reading parse data: " + e.getMessage());
            System.out.println("Skipped reading parse data");

            return List.of(DataFile.values());
        }
    }

    private CommitData readLocalCommitData() {
        try (JsonParser parser = mapper.createParser(COMMIT_FILE)) {
            return readCommitData(parser);
        }
        catch (JacksonIOException | TwigJsonAssertException e) {
            System.out.println("Couldn't read local commit file: " + e.getMessage());
            return null;
        }
    }

    private CommitData readDbxCommitData() {
        try (var remoteCommitFile = dbxClient.get().files().downloadBuilder("/" + COMMIT_FILE.getName()).start();
             JsonParser parser = mapper.createParser(remoteCommitFile.getInputStream()))
        {
            return readCommitData(parser);
        }
        catch (DbxException | TwigJsonAssertException e) {
            System.out.println("Couldn't read remote commit file: " + e.getMessage());
            return null;
        }
    }

    private CommitData readLastSyncedCommitData() {
        try (JsonParser parser = mapper.createParser(LAST_SYNCED_COMMIT_FILE)) {
            return readCommitData(parser);
        }
        catch (JacksonIOException | TwigJsonAssertException e) {
            System.out.println("Couldn't read last synced commit file: " + e.getMessage());
            return CommitData.NONE;
        }
    }


// -------------------------------------------- Comparing and Syncing Data ---------------------------------------------

    public enum FileAction {
        DOWNLOAD,
        UPLOAD,
        MERGE,
        CONFLICT,
        NONE
    }
    public record CommitDiff(FileAction action, Map<DataFile, FileAction> files,
                             CommitData localCommitData, CommitData remoteCommitData) {}

    @FunctionalInterface
    public interface SyncConflictCallback {
        FileAction getUserChoice(FileAction overallAction, Map<DataFile, FileAction> fileActions);
    }
    public CommitDiff compareCommitToDbx(SyncConflictCallback conflictCallback) {
        Map<DataFile, FileAction> fileSyncActions = new HashMap<>();
        CommitData localCommit = readLocalCommitData();
        CommitData remoteCommit = readDbxCommitData();
        FileAction fileAction = FileAction.NONE;

        if (localCommit == null || remoteCommit == null || Arrays.equals(localCommit.commitHash(), remoteCommit.commitHash())) {
            if (localCommit == null && remoteCommit != null) {
                Arrays.stream(DataFile.values()).forEach(file -> fileSyncActions.put(file, FileAction.DOWNLOAD));
                fileAction = FileAction.DOWNLOAD;
            }
            else if (localCommit != null && remoteCommit == null) {
                Arrays.stream(DataFile.values()).forEach(file -> fileSyncActions.put(file, FileAction.UPLOAD));
                fileAction = FileAction.UPLOAD;
            }
        }
        else {

            for (DataFile file : DataFile.values()) {
                byte[] localHash = localCommit.fileHashes.get(file);
                byte[] remoteHash = remoteCommit.fileHashes.get(file);
                byte[] lastSyncedHash = lastSyncedCommit.fileHashes.get(file);

                if (!Arrays.equals(localHash, remoteHash)) {
                    if (Arrays.equals(remoteHash, lastSyncedHash)) {
                        fileSyncActions.put(file, FileAction.UPLOAD);

                        if (fileAction == FileAction.NONE)
                            fileAction = FileAction.UPLOAD;
                        else if (fileAction == FileAction.DOWNLOAD)
                            fileAction = FileAction.MERGE;
                    }
                    else if (Arrays.equals(localHash, lastSyncedHash)) {
                        fileSyncActions.put(file, FileAction.DOWNLOAD);

                        if (fileAction == FileAction.NONE)
                            fileAction = FileAction.DOWNLOAD;
                        else if (fileAction == FileAction.UPLOAD)
                            fileAction = FileAction.MERGE;
                    }
                    else {
                        fileSyncActions.put(file, FileAction.CONFLICT);
                        fileAction = FileAction.CONFLICT;
                    }
                }
            }

            if (fileAction == FileAction.MERGE || fileAction == FileAction.CONFLICT) {
                if (conflictCallback != null) {
                    fileAction = conflictCallback.getUserChoice(fileAction, fileSyncActions);
                    System.out.println("User chose: " + fileAction);
                }
            }
        }

        return new CommitDiff(fileAction, fileSyncActions, localCommit, remoteCommit);
    }

    public void dbxSync(CommitDiff commitDiff) {
        System.out.println("sync: " + commitDiff.action() + " " + commitDiff.files());
        if (commitDiff.action() == FileAction.DOWNLOAD || commitDiff.action() == FileAction.UPLOAD || commitDiff.action() == FileAction.MERGE) {
            for (Map.Entry<DataFile, FileAction> file : commitDiff.files().entrySet()) {
                try {
                    if (commitDiff.action() == FileAction.DOWNLOAD) {
                        dbxDownloadFile(file.getKey());
                    }
                    else if (commitDiff.action() == FileAction.UPLOAD || file.getValue() == FileAction.UPLOAD) {
                        dbxUploadFile(file.getKey());
                    }
                    else if (file.getValue() == FileAction.DOWNLOAD) {
                        dbxDownloadFile(file.getKey());
                    }
                } catch (IOException | DbxException e) {
                    System.err.println("Failed to " + file.getValue() + " file: " + file.getKey());
                    System.err.println(e.getMessage());
                }
            }
        }

        try {
            switch (commitDiff.action()) {
                case DOWNLOAD -> {
                    lastSyncedCommit = commitDiff.remoteCommitData();
                    writeCommitData(lastSyncedCommit, COMMIT_FILE);
                    writeCommitData(lastSyncedCommit, LAST_SYNCED_COMMIT_FILE);

                    readDataFiles();
                }
                case UPLOAD -> {
                    lastSyncedCommit = commitDiff.localCommitData();
                    writeCommitData(lastSyncedCommit, LAST_SYNCED_COMMIT_FILE);
                    dbxUploadCommitFile(LAST_SYNCED_COMMIT_FILE);
                }
                case MERGE -> {
                    readDataFiles();

                    lastSyncedCommit = genLiveCommitData();
                    writeCommitData(lastSyncedCommit, COMMIT_FILE);
                    writeCommitData(lastSyncedCommit, LAST_SYNCED_COMMIT_FILE);
                    dbxUploadCommitFile(LAST_SYNCED_COMMIT_FILE);
                }
            }
        } catch (IOException | DbxException e) {
            throw new RuntimeException(e);
        }
    }


// ----------------------------------------------------- Dropbox -------------------------------------------------------

    public String genDbxAuthUrl() {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("TaskTwig/alpha").build();

        DbxAppInfo appInfo = new DbxAppInfo(DBX_API_KEY);
        currentDbxAuthAttempt = new DbxPKCEWebAuth(config, appInfo);
        DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder()
                .withNoRedirect()
                .withTokenAccessType(TokenAccessType.OFFLINE)
                .build();

        return currentDbxAuthAttempt.authorize(webAuthRequest);
    }

    public void authDbxFromCode(String code) throws DbxException{
        if (currentDbxAuthAttempt == null) {
            return;
        }

        DbxAuthFinish authFinish = currentDbxAuthAttempt.finishFromCode(code);
        System.out.println("authFinish scopes: " + authFinish.getScope());

        dbxCredential = new DbxCredential(authFinish.getAccessToken(), authFinish.getExpiresAt(), authFinish.getRefreshToken(), DBX_API_KEY);
        initDbxClient(dbxCredential);
        currentDbxAuthAttempt = null;

        try {
            DBX_CRED_FILE.createNewFile();
            DbxCredential.Writer.writeToFile(dbxCredential, DBX_CRED_FILE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean authDbxFromFile() {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("TaskTwig/alpha").build();

        if (DBX_CRED_FILE.exists()) {
            try {
                dbxCredential = DbxCredential.Reader.readFromFile(DBX_CRED_FILE);
                if (dbxCredential.aboutToExpire()) {
                    dbxCredential.refresh(config);
                }

                initDbxClient(dbxCredential);
                return true;
            }
            catch (JsonReader.FileLoadException e) {
                System.out.println("Error reading credential file: " + e.getMessage());
                System.out.println("Reauthorizing user");
            }
            catch (DbxException e) {
                System.out.println("Error refreshing credential: " + e.getMessage());
                System.out.println("Reauthorizing user");
            }
        }

        return false;
    }

    public void dbxLogout() {
        DBX_CRED_FILE.delete();
        dbxCredential = null;
        dbxClient.set(null);
    }

    private void initDbxClient(DbxCredential credential) {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("TaskTwig/alpha").build();
        Platform.runLater(() -> dbxClient.set(new DbxClientV2(config, credential)));
    }

    public void dbxDownloadFile(DataFile dataFile) throws IOException, DbxException {
        try (OutputStream fileStream = new FileOutputStream(dataFile.file)) {
            dbxClient.get().files().downloadBuilder("/" + dataFile.file.getName()).download(fileStream);
        }
        catch (IOException | DbxException e) {
            System.err.println("Error downloading file " + dataFile.file.getName() + ": " + e.getMessage());
            throw e;
        }
    }

    public void dbxUploadFile(DataFile dataFile) throws IOException, DbxException {
        try (InputStream fileStream = new FileInputStream(dataFile.file)) {
            dbxClient.get().files().uploadBuilder("/" + dataFile.file.getName()).withMode(WriteMode.OVERWRITE).uploadAndFinish(fileStream);
        }
        catch (IOException | DbxException e) {
            System.err.println("Error uploading file " + dataFile.file.getName() + ": " + e.getMessage());
            throw e;
        }
    }

    public void dbxUploadCommitFile(File commitFile) throws IOException, DbxException {
        try (InputStream fileStream = new FileInputStream(commitFile)) {
            dbxClient.get().files().uploadBuilder("/commit.json").withMode(WriteMode.OVERWRITE).uploadAndFinish(fileStream);
        }
        catch (IOException | DbxException e) {
            System.err.println("Error uploading file " + commitFile.getName() + " as remote commit file: " + e.getMessage());
            throw e;
        }
    }
}
